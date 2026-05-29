package com.gpu.sharing.controller;

import com.gpu.sharing.dto.CreateContainerRequest;
import com.gpu.sharing.entity.User;
import com.gpu.sharing.repository.UserRepository;
import com.gpu.sharing.service.ContainerService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class ContainerController {
    
    @Autowired
    private ContainerService containerService;
    
    @Autowired
    private UserRepository userRepository;
    
    @PostMapping("/create_container")
    public ResponseEntity<Map<String, Object>> createContainer(
            @Valid @RequestBody CreateContainerRequest request,
            Authentication authentication) {
        
        Long userId = getUserIdFromAuthentication(authentication);
        Map<String, Object> response = containerService.createContainer(request, userId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/get_containers")
    public ResponseEntity<Map<String, Object>> getContainers(Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        Map<String, Object> response = containerService.getContainers(userId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/get_container/{containerId}")
    public ResponseEntity<Map<String, Object>> getContainer(
            @PathVariable String containerId,
            Authentication authentication) {
        
        Long userId = getUserIdFromAuthentication(authentication);
        Map<String, Object> response = containerService.getContainer(containerId, userId);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/delete_container/{containerId}")
    public ResponseEntity<Map<String, Object>> deleteContainer(
            @PathVariable String containerId,
            Authentication authentication) {
        
        Long userId = getUserIdFromAuthentication(authentication);
        Map<String, Object> response = containerService.deleteContainer(containerId, userId);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/reconcile_sessions")
    public ResponseEntity<Map<String, Object>> reconcileSessions(Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        Map<String, Object> response = containerService.reconcileSessions(userId);
        return ResponseEntity.ok(response);
    }
    
    private Long getUserIdFromAuthentication(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.getId();
    }
}
