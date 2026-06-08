package com.gpu.sharing.scheduler;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * GPU ID → Worker 서버 URL 매핑 레지스트리
 * 각 GPU 노드에 배포된 Worker FastAPI 서버의 주소를 관리합니다.
 */
@Component
public class GpuNodeRegistry {

    /**
     * GPU ID별 Worker API 베이스 URL 매핑
     * - g2 (RTX 6000): 연구실 서버 (155.230.118.52:5001)
     * - g0 (RTX 3090): 미연결 (향후 Vast.ai 추가)
     * - g1 (RTX 4090): 미연결 (향후 Vast.ai 추가)
     */
    private final Map<String, String> GPU_WORKER_URLS = Map.of(
        "g2", "http://localhost:5001"   // RTX 6000 연구실 서버 (SSH 터널링 포트 포워딩)
        // "g0", "http://[RTX3090_IP]:5001",  // 향후 추가
        // "g1", "http://[RTX4090_IP]:5001",  // 향후 추가
    );

    /**
     * GPU ID에 해당하는 Worker URL 반환
     * @param gpuId GPU 식별자 (예: "g2")
     * @return Worker URL 또는 null (미연결 노드)
     */
    public String getWorkerUrl(String gpuId) {
        return GPU_WORKER_URLS.get(gpuId);
    }

    /**
     * 해당 GPU가 실제 Worker에 연결되어 있는지 확인
     */
    public boolean isRealGpu(String gpuId) {
        return GPU_WORKER_URLS.containsKey(gpuId);
    }
}
