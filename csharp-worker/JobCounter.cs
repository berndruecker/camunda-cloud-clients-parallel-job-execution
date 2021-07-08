using System;
using System.Diagnostics;
using System.Threading;
using Microsoft.Extensions.Logging;
using NLog.Extensions.Logging;

namespace io.berndruecker.experiments
{
class JobCounter {
    private static readonly ILoggerFactory LoggerFactory = new NLogLoggerFactory();
    private static readonly ILogger<JobCounter> Log = LoggerFactory.CreateLogger<JobCounter>();

    private long count;
    private Stopwatch stopWatch = new Stopwatch();
    private long throughputMax = 0;
    
    public void inc() {
        init();
        Interlocked.Increment(ref count);        
        Log.LogInformation("...completed (" + count + "). " + getThroughputInfoFor(count));
    }

    private String getThroughputInfoFor(long currentCount) {

        long timeDiff = stopWatch.ElapsedMilliseconds / 1000;

        //Log.LogInformation("diff: " + timeDiff);

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
        stopWatch.Start();
    }
}
}