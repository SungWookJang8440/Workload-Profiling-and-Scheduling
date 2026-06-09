package com.gpu.sharing.scheduler;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class McdmSchedulerTest {

    @Test
    public void testMcdmScoring() {
        McdmScheduler scheduler = new McdmScheduler();
        
        // Compute scores for workload w0 (resnet50-train batch 32)
        List<McdmScheduler.ScoreDetail> scores = scheduler.computeScores("w0", SchedulerData.PERF_MATRIX.get("w0"));
        
        assertNotNull(scores);
        assertEquals(3, scores.size());

        System.out.println("=== MCDM Test Scores for w0 ===");
        for (McdmScheduler.ScoreDetail sd : scores) {
            System.out.printf("GPU: %s, Total Score: %.2f (Perf: %.2f, Fit: %.2f, Cost: %.2f, Power: %.2f)%n",
                    sd.getGpu().getName(), sd.getSTotal(),
                    sd.getSPerf(), sd.getSFit(), sd.getSCost(), sd.getSPower());
            
            assertTrue(sd.getSTotal() >= 0.0 && sd.getSTotal() <= 100.0);
            assertTrue(sd.getSPerf() >= 0.0 && sd.getSPerf() <= 100.0);
            assertTrue(sd.getSFit() >= 0.0 && sd.getSFit() <= 100.0);
            assertTrue(sd.getSCost() >= 0.0 && sd.getSCost() <= 100.0);
            assertTrue(sd.getSPower() >= 0.0 && sd.getSPower() <= 100.0);
        }
    }
}
