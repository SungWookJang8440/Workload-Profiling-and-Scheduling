package com.gpu.sharing.scheduler;

import java.util.List;
import java.util.Map;

public class SchedulerData {

    public static class GPU {
        private final String id;
        private final String name;
        private final double costPerHour;
        private final double watts;

        public GPU(String id, String name, double costPerHour, double watts) {
            this.id = id;
            this.name = name;
            this.costPerHour = costPerHour;
            this.watts = watts;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public double getCostPerHour() { return costPerHour; }
        public double getWatts() { return watts; }
    }

    public static class Workload {
        private final String id;
        private final String name;

        public Workload(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public String getId() { return id; }
        public String getName() { return name; }
    }

    public static final List<GPU> GPUS = List.of(
        new GPU("g0", "RTX 3090", 1.00, 350),
        new GPU("g1", "RTX 4090", 2.50, 450),
        new GPU("g2", "RTX 6000", 5.00, 600)
    );

    public static final List<Workload> WORKLOADS = List.of(
        new Workload("w0", "resnet50-train (batch32)"),
        new Workload("w1", "resnet50-train (batch64)"),
        new Workload("w2", "resnet50-train (batch128)"),
        new Workload("w3", "bert-base-cased-train (batch8)"),
        new Workload("w4", "bert-base-cased-train (batch16)"),
        new Workload("w5", "bert-base-cased-train (batch32)"),
        new Workload("w6", "openai-whisper-large-v2-inf (batch4)"),
        new Workload("w7", "openai-whisper-large-v2-inf (batch8)"),
        new Workload("w8", "openai-whisper-large-v2-inf (batch16)"),
        new Workload("w9", "google-mobilenet_v2-inf (batch16)"),
        new Workload("w10", "google-mobilenet_v2-inf (batch32)"),
        new Workload("w11", "google-mobilenet_v2-inf (batch64)"),
        new Workload("w12", "google-vit-base-patch16-224-inf (batch8)"),
        new Workload("w13", "google-vit-base-patch16-224-inf (batch16)"),
        new Workload("w14", "google-vit-base-patch16-224-inf (batch32)"),
        new Workload("w15", "bert-base-cased-inf (batch16)"),
        new Workload("w16", "bert-base-cased-inf (batch32)"),
        new Workload("w17", "bert-base-cased-inf (batch64)")
    );

    // Performance throughput matrix: samples/sec
    public static final Map<String, Map<String, Double>> PERF_MATRIX = Map.ofEntries(
        Map.entry("w0", Map.of("g0", 1638.02, "g1", 1430.12, "g2", 1658.52)),
        Map.entry("w1", Map.of("g0", 3205.87, "g1", 2797.31, "g2", 1491.43)),
        Map.entry("w2", Map.of("g0", 5408.68, "g1", 5103.74, "g2", 1351.44)),
        Map.entry("w3", Map.of("g0", 46.83, "g1", 71.64, "g2", 498.11)),
        Map.entry("w4", Map.of("g0", 49.91, "g1", 83.18, "g2", 676.18)),
        Map.entry("w5", Map.of("g0", 51.55, "g1", 84.09, "g2", 787.51)),
        Map.entry("w6", Map.of("g0", 5.40, "g1", 7.25, "g2", 743.15)),
        Map.entry("w7", Map.of("g0", 5.74, "g1", 9.58, "g2", 907.26)),
        Map.entry("w8", Map.of("g0", 5.88, "g1", 9.04, "g2", 970.49)),
        Map.entry("w9", Map.of("g0", 273.20, "g1", 210.35, "g2", 12483.96)),
        Map.entry("w10", Map.of("g0", 253.20, "g1", 214.84, "g2", 13250.14)),
        Map.entry("w11", Map.of("g0", 258.63, "g1", 201.83, "g2", 11641.03)),
        Map.entry("w12", Map.of("g0", 192.82, "g1", 195.48, "g2", 1176.46)),
        Map.entry("w13", Map.of("g0", 196.58, "g1", 197.50, "g2", 1246.63)),
        Map.entry("w14", Map.of("g0", 181.65, "g1", 180.81, "g2", 1254.23)),
        Map.entry("w15", Map.of("g0", 146.98, "g1", 245.91, "g2", 1768.03)),
        Map.entry("w16", Map.of("g0", 150.32, "g1", 244.78, "g2", 1857.49)),
        Map.entry("w17", Map.of("g0", 151.35, "g1", 243.21, "g2", 1897.73))
    );

    // SM Utilization matrix (%)
    public static final Map<String, Map<String, Double>> SM_MATRIX = Map.ofEntries(
        Map.entry("w0", Map.of("g0", 22.11, "g1", 33.66, "g2", 97.00)),
        Map.entry("w1", Map.of("g0", 30.46, "g1", 41.68, "g2", 98.77)),
        Map.entry("w2", Map.of("g0", 34.33, "g1", 56.30, "g2", 99.00)),
        Map.entry("w3", Map.of("g0", 32.58, "g1", 66.29, "g2", 88.24)),
        Map.entry("w4", Map.of("g0", 39.49, "g1", 81.67, "g2", 71.46)),
        Map.entry("w5", Map.of("g0", 44.11, "g1", 88.70, "g2", 90.55)),
        Map.entry("w6", Map.of("g0", 39.56, "g1", 59.84, "g2", 48.00)),
        Map.entry("w7", Map.of("g0", 43.75, "g1", 78.38, "g2", 2.00)),
        Map.entry("w8", Map.of("g0", 46.76, "g1", 87.68, "g2", 91.27)),
        Map.entry("w9", Map.of("g0", 2.34, "g1", 2.25, "g2", 99.00)),
        Map.entry("w10", Map.of("g0", 3.00, "g1", 3.13, "g2", 99.00)),
        Map.entry("w11", Map.of("g0", 4.45, "g1", 4.08, "g2", 97.91)),
        Map.entry("w12", Map.of("g0", 6.85, "g1", 8.33, "g2", 97.64)),
        Map.entry("w13", Map.of("g0", 8.58, "g1", 12.29, "g2", 98.92)),
        Map.entry("w14", Map.of("g0", 11.33, "g1", 15.04, "g2", 99.90)),
        Map.entry("w15", Map.of("g0", 23.19, "g1", 43.53, "g2", 30.95)),
        Map.entry("w16", Map.of("g0", 32.94, "g1", 65.33, "g2", 98.94)),
        Map.entry("w17", Map.of("g0", 40.23, "g1", 77.59, "g2", 83.82))
    );

    // Memory Utilization matrix (%)
    public static final Map<String, Map<String, Double>> MEM_MATRIX = Map.ofEntries(
        Map.entry("w0", Map.of("g0", 10.00, "g1", 14.64, "g2", 80.36)),
        Map.entry("w1", Map.of("g0", 11.00, "g1", 15.13, "g2", 88.07)),
        Map.entry("w2", Map.of("g0", 12.26, "g1", 15.24, "g2", 91.21)),
        Map.entry("w3", Map.of("g0", 19.34, "g1", 41.12, "g2", 21.42)),
        Map.entry("w4", Map.of("g0", 21.40, "g1", 56.21, "g2", 18.23)),
        Map.entry("w5", Map.of("g0", 23.85, "g1", 66.37, "g2", 25.37)),
        Map.entry("w6", Map.of("g0", 19.58, "g1", 36.04, "g2", 11.00)),
        Map.entry("w7", Map.of("g0", 19.59, "g1", 45.34, "g2", 0.01)),
        Map.entry("w8", Map.of("g0", 20.48, "g1", 51.43, "g2", 28.82)),
        Map.entry("w9", Map.of("g0", 2.09, "g1", 1.13, "g2", 92.00)),
        Map.entry("w10", Map.of("g0", 2.51, "g1", 2.22, "g2", 92.00)),
        Map.entry("w11", Map.of("g0", 3.93, "g1", 3.38, "g2", 87.09)),
        Map.entry("w12", Map.of("g0", 2.94, "g1", 2.33, "g2", 39.93)),
        Map.entry("w13", Map.of("g0", 4.72, "g1", 4.94, "g2", 18.62)),
        Map.entry("w14", Map.of("g0", 5.44, "g1", 7.00, "g2", 18.90)),
        Map.entry("w15", Map.of("g0", 12.17, "g1", 27.80, "g2", 5.37)),
        Map.entry("w16", Map.of("g0", 17.19, "g1", 46.90, "g2", 16.63)),
        Map.entry("w17", Map.of("g0", 21.61, "g1", 52.41, "g2", 18.44))
    );

    // Latency (execution time) matrix (seconds)
    public static final Map<String, Map<String, Double>> LATENCY_MATRIX = Map.ofEntries(
        Map.entry("w0", Map.of("g0", 1.9535, "g1", 2.2376, "g2", 1.9294)),
        Map.entry("w1", Map.of("g0", 1.9963, "g1", 2.2879, "g2", 4.2911)),
        Map.entry("w2", Map.of("g0", 2.3665, "g1", 2.5080, "g2", 9.4712)),
        Map.entry("w3", Map.of("g0", 17.0827, "g1", 11.1672, "g2", 1.6060)),
        Map.entry("w4", Map.of("g0", 32.0577, "g1", 19.2354, "g2", 2.3660)),
        Map.entry("w5", Map.of("g0", 62.0757, "g1", 38.0545, "g2", 4.0634)),
        Map.entry("w6", Map.of("g0", 74.0741, "g1", 55.1724, "g2", 0.5382)),
        Map.entry("w7", Map.of("g0", 139.3574, "g1", 83.5073, "g2", 0.8817)),
        Map.entry("w8", Map.of("g0", 272.1088, "g1", 176.9912, "g2", 1.6487)),
        Map.entry("w9", Map.of("g0", 5.8566, "g1", 7.6065, "g2", 0.1282)),
        Map.entry("w10", Map.of("g0", 12.6374, "g1", 14.8951, "g2", 0.2415)),
        Map.entry("w11", Map.of("g0", 24.7499, "g1", 31.7096, "g2", 0.5497)),
        Map.entry("w12", Map.of("g0", 4.1488, "g1", 4.0924, "g2", 0.6800)),
        Map.entry("w13", Map.of("g0", 8.1393, "g1", 8.1013, "g2", 1.2835)),
        Map.entry("w14", Map.of("g0", 17.6155, "g1", 17.6984, "g2", 2.5516)),
        Map.entry("w15", Map.of("g0", 10.8858, "g1", 6.5065, "g2", 0.9049)),
        Map.entry("w16", Map.of("g0", 21.2882, "g1", 13.0728, "g2", 1.7228)),
        Map.entry("w17", Map.of("g0", 42.2869, "g1", 26.3175, "g2", 3.3724))
    );
}
