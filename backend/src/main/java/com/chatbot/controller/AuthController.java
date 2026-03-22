package com.chatbot.controller;

import com.chatbot.dto.AuthRequest;
import com.chatbot.model.User;
import com.chatbot.security.JwtUtil;
import com.chatbot.service.ExcelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * AuthController — handles user registration, login, and profile.
 *
 * Endpoints:
 *   POST /api/auth/register  — create a new user
 *   POST /api/auth/login     — authenticate and return JWT
 *   GET  /api/auth/me        — get current user info (requires JWT)
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private ExcelService excelService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    // Default tokens for new users
    @Value("${tokens.default.count}")
    private int defaultTokens;

    /**
     * Register a new user.
     * Validates that username and email are unique.
     * Hashes password with BCrypt.
     * Assigns default tokens.
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody AuthRequest request) {
        Map<String, Object> response = new HashMap<>();

        // Validate input
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            response.put("success", false);
            response.put("error", "Username is required");
            return ResponseEntity.badRequest().body(response);
        }
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            response.put("success", false);
            response.put("error", "Email is required");
            return ResponseEntity.badRequest().body(response);
        }
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            response.put("success", false);
            response.put("error", "Password must be at least 6 characters");
            return ResponseEntity.badRequest().body(response);
        }

        // Check username uniqueness
        if (excelService.findByUsername(request.getUsername()).isPresent()) {
            response.put("success", false);
            response.put("error", "Username already taken");
            return ResponseEntity.badRequest().body(response);
        }

        // Check email uniqueness
        if (excelService.findByEmail(request.getEmail()).isPresent()) {
            response.put("success", false);
            response.put("error", "Email already registered");
            return ResponseEntity.badRequest().body(response);
        }

        // Create new user
        User newUser = new User(
                UUID.randomUUID().toString(),       // Unique ID
                request.getUsername().trim(),
                request.getEmail().toLowerCase().trim(),
                passwordEncoder.encode(request.getPassword()),  // BCrypt hash
                defaultTokens,                      // Default token balance
                LocalDateTime.now()                 // Reset time starts now
        );

        // Save to users.xlsx
        excelService.saveUser(newUser);

        // Generate JWT for immediate login
        String token = jwtUtil.generateToken(newUser.getId());

        response.put("success", true);
        response.put("token", token);
        response.put("userId", newUser.getId());
        response.put("username", newUser.getUsername());
        response.put("tokens", newUser.getTokens());
        response.put("message", "Registration successful! You have " + defaultTokens + " free tokens.");
        return ResponseEntity.ok(response);
    }

    /**
     * Login with username and password.
     * Returns JWT token on success.
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody AuthRequest request) {
        Map<String, Object> response = new HashMap<>();

        if (request.getUsername() == null || request.getPassword() == null) {
            response.put("success", false);
            response.put("error", "Username and password are required");
            return ResponseEntity.badRequest().body(response);
        }

        // Find user by username
        Optional<User> optUser = excelService.findByUsername(request.getUsername());
        if (optUser.isEmpty()) {
            response.put("success", false);
            response.put("error", "Invalid username or password");
            return ResponseEntity.status(401).body(response);
        }

        User user = optUser.get();

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            response.put("success", false);
            response.put("error", "Invalid username or password");
            return ResponseEntity.status(401).body(response);
        }

        // Generate JWT
        String token = jwtUtil.generateToken(user.getId());

        response.put("success", true);
        response.put("token", token);
        response.put("userId", user.getId());
        response.put("username", user.getUsername());
        response.put("tokens", user.getTokens());
        return ResponseEntity.ok(response);
    }

    /**
     * Get current logged-in user's info.
     * The userId is extracted from the JWT by JwtFilter.
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(Authentication auth) {
        Map<String, Object> response = new HashMap<>();

        String userId = (String) auth.getPrincipal();
        Optional<User> optUser = excelService.findUserById(userId);

        if (optUser.isEmpty()) {
            response.put("success", false);
            response.put("error", "User not found");
            return ResponseEntity.notFound().build();
        }

        User user = optUser.get();
        response.put("success", true);
        response.put("userId", user.getId());
        response.put("username", user.getUsername());
        response.put("email", user.getEmail());
        response.put("tokens", user.getTokens());
        return ResponseEntity.ok(response);
    }
}

