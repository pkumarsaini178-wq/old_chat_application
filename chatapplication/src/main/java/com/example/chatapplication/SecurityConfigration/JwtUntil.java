package com.example.chatapplication.SecurityConfigration;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;

@Component
public class JwtUntil {

    @Value("${jwt.secret}")
    private String secrectKey;
    @Value("${jwt.expiry}")
    private Long expiry;

    private Key getsigineky() {
        byte[] keyBytes = secrectKey.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            byte[] paddedKey = new byte[32];
            System.arraycopy(keyBytes, 0, paddedKey, 0, keyBytes.length);
            for (int i = keyBytes.length; i < 32; i++) {
                paddedKey[i] = (byte) (i * 31);
            }
            return Keys.hmacShaKeyFor(paddedKey);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String gunrateToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiry))
                .signWith(getsigineky(), SignatureAlgorithm.HS256)
                .compact();

    }

    public String FechEmailfromToke(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getsigineky())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean Token_is_vailid(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getsigineky())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            System.out.println("error is " + e.getMessage());
            return false;
        }
    }

    public long getTokenAge(String token) {
        try {
            var claims = Jwts.parserBuilder()
                    .setSigningKey(getsigineky())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            Date issuedAt = claims.getIssuedAt();
            return System.currentTimeMillis() - issuedAt.getTime();
        } catch (JwtException | IllegalArgumentException e) {
            return Long.MAX_VALUE;
        }
    }

    public Date getTokenExpiration(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getsigineky())
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getExpiration();
        } catch (Exception e) {
            return new Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L);
        }
    }

}
