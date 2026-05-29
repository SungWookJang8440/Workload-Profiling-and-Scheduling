package com.gpu.sharing.service;

import com.gpu.sharing.dto.AuthRequest;
import com.gpu.sharing.dto.AuthResponse;
import com.gpu.sharing.dto.ChangePasswordRequest;
import com.gpu.sharing.dto.RegisterRequest;
import com.gpu.sharing.dto.UserDto;
import com.gpu.sharing.entity.User;
import com.gpu.sharing.repository.UserRepository;
import com.gpu.sharing.security.CustomUserDetailsService;
import com.gpu.sharing.security.JwtTokenProvider;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private JwtTokenProvider tokenProvider;
    
    @Autowired
    private CustomUserDetailsService userDetailsService;
    
    public AuthResponse register(RegisterRequest registerRequest) {
        // Normalize email
        String email = StringUtils.lowerCase(StringUtils.trimToEmpty(registerRequest.getEmail()));
        
        // Check if user already exists
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already registered");
        }
        
        // Generate unique username
        String baseUsername = StringUtils.isNotBlank(registerRequest.getName()) ? 
            registerRequest.getName() : email;
        String username = generateUniqueUsername(baseUsername);
        
        // Create new user
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(registerRequest.getPassword()));
        user.setIsAdmin(false);
        
        User savedUser = userRepository.save(user);
        
        // Generate JWT token
        String token = tokenProvider.generateTokenFromUsername(savedUser.getEmail());
        
        UserDto userDto = new UserDto(
            savedUser.getId(),
            savedUser.getUsername(),
            savedUser.getEmail(),
            savedUser.getIsAdmin()
        );
        
        return new AuthResponse(token, userDto);
    }
    
    public AuthResponse login(AuthRequest authRequest) {
        // Normalize email
        String email = StringUtils.lowerCase(StringUtils.trimToEmpty(authRequest.getEmail()));
        
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(email, authRequest.getPassword())
        );
        
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        String jwt = tokenProvider.generateToken(authentication);
        
        User user = userDetailsService.getUserByEmail(email);
        UserDto userDto = new UserDto(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getIsAdmin()
        );
        
        return new AuthResponse(jwt, userDto);
    }
    
    public UserDto getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }
        
        String email = authentication.getName();
        User user = userDetailsService.getUserByEmail(email);
        
        return new UserDto(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getIsAdmin()
        );
    }
    
    public void changePassword(ChangePasswordRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }
        
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        // Validate current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new RuntimeException("현재 비밀번호가 올바르지 않습니다.");
        }
        
        // Validate new password is different
        if (request.getCurrentPassword().equals(request.getNewPassword())) {
            throw new RuntimeException("새 비밀번호는 기존 비밀번호와 달라야 합니다.");
        }
        
        // Update password
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
    
    private String generateUniqueUsername(String baseUsername) {
        String username = StringUtils.trimToEmpty(baseUsername);
        int suffix = 1;
        
        while (userRepository.existsByUsername(username)) {
            username = baseUsername + "-" + suffix;
            suffix++;
        }
        
        return username;
    }
}
