package com.gpu.sharing.dto;

import java.util.List;

public class AddTemplatesRequest {
    
    private String imageName;
    private List<String> imageNames;
    
    // Constructors
    public AddTemplatesRequest() {}
    
    // Getters and Setters
    public String getImageName() {
        return imageName;
    }
    
    public void setImageName(String imageName) {
        this.imageName = imageName;
    }
    
    public List<String> getImageNames() {
        return imageNames;
    }
    
    public void setImageNames(List<String> imageNames) {
        this.imageNames = imageNames;
    }
}
