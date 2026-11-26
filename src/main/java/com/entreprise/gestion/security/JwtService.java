package com.entreprise.gestion.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;                       // [CORRECTION] remplace java.security.Key
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration}")
    private long jwtExpiration;

    // ── Génération ────────────────────────────────────────────────────────────

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));

        return Jwts.builder()
                .claims(claims)                                   // [CORRECTION] était setClaims
                .subject(userDetails.getUsername())               // [CORRECTION] était setSubject
                .issuedAt(new Date())                             // [CORRECTION] était setIssuedAt
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration)) // [CORRECTION] était setExpiration
                .signWith(getSignKey())                           // [CORRECTION] SignatureAlgorithm.HS256 supprimé (déduit de la clé)
                .compact();
    }


    // ── Validation ────────────────────────────────────────────────────────────

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = Jwts.parser()                         // [CORRECTION] était parserBuilder()
                .verifyWith(getSignKey())                         // [CORRECTION] était setSigningKey
                .build()
                .parseSignedClaims(token)                         // [CORRECTION] était parseClaimsJws
                .getPayload();                                    // [CORRECTION] était getBody
        return resolver.apply(claims);
    }

    private SecretKey getSignKey() {                          // [CORRECTION] type Key -> SecretKey
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
