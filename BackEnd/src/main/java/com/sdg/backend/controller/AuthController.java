package com.sdg.backend.controller;

import com.sdg.backend.dto.AuthDtos;
import com.sdg.backend.entity.User;
import com.sdg.backend.service.RsaKeyService;
import com.sdg.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final RsaKeyService rsaKeyService;
    private final com.sdg.backend.util.JwtUtil jwtUtil;

    public AuthController(UserService userService, RsaKeyService rsaKeyService, com.sdg.backend.util.JwtUtil jwtUtil) {
        this.userService = userService;
        this.rsaKeyService = rsaKeyService;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/public-key")
    public ResponseEntity<?> getPublicKey() {
        return ResponseEntity.ok(Map.of("publicKey", rsaKeyService.getPublicKey()));
    }

    @PostMapping("/send-code")
    public ResponseEntity<?> sendCode(@RequestBody AuthDtos.SendCodeRequest request) {
        try {
            userService.sendVerificationCode(request);
            return ResponseEntity.ok("Verification code sent to email (MOCK)");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/validate-code")
    public ResponseEntity<?> validateCode(@RequestBody AuthDtos.VerifyRequest request) {
        try {
            userService.validateCode(request);
            return ResponseEntity.ok("Verification code valid");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthDtos.RegisterRequest request) {
        try {
            User newUser = userService.register(request);
            return ResponseEntity.ok("User registered: " + newUser.getUsername());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthDtos.LoginRequest request,
            jakarta.servlet.http.HttpServletResponse response) {
        try {
            User user = userService.login(request);

            // Generate JWT
            String token = jwtUtil.generateToken(user.getUsername());

            // Set HttpOnly Cookie with JWT
            jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("token", token);
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            cookie.setMaxAge(24 * 60 * 60); // 1 day
            cookie.setAttribute("SameSite", "Lax");
            // cookie.setSecure(true); // Should be true in production with HTTPS
            response.addCookie(cookie);

            return ResponseEntity.ok(Map.of("message", "Login successful", "username", user.getUsername()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    @PostMapping("/admin-login")
    public ResponseEntity<?> adminLogin(@RequestBody AuthDtos.LoginRequest request,
            jakarta.servlet.http.HttpServletResponse response) {
        try {
            User user = userService.adminLogin(request);

            // Generate JWT
            String token = jwtUtil.generateToken(user.getUsername());

            // Set HttpOnly Cookie with JWT
            jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("token", token);
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            cookie.setMaxAge(24 * 60 * 60); // 1 day
            cookie.setAttribute("SameSite", "Lax");
            // cookie.setSecure(true); // Should be true in production with HTTPS
            response.addCookie(cookie);

            return ResponseEntity.ok(Map.of(
                    "message", "Admin Login successful",
                    "username", user.getUsername(),
                    "token", token));
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(jakarta.servlet.http.HttpServletResponse response) {
        jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("token", null);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        return ResponseEntity.ok("Logged out");
    }

    @GetMapping("/me")
    public ResponseEntity<?> me() {
        org.springframework.security.core.Authentication authentication = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !("anonymousUser".equals(authentication.getPrincipal()))) {
            // Principal is likely UserDetails or username string depending on how it was
            // set
            Object principal = authentication.getPrincipal();
            String username;
            if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
                username = ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
            } else {
                username = principal.toString();
            }
            return ResponseEntity.ok(Map.of("username", username));
        }
        return ResponseEntity.status(401).body("Not logged in");
    }
}
