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

    public AuthController(UserService userService, RsaKeyService rsaKeyService) {
        this.userService = userService;
        this.rsaKeyService = rsaKeyService;
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

            // Set Cookie
            jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("user", user.getUsername());
            cookie.setPath("/");
            cookie.setMaxAge(24 * 60 * 60); // 1 day
            response.addCookie(cookie);

            return ResponseEntity.ok(Map.of("message", "Login successful", "username", user.getUsername()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(jakarta.servlet.http.HttpServletResponse response) {
        jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("user", null);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        return ResponseEntity.ok("Logged out");
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@CookieValue(value = "user", required = false) String username) {
        if (username != null && !username.isEmpty()) {
            return ResponseEntity.ok(Map.of("username", username));
        }
        return ResponseEntity.status(401).body("Not logged in");
    }
}
