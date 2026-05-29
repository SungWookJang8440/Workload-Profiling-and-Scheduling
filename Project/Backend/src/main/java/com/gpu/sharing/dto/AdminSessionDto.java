package com.gpu.sharing.dto;

import java.time.LocalDateTime;

public class AdminSessionDto {
    private Long id;
    private Long userId;
    private String ownerEmail;
    private String ownerName;
    private Long clusterId;
    private String containerId;
    private String imageName;
    private Integer sshPortMapped;
    private Integer jupyterPortMapped;
    private String sshCommand;
    private String sshPassword;
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime stoppedAt;
    
    public AdminSessionDto(Long id, Long userId, String ownerEmail, String ownerName,
                          Long clusterId, String containerId, String imageName,
                          Integer sshPortMapped, Integer jupyterPortMapped,
                          String sshCommand, String sshPassword, String status,
                          LocalDateTime startedAt, LocalDateTime stoppedAt) {
        this.id = id;
        this.userId = userId;
        this.ownerEmail = ownerEmail;
        this.ownerName = ownerName;
        this.clusterId = clusterId;
        this.containerId = containerId;
        this.imageName = imageName;
        this.sshPortMapped = sshPortMapped;
        this.jupyterPortMapped = jupyterPortMapped;
        this.sshCommand = sshCommand;
        this.sshPassword = sshPassword;
        this.status = status;
        this.startedAt = startedAt;
        this.stoppedAt = stoppedAt;
    }
    
    // Getters
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getOwnerEmail() { return ownerEmail; }
    public String getOwnerName() { return ownerName; }
    public Long getClusterId() { return clusterId; }
    public String getContainerId() { return containerId; }
    public String getImageName() { return imageName; }
    public Integer getSshPortMapped() { return sshPortMapped; }
    public Integer getJupyterPortMapped() { return jupyterPortMapped; }
    public String getSshCommand() { return sshCommand; }
    public String getSshPassword() { return sshPassword; }
    public String getStatus() { return status; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getStoppedAt() { return stoppedAt; }
}
