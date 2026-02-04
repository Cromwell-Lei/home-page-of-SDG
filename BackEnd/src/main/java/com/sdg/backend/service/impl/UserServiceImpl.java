package com.sdg.backend.service.impl;

import com.sdg.backend.dto.AuthDtos;
import com.sdg.backend.dto.UserManagementDtos;
import com.sdg.backend.entity.User;
import com.sdg.backend.repository.UserRepository;
import com.sdg.backend.service.RsaKeyService;
import com.sdg.backend.service.UserService;
import com.sdg.backend.service.VerificationService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

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

        // 3.3 Create User with retry mechanism for concurrent ID conflicts
        String decryptedPassword = rsaKeyService.decrypt(request.getPassword());

        int maxRetries = 5;
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                User newUser = new User();
                newUser.setId(findNextAvailableId());
                newUser.setUsername(request.getUsername());
                newUser.setEmail(request.getEmail());
                newUser.setPassword(passwordEncoder.encode(decryptedPassword));
                newUser.setEnabled(true);
                newUser.setRole("ROLE_USER"); // Set default role for registered users

                return userRepository.save(newUser);
            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                // ID conflict detected, retry
                if (attempt == maxRetries - 1) {
                    throw new RuntimeException(
                            "Failed to register user after " + maxRetries + " attempts due to ID conflicts");
                }
                // Wait a bit before retry
                try {
                    Thread.sleep(10 * (attempt + 1));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("User registration interrupted");
                }
            }
        }
        throw new RuntimeException("Unexpected error in user registration");
    }

    // Login with RSA decryption
    @Override
    public User login(AuthDtos.LoginRequest request) {
        // Decrypt password
        String decryptedPassword = rsaKeyService.decrypt(request.getPassword());

        // Only find users with ROLE_USER to prevent admin discovery
        User user = userRepository.findByEmailAndRole(request.getEmail(), "ROLE_USER")
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

    @Override
    public User adminLogin(AuthDtos.LoginRequest request) {
        // Decrypt password
        String decryptedPassword = rsaKeyService.decrypt(request.getPassword());

        // Find user by username and ROLE_ADMIN
        User user = userRepository.findByUsernameAndRole(request.getUsername(), "ROLE_ADMIN")
                .orElseThrow(() -> new RuntimeException("Admin not found or invalid credentials"));

        if (!passwordEncoder.matches(decryptedPassword, user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        if (!user.isEnabled()) {
            throw new RuntimeException("Account not enabled");
        }

        return user;
    }

    // User management methods
    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }

    @Override
    public User createUser(UserManagementDtos.CreateUserRequest request) {
        // Validate username and email uniqueness
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        // Retry mechanism for concurrent ID conflicts
        int maxRetries = 5;
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                // Create new user with smart ID allocation
                User newUser = new User();
                newUser.setId(findNextAvailableId());
                newUser.setUsername(request.getUsername());
                newUser.setEmail(request.getEmail());
                newUser.setPassword(passwordEncoder.encode(request.getPassword()));
                newUser.setRole(request.getRole() != null ? request.getRole() : "ROLE_USER");
                newUser.setEnabled(request.isEnabled());

                return userRepository.save(newUser);
            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                // ID conflict detected, retry
                if (attempt == maxRetries - 1) {
                    throw new RuntimeException(
                            "Failed to create user after " + maxRetries + " attempts due to ID conflicts");
                }
                // Wait a bit before retry to reduce contention
                try {
                    Thread.sleep(10 * (attempt + 1));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("User creation interrupted");
                }
            }
        }
        throw new RuntimeException("Unexpected error in user creation");
    }

    @Override
    public User updateUser(Long id, UserManagementDtos.UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        // Update fields if provided
        if (request.getUsername() != null && !request.getUsername().isEmpty()) {
            // Check if username is taken by another user
            if (userRepository.existsByUsername(request.getUsername())
                    && !user.getUsername().equals(request.getUsername())) {
                throw new RuntimeException("Username already exists");
            }
            user.setUsername(request.getUsername());
        }

        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            // Check if email is taken by another user
            if (userRepository.existsByEmail(request.getEmail())
                    && !user.getEmail().equals(request.getEmail())) {
                throw new RuntimeException("Email already exists");
            }
            user.setEmail(request.getEmail());
        }

        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getRole() != null && !request.getRole().isEmpty()) {
            user.setRole(request.getRole());
        }

        if (request.getEnabled() != null) {
            user.setEnabled(request.getEnabled());
        }

        return userRepository.save(user);
    }

    @Override
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    @Override
    public long getUserCount() {
        return userRepository.count();
    }

    /**
     * Find the next available ID by filling gaps or incrementing.
     * Algorithm:
     * 1. Get all existing IDs in ascending order
     * 2. Find the first missing ID starting from 1
     * 3. If no gaps, return max(ID) + 1
     */
    private Long findNextAvailableId() {
        java.util.List<Long> existingIds = userRepository.findAllIds();

        if (existingIds.isEmpty()) {
            return 1L;
        }

        // Find first gap in sequence
        long expectedId = 1;
        for (Long id : existingIds) {
            if (id > expectedId) {
                // Found a gap
                return expectedId;
            }
            expectedId = id + 1;
        }

        // No gaps found, return next sequential ID
        return expectedId;
    }
}
