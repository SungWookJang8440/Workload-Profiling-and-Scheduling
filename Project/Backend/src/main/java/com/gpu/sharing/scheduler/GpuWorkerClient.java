package com.gpu.sharing.scheduler;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;

/**
 * GPU Worker API HTTP 클라이언트
 * RTX 6000 서버(gpu_worker.py)와 통신하여 작업 실행 및 메트릭 수집을 담당합니다.
 */
@Component
public class GpuWorkerClient {

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Worker 서버 헬스체크
     * @param workerUrl Worker 베이스 URL
     * @return 정상 응답 여부
     */
    public boolean isHealthy(String workerUrl) {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                workerUrl + "/health", Map.class
            );
            return response.getStatusCode().is2xxSuccessful();
        } catch (RestClientException e) {
            System.out.println("[GpuWorkerClient] 헬스체크 실패 (" + workerUrl + "): " + e.getMessage());
            return false;
        }
    }

    /**
     * GPU 작업 실행 요청
     * @param workerUrl Worker 베이스 URL
     * @param jobId     작업 고유 ID
     * @param workloadName 워크로드 이름
     * @param durationSec  실행 지속 시간(초)
     * @return 성공 여부
     */
    public boolean executeJob(String workerUrl, String jobId, String workloadName, int durationSec) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("job_id", jobId);
            body.put("workload_name", workloadName);
            body.put("duration_sec", durationSec);
            body.put("intensity", 4096);  // 행렬 크기 (GPU 부하 조절)

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                workerUrl + "/run", request, Map.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("[GpuWorkerClient] ✅ 작업 전송 성공: " + workloadName
                    + " → " + workerUrl + " (duration: " + durationSec + "s)");
                return true;
            }
            return false;

        } catch (RestClientException e) {
            System.out.println("[GpuWorkerClient] ❌ 작업 전송 실패 (" + workerUrl + "): " + e.getMessage());
            return false;
        }
    }

    /**
     * 실시간 GPU 메트릭 수집
     * @param workerUrl Worker 베이스 URL
     * @return nvidia-smi 수치 Map (sm_util, mem_util, mem_used_mb, mem_total_mb, power_w, temp_c)
     *         또는 null (연결 실패 시)
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getMetrics(String workerUrl) {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                workerUrl + "/metrics", Map.class
            );
            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            }
            return null;
        } catch (RestClientException e) {
            System.out.println("[GpuWorkerClient] 메트릭 수집 실패 (" + workerUrl + "): " + e.getMessage());
            return null;
        }
    }

    /**
     * 실행 중인 작업 상태 조회
     * @param workerUrl Worker 베이스 URL
     * @return status Map (job_id, status, elapsed_sec, remaining_sec)
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getJobStatus(String workerUrl) {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                workerUrl + "/status", Map.class
            );
            return response.getStatusCode().is2xxSuccessful() ? response.getBody() : null;
        } catch (RestClientException e) {
            return null;
        }
    }

    /**
     * 실행 중인 작업 강제 종료
     * @param workerUrl Worker 베이스 URL
     */
    public void stopJob(String workerUrl) {
        try {
            restTemplate.postForEntity(workerUrl + "/stop", null, Map.class);
            System.out.println("[GpuWorkerClient] 작업 종료 요청 전송: " + workerUrl);
        } catch (RestClientException e) {
            System.out.println("[GpuWorkerClient] 작업 종료 실패: " + e.getMessage());
        }
    }
}
