package com.gpu.sharing.dto;

import java.time.LocalDateTime;

public class UserDto {
    
    private Long id;
    private String name;
    private String email;
    private Boolean isAdmin;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Constructors
    public UserDto() {}
    
    public UserDto(Long id, String name, String email, Boolean isAdmin) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.isAdmin = isAdmin;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public Boolean getIsAdmin() {
        return isAdmin;
    }
    
    public void setIsAdmin(Boolean isAdmin) {
        this.isAdmin = isAdmin;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
