import os

BASE_DIR = r"D:\My Project\Couplestory\API_V2\src\main\java\com\couplestory"

files = {
    "entity/WebsiteCollaborator.java": """package com.couplestory.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "website_collaborators")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WebsiteCollaborator {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "website_id", nullable = false)
    private String websiteId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(nullable = false)
    private String role; // PARTNER

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}""",
    "entity/Photo.java": """package com.couplestory.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "photos")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Photo {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "website_id", nullable = false)
    private String websiteId;

    @Column(nullable = false)
    private String url;

    private String caption;
    private String category; // gallery, album, background
    
    @Column(name = "sort_order")
    private Integer sortOrder;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}""",
    "entity/WebsiteEvent.java": """package com.couplestory.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "website_events")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WebsiteEvent {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "website_id", nullable = false)
    private String websiteId;

    @Column(nullable = false)
    private String title;

    private String description;
    
    @Column(name = "event_date")
    private LocalDate eventDate;
    
    @Column(name = "icon_url")
    private String iconUrl;
    
    @Column(name = "photo_url")
    private String photoUrl;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}""",
    "entity/WebsiteMessage.java": """package com.couplestory.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "website_messages")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WebsiteMessage {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "website_id", nullable = false)
    private String websiteId;
    
    @Column(nullable = false)
    private String author;
    
    @Column(nullable = false, length = 2000)
    private String content;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}""",
    "entity/FavoriteMoment.java": """package com.couplestory.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "favorite_moments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FavoriteMoment {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "website_id", nullable = false)
    private String websiteId;
    
    @Column(nullable = false)
    private String title;
    
    @Column(length = 1000)
    private String description;
    
    @Column(name = "photo_url")
    private String photoUrl;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}""",
    "entity/Notification.java": """package com.couplestory.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;
    
    @Column(nullable = false)
    private String title;
    
    private String message;
    
    @Column(name = "is_read")
    private boolean read = false;
    
    @Column(name = "notification_type")
    private String type;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}"""
}

# Add repositories
entities = ["User", "Website", "WebsiteCollaborator", "Photo", "WebsiteEvent", "WebsiteMessage", "FavoriteMoment", "Notification"]

for entity in entities:
    files[f"repository/{entity}Repository.java"] = f"""package com.couplestory.repository;
import com.couplestory.entity.{entity};
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface {entity}Repository extends JpaRepository<{entity}, String> {{
"""
    if entity == "User":
        files[f"repository/{entity}Repository.java"] += "    java.util.Optional<User> findByEmail(String email);\n"
        files[f"repository/{entity}Repository.java"] += "    boolean existsByEmail(String email);\n"
    elif entity in ["WebsiteCollaborator", "Photo", "WebsiteEvent", "WebsiteMessage", "FavoriteMoment"]:
        files[f"repository/{entity}Repository.java"] += f"    java.util.List<{entity}> findByWebsiteId(String websiteId);\n"
    elif entity == "Website":
        files[f"repository/{entity}Repository.java"] += "    java.util.List<Website> findByUserId(String userId);\n"
        files[f"repository/{entity}Repository.java"] += "    java.util.Optional<Website> findBySubdomain(String subdomain);\n"
    elif entity == "Notification":
        files[f"repository/{entity}Repository.java"] += "    java.util.List<Notification> findByUserId(String userId);\n"
        
    files[f"repository/{entity}Repository.java"] += "}\n"

# Security
files["security/JwtUtils.java"] = """package com.couplestory.security;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtils {
    @Value("${app.jwtSecret:mySecretKey1234567890123456789012345678901234567890}")
    private String jwtSecret;

    @Value("${app.jwtExpirationMs:86400000}")
    private int jwtExpirationMs;

    public String generateJwtToken(Authentication authentication) {
        UserDetailsImpl userPrincipal = (UserDetailsImpl) authentication.getPrincipal();
        return Jwts.builder()
                .setSubject((userPrincipal.getEmail()))
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }
    
    private Key key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    public String getUserNameFromJwtToken(String token) {
        return Jwts.parserBuilder().setSigningKey(key()).build()
                   .parseClaimsJws(token).getBody().getSubject();
    }

    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parserBuilder().setSigningKey(key()).build().parseClaimsJws(authToken);
            return true;
        } catch (Exception e) {
            // log error
        }
        return false;
    }
}
"""

files["security/UserDetailsImpl.java"] = """package com.couplestory.security;
import com.couplestory.entity.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.Collections;

@Getter
@AllArgsConstructor
public class UserDetailsImpl implements UserDetails {
    private String id;
    private String email;
    @JsonIgnore
    private String password;
    private Collection<? extends GrantedAuthority> authorities;

    public static UserDetailsImpl build(User user) {
        return new UserDetailsImpl(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole())));
    }

    @Override public String getUsername() { return email; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
"""

files["security/UserDetailsServiceImpl.java"] = """package com.couplestory.security;
import com.couplestory.entity.User;
import com.couplestory.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with email: " + email));
        return UserDetailsImpl.build(user);
    }
}
"""

files["security/AuthTokenFilter.java"] = """package com.couplestory.security;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

public class AuthTokenFilter extends OncePerRequestFilter {
    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = parseJwt(request);
            if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
                String username = jwtUtils.getUserNameFromJwtToken(jwt);
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            // log error
        }
        filterChain.doFilter(request, response);
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }
}
"""

files["security/WebSecurityConfig.java"] = """package com.couplestory.security;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableMethodSecurity
public class WebSecurityConfig {
    private final UserDetailsServiceImpl userDetailsService;

    public WebSecurityConfig(UserDetailsServiceImpl userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public AuthTokenFilter authenticationJwtTokenFilter() {
        return new AuthTokenFilter();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("*")); // Adjust in prod
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("authorization", "content-type", "x-auth-token"));
        configuration.setExposedHeaders(Arrays.asList("x-auth-token"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> 
                auth.requestMatchers("/api/auth/**").permitAll()
                    .requestMatchers("/api/public/**").permitAll()
                    .anyRequest().authenticated()
            );
        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
"""

files["dto/LoginRequest.java"] = """package com.couplestory.dto;
import lombok.Data;
@Data public class LoginRequest { private String email; private String password; }
"""

files["dto/SignupRequest.java"] = """package com.couplestory.dto;
import lombok.Data;
@Data public class SignupRequest { private String email; private String password; }
"""

files["dto/JwtResponse.java"] = """package com.couplestory.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;
@Data @AllArgsConstructor
public class JwtResponse {
    private String token;
    private String id;
    private String email;
    private List<String> roles;
}
"""

files["controller/AuthController.java"] = """package com.couplestory.controller;
import com.couplestory.dto.JwtResponse;
import com.couplestory.dto.LoginRequest;
import com.couplestory.dto.SignupRequest;
import com.couplestory.entity.User;
import com.couplestory.repository.UserRepository;
import com.couplestory.security.JwtUtils;
import com.couplestory.security.UserDetailsImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private final JwtUtils jwtUtils;

    public AuthController(AuthenticationManager authenticationManager, UserRepository userRepository,
                          PasswordEncoder encoder, JwtUtils jwtUtils) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.encoder = encoder;
        this.jwtUtils = jwtUtils;
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());
        return ResponseEntity.ok(new JwtResponse(jwt, userDetails.getId(), userDetails.getEmail(), roles));
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody SignupRequest signUpRequest) {
        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity.badRequest().body("Error: Email is already in use!");
        }
        User user = User.builder()
                .email(signUpRequest.getEmail())
                .passwordHash(encoder.encode(signUpRequest.getPassword()))
                .role("USER")
                .build();
        userRepository.save(user);
        return ResponseEntity.ok("User registered successfully!");
    }
}
"""

files["service/WebsiteService.java"] = """package com.couplestory.service;
import com.couplestory.entity.Website;
import com.couplestory.repository.WebsiteRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class WebsiteService {
    private final WebsiteRepository websiteRepository;

    public WebsiteService(WebsiteRepository websiteRepository) {
        this.websiteRepository = websiteRepository;
    }

    public List<Website> getWebsitesByUserId(String userId) {
        return websiteRepository.findByUserId(userId);
    }

    public Optional<Website> getWebsiteById(String id) {
        return websiteRepository.findById(id);
    }

    public Website createWebsite(Website website) {
        website.setStatus("draft");
        return websiteRepository.save(website);
    }

    public Website updateWebsite(String id, Website websiteDetails) {
        Website website = websiteRepository.findById(id).orElseThrow(() -> new RuntimeException("Website not found"));
        if (websiteDetails.getSubdomain() != null) website.setSubdomain(websiteDetails.getSubdomain());
        if (websiteDetails.getTemplateCode() != null) website.setTemplateCode(websiteDetails.getTemplateCode());
        if (websiteDetails.getTemplateConfig() != null) website.setTemplateConfig(websiteDetails.getTemplateConfig());
        if (websiteDetails.getStatus() != null) website.setStatus(websiteDetails.getStatus());
        return websiteRepository.save(website);
    }

    public void deleteWebsite(String id) {
        websiteRepository.deleteById(id);
    }
}
"""

files["controller/WebsiteController.java"] = """package com.couplestory.controller;
import com.couplestory.entity.Website;
import com.couplestory.security.UserDetailsImpl;
import com.couplestory.service.WebsiteService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/websites")
public class WebsiteController {
    private final WebsiteService websiteService;

    public WebsiteController(WebsiteService websiteService) {
        this.websiteService = websiteService;
    }

    @GetMapping
    public ResponseEntity<List<Website>> getMyWebsites(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        List<Website> websites = websiteService.getWebsitesByUserId(userDetails.getId());
        return ResponseEntity.ok(websites);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Website> getWebsiteById(@PathVariable String id) {
        return websiteService.getWebsiteById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Website> createWebsite(@RequestBody Website website, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        website.setUserId(userDetails.getId());
        Website createdWebsite = websiteService.createWebsite(website);
        return ResponseEntity.ok(createdWebsite);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Website> updateWebsite(@PathVariable String id, @RequestBody Website websiteDetails) {
        Website updatedWebsite = websiteService.updateWebsite(id, websiteDetails);
        return ResponseEntity.ok(updatedWebsite);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWebsite(@PathVariable String id) {
        websiteService.deleteWebsite(id);
        return ResponseEntity.ok().build();
    }
}
"""

for path, content in files.items():
    full_path = os.path.join(BASE_DIR, path.replace("/", "\\"))
    os.makedirs(os.path.dirname(full_path), exist_ok=True)
    with open(full_path, "w", encoding="utf-8") as f:
        f.write(content)
