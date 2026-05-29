package com.gpu.sharing.controller;

import com.gpu.sharing.dto.AddTemplatesRequest;
import com.gpu.sharing.entity.ContainerTemplate;
import com.gpu.sharing.repository.ContainerTemplateRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
public class TemplateController {
    
    @Autowired
    private ContainerTemplateRepository templateRepository;
    
    @GetMapping("/get_templates")
    public ResponseEntity<Map<String, Object>> getTemplates() {
        List<ContainerTemplate> templates = templateRepository.findAllByOrderByIdAsc();
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> templateList = (List<Map<String, Object>>) (Object) templates.stream()
            .map(t -> Map.of(
                "id", t.getId(),
                "image_name", t.getImageName()
            ))
            .toList();
        
        return ResponseEntity.ok(Map.of("templates", templateList));
    }
    
    @PostMapping("/add_templates")
    public ResponseEntity<Map<String, Object>> addTemplates(@Valid @RequestBody AddTemplatesRequest request) {
        // Collect all image names
        Set<String> candidates = new LinkedHashSet<>();
        if (request.getImageName() != null && !request.getImageName().trim().isEmpty()) {
            candidates.add(request.getImageName().trim());
        }
        if (request.getImageNames() != null) {
            for (String name : request.getImageNames()) {
                if (name != null && !name.trim().isEmpty()) {
                    candidates.add(name.trim());
                }
            }
        }
        
        if (candidates.isEmpty()) {
            throw new RuntimeException("추가할 이미지 이름이 없습니다. image_name 또는 image_names를 전달하세요.");
        }
        
        List<String> added = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        
        for (String imageName : candidates) {
            if (templateRepository.existsByImageName(imageName)) {
                skipped.add(imageName);
            } else {
                ContainerTemplate template = new ContainerTemplate(imageName);
                templateRepository.save(template);
                added.add(imageName);
            }
        }
        
        return ResponseEntity.ok(Map.of(
            "added", added,
            "skipped", skipped
        ));
    }
}
