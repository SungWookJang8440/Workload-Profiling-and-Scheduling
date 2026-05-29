package com.gpu.sharing.controller;

import com.gpu.sharing.dto.AdminSessionDto;
import com.gpu.sharing.repository.SessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AdminController {
    
    @Autowired
    private SessionRepository sessionRepository;
    
    @GetMapping("/containers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getAllContainers() {
        List<AdminSessionDto> sessions = sessionRepository.findAllForAdmin();
        return ResponseEntity.ok(Map.of(
            "message", "All sessions (admin)",
            "data", sessions
        ));
    }
}
