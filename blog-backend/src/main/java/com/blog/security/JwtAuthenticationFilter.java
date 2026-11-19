package com.blog.security;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.blog.entity.User;
import com.blog.mapper.UserMapper;

/**
 * JWT 认证过滤器：解析 Authorization: Bearer <token>，写入 SecurityContext。
 * 无 token / token 无效时不阻断请求，由授权规则决定 401。
 * <p>
 * 每次请求回查用户表：用户已删除或已封禁（status=1）立即按未认证处理；
 * 角色以数据库为准，保证封禁 / 降权即时生效。
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider tokenProvider;
    private final InMemoryTokenBlacklist blacklist;
    private final UserMapper userMapper;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider, InMemoryTokenBlacklist blacklist,
                                   UserMapper userMapper) {
        this.tokenProvider = tokenProvider;
        this.blacklist = blacklist;
        this.userMapper = userMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = resolveToken(request);
        if (token != null && !blacklist.contains(token)) {
            try {
                Claims claims = tokenProvider.parseToken(token);
                Long userId = Long.valueOf(claims.getSubject());
                User user = userMapper.selectById(userId);
                if (user == null || Integer.valueOf(1).equals(user.getStatus())) {
                    // 用户已删除或已封禁：按未认证处理
                    SecurityContextHolder.clearContext();
                } else {
                    UserPrincipal principal = new UserPrincipal(userId, user.getUsername());
                    List<SimpleGrantedAuthority> authorities = "admin".equals(user.getRole())
                            ? List.of(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_ADMIN"))
                            : List.of(new SimpleGrantedAuthority("ROLE_USER"));
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            principal, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (JwtException | IllegalArgumentException e) {
                // token 无效或已过期：清空上下文，按未认证处理
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX) && header.length() > BEARER_PREFIX.length()) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
