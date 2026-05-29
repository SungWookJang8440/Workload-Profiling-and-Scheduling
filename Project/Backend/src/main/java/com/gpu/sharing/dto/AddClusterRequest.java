package com.gpu.sharing.dto;

import jakarta.validation.constraints.NotBlank;

public class AddClusterRequest {
    
    @NotBlank(message = "Machine name is required")
    private String machineName;
    
    private String ipAddress;
    private String description;
    
    // Constructors
    public AddClusterRequest() {}
    
    // Getters and Setters
    public String getMachineName() {
        return machineName;
    }
    
    public void setMachineName(String machineName) {
        this.machineName = machineName;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
}
