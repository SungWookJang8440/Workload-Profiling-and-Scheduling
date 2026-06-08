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
        private final double duration; // original duration in sec
        private double remainingTime; // remaining time in sec
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

    public synchronized Map<String, Object> submitJob(String workloadId, String originalInput) {
        String jobName = SchedulerData.WORKLOADS.stream()
                .filter(w -> w.getId().equals(workloadId))
                .map(SchedulerData.Workload::getName)
                .findFirst()
                .orElse("Custom Workload");

        String jobId = "job-" + (++jobCounter);

        // 1. Calculate MCDM Scores for all 3 GPUs
        List<McdmScheduler.ScoreDetail> scores = scheduler.computeScores(workloadId);
        
        // Find best GPU based strictly on MCDM Score
        McdmScheduler.ScoreDetail bestMcdm = scores.get(0);
        String bestMcdmGpuId = bestMcdm.getGpu().getId();
        String bestMcdmGpuName = bestMcdm.getGpu().getName();
        double bestMcdmScore = bestMcdm.getSTotal();

        // 2. Time estimation metrics for each GPU
        Map<String, Map<String, Object>> timeMetrics = new HashMap<>();
        String chosenGpuId = bestMcdmGpuId; // default
        double minTotalTime = Double.MAX_VALUE;

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

        for (SchedulerData.GPU gpu : SchedulerData.GPUS) {
            String gId = gpu.getId();
            double ttc = SchedulerData.LATENCY_MATRIX.getOrDefault(workloadId, Map.of()).getOrDefault(gId, 1.0);
            
            // TTE = Sum of remaining times of queued jobs on this GPU
            double tte = gpuStates.get(gId).getTotalPendingTime();
            double totalTime = tte + ttc;

            LocalDateTime etaTime = LocalDateTime.now().plusSeconds((long) totalTime);
            String eta = etaTime.format(timeFormatter);

            Map<String, Object> metrics = new HashMap<>();
            metrics.put("tte", Math.round(tte * 100.0) / 100.0);
            metrics.put("ttc", Math.round(ttc * 100.0) / 100.0);
            metrics.put("total", Math.round(totalTime * 100.0) / 100.0);
            metrics.put("eta", eta);
            timeMetrics.put(gId, metrics);

            // Dynamic load-balancing routing logic: Select the GPU that MINIMIZES overall completion time (TTE + TTC)
            if (totalTime < minTotalTime) {
                minTotalTime = totalTime;
                chosenGpuId = gId;
            }
        }

        String chosenGpuName = gpuStates.get(chosenGpuId).getGpuName();
        double chosenGpuTtc = (double) timeMetrics.get(chosenGpuId).get("ttc");
        double chosenGpuTte = (double) timeMetrics.get(chosenGpuId).get("tte");

        // 3. Generate Decision Reason Log
        String decisionLog;
        boolean bypassed = !chosenGpuId.equals(bestMcdmGpuId);
        
        if (bypassed) {
            double bestMcdmTotalTime = (double) timeMetrics.get(bestMcdmGpuId).get("total");
            double savedTime = bestMcdmTotalTime - minTotalTime;
            
            decisionLog = String.format(
                "[%s] %s 작업이 접수되었습니다. MCDM 알고리즘 점수로는 %s 노드가 최적(%.0f점)이지만, 현재 해당 노드의 대기열(Queue)이 %.1f초 밀려 있습니다. " +
                "따라서 총 대기 시간 및 실행 시간을 고려하여 즉시 처리가 가능하거나 대기열이 짧은 %s 노드로 동적 우회 할당(Bypass Routing)을 수행하였습니다. " +
                "이를 통해 작업 완료 예상 시간(ETA)을 약 %.1f초 단축시켰습니다.",
                LocalDateTime.now().format(timeFormatter),
                jobName,
                bestMcdmGpuName,
                bestMcdmScore,
                chosenGpuTte, // Wait time on target
                chosenGpuName,
                savedTime
            );
        } else {
            decisionLog = String.format(
                "[%s] %s 작업이 접수되었습니다. MCDM 알고리즘 점수 및 대기 시간을 분석한 결과 %s 노드가 최적(점수: %.0f점, 대기 시간: %.1f초)으로 판단되어 해당 노드로 최종 할당(Routing) 하였습니다.",
                LocalDateTime.now().format(timeFormatter),
                jobName,
                chosenGpuName,
                bestMcdmScore,
                chosenGpuTte
            );
        }

        decisionLogs.add(0, decisionLog); // Prepend to show latest logs first

        // 4. Push job to the selected GPU's queue (in-memory simulation)
        QueuedJob newJob = new QueuedJob(jobId, workloadId, jobName, chosenGpuTtc);
        GpuQueueState targetQueue = gpuStates.get(chosenGpuId);
        targetQueue.getQueue().add(newJob);
        targetQueue.recalculatePendingTime();

        // 5. 실제 GPU Worker 연동 (RTX 6000 등 실제 서버가 연결된 경우)
        boolean realGpuConnected = false;
        String realGpuLog = null;
        if (gpuNodeRegistry.isRealGpu(chosenGpuId)) {
            String workerUrl = gpuNodeRegistry.getWorkerUrl(chosenGpuId);
            System.out.println("[QueueManager] 실제 GPU Worker 호출 시도: " + workerUrl);

            if (gpuWorkerClient.isHealthy(workerUrl)) {
                int durationSec = Math.max(10, (int) Math.ceil(chosenGpuTtc));
                boolean sent = gpuWorkerClient.executeJob(workerUrl, jobId, jobName, durationSec);
                realGpuConnected = sent;
                realGpuLog = sent
                    ? String.format("[%s] [실제 GPU 실행] %s 작업이 RTX 6000 서버(%s)에 실제로 전달되었습니다. GPU 사용량은 대시보드 메트릭 패널에서 실시간으로 확인하세요.",
                        LocalDateTime.now().format(timeFormatter), jobName, workerUrl)
                    : String.format("[%s] [경고] RTX 6000 Worker 작업 전송에 실패했습니다. 시뮬레이션 모드로 대체합니다.",
                        LocalDateTime.now().format(timeFormatter));
            } else {
                realGpuLog = String.format("[%s] [경고] RTX 6000 Worker 서버(%s)에 연결할 수 없습니다. 시뮬레이션 모드로 진행합니다.",
                    LocalDateTime.now().format(timeFormatter), workerUrl);
            }

            if (realGpuLog != null) {
                decisionLogs.add(0, realGpuLog);
            }
        }

        // Prepare return payload
        Map<String, Object> result = new HashMap<>();
        result.put("job_id", jobId);
        result.put("workload_id", workloadId);
        result.put("workload_name", jobName);
        result.put("recommended_gpu", chosenGpuName);
        result.put("recommended_gpu_id", chosenGpuId);
        result.put("mcdm_scores", scores);
        result.put("time_metrics", timeMetrics);
        result.put("decision_log", decisionLog);
        result.put("bypassed", bypassed);
        result.put("real_gpu_connected", realGpuConnected);
        result.put("real_gpu_log", realGpuLog);

        return result;
    }

    public synchronized void tick() {
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        
        for (GpuQueueState state : gpuStates.values()) {
            List<QueuedJob> queue = state.getQueue();
            if (queue.isEmpty()) {
                continue;
            }

            // Get currently executing job (first in queue)
            QueuedJob activeJob = queue.get(0);
            double remaining = activeJob.getRemainingTime() - 1.0;
            
            if (remaining <= 0.0) {
                // Job complete!
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
