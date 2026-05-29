package com.gpu.sharing.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
public class RelayService {
    
    @Autowired
    private WebClient webClient;
    
    @Value("${relay.server-url}")
    private String relayServerUrl;
    
    public Mono<Map<String, Object>> allocateWorkerServer(Long userId, String imageName) {
        Map<String, Object> request = Map.of(
            "user_id", userId,
            "image_name", imageName
        );
        
        return webClient.post()
                .uri(relayServerUrl + "/allocate_server")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {})
                .onErrorMap(e -> new RuntimeException("Failed to allocate worker server: " + e.getMessage()));
    }
}
