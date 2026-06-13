package com.zhihui.bishe.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Claims;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String SECRET_KEY = "your_secret_key";  // 用你自己的密钥替代
    private static final String TOKEN_PREFIX = "Bearer ";  // JWT 前缀
    private static final String HEADER_STRING = "Authorization";  // 请求头字段名

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = request.getHeader(HEADER_STRING);  // 从请求头中获取 token

        if (token != null && token.startsWith(TOKEN_PREFIX)) {
            token = token.substring(TOKEN_PREFIX.length());  // 去掉 "Bearer " 前缀

            try {
                // 解析 JWT token
                Claims claims = Jwts.parser()
                        .setSigningKey(SECRET_KEY)  // 使用密钥来验证 token
                        .parseClaimsJws(token)  // 解析 token 并获取 claims
                        .getBody();

                // 从 claims 中获取用户名并构造认证信息
                String username = claims.getSubject();
                if (username != null) {
                    // 创建认证对象
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(username, null, null); // 你可以添加权限等信息
                    // 将认证信息存入 SecurityContext
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }

            } catch (Exception e) {
                // 如果解析失败，可以记录错误信息
                logger.error("Invalid JWT token", e);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Invalid JWT token");
                return;
            }
        }

        filterChain.doFilter(request, response);  // 继续过滤链
    }
}
