package com.couplestory.controller;

import com.couplestory.dto.JwtResponse;
import com.couplestory.dto.LoginRequest;
import com.couplestory.dto.SignupRequest;
import com.couplestory.entity.Role;
import com.couplestory.entity.User;
import com.couplestory.repository.RoleRepository;
import com.couplestory.repository.UserRepository;
import com.couplestory.security.JwtUtils;
import com.couplestory.security.LoginRateLimiter;
import com.couplestory.security.UserDetailsImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.couplestory.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder encoder;
    private final JwtUtils jwtUtils;
    private final LoginRateLimiter loginRateLimiter;
    private final NotificationService notificationService;
    private final com.couplestory.service.DeviceService deviceService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.cookie-secure:true}")
    private boolean cookieSecure;

    @Value("${jwt.expiration:900000}")
    private long jwtExpirationMs;

    @Value("${app.google-client-id:}")
    private String googleClientId;

    public AuthController(AuthenticationManager authenticationManager, UserRepository userRepository,
                          RoleRepository roleRepository,
                          PasswordEncoder encoder, JwtUtils jwtUtils, LoginRateLimiter loginRateLimiter,
                          NotificationService notificationService,
                          com.couplestory.service.DeviceService deviceService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.encoder = encoder;
        this.jwtUtils = jwtUtils;
        this.loginRateLimiter = loginRateLimiter;
        this.notificationService = notificationService;
        this.deviceService = deviceService;
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuthController.class);

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest, HttpServletRequest httpRequest) {
        String rateLimitKey = httpRequest.getRemoteAddr();
        loginRateLimiter.checkAllowed(rateLimitKey);

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));
        } catch (BadCredentialsException e) {
            loginRateLimiter.recordFailure(rateLimitKey);
            log.warn("Login failed for email: {}, IP: {}", loginRequest.getEmail(), rateLimitKey);
            
            userRepository.findByEmail(loginRequest.getEmail()).ifPresent(user -> {
                user.setFailedLoginCount(user.getFailedLoginCount() + 1);
                userRepository.save(user);
            });
            throw e;
        }
        loginRateLimiter.recordSuccess(rateLimitKey);
        log.info("Login successful for email: {}, IP: {}", loginRequest.getEmail(), rateLimitKey);

        String userAgent = httpRequest.getHeader("User-Agent");

        userRepository.findByEmail(loginRequest.getEmail()).ifPresent(user -> {
            user.setLastLoginAt(java.time.OffsetDateTime.now());
            user.setLastLoginIp(rateLimitKey);
            user.setLastLoginDevice(userAgent != null ? (userAgent.length() > 255 ? userAgent.substring(0, 255) : userAgent) : "Unknown");
            user.setSuccessfulLoginCount(user.getSuccessfulLoginCount() + 1);
            user.setFailedLoginCount(0); // Reset failed attempts on success
            user.setTokenVersion(user.getTokenVersion() + 1); // Đẩy session cũ ra
            userRepository.save(user);
            
            // Create a notification for successful login
            deviceService.recordLogin(user.getId(), userAgent, rateLimitKey, "PASSWORD");
            notificationService.createNotification(
                    user.getId(),
                    "Đăng nhập thành công",
                    "Tài khoản của bạn vừa được đăng nhập thành công.",
                    "SYSTEM",
                    null
            );

            // Update auth token so the current session uses the new token_version
            ((UserDetailsImpl) authentication.getPrincipal()).setTokenVersion(user.getTokenVersion());
        });

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.startsWith("ROLE_") ? a.substring(5) : a)
                .collect(Collectors.toList());

        ResponseCookie cookie = ResponseCookie.from("access_token", jwt)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(jwtExpirationMs / 1000)
                .sameSite("Strict")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new JwtResponse(userDetails.getId().toString(), userDetails.getEmail(), userDetails.getDisplayName(), userDetails.getPlanType(), roles));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser() {
        ResponseCookie cookie = ResponseCookie.from("access_token", "")
                .httpOnly(true).secure(cookieSecure).path("/").maxAge(0).build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body("Logged out successfully");
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            return ResponseEntity.status(401).body("Not authenticated");
        }
        UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.startsWith("ROLE_") ? a.substring(5) : a)
                .collect(Collectors.toList());
        return ResponseEntity.ok(new JwtResponse(userDetails.getId().toString(), userDetails.getEmail(), userDetails.getDisplayName(), userDetails.getPlanType(), roles));
    }

    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String credential = body.get("credential");
        if (credential == null || credential.isBlank()) {
            return ResponseEntity.badRequest().body("Missing credential");
        }
        if (googleClientId == null || googleClientId.isBlank()) {
            return ResponseEntity.status(500).body("Google login is not configured");
        }

        try {
            JsonNode payload = verifyGoogleToken(credential);
            if (payload == null) {
                return ResponseEntity.status(401).body("Invalid Google token");
            }

            String email = payload.get("email").asText();
            String name = payload.has("name") ? payload.get("name").asText() : email.split("@")[0];
            String aud = payload.get("aud").asText();

            if (!googleClientId.equals(aud)) {
                return ResponseEntity.status(401).body("Token audience mismatch");
            }

            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) {
                Role userRole = roleRepository.findByName("USER")
                        .orElseThrow(() -> new RuntimeException("Default role USER not found"));
                user = User.builder()
                        .email(email)
                        .passwordHash(encoder.encode(java.util.UUID.randomUUID().toString()))
                        .displayName(name)
                        .roles(Set.of(userRole))
                        .build();
                userRepository.save(user);
            }

            String jwt = jwtUtils.generateTokenFromEmail(user.getEmail());
            List<String> roles = user.getRoles().stream()
                    .map(r -> r.getName())
                    .collect(Collectors.toList());

            // Create a notification for successful login
            String userAgent = request.getHeader("User-Agent");
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty()) ip = request.getRemoteAddr();
            deviceService.recordLogin(user.getId(), userAgent, ip, "GOOGLE");
            notificationService.createNotification(
                    user.getId(),
                    "Đăng nhập thành công",
                    "Tài khoản của bạn vừa được đăng nhập thành công bằng Google.",
                    "SYSTEM",
                    null
            );

            ResponseCookie cookie = ResponseCookie.from("access_token", jwt)
                    .httpOnly(true)
                    .secure(cookieSecure)
                    .path("/")
                    .maxAge(jwtExpirationMs / 1000)
                    .sameSite("Strict")
                    .build();

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body(new JwtResponse(user.getId().toString(), user.getEmail(), user.getDisplayName(), user.getPlanType(), roles));
        } catch (Exception e) {
            log.error("Google login error", e);
            return ResponseEntity.status(500).body("Google login failed: " + e.getMessage());
        }
    }

    private JsonNode verifyGoogleToken(String idToken) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            return null;
        }
        JsonNode node = objectMapper.readTree(response.body());
        if (node.has("error_description")) {
            return null;
        }
        return node;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity.badRequest().body("Error: Email is already in use!");
        }
        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new RuntimeException("Default role USER not found"));
        String name = signUpRequest.getName();
        if (name == null || name.isBlank()) {
            name = signUpRequest.getEmail().split("@")[0];
        }
        User user = User.builder()
                .email(signUpRequest.getEmail())
                .passwordHash(encoder.encode(signUpRequest.getPassword()))
                .displayName(name)
                .roles(Set.of(userRole))
                .build();
        userRepository.save(user);
        return ResponseEntity.ok("User registered successfully!");
    }
}
