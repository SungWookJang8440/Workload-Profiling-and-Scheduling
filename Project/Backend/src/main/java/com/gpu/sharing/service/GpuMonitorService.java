package com.gpu.sharing.service;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class GpuMonitorService {

    private static final Logger log = LoggerFactory.getLogger(GpuMonitorService.class);
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public void startMonitoring(SseEmitter emitter, String ip, int port, String user, String password, String keyPath) {
        executor.execute(() -> {
            Session session = null;
            try {
                JSch jsch = new JSch();
                if (keyPath != null && !keyPath.isEmpty()) {
                    jsch.addIdentity(keyPath);
                }
                session = jsch.getSession(user, ip, port);
                if (password != null && !password.isEmpty()) {
                    session.setPassword(password);
                }

                Properties config = new Properties();
                config.put("StrictHostKeyChecking", "no");
                session.setConfig(config);
                session.connect(5000);

                log.info("SSH Connected for monitoring: {}@{}:{}", user, ip, port);

                try {
                    String migHelp = executeCommand(session, "nvidia-smi --help-query-mig");
                    log.info("MIG HELP QUERY OPTIONS:\n{}", migHelp);
                } catch(Exception e) {
                    log.error("Failed to query mig help", e);
                }

                // Send initial connection success event
                emitter.send(SseEmitter.event().name("status").data("CONNECTED"));

                while (session.isConnected()) {
                    String gpuData = executeCommand(session, "nvidia-smi --query-gpu=uuid,name,temperature.gpu,power.draw,power.limit,memory.total,memory.used --format=csv,noheader");
                    String migData = executeCommand(session, "nvidia-smi -L");

                    Map<String, Object> data = new HashMap<>();
                    data.put("gpu", parseGpuData(gpuData));
                    data.put("mig", parseMigData(migData));

                    emitter.send(SseEmitter.event().name("gpu_metrics").data(data));
                    
                    Thread.sleep(2000); // 2초마다 갱신
                }

            } catch (Exception e) {
                log.error("SSH Monitoring Error", e);
                try {
                    emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
                    emitter.completeWithError(e);
                } catch (Exception ex) {
                    // Ignore
                }
            } finally {
                if (session != null && session.isConnected()) {
                    session.disconnect();
                }
                emitter.complete();
            }
        });
    }

    private String executeCommand(Session session, String command) throws Exception {
        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        channel.setCommand(command);
        channel.setErrStream(System.err);
        InputStream in = channel.getInputStream();
        channel.connect();

        byte[] tmp = new byte[1024];
        StringBuilder output = new StringBuilder();
        while (true) {
            while (in.available() > 0) {
                int i = in.read(tmp, 0, 1024);
                if (i < 0) break;
                output.append(new String(tmp, 0, i));
            }
            if (channel.isClosed()) {
                if (in.available() > 0) continue;
                break;
            }
            Thread.sleep(100);
        }
        channel.disconnect();
        return output.toString().trim();
    }

    private Object parseGpuData(String csv) {
        if (csv == null || csv.isEmpty()) return null;
        String[] parts = csv.split(",");
        if (parts.length >= 7) {
            Map<String, String> map = new HashMap<>();
            map.put("uuid", parts[0].trim());
            map.put("name", parts[1].trim());
            map.put("temperature", parts[2].trim());
            map.put("power_draw", parts[3].trim());
            map.put("power_limit", parts[4].trim());
            map.put("memory_total", parts[5].trim());
            map.put("memory_used", parts[6].trim());
            return map;
        }
        return null;
    }

    private Object parseMigData(String output) {
        if (output == null || output.isEmpty()) return new java.util.ArrayList<>();
        String[] lines = output.split("\n");
        java.util.List<Map<String, String>> list = new java.util.ArrayList<>();
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("MIG")) {
                try {
                    Map<String, String> map = new HashMap<>();
                    String[] parts = line.split(":");
                    if (parts.length >= 2) {
                        String profile = parts[0].split("Device")[0].replace("MIG", "").trim();
                        String deviceId = parts[0].split("Device")[1].trim();
                        String uuid = parts[1].replaceAll("[^a-zA-Z0-9\\-]", "").replace("UUID", "").trim();
                        
                        map.put("profile", profile);
                        map.put("instance_id", deviceId);
                        map.put("uuid", uuid);
                        
                        // 하드코딩된 프로파일 기반 대략적인 VRAM 용량
                        if(profile.contains("48gb")) {
                            map.put("memory_total", "49152 MiB");
                        } else if(profile.contains("24gb")) {
                            map.put("memory_total", "24576 MiB");
                        } else {
                            map.put("memory_total", "Unknown");
                        }
                        
                        list.add(map);
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse MIG line: {}", line);
                }
            }
        }
        return list;
    }
}
