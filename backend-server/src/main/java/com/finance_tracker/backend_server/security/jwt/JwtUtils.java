package com.finance_tracker.backend_server.security.jwt;


import com.finance_tracker.backend_server.security.service.UserDetailsImpl;
import com.finance_tracker.backend_server.user.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtils {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    @Value("${finance_tracker.app.jwtSecret}")
    private String jwtSecret;

    @Value("${finance_tracker.app.jwtExpirationMs}")
    private int jwtExpirationMs;

    @Value("${finance_tracker.app.jwtRefreshExpirationMs}")
    private long jwtRefreshExpirationMs;

    @Value("${finance_tracker.app.jwtCookieName}")
    private String jwtCookie;

    @Value("${finance_tracker.app.jwtRefreshCookieName}")
    private String jwtRefreshCookie;

    @PostConstruct
    public void init() {
        // Trim whitespace from cookie names (Safari is sensitive to this)
        if (jwtCookie != null) {
            jwtCookie = jwtCookie.trim();
        }
        if (jwtRefreshCookie != null) {
            jwtRefreshCookie = jwtRefreshCookie.trim();
        }
    }

    public ResponseCookie generateJwtCookie(UserDetailsImpl userPrincipal) {
        String jwt = generateTokenFromUsername(userPrincipal.getUsername());
        // Convert milliseconds to seconds for maxAge
        long maxAgeSeconds = jwtExpirationMs / 1000;
        return generateCookie(jwtCookie, jwt, "/api", maxAgeSeconds);
    }

    public ResponseCookie generateJwtCookie(User user) {
        String jwt = generateTokenFromUsername(user.getUsername());
        // Convert milliseconds to seconds for maxAge
        long maxAgeSeconds = jwtExpirationMs / 1000;
        return generateCookie(jwtCookie, jwt, "/api", maxAgeSeconds);
    }

    public ResponseCookie generateRefreshJwtCookie(String refreshToken) {
        // Use /api path instead of /api/auth/refreshtoken for Safari/iOS compatibility
        // Convert milliseconds to seconds for maxAge
        long maxAgeSeconds = jwtRefreshExpirationMs / 1000;
        return generateCookie(jwtRefreshCookie, refreshToken, "/api", maxAgeSeconds);
    }

    public String getJwtFromCookies(HttpServletRequest request) {
        return getCookieValueByName(request, jwtCookie);
    }

    public String getJwtFromHeader(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    public String getJwtRefreshFromCookies(HttpServletRequest request) {
        return getCookieValueByName(request, jwtRefreshCookie);
    }

    public ResponseCookie getCleanJwtCookie() {
        ResponseCookie cookie = ResponseCookie.from(jwtCookie, null)
                .path("/api")
                .maxAge(0)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .build();
        return cookie;
    }

    public ResponseCookie getCleanJwtRefreshCookie() {
        // Use /api path to match the cookie path used when setting the cookie
        ResponseCookie cookie = ResponseCookie.from(jwtRefreshCookie, null)
                .path("/api")
                .maxAge(0)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .build();
        return cookie;
    }

    public String getUserNameFromJwtToken(String token) {
        return Jwts.parserBuilder().setSigningKey(key()).build()
                .parseClaimsJws(token).getBody().getSubject();
    }

    private Key key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parserBuilder().setSigningKey(key()).build().parse(authToken);
            return true;
        } catch (MalformedJwtException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty: {}", e.getMessage());
        }

        return false;
    }

    public String generateTokenFromUsername(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }

    private ResponseCookie generateCookie(String name, String value, String path, long maxAgeSeconds) {
        return ResponseCookie.from(name, value)
                .path(path)
                .maxAge(maxAgeSeconds)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .build();
    }

    private String getCookieValueByName(HttpServletRequest request, String name) {
        Cookie cookie = WebUtils.getCookie(request, name);
        if (cookie != null && cookie.getValue() != null) {
            // Trim whitespace that Safari might add
            return cookie.getValue().trim();
        } else {
            return null;
        }
    }
}