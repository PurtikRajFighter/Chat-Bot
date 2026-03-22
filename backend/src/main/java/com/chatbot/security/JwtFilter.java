package com.chatbot.security;

import com.chatbot.service.ExcelService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JwtFilter — intercepts every request and validates the JWT token.
 *
 * If a valid token is found in the Authorization header,
 * it sets the authentication in the SecurityContext so that
 * the rest of the application knows who is logged in.
 */
@Component
public class JwtFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ExcelService excelService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // Extract the Authorization header
        String authHeader = request.getHeader("Authorization");

        String token = null;
        String userId = null;

        // Header format: "Bearer <token>"
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            try {
                if (jwtUtil.isTokenValid(token)) {
                    userId = jwtUtil.extractUserId(token);
                }
            } catch (Exception e) {
                // Invalid token — just don't set authentication
            }
        }

        // Set authentication in context if user found and not already authenticated
        if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // Verify user actually exists in Excel
            boolean userExists = excelService.findUserById(userId).isPresent();
            if (userExists) {
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        filterChain.doFilter(request, response);
    }
}

