package com.gpu.sharing.repository;

import com.gpu.sharing.dto.AdminSessionDto;
import com.gpu.sharing.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {
    
    List<Session> findByUserIdOrderByStartedAtDesc(Long userId);
    
    Optional<Session> findByContainerIdAndUserId(String containerId, Long userId);
    
    List<Session> findByClusterId(Long clusterId);
    
    List<Session> findByUserIdAndContainerIdIsNotNull(Long userId);
    
    @Query("SELECT s.containerId, s.clusterId, c.ipAddress " +
           "FROM Session s " +
           "LEFT JOIN Cluster c ON c.id = s.clusterId " +
           "WHERE s.userId = :userId AND s.containerId IS NOT NULL")
    List<Object[]> findContainersWithClustersByUserId(@Param("userId") Long userId);
    
    @Query("SELECT new com.gpu.sharing.dto.AdminSessionDto(" +
           "s.id, s.userId, u.email, u.username, s.clusterId, s.containerId, " +
           "s.imageName, s.sshPortMapped, s.jupyterPortMapped, s.sshCommand, " +
           "s.sshPassword, s.status, s.startedAt, s.endedAt) " +
           "FROM Session s LEFT JOIN User u ON u.id = s.userId ORDER BY s.id DESC")
    List<AdminSessionDto> findAllForAdmin();
    
    void deleteByContainerIdAndUserId(String containerId, Long userId);
    
    void deleteByContainerId(String containerId);
}
