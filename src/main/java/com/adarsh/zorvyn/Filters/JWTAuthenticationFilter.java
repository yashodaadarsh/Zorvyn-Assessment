package com.adarsh.zorvyn.Filters;

import com.adarsh.zorvyn.Service.CustomUserDetailService;
import com.adarsh.zorvyn.Utils.JWTUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWTAuthenticationFilter intercepts every incoming HTTP request exactly once
 * (extends OncePerRequestFilter) and validates the JWT token if present.
 *
 * How it works:
 *   1. Checks if the request path is public (login, Swagger) — if so, skip and pass through.
 *   2. Extracts the Bearer token from the Authorization header.
 *   3. Parses the username from the token.
 *   4. Loads the user from the database and validates the token.
 *   5. If valid, sets the authentication in the SecurityContext so that
 *      @PreAuthorize checks in controllers can run correctly.
 *
 * If no token is present or the token is invalid/expired, the request is
 * not authenticated and will be rejected by Spring Security at the controller level.
 */
public class JWTAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JWTUtil jwtUtil;

    @Autowired
    private CustomUserDetailService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getServletPath();

        // Skip JWT validation for public endpoints.
        // Login doesn't have a token yet, and Swagger UI must be accessible without auth.
        if (path.startsWith("/api/v1/login") ||
                path.startsWith("/swagger-ui") ||
                path.startsWith("/v3/api-docs")) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        String token = null;
        String username = null;

        // Extract the token from the Authorization header.
        // Expected format: "Bearer <jwt_token>"
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            username = jwtUtil.extractUsername(token); // parse username from token claims
        }

        // Only proceed if we have a username and no existing authentication in the context.
        // The second check prevents re-authenticating an already authenticated request.
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Load the full UserDetails (including role/authorities) from the database.
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // Validate: token username matches the loaded user AND the token is not expired.
            if (jwtUtil.validateToken(username, userDetails, token)) {

                // Build a Spring Security authentication token with the user's authorities (role).
                // This is what @PreAuthorize checks against.
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities()
                        );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Set the authentication in the SecurityContext for this request thread.
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // Pass the request to the next filter in the chain regardless of auth outcome.
        filterChain.doFilter(request, response);
    }
}