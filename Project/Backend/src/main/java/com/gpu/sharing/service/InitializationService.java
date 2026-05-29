package com.gpu.sharing.service;

import com.gpu.sharing.entity.Cluster;
import com.gpu.sharing.entity.ContainerTemplate;
import com.gpu.sharing.repository.ClusterRepository;
import com.gpu.sharing.repository.ContainerTemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
public class InitializationService implements CommandLineRunner {
    
    @Autowired
    private ContainerTemplateRepository containerTemplateRepository;
    
    @Autowired
    private ClusterRepository clusterRepository;
    
    private final List<String> defaultImages = Arrays.asList(
        "python:3.8.17-slim",
        "python:3.11-slim",
        "pytorch/pytorch:2.0.1-cuda11.7-cudnn8-runtime",
        "tensorflow/tensorflow:2.11.0-gpu",
        "nvidia/cuda:11.8.0-cudnn8-devel-ubuntu22.04",
        "jupyter/datascience-notebook:latest",
        "pytorch/pytorch:1.13.1-cuda11.6-cudnn8-runtime",
        "continuumio/miniconda3:latest",
        "ubuntu:22.04",
        "debian:bullseye-slim"
    );
    
    private final List<Cluster> defaultMachines = Arrays.asList(
        new Cluster("localhost", "127.0.0.1"),
        new Cluster("pod1", "155.230.118.69"),
        new Cluster("pod2", "155.230.118.69"),
        new Cluster("pod3", "155.230.118.69")
    );
    
    @Override
    @Transactional
    public void run(String... args) throws Exception {
        initializeContainerTemplates();
        initializeClusters();
    }
    
    private void initializeContainerTemplates() {
        for (String imageName : defaultImages) {
            if (!containerTemplateRepository.existsByImageName(imageName)) {
                ContainerTemplate template = new ContainerTemplate(imageName);
                containerTemplateRepository.save(template);
            }
        }
        System.out.println("Container templates initialized successfully.");
    }
    
    private void initializeClusters() {
        for (Cluster cluster : defaultMachines) {
            cluster.setDescription("sslab_machine");
            cluster.setStatus("READY");
            cluster.setIsActive(true);
            
            if (!clusterRepository.existsByNameAndIpAddress(cluster.getName(), cluster.getIpAddress())) {
                clusterRepository.save(cluster);
            }
        }
        System.out.println("Clusters initialized successfully.");
    }
}
