package com.example.jobup.services;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
@Slf4j
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    // subject is now the userId
    public String extractUserId(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // keep old name if other code still calls it; now returns userId
    public String extractUsername(String token) {
        return extractUserId(token);
    }

    // get display username from claim
    public String extractPreferredUsername(String token) {
        return extractClaim(token, c -> (String) c.get("preferred_username"));
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("preferred_username", userDetails.getUsername());
        String userId = (userDetails instanceof com.example.jobup.entities.User u) ? u.getId()
                : userDetails.getUsername();
        return createToken(claims, userId);
    }

    public String generateToken(UserDetails userDetails, Map<String, Object> extraClaims) {
        // If your principal is your own User entity, grab the DB id; otherwise fall back
        String userId = (userDetails instanceof com.example.jobup.entities.User u) ? u.getId()
                : userDetails.getUsername(); // fallback for safety
        // Ensure preferred_username is present for downstream lookups
        extraClaims.putIfAbsent("preferred_username", userDetails.getUsername());
        return createToken(extraClaims, userId);
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String preferred = extractPreferredUsername(token);
            return (preferred != null && preferred.equals(userDetails.getUsername()) && !isTokenExpired(token));
        } catch (JwtException | IllegalArgumentException e) {
            log.error("JWT validation error: {}", e.getMessage());
            return false;
        }
    }

    public Boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("JWT validation error: {}", e.getMessage());
            return false;
        }
    }
}
