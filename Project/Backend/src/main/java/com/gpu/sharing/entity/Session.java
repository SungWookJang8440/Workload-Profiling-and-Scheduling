package com.gpu.sharing.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "sessions")
@EntityListeners(AuditingEntityListener.class)
public class Session {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id")
    private Long userId;
    
    @Column(name = "cluster_id")
    private Long clusterId;
    
    @Column(name = "container_id", length = 100)
    private String containerId;
    
    @Column(name = "image_name", nullable = false, length = 200)
    private String imageName;
    
    @Column(name = "ssh_port_mapped")
    private Integer sshPortMapped;
    
    @Column(name = "jupyter_port_mapped")
    private Integer jupyterPortMapped;
    
    @Column(name = "ssh_command", length = 150)
    private String sshCommand;
    
    @Column(name = "ssh_password", length = 100)
    private String sshPassword;
    
    @Column(nullable = false, length = 20)
    private String status = "STARTING";
    
    @Column(name = "error_msg", columnDefinition = "TEXT")
    private String errorMsg;
    
    @CreatedDate
    @Column(name = "started_at", nullable = false, updatable = false)
    private LocalDateTime startedAt;
    
    @Column(name = "ended_at")
    private LocalDateTime endedAt;
    
    @Column(name = "uptime_seconds")
    private Long uptimeSeconds = 0L;
    
    // Constructors
    public Session() {}
    
    public Session(Long userId, String imageName) {
        this.userId = userId;
        this.imageName = imageName;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public Long getClusterId() {
        return clusterId;
    }
    
    public void setClusterId(Long clusterId) {
        this.clusterId = clusterId;
    }
    
    public String getContainerId() {
        return containerId;
    }
    
    public void setContainerId(String containerId) {
        this.containerId = containerId;
    }
    
    public String getImageName() {
        return imageName;
    }
    
    public void setImageName(String imageName) {
        this.imageName = imageName;
    }
    
    public Integer getSshPortMapped() {
        return sshPortMapped;
    }
    
    public void setSshPortMapped(Integer sshPortMapped) {
        this.sshPortMapped = sshPortMapped;
    }
    
    public Integer getJupyterPortMapped() {
        return jupyterPortMapped;
    }
    
    public void setJupyterPortMapped(Integer jupyterPortMapped) {
        this.jupyterPortMapped = jupyterPortMapped;
    }
    
    public String getSshCommand() {
        return sshCommand;
    }
    
    public void setSshCommand(String sshCommand) {
        this.sshCommand = sshCommand;
    }
    
    public String getSshPassword() {
        return sshPassword;
    }
    
    public void setSshPassword(String sshPassword) {
        this.sshPassword = sshPassword;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getErrorMsg() {
        return errorMsg;
    }
    
    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }
    
    public LocalDateTime getStartedAt() {
        return startedAt;
    }
    
    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }
    
    public LocalDateTime getEndedAt() {
        return endedAt;
    }
    
    public void setEndedAt(LocalDateTime endedAt) {
        this.endedAt = endedAt;
    }
    
    public Long getUptimeSeconds() {
        return uptimeSeconds;
    }
    
    public void setUptimeSeconds(Long uptimeSeconds) {
        this.uptimeSeconds = uptimeSeconds;
    }
}
