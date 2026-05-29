package com.gpu.sharing.repository;

import com.gpu.sharing.entity.Cluster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClusterRepository extends JpaRepository<Cluster, Long> {
    
    List<Cluster> findByStatus(String status);
    
    List<Cluster> findByIsActiveTrue();
    
    List<Cluster> findByIsActiveTrueAndStatus(String status);
    
    @Query("SELECT c FROM Cluster c WHERE c.status = 'READY' AND c.isActive = true ORDER BY c.id ASC")
    List<Cluster> findReadyClusters();
    
    List<Cluster> findAllByOrderByIdAsc();
    
    boolean existsByNameAndIpAddress(String name, String ipAddress);
}
