package com.adarsh.zorvyn.Controllers;

import com.adarsh.zorvyn.Request.AuthRequest;
import com.adarsh.zorvyn.Utils.JWTUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * AuthController handles authentication for the Finance Dashboard API.
 *
 * This is the only public-facing controller — no JWT token is required to call /login.
 * On successful login, a signed JWT is returned which the client must include
 * in the Authorization header of all subsequent requests.
 *
 * Flow:
 *   POST /api/v1/login → validate credentials → return JWT token
 */
@RestController
@RequestMapping("/api/v1")
public class AuthController {

    // AuthenticationManager delegates to Spring Security's DaoAuthenticationProvider,
    // which loads the user from DB and validates the BCrypt-encoded password.
    @Autowired
    AuthenticationManager authenticationManager;

    // JWTUtil handles token generation and validation logic.
    @Autowired
    private JWTUtil jwtUtil;

    /**
     * POST /api/v1/login
     *
     * Accepts a username and password, authenticates the user,
     * and returns a signed JWT token on success.
     *
     * The token expires in 1 hour. Include it in subsequent requests as:
     *   Authorization: Bearer <token>
     *
     * Returns:
     *   200 OK      - { "token": "eyJhbGci..." }
     *   401         - { "error": "Invalid username or password" }
     */
    @PostMapping("/login")
    public ResponseEntity<?> generateToken(@RequestBody AuthRequest authRequest) {
        try {
            // Attempt to authenticate using Spring Security's authentication flow.
            // Internally this loads the user by username, then checks the password with BCrypt.
            // If the user is INACTIVE, isEnabled() returns false and authentication is rejected.
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            authRequest.getUsername(),
                            authRequest.getPassword()
                    )
            );

            if (authentication.isAuthenticated()) {
                // Generate a JWT signed with our secret key and return it.
                String token = jwtUtil.generateToken(authRequest.getUsername());
                return ResponseEntity.ok(Map.of("token", token));
            }

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Authentication failed"));

        } catch (BadCredentialsException e) {
            // Wrong username or password — return 401 with a clear message.
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid username or password"));
        }
    }
}