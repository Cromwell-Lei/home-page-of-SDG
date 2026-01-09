package com.sdg.backend.service;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class VerificationService {

    // Key: Email, Value: Code
    private final Map<String, String> codeStorage = new ConcurrentHashMap<>();

    // Key: Email, Value: Pending Registration Data (JSON/Object)
    private final Map<String, Object> pendingUsers = new ConcurrentHashMap<>();

    public String generateCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    public void storeCode(String email, String code) {
        codeStorage.put(email, code);
        // In a real app, set expiration here (e.g., Redis TTL)
    }

    public boolean verifyCode(String email, String inputCode) {
        String storedCode = codeStorage.get(email);
        if (storedCode != null && storedCode.equals(inputCode)) {
            codeStorage.remove(email);
            return true;
        }
        return false;
    }

    public boolean checkCode(String email, String inputCode) {
        String storedCode = codeStorage.get(email);
        return storedCode != null && storedCode.equals(inputCode);
    }

    public void storePendingUser(String email, Object userData) {
        pendingUsers.put(email, userData);
    }

    public Object getPendingUser(String email) {
        return pendingUsers.get(email);
    }

    public void removePendingUser(String email) {
        pendingUsers.remove(email);
    }
}
