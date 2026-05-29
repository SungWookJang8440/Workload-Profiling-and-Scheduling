package com.gpu.sharing.controller;

import com.gpu.sharing.dto.AddClusterRequest;
import com.gpu.sharing.entity.Cluster;
import com.gpu.sharing.repository.ClusterRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
public class ClusterController {
    
    @Autowired
    private ClusterRepository clusterRepository;
    
    @GetMapping("/get_clusters")
    public ResponseEntity<Map<String, Object>> getClusters() {
        List<Cluster> clusters = clusterRepository.findAllByOrderByIdAsc();
        
        List<Map<String, Object>> clusterList = clusters.stream()
            .map(c -> {
                Map<String, Object> map = new java.util.HashMap<>();
                map.put("id", c.getId());
                map.put("name", c.getName());
                map.put("ip_address", c.getIpAddress());
                map.put("ssh_port", c.getSshPort());
                map.put("gpu_name", c.getGpuName() != null ? c.getGpuName() : "");
                map.put("gpu_count", c.getGpuCount());
                map.put("gpu_vram_gb", c.getGpuVramGb() != null ? c.getGpuVramGb() : 0);
                map.put("specs", c.getSpecs() != null ? c.getSpecs() : Map.of());
                map.put("is_active", c.getIsActive());
                map.put("status", c.getStatus());
                map.put("created_at", c.getCreatedAt() != null ? c.getCreatedAt().toString() : null);
                return map;
            })
            .toList();
        
        return ResponseEntity.ok(Map.of("clusters", clusterList));
    }
    
    @PostMapping("/add_clusters")
    public ResponseEntity<Map<String, Object>> addCluster(@Valid @RequestBody AddClusterRequest request) {
        String machineName = request.getMachineName() != null ? request.getMachineName().trim() : "";
        String ipAddress = request.getIpAddress() != null ? request.getIpAddress().trim() : "";
        
        if (machineName.isEmpty()) {
            throw new RuntimeException("추가할 머신 이름이 없습니다. machine_name을 전달하세요.");
        }
        
        List<String> added = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        
        if (clusterRepository.existsByNameAndIpAddress(machineName, ipAddress.isEmpty() ? machineName : ipAddress)) {
            skipped.add(machineName);
        } else {
            Cluster cluster = new Cluster();
            cluster.setName(machineName);
            cluster.setIpAddress(ipAddress.isEmpty() ? machineName : ipAddress);
            if (request.getDescription() != null) {
                cluster.setDescription(request.getDescription());
            }
            cluster.setStatus("READY");
            cluster.setIsActive(true);
            clusterRepository.save(cluster);
            added.add(machineName);
        }
        
        return ResponseEntity.ok(Map.of(
            "added", added,
            "skipped", skipped
        ));
    }
}
