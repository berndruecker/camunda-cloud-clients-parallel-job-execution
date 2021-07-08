package io.berndruecker.experiments.cloudclient.java;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class JobCounter {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobCounter.class);

    private AtomicInteger count = new AtomicInteger();
    private long startTimestamp;
    private long endTimestamp;
    private long throughputMax = 0;
    
    public void inc() {
        init();
        endTimestamp = System.currentTimeMillis();
        int currentCount = count.addAndGet(1);

        LOGGER.info("...completed (" + currentCount + "). " + getThroughputInfoFor(currentCount));
    }

    private String getThroughputInfoFor(int currentCount) {

        long timeDiff = (endTimestamp - startTimestamp) / 1000;

        //System.out.println("startTimestamp: " + startTimestamp + " endTimestamp: " + endTimestamp + " diff: " + timeDiff);

        if (timeDiff == 0) {
            return "Current throughput (jobs/s ): " + currentCount;
        } else {
            long throughput = currentCount / timeDiff;
            if (throughput>throughputMax) {
                throughputMax = throughput;
            }
            return "Current throughput (jobs/s ): " + throughput + ", Max: " + throughputMax;
        }
    }

    public void init() {
        if (startTimestamp==0) {
            startTimestamp = System.currentTimeMillis();
        }
    }
}
