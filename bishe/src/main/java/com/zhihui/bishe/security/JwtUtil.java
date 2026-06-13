package com.zhihui.bishe.security;

import com.zhihui.bishe.util.RoleUtil;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.Claims;
import java.util.Date;

public class JwtUtil {

    private static final String SECRET_KEY = "your_secret_key";  // 用你自己的密钥替代
    private static final long EXPIRATION_TIME = 864_000_000; // 10 天的过期时间

    private static final String CLAIM_ROLE = "role";

    // 生成 JWT（含 role 用于权限与脱敏）
    public static String generateToken(String username, String role) {
        return Jwts.builder()
                .setSubject(username)
                .claim(CLAIM_ROLE, RoleUtil.normalize(role))
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
                .compact();
    }

    public static String generateToken(String username) {
        return generateToken(username, null);
    }

    public static String getRoleFromToken(String token) {
        if (token == null || !token.startsWith("Bearer ")) return null;
        try {
            Claims c = parseToken(token.substring(7).trim());
            return RoleUtil.normalize(c.get(CLAIM_ROLE, String.class));
        } catch (Exception e) {
            return null;
        }
    }

    /** 从 Authorization: Bearer xxx 中提取用户名（subject） */
    public static String getUsernameFromAuthorization(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) return null;
        try {
            Claims c = parseToken(authorizationHeader.substring(7).trim());
            return c.getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    // 验证 JWT
    public static Claims parseToken(String token) {
        return Jwts.parser()
                .setSigningKey(SECRET_KEY)  // 使用密钥解析 token
                .parseClaimsJws(token)
                .getBody();
    }

    // 获取用户名
    public static String getUsernameFromToken(String token) {
        return parseToken(token).getSubject();
    }
}
