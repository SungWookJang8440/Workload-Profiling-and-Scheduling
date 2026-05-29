package com.gpu.sharing.controller;

import com.gpu.sharing.service.GpuMonitorService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
    public SseEmitter streamGpuMetrics() {
        SseEmitter emitter = new SseEmitter(600000L); // 10분 타임아웃
        
        // 현재는 하드코딩된 RTX 6000 정보 (추후 DB에서 가져오도록 확장 가능)
        String ip = "155.230.118.52";
        int port = 22345;
        String user = "sslab";
        String password = "sslab1!2";
        
        gpuMonitorService.startMonitoring(emitter, ip, port, user, password);

        emitter.onCompletion(() -> System.out.println("SSE Emitter Completed"));
        emitter.onTimeout(emitter::complete);
        
        return emitter;
    }
}
