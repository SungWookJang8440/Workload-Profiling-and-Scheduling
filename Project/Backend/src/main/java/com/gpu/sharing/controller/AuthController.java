package com.gpu.sharing.controller;

import com.gpu.sharing.dto.AuthRequest;
import com.gpu.sharing.dto.AuthResponse;
import com.gpu.sharing.dto.ChangePasswordRequest;
import com.gpu.sharing.dto.RegisterRequest;
import com.gpu.sharing.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {
    
    @Autowired
    private AuthService authService;
    
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {
        AuthResponse response = authService.register(registerRequest);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest authRequest) {
        AuthResponse response = authService.login(authRequest);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser() {
        return ResponseEntity.ok(Map.of(
            "user", authService.getCurrentUser()
        ));
    }
    
    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return ResponseEntity.ok(Map.of("message", "비밀번호가 변경되었습니다."));
    }
}
