package com.example.EmployeeManagementSystem.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long expiration;


    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }


    //create the jwt and sign it
    public String generateToken(String username, String role) {
        return Jwts.builder()
                .setSubject(username)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey() , SignatureAlgorithm.HS256)
                .compact();

    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token).getBody();
    }



    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }


    private String extractRole(String token) {
        return extractAllClaims(token).get("role").toString();
    }

    public boolean isTokenValid(String token) {
        try
        {
            extractAllClaims(token);
            return true;
        }
        catch (JwtException | IllegalArgumentException e)
        {
            return false;
        }
    }




}
