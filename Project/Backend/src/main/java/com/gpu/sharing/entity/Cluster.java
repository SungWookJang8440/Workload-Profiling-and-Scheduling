package com.gpu.sharing.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "clusters")
@EntityListeners(AuditingEntityListener.class)
public class Cluster {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(length = 500)
    private String description;
    
    @Column(name = "ip_address", nullable = false)
    private String ipAddress;
    
    @Column(name = "ssh_port", nullable = false)
    private Integer sshPort = 22;
    
    @Column(name = "gpu_name", length = 100)
    private String gpuName;
    
    @Column(name = "gpu_count", nullable = false)
    private Integer gpuCount = 1;
    
    @Column(name = "gpu_vram_gb")
    private Integer gpuVramGb;
    
    @Column(name = "specs", columnDefinition = "TEXT")
    @Convert(converter = MapJsonConverter.class)
    private Map<String, Object> specs = Map.of();
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(nullable = false, length = 20)
    private String status = "READY";
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    // Constructors
    public Cluster() {}
    
    public Cluster(String name, String ipAddress) {
        this.name = name;
        this.ipAddress = ipAddress;
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
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    public Integer getSshPort() {
        return sshPort;
    }
    
    public void setSshPort(Integer sshPort) {
        this.sshPort = sshPort;
    }
    
    public String getGpuName() {
        return gpuName;
    }
    
    public void setGpuName(String gpuName) {
        this.gpuName = gpuName;
    }
    
    public Integer getGpuCount() {
        return gpuCount;
    }
    
    public void setGpuCount(Integer gpuCount) {
        this.gpuCount = gpuCount;
    }
    
    public Integer getGpuVramGb() {
        return gpuVramGb;
    }
    
    public void setGpuVramGb(Integer gpuVramGb) {
        this.gpuVramGb = gpuVramGb;
    }
    
    public Map<String, Object> getSpecs() {
        return specs;
    }
    
    public void setSpecs(Map<String, Object> specs) {
        this.specs = specs;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
