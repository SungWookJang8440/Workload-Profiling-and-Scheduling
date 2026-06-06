package com.gpu.sharing.controller;

import com.gpu.sharing.scheduler.GeminiParsingService;
import com.gpu.sharing.scheduler.QueueManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/scheduler")
public class SchedulerController {

    @Autowired
    private QueueManager queueManager;

    @Autowired
    private GeminiParsingService geminiParsingService;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(queueManager.getStatus());
    }

    public static class SubmitRequest {
        private String prompt;

        public String getPrompt() { return prompt; }
        public void setPrompt(String prompt) { this.prompt = prompt; }
    }

    @PostMapping("/submit")
    public ResponseEntity<Map<String, Object>> submitJob(@RequestBody SubmitRequest request) {
        String prompt = request.getPrompt();
        if (prompt == null || prompt.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Workload prompt cannot be empty"));
        }

        // Step 1: Map the natural language prompt to a workload ID (using Gemini / local fallback)
        String workloadId = geminiParsingService.parseWorkload(prompt);
        System.out.println("Mapped prompt '" + prompt + "' to workload ID: " + workloadId);

        // Step 2: Queue the job and compute MCDM scores and load-balancing decisions
        Map<String, Object> result = queueManager.submitJob(workloadId, prompt);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/tick")
    public ResponseEntity<Map<String, Object>> tickSimulation() {
        queueManager.tick();
        return ResponseEntity.ok(queueManager.getStatus());
    }

    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> resetSimulation() {
        queueManager.resetQueues();
        return ResponseEntity.ok(queueManager.getStatus());
    }
}
