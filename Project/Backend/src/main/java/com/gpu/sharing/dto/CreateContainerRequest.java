package com.gpu.sharing.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateContainerRequest {
    
    @NotBlank(message = "Image name is required")
    private String imageName;
    
    // Constructors
    public CreateContainerRequest() {}
    
    public CreateContainerRequest(String imageName) {
        this.imageName = imageName;
    }
    
    // Getters and Setters
    public String getImageName() {
        return imageName;
    }
    
    public void setImageName(String imageName) {
        this.imageName = imageName;
    }
}
