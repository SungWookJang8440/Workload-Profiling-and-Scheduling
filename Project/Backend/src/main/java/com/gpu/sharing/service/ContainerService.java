package com.gpu.sharing.service;

import com.gpu.sharing.dto.CreateContainerRequest;
import com.gpu.sharing.entity.Session;
import com.gpu.sharing.entity.Cluster;
import com.gpu.sharing.repository.ClusterRepository;
import com.gpu.sharing.repository.SessionRepository;
import com.gpu.sharing.security.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class ContainerService {
    
    @Autowired
    private RelayService relayService;
    
    @Autowired
    private WorkerService workerService;
    
    @Autowired
    private SessionRepository sessionRepository;
    
    @Autowired
    private ClusterRepository clusterRepository;
    
    @Autowired
    private CustomUserDetailsService userDetailsService;
    
    public Map<String, Object> createContainer(CreateContainerRequest request, Long userId) {
        try {
            // Step 1: Allocate worker server from relay
            Map<String, Object> allocation = relayService.allocateWorkerServer(userId, request.getImageName()).block();
            
            Long clusterId = ((Number) allocation.get("cluster_id")).longValue();
            String serverUrl = (String) allocation.get("server_url");
            
            // Step 2: Create container on worker
            Map<String, Object> containerResponse = workerService.createContainer(serverUrl, request.getImageName()).block();
            
            String containerId = (String) containerResponse.get("container_id");
            if (containerId == null) {
                containerId = (String) containerResponse.get("id");
            }
            
            String sshCommand = (String) containerResponse.get("ssh_command");
            String sshPassword = (String) containerResponse.get("ssh_password");
            Integer sshPort = (Integer) containerResponse.get("ssh_port");
            
            // Step 3: Save session to database
            Session session = new Session();
            session.setUserId(userId);
            session.setClusterId(clusterId);
            session.setContainerId(containerId);
            session.setImageName(request.getImageName());
            session.setSshPortMapped(sshPort);
            session.setSshCommand(sshCommand);
            session.setSshPassword(sshPassword);
            session.setStatus("STARTING");
            
            Session savedSession = sessionRepository.save(session);
            
            // Prepare response
            Map<String, Object> response = new HashMap<>(containerResponse);
            response.put("session_id", savedSession.getId());
            response.put("cluster_id", clusterId);
            response.put("server_url", serverUrl);
            
            return Map.of(
                "message", "Container created",
                "data", response
            );
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to create container: " + e.getMessage());
        }
    }
    
    public Map<String, Object> getContainers(Long userId) {
        try {
            // Reconcile sessions with workers first
            reconcileSessions(userId);
            
            List<Session> sessions = sessionRepository.findByUserIdOrderByStartedAtDesc(userId);
            
            return Map.of(
                "message", "All sessions",
                "data", sessions.stream().map(this::sessionToMap).toList()
            );
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to get containers: " + e.getMessage());
        }
    }
    
    public Map<String, Object> deleteContainer(String containerId, Long userId) {
        try {
            // Get session and cluster info
            Session session = sessionRepository.findByContainerIdAndUserId(containerId, userId)
                    .orElseThrow(() -> new RuntimeException("Container not found or access denied"));
            
            if (session.getClusterId() == null) {
                throw new RuntimeException("Session has no cluster information");
            }
            
            // Fetch cluster to get the actual IP
            Cluster cluster = clusterRepository.findById(session.getClusterId())
                    .orElseThrow(() -> new RuntimeException("Associated cluster not found"));
            
            // Build worker URL (we need cluster IP address)
            String workerUrl = workerService.buildWorkerUrl(cluster.getIpAddress());
            
            // Delete container from worker
            Map<String, Object> response = workerService.deleteContainer(workerUrl, containerId).block();
            
            // Remove session from database
            sessionRepository.deleteByContainerIdAndUserId(containerId, userId);
            
            return response;
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete container: " + e.getMessage());
        }
    }
    
    public Map<String, Object> getContainer(String containerId, Long userId) {
        Session session = sessionRepository.findByContainerIdAndUserId(containerId, userId)
                .orElseThrow(() -> new RuntimeException("Session not found for given container_id"));
        
        return Map.of(
            "message", "Session for container",
            "data", sessionToMap(session)
        );
    }
    
    public Map<String, Object> reconcileSessions(Long userId) {
        List<String> deleted = new java.util.ArrayList<>();
        List<String> errors = new java.util.ArrayList<>();
        
        List<Object[]> containersWithClusters = sessionRepository.findContainersWithClustersByUserId(userId);
        
        for (Object[] row : containersWithClusters) {
            String containerId = (String) row[0];
            Long clusterId = (Long) row[1];
            String ipAddress = (String) row[2];
            
            if (clusterId == null || ipAddress == null || containerId == null) {
                continue;
            }
            
            try {
                String workerUrl = workerService.buildWorkerUrl(ipAddress);
                Map<String, Object> containersResponse = workerService.getContainers(workerUrl).block();
                
                @SuppressWarnings("unchecked")
                List<String> liveContainers = (List<String>) containersResponse.get("data");
                
                if (liveContainers == null || !liveContainers.contains(containerId)) {
                    sessionRepository.deleteByContainerIdAndUserId(containerId, userId);
                    deleted.add(containerId);
                }
            } catch (Exception e) {
                errors.add("cluster_id=" + clusterId + " 조회 실패: " + e.getMessage());
            }
        }
        
        return Map.of(
            "ok", errors.isEmpty(),
            "deleted_container_ids", deleted,
            "errors", errors
        );
    }
    
    private Map<String, Object> sessionToMap(Session session) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", session.getId());
        map.put("user_id", session.getUserId());
        map.put("cluster_id", session.getClusterId());
        map.put("container_id", session.getContainerId());
        map.put("image_name", session.getImageName());
        map.put("ssh_port_mapped", session.getSshPortMapped());
        map.put("jupyter_port_mapped", session.getJupyterPortMapped());
        map.put("ssh_command", session.getSshCommand());
        map.put("ssh_password", session.getSshPassword());
        map.put("status", session.getStatus());
        map.put("started_at", session.getStartedAt());
        map.put("stopped_at", session.getEndedAt());
        return map;
    }
}
