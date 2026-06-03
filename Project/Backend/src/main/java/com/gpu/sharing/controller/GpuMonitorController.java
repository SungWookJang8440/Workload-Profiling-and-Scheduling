package com.gpu.sharing.controller;

import com.gpu.sharing.service.GpuMonitorService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/monitoring")
public class GpuMonitorController {

    private final GpuMonitorService gpuMonitorService;

    public GpuMonitorController(GpuMonitorService gpuMonitorService) {
        this.gpuMonitorService = gpuMonitorService;
    }

    @GetMapping(value = "/gpu-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamGpuMetrics(@RequestParam(defaultValue = "6000") String serverId) {
        SseEmitter emitter = new SseEmitter(600000L); // 10분 타임아웃
        
        if ("4090".equals(serverId)) {
            // 새로 추가된 vast.ai 4090 서버
            String ip = "194.14.47.19";
            int port = 22059;
            String user = "root";
            String keyPath = System.getProperty("user.home") + "/.ssh/id_ed25519";
            gpuMonitorService.startMonitoring(emitter, ip, port, user, null, keyPath);
        } else {
            // 현재 하드코딩된 기존 RTX 6000 정보
            String ip = "155.230.118.52";
            int port = 22345;
            String user = "sslab";
            String password = "sslab1!2";
            gpuMonitorService.startMonitoring(emitter, ip, port, user, password, null);
        }

        emitter.onCompletion(() -> System.out.println("SSE Emitter Completed"));
        emitter.onTimeout(emitter::complete);
        
        return emitter;
    }
}
