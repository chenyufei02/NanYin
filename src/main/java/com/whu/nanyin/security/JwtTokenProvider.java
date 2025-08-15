package com.whu.nanyin.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets; // 1. 导入 StandardCharsets
import java.util.Date;

@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    // --- 【【【 核心修正：使用固定的密钥字符串！！！ 】】】 ---
    // 2. 定义一个你自己的、足够长且复杂的密钥字符串。
    //    这个字符串将在多次服务重启之间保持不变。
    //    为了安全，这个字符串至少需要512位，即64个ASCII字符。
    private final String jwtSecretString = "ThisIsMyCustomSecretKeyForNanyinFundProjectAndItIsLongEnoughForHS512";

    // 3. 在构造时，从固定的字符串生成一个SecretKey对象。
    private final SecretKey jwtSecretKey = Keys.hmacShaKeyFor(jwtSecretString.getBytes(StandardCharsets.UTF_8));


    // Token有效期（例如：24小时）
    private final long jwtExpirationInMs = 1000 * 60 * 60 * 24;

    // 生成JWT
    public String generateToken(Authentication authentication) {
        String username = authentication.getName();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(jwtSecretKey, SignatureAlgorithm.HS512)
                .compact();
    }

    // 从JWT中获取用户名
    public String getUsernameFromJWT(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(jwtSecretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    // 验证JWT的有效性
    public boolean validateToken(String authToken) {
        try {
            Jwts.parserBuilder().setSigningKey(jwtSecretKey).build().parseClaimsJws(authToken);
            return true;
        } catch (SecurityException | MalformedJwtException ex) {
            logger.error("Invalid JWT signature/token");
        } catch (ExpiredJwtException ex) {
            logger.error("Expired JWT token");
        } catch (UnsupportedJwtException ex) {
            logger.error("Unsupported JWT token");
        } catch (IllegalArgumentException ex) {
            logger.error("JWT claims string is empty.");
        }
        return false;
    }
}