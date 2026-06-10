package com.gpu.sharing.scheduler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class McdmScheduler {

    private final List<SchedulerData.GPU> gpus;
    private double wPerf;
    private double wFit;
    private double wCost;
    private double wPower;

    public static class ScoreDetail {
        private final SchedulerData.GPU gpu;
        private final double sPerf;
        private final double sFit;
        private final double sCost;
        private final double sPower;
        private final double sTotal;

        public ScoreDetail(SchedulerData.GPU gpu, double sPerf, double sFit, double sCost, double sPower, double sTotal) {
            this.gpu = gpu;
            this.sPerf = sPerf;
            this.sFit = sFit;
            this.sCost = sCost;
            this.sPower = sPower;
            this.sTotal = sTotal;
        }

        public SchedulerData.GPU getGpu() { return gpu; }
        public double getSPerf() { return sPerf; }
        public double getSFit() { return sFit; }
        public double getSCost() { return sCost; }
        public double getSPower() { return sPower; }
        public double getSTotal() { return sTotal; }
    }

    public McdmScheduler() {
        this.gpus = SchedulerData.GPUS;
        this.wPerf = 0.15;
        this.wFit = 0.60;
        this.wCost = 0.15;
        this.wPower = 0.10;
    }

    public double scorePerformance(String gpuId, Map<String, Double> dynamicThroughputs) {
        if (dynamicThroughputs == null || dynamicThroughputs.isEmpty()) {
            return 0.0;
        }

        double pIj = dynamicThroughputs.getOrDefault(gpuId, 0.0);
        double pMax = dynamicThroughputs.values().stream().max(Double::compare).orElse(1.0);

        if (Math.abs(pMax) < 1e-9) {
            return 0.0;
        }

        return Math.sqrt(pIj / pMax) * 100.0;
    }

    private double smFitScore(double uSm) {
        if (uSm < 75.0) {
            return (uSm / 75.0) * 100.0;
        } else if (uSm <= 90.0) {
            return 100.0;
        } else {
            return 100.0 - ((uSm - 90.0) / 10.0) * 10.0;
        }
    }

    private double memFitScore(double uMem) {
        if (uMem < 55.0) {
            return (uMem / 55.0) * 100.0;
        } else if (uMem <= 75.0) {
            return 100.0;
        } else {
            return Math.max(0.0, 100.0 - ((uMem - 75.0) / 25.0) * 50.0);
        }
    }

    public double scoreResourceFit(String workloadId, String gpuId) {
        double uSm = SchedulerData.SM_MATRIX.getOrDefault(workloadId, Map.of()).getOrDefault(gpuId, 0.0);
        double uMem = SchedulerData.MEM_MATRIX.getOrDefault(workloadId, Map.of()).getOrDefault(gpuId, 0.0);
        
        double sSm = smFitScore(uSm);
        double sMem = memFitScore(uMem);

        return (sSm + sMem) / 2.0;
    }

    public double scoreCostEfficiency(String gpuId, Map<String, Double> dynamicThroughputs) {
        SchedulerData.GPU targetGpu = null;
        for (SchedulerData.GPU g : gpus) {
            if (g.getId().equals(gpuId)) {
                targetGpu = g;
                break;
            }
        }
        if (targetGpu == null) return 0.0;

        double tpdJ = calculateTpd(targetGpu, dynamicThroughputs);
        double maxTpd = 0.0;
        for (SchedulerData.GPU g : gpus) {
            double val = calculateTpd(g, dynamicThroughputs);
            if (val > maxTpd) maxTpd = val;
        }

        if (maxTpd < 1e-9) maxTpd = 1.0;
        return Math.sqrt(tpdJ / maxTpd) * 100.0;
    }

    private double calculateTpd(SchedulerData.GPU g, Map<String, Double> dynamicThroughputs) {
        double throughput = dynamicThroughputs.getOrDefault(g.getId(), 0.0);
        return g.getCostPerHour() > 0 ? throughput / Math.pow(g.getCostPerHour(), 2) : 0.0;
    }

    public double scorePowerEfficiency(String gpuId, Map<String, Double> dynamicThroughputs) {
        SchedulerData.GPU targetGpu = null;
        for (SchedulerData.GPU g : gpus) {
            if (g.getId().equals(gpuId)) {
                targetGpu = g;
                break;
            }
        }
        if (targetGpu == null) return 0.0;

        double tpwJ = calculateTpw(targetGpu, dynamicThroughputs);
        double maxTpw = 0.0;
        for (SchedulerData.GPU g : gpus) {
            double val = calculateTpw(g, dynamicThroughputs);
            if (val > maxTpw) maxTpw = val;
        }

        if (maxTpw < 1e-9) maxTpw = 1.0;
        return Math.sqrt(tpwJ / maxTpw) * 100.0;
    }

    private double calculateTpw(SchedulerData.GPU g, Map<String, Double> dynamicThroughputs) {
        double throughput = dynamicThroughputs.getOrDefault(g.getId(), 0.0);
        return g.getWatts() > 0 ? throughput / Math.pow(g.getWatts(), 2) : 0.0;
    }

    public List<ScoreDetail> computeScores(String workloadId, Map<String, Double> dynamicThroughputs, boolean useCostPriority) {
        if (useCostPriority) {
            this.wPerf = 0.10;
            this.wFit = 0.50;
            this.wCost = 0.30;
            this.wPower = 0.10;
        } else {
            this.wPerf = 0.15;
            this.wFit = 0.60;
            this.wCost = 0.15;
            this.wPower = 0.10;
        }

        List<ScoreDetail> details = new ArrayList<>();
        for (SchedulerData.GPU gpu : gpus) {
            double sPerf = scorePerformance(gpu.getId(), dynamicThroughputs);
            double sFit = scoreResourceFit(workloadId, gpu.getId());
            double sCost = scoreCostEfficiency(gpu.getId(), dynamicThroughputs);
            double sPower = scorePowerEfficiency(gpu.getId(), dynamicThroughputs);

            double sTotal = wPerf * sPerf + wFit * sFit + wCost * sCost + wPower * sPower;

            details.add(new ScoreDetail(
                gpu,
                round(sPerf),
                round(sFit),
                round(sCost),
                round(sPower),
                round(sTotal)
            ));
        }

        details.sort(Comparator.comparingDouble(ScoreDetail::getSTotal).reversed());
        return details;
    }

    // Overloaded method to keep compatibility with existing calls
    public List<ScoreDetail> computeScores(String workloadId, Map<String, Double> dynamicThroughputs) {
        return computeScores(workloadId, dynamicThroughputs, false);
    }

    private double round(double val) {
        return Math.round(val * 100.0) / 100.0;
    }
}
