package com.sdg.backend.service;

import com.sdg.backend.dto.AuthDtos;
import com.sdg.backend.entity.User;

public interface UserService {
    User login(AuthDtos.LoginRequest request);

    void sendVerificationCode(AuthDtos.SendCodeRequest request);

    void validateCode(AuthDtos.VerifyRequest request);

    User register(AuthDtos.RegisterRequest request);
}
