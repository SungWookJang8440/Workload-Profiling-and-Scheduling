package com.gpu.sharing.controller;

import com.gpu.sharing.scheduler.GeminiParsingService;
import com.gpu.sharing.scheduler.GpuNodeRegistry;
import com.gpu.sharing.scheduler.GpuWorkerClient;
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

    @Autowired
    private GpuNodeRegistry gpuNodeRegistry;

    @Autowired
    private GpuWorkerClient gpuWorkerClient;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(queueManager.getStatus());
    }

    public static class SubmitRequest {
        private String prompt;

        public String getPrompt() { return prompt; }
        public void setPrompt(String prompt) { this.prompt = prompt; }
    }

    public static class ExecuteRequest {
        private String workloadId;
        private String gpuId;

        public String getWorkloadId() { return workloadId; }
        public void setWorkloadId(String workloadId) { this.workloadId = workloadId; }
        public String getGpuId() { return gpuId; }
        public void setGpuId(String gpuId) { this.gpuId = gpuId; }
    }

    @PostMapping("/analyze")
    public ResponseEntity<Map<String, Object>> analyzeJob(@RequestBody SubmitRequest request) {
        String prompt = request.getPrompt();
        if (prompt == null || prompt.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Workload prompt cannot be empty"));
        }
        String workloadId = geminiParsingService.parseWorkload(prompt);
        Map<String, Object> result = queueManager.analyzeJob(workloadId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/execute")
    public ResponseEntity<Map<String, Object>> executeJob(@RequestBody ExecuteRequest request) {
        String workloadId = request.getWorkloadId();
        String gpuId = request.getGpuId();
        if (workloadId == null || gpuId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "workloadId and gpuId are required"));
        }
        Map<String, Object> result = queueManager.executeJob(workloadId, gpuId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/submit")
    public ResponseEntity<Map<String, Object>> submitJob(@RequestBody SubmitRequest request) {
        String prompt = request.getPrompt();
        if (prompt == null || prompt.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Workload prompt cannot be empty"));
        }

        String workloadId = geminiParsingService.parseWorkload(prompt);
        System.out.println("Mapped prompt '" + prompt + "' to workload ID: " + workloadId);

        Map<String, Object> analysis = queueManager.analyzeJob(workloadId);
        String recommendedGpuId = (String) analysis.get("recommended_gpu_id");
        Map<String, Object> execution = queueManager.executeJob(workloadId, recommendedGpuId);

        Map<String, Object> result = new java.util.HashMap<>(analysis);
        result.putAll(execution);
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

    /**
     * GET /scheduler/metrics/{gpuId}
     * RTX 6000 Worker 서버에서 실시간 nvidia-smi 메트릭을 가져와 프론트엔드에 전달합니다.
     * gpuId: g2 (RTX 6000)
     */
    @GetMapping("/metrics/{gpuId}")
    public ResponseEntity<?> getGpuMetrics(@PathVariable String gpuId) {
        if (!gpuNodeRegistry.isRealGpu(gpuId)) {
            return ResponseEntity.ok(Map.of(
                "real_gpu", false,
                "message", "이 GPU는 실제 서버에 연결되지 않았습니다. (시뮬레이션 모드)"
            ));
        }

        String workerUrl = gpuNodeRegistry.getWorkerUrl(gpuId);
        Map<String, Object> metrics = gpuWorkerClient.getMetrics(workerUrl);

        if (metrics == null) {
            return ResponseEntity.ok(Map.of(
                "real_gpu", true,
                "connected", false,
                "message", "Worker 서버에 연결할 수 없습니다. " + workerUrl
            ));
        }

        metrics.put("real_gpu", true);
        metrics.put("connected", true);
        metrics.put("worker_url", workerUrl);
        return ResponseEntity.ok(metrics);
    }

    /**
     * GET /scheduler/gpu-status/{gpuId}
     * Worker 서버에서 현재 실행 중인 작업 상태를 조회합니다.
     */
    @GetMapping("/gpu-status/{gpuId}")
    public ResponseEntity<?> getGpuJobStatus(@PathVariable String gpuId) {
        if (!gpuNodeRegistry.isRealGpu(gpuId)) {
            return ResponseEntity.ok(Map.of("real_gpu", false));
        }

        String workerUrl = gpuNodeRegistry.getWorkerUrl(gpuId);
        Map<String, Object> status = gpuWorkerClient.getJobStatus(workerUrl);

        if (status == null) {
            return ResponseEntity.ok(Map.of("real_gpu", true, "connected", false));
        }

        status.put("real_gpu", true);
        status.put("connected", true);
        return ResponseEntity.ok(status);
    }
}
