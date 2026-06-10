package com.gpu.sharing.scheduler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class QueueManager {

    public static class QueuedJob {
        private final String jobId;
        private final String workloadId;
        private final String workloadName;
        private final double duration; 
        private double remainingTime; 
        private final String submittedAt;

        public QueuedJob(String jobId, String workloadId, String workloadName, double duration) {
            this.jobId = jobId;
            this.workloadId = workloadId;
            this.workloadName = workloadName;
            this.duration = duration;
            this.remainingTime = duration;
            this.submittedAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        }

        public String getJobId() { return jobId; }
        public String getWorkloadId() { return workloadId; }
        public String getWorkloadName() { return workloadName; }
        public double getDuration() { return duration; }
        public double getRemainingTime() { return remainingTime; }
        public void setRemainingTime(double remainingTime) { this.remainingTime = remainingTime; }
        public String getSubmittedAt() { return submittedAt; }
    }

    public static class GpuQueueState {
        private final String gpuId;
        private final String gpuName;
        private final List<QueuedJob> queue;
        private double totalPendingTime;

        public GpuQueueState(String gpuId, String gpuName) {
            this.gpuId = gpuId;
            this.gpuName = gpuName;
            this.queue = new CopyOnWriteArrayList<>();
            this.totalPendingTime = 0.0;
        }

        public String getGpuId() { return gpuId; }
        public String getGpuName() { return gpuName; }
        public List<QueuedJob> getQueue() { return queue; }
        public double getTotalPendingTime() { return totalPendingTime; }
        public void recalculatePendingTime() {
            this.totalPendingTime = this.queue.stream().mapToDouble(QueuedJob::getRemainingTime).sum();
        }
    }

    private final Map<String, GpuQueueState> gpuStates = new ConcurrentHashMap<>();
    private final List<String> decisionLogs = new CopyOnWriteArrayList<>();
    private final McdmScheduler scheduler = new McdmScheduler();

    @Autowired
    private GpuNodeRegistry gpuNodeRegistry;

    @Autowired
    private GpuWorkerClient gpuWorkerClient;
    private int jobCounter = 100;

    public QueueManager() {
        resetQueues();
    }

    public synchronized void resetQueues() {
        gpuStates.clear();
        decisionLogs.clear();

        gpuStates.put("g0", new GpuQueueState("g0", "RTX 3090"));
        gpuStates.put("g1", new GpuQueueState("g1", "RTX 4090"));
        gpuStates.put("g2", new GpuQueueState("g2", "RTX 6000"));

        gpuStates.get("g0").recalculatePendingTime();
        gpuStates.get("g1").recalculatePendingTime();
        gpuStates.get("g2").recalculatePendingTime();

        decisionLogs.add("[시스템 초기화] GPU 노드 대기열이 비어있는 상태로 초기화되었습니다.");
    }

    public synchronized Map<String, Object> analyzeJob(String workloadId, String originalInput) {
        String jobName = SchedulerData.WORKLOADS.stream()
                .filter(w -> w.getId().equals(workloadId))
                .map(SchedulerData.Workload::getName)
                .findFirst()
                .orElse("Custom Workload");

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

        Map<String, Double> dynamicThroughputs = new HashMap<>();
        Map<String, Map<String, Object>> timeMetrics = new HashMap<>();

        for (SchedulerData.GPU gpu : SchedulerData.GPUS) {
            String gId = gpu.getId();
            double staticThroughput = SchedulerData.PERF_MATRIX.getOrDefault(workloadId, Map.of()).getOrDefault(gId, 0.0);
            double ttc = SchedulerData.LATENCY_MATRIX.getOrDefault(workloadId, Map.of()).getOrDefault(gId, 1.0);
            
            double tte = gpuStates.get(gId).getTotalPendingTime();
            double totalTime = tte + ttc; 

            double totalSamples = staticThroughput * ttc;
            double dynamicThroughput = (totalTime > 0) ? (totalSamples / totalTime) : 0.0;
            dynamicThroughputs.put(gId, dynamicThroughput);

            LocalDateTime etaTime = LocalDateTime.now().plusSeconds((long) totalTime);
            String eta = etaTime.format(timeFormatter);

            Map<String, Object> metrics = new HashMap<>();
            metrics.put("tte", Math.round(tte * 100.0) / 100.0);
            metrics.put("ttc", Math.round(ttc * 100.0) / 100.0);
            metrics.put("total", Math.round(totalTime * 100.0) / 100.0);
            metrics.put("eta", eta);
            timeMetrics.put(gId, metrics);
        }

        boolean useCostPriority = false;
        if (originalInput != null) {
            String lowerInput = originalInput.toLowerCase().trim();
            if (lowerInput.contains("비용") || lowerInput.contains("cost") || lowerInput.contains("저렴")) {
                useCostPriority = true;
            }
        }

        List<McdmScheduler.ScoreDetail> scores = scheduler.computeScores(workloadId, dynamicThroughputs, useCostPriority);
        
        McdmScheduler.ScoreDetail bestMcdm = scores.get(0);
        String chosenGpuId = bestMcdm.getGpu().getId();
        String chosenGpuName = bestMcdm.getGpu().getName();
        double bestMcdmScore = bestMcdm.getSTotal();

        double chosenGpuTtc = (double) timeMetrics.get(chosenGpuId).get("ttc");
        double chosenGpuTte = (double) timeMetrics.get(chosenGpuId).get("tte");

        String decisionLog;
        if (chosenGpuTte > 0) {
            decisionLog = String.format(
                "[%s] %s 작업이 분석되었습니다. 각 GPU의 실시간 대기열(Queue) 시간을 성능 패널티로 환산하여 MCDM 알고리즘을 분석한 결과, 최종적으로 %s 노드가 최적(점수: %.0f점, 대기 시간: %.1f초)으로 판단되었습니다.",
                LocalDateTime.now().format(timeFormatter), jobName, chosenGpuName, bestMcdmScore, chosenGpuTte
            );
        } else {
            decisionLog = String.format(
                "[%s] %s 작업이 분석되었습니다. 현재 즉시 실행이 가능한 %s 노드(점수: %.0f점)가 최적으로 판단되었습니다.",
                LocalDateTime.now().format(timeFormatter), jobName, chosenGpuName, bestMcdmScore
            );
        }

        Map<String, Object> result = new HashMap<>();
        result.put("workload_id", workloadId);
        result.put("workload_name", jobName);
        result.put("recommended_gpu", chosenGpuName);
        result.put("recommended_gpu_id", chosenGpuId);
        result.put("recommended_gpu_ttc", chosenGpuTtc);
        result.put("mcdm_scores", scores);
        result.put("time_metrics", timeMetrics);
        result.put("decision_log", decisionLog);
        result.put("use_cost_priority", useCostPriority);

        return result;
    }

    public synchronized Map<String, Object> commitJob(String workloadId, String originalInput, String chosenGpuId, double chosenGpuTtc) {
        String jobName = SchedulerData.WORKLOADS.stream()
                .filter(w -> w.getId().equals(workloadId))
                .map(SchedulerData.Workload::getName)
                .findFirst()
                .orElse("Custom Workload");

        String jobId = "job-" + (++jobCounter);
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

        String chosenGpuName = gpuStates.get(chosenGpuId).getGpuName();
        double chosenGpuTte = gpuStates.get(chosenGpuId).getTotalPendingTime();

        String decisionLog;
        if (chosenGpuTte > 0) {
            decisionLog = String.format(
                "[%s] %s 작업이 최종 승인되었습니다. %s 노드 대기열(대기 시간: %.1f초)에 배치하여 실행을 시작합니다.",
                LocalDateTime.now().format(timeFormatter), jobName, chosenGpuName, chosenGpuTte
            );
        } else {
            decisionLog = String.format(
                "[%s] %s 작업이 최종 승인되었습니다. %s 노드에 즉시 실행(Routing) 처리되었습니다.",
                LocalDateTime.now().format(timeFormatter), jobName, chosenGpuName
            );
        }
        decisionLogs.add(0, decisionLog);

        QueuedJob newJob = new QueuedJob(jobId, workloadId, jobName, chosenGpuTtc);
        GpuQueueState targetQueue = gpuStates.get(chosenGpuId);
        targetQueue.getQueue().add(newJob);
        targetQueue.recalculatePendingTime();

        boolean realGpuConnected = false;
        String realGpuLog = null;
        if (gpuNodeRegistry.isRealGpu(chosenGpuId)) {
            String workerUrl = gpuNodeRegistry.getWorkerUrl(chosenGpuId);
            if (gpuWorkerClient.isHealthy(workerUrl)) {
                int durationSec = Math.max(10, (int) Math.ceil(chosenGpuTtc));
                boolean sent = gpuWorkerClient.executeJob(workerUrl, jobId, jobName, durationSec);
                realGpuConnected = sent;
                realGpuLog = sent
                    ? String.format("[%s] [실제 GPU 실행] %s 작업이 %s 서버(%s)에 실제로 전달되었습니다.",
                        LocalDateTime.now().format(timeFormatter), jobName, chosenGpuName, workerUrl)
                    : String.format("[%s] [경고] %s Worker 전송 실패. 시뮬레이션 대체.",
                        LocalDateTime.now().format(timeFormatter), chosenGpuName);
            } else {
                realGpuLog = String.format("[%s] [경고] %s 서버(%s) 연결 실패. 시뮬레이션 대체.",
                    LocalDateTime.now().format(timeFormatter), chosenGpuName, workerUrl);
            }
            if (realGpuLog != null) decisionLogs.add(0, realGpuLog);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("job_id", jobId);
        result.put("workload_id", workloadId);
        result.put("workload_name", jobName);
        result.put("recommended_gpu", chosenGpuName);
        result.put("recommended_gpu_id", chosenGpuId);
        result.put("decision_log", decisionLog);
        result.put("real_gpu_connected", realGpuConnected);
        result.put("real_gpu_log", realGpuLog);

        return result;
    }

    public synchronized Map<String, Object> submitJob(String workloadId, String originalInput) {
        Map<String, Object> analysis = analyzeJob(workloadId, originalInput);
        String recommendedGpuId = (String) analysis.get("recommended_gpu_id");
        double recommendedGpuTtc = (double) analysis.get("recommended_gpu_ttc");
        
        Map<String, Object> execution = commitJob(workloadId, originalInput, recommendedGpuId, recommendedGpuTtc);
        execution.put("mcdm_scores", analysis.get("mcdm_scores"));
        execution.put("time_metrics", analysis.get("time_metrics"));
        execution.put("use_cost_priority", analysis.get("use_cost_priority"));
        execution.put("bypassed", false);
        return execution;
    }

    public synchronized void tick() {
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        
        for (GpuQueueState state : gpuStates.values()) {
            List<QueuedJob> queue = state.getQueue();
            if (queue.isEmpty()) {
                continue;
            }

            QueuedJob activeJob = queue.get(0);
            double remaining = activeJob.getRemainingTime() - 1.0;
            
            if (remaining <= 0.0) {
                queue.remove(0);
                String completeLog = String.format(
                    "[%s] [시스템 알림] %s 노드에서 작업 [%s] %s이(가) 완료 처리되어 퇴장(Pop)하였습니다.",
                    LocalDateTime.now().format(timeFormatter),
                    state.getGpuName(),
                    activeJob.getJobId(),
                    activeJob.getWorkloadName()
                );
                decisionLogs.add(0, completeLog);
            } else {
                activeJob.setRemainingTime(remaining);
            }
            state.recalculatePendingTime();
        }
    }

    public synchronized Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("gpu_states", gpuStates.values());
        status.put("logs", decisionLogs);
        return status;
    }
}
