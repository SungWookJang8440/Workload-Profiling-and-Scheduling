package com.gpu.sharing.dto;

public class AuthResponse {
    
    private String accessToken;
    private String tokenType = "bearer";
    private UserDto user;
    
    // Constructors
    public AuthResponse() {}
    
    public AuthResponse(String accessToken, UserDto user) {
        this.accessToken = accessToken;
        this.user = user;
    }
    
    // Getters and Setters
    public String getAccessToken() {
        return accessToken;
    }
    
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
    
    public String getTokenType() {
        return tokenType;
    }
    
    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }
    
    public UserDto getUser() {
        return user;
    }
    
    public void setUser(UserDto user) {
        this.user = user;
    }
}
