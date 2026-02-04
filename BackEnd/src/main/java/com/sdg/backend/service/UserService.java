package com.sdg.backend.service;

import com.sdg.backend.dto.AuthDtos;
import com.sdg.backend.dto.UserManagementDtos;
import com.sdg.backend.entity.User;

import java.util.List;

public interface UserService {
    User login(AuthDtos.LoginRequest request);

    void sendVerificationCode(AuthDtos.SendCodeRequest request);

    void validateCode(AuthDtos.VerifyRequest request);

    User register(AuthDtos.RegisterRequest request);

    User adminLogin(AuthDtos.LoginRequest request);

    // User management methods
    List<User> getAllUsers();

    User getUserById(Long id);

    User createUser(UserManagementDtos.CreateUserRequest request);

    User updateUser(Long id, UserManagementDtos.UpdateUserRequest request);

    void deleteUser(Long id);

    long getUserCount();
}
