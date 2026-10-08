package com.couplestory.security;

import com.couplestory.entity.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SessionVersionTest {

    private static final String SECRET = Base64.getEncoder()
            .encodeToString("0123456789abcdef0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8));
    private static final String EMAIL = "a@b.c";

    private JwtUtils jwtUtils;
    private UserDetailsServiceImpl users;
    private AuthTokenFilter filter;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", SECRET);
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", 60_000);
        users = mock(UserDetailsServiceImpl.class);
        filter = new AuthTokenFilter();
        ReflectionTestUtils.setField(filter, "jwtUtils", jwtUtils);
        ReflectionTestUtils.setField(filter, "userDetailsService", users);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private UserDetailsImpl principal(int version) {
        return UserDetailsImpl.build(User.builder()
                .id(UUID.randomUUID()).email(EMAIL).displayName("A").passwordHash("x")
                .tokenVersion(version).roles(new HashSet<>()).build());
    }

    private String tokenWithVersion(int version) {
        return jwtUtils.generateJwtToken(new UsernamePasswordAuthenticationToken(principal(version), null, List.of()));
    }

    /** What the old Google login issued: a valid signature but no token_version claim. */
    private String tokenWithoutVersionClaim() {
        return Jwts.builder().setSubject(EMAIL)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET)), SignatureAlgorithm.HS256)
                .compact();
    }

    private MockHttpServletRequest run(String jwt, int currentUserVersion) throws Exception {
        when(users.loadUserByUsername(EMAIL)).thenReturn(principal(currentUserVersion));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/stories");
        request.setCookies(new Cookie("access_token", jwt));
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
        return request;
    }

    @Test
    void tokenWithCurrentVersionAuthenticates() throws Exception {
        run(tokenWithVersion(3), 3);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test
    void tokenWithoutVersionClaimIsRejectedAsInvalid() throws Exception {
        MockHttpServletRequest request = run(tokenWithoutVersionClaim(), 3);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(request.getAttribute("auth.reason")).isEqualTo("TOKEN_INVALID");
    }

    @Test
    void olderVersionIsRejectedAsSessionReplaced() throws Exception {
        MockHttpServletRequest request = run(tokenWithVersion(2), 3);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(request.getAttribute("auth.reason")).isEqualTo("SESSION_REPLACED");
    }

    @Test
    void tokenIssuedFromPrincipalCarriesItsVersion() {
        String token = jwtUtils.generateJwtToken(principal(5));

        assertThat(jwtUtils.getTokenVersionFromJwtToken(token)).isEqualTo(5);
    }

    @Test
    void sessionIssuedAtAnEarlierLoginIsReplacedByANewerOne() throws Exception {
        String token = jwtUtils.generateJwtToken(principal(4));

        MockHttpServletRequest request = run(token, 5);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(request.getAttribute("auth.reason")).isEqualTo("SESSION_REPLACED");
    }

    @Test
    void entryPointReportsTheReasonCode() throws Exception {
        AuthenticationEntryPoint entryPoint = new WebSecurityConfig(users).authenticationEntryPoint();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/stories");
        request.setAttribute("auth.reason", "SESSION_REPLACED");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new BadCredentialsException("x"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString(StandardCharsets.UTF_8)).contains("\"code\":\"SESSION_REPLACED\"");
    }

    @Test
    void entryPointDefaultsToUnauthenticatedWhenThereIsNoReason() throws Exception {
        AuthenticationEntryPoint entryPoint = new WebSecurityConfig(users).authenticationEntryPoint();
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(new MockHttpServletRequest("GET", "/api/stories"), response, new BadCredentialsException("x"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString(StandardCharsets.UTF_8))
                .contains("\"code\":\"UNAUTHENTICATED\"")
                .contains("Authentication required");
    }
}
