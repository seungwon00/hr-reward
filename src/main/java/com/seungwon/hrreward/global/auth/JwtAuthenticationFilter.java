package com.seungwon.hrreward.global.auth;

import com.seungwon.hrreward.global.exception.BusinessException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

// Bearer 토큰이 유효하면 userId를 principal로 인증 처리한다.
// 토큰이 잘못돼도 여기서 응답하지 않고 에러 코드만 남긴다: 인증이 필요 없는 경로는 그대로 통과하고,
// 인증이 필요한 경로는 JwtAuthenticationEntryPoint가 그 코드로 401을 내려준다.
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    static final String AUTH_ERROR_ATTRIBUTE = JwtAuthenticationFilter.class.getName() + ".ERROR";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            try {
                Long userId = jwtTokenProvider.getUserId(header.substring(BEARER_PREFIX.length()));
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(userId, null, List.of()));
                SecurityContextHolder.setContext(context);
            } catch (BusinessException e) {
                request.setAttribute(AUTH_ERROR_ATTRIBUTE, e.getErrorCode());
            }
        }
        chain.doFilter(request, response);
    }
}
