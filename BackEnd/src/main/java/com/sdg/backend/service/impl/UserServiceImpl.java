package com.sdg.backend.service.impl;

import com.sdg.backend.dto.AuthDtos;
import com.sdg.backend.entity.User;
import com.sdg.backend.repository.UserRepository;
import com.sdg.backend.service.RsaKeyService;
import com.sdg.backend.service.UserService;
import com.sdg.backend.service.VerificationService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RsaKeyService rsaKeyService;
    private final VerificationService verificationService;

    public UserServiceImpl(UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            RsaKeyService rsaKeyService,
            VerificationService verificationService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.rsaKeyService = rsaKeyService;
        this.verificationService = verificationService;
    }

    // Step 1: Validate Email and Send Code
    public void sendVerificationCode(AuthDtos.SendCodeRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        // Generate and send code
        String code = verificationService.generateCode();
        verificationService.storeCode(request.getEmail(), code);

        // Mock sending email
        System.out.println("--------------------------------------------------");
        System.out.println("MOCK EMAIL TO: " + request.getEmail());
        System.out.println("VERIFICATION CODE: " + code);
        System.out.println("--------------------------------------------------");
    }

    // Step 2: Validate Code (Read-only)
    public void validateCode(AuthDtos.VerifyRequest request) {
        String decryptedCode = rsaKeyService.decrypt(request.getCode());
        if (!verificationService.checkCode(request.getEmail(), decryptedCode)) {
            throw new RuntimeException("Invalid or expired verification code");
        }
    }

    // Step 3: Final Registration
    public User register(AuthDtos.RegisterRequest request) {
        // 3.1 Verify Code (Again + Consume)
        String decryptedCode = rsaKeyService.decrypt(request.getCode());
        if (!verificationService.verifyCode(request.getEmail(), decryptedCode)) {
            throw new RuntimeException("Invalid or expired verification code");
        }

        // 3.2 Check Username
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        // 3.3 Create User
        String decryptedPassword = rsaKeyService.decrypt(request.getPassword());
        User newUser = new User();
        newUser.setUsername(request.getUsername());
        newUser.setEmail(request.getEmail());
        newUser.setPassword(passwordEncoder.encode(decryptedPassword));
        newUser.setEnabled(true);

        return userRepository.save(newUser);
    }

    // Login with RSA decryption
    @Override
    public User login(AuthDtos.LoginRequest request) {
        // Decrypt password
        String decryptedPassword = rsaKeyService.decrypt(request.getPassword());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(decryptedPassword, user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        if (!user.isEnabled()) {
            // Optional: allow login if they are verifying? For now strictly enforce
            // enabled.
            throw new RuntimeException("Account not verified");
        }

        return user;
    }
}
