package com.gpu.sharing.repository;

import com.gpu.sharing.entity.ContainerTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContainerTemplateRepository extends JpaRepository<ContainerTemplate, Long> {
    
    Optional<ContainerTemplate> findByImageName(String imageName);
    
    boolean existsByImageName(String imageName);
    
    List<ContainerTemplate> findAllByOrderByIdAsc();
}
