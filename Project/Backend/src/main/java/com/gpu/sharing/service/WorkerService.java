package com.gpu.sharing.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
public class WorkerService {
    
    @Autowired
    private WebClient webClient;
    
    @Value("${worker.api-port}")
    private Integer workerApiPort;
    
    public Mono<Map<String, Object>> createContainer(String serverUrl, String imageName) {
        Map<String, Object> request = Map.of("image", imageName);
        
        return webClient.post()
                .uri(serverUrl + "/containers")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {})
                .onErrorMap(e -> new RuntimeException("Failed to create container: " + e.getMessage()));
    }
    
    public Mono<Map<String, Object>> getContainers(String serverUrl) {
        return webClient.get()
                .uri(serverUrl + "/containers")
                .retrieve()
                .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {})
                .onErrorMap(e -> new RuntimeException("Failed to get containers: " + e.getMessage()));
    }
    
    public Mono<Map<String, Object>> deleteContainer(String serverUrl, String containerId) {
        return webClient.delete()
                .uri(serverUrl + "/containers/" + containerId)
                .retrieve()
                .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {})
                .onErrorMap(e -> new RuntimeException("Failed to delete container: " + e.getMessage()));
    }
    
    public String buildWorkerUrl(String ipAddress) {
        return "http://" + ipAddress + ":" + workerApiPort;
    }
}
