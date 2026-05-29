package com.gpu.sharing.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "container_templates")
public class ContainerTemplate {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "image_name", nullable = false, length = 200)
    private String imageName;
    
    // Constructors
    public ContainerTemplate() {}
    
    public ContainerTemplate(String imageName) {
        this.imageName = imageName;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getImageName() {
        return imageName;
    }
    
    public void setImageName(String imageName) {
        this.imageName = imageName;
    }
}
