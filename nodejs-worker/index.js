const { ZBClient } = require('zeebe-node')
const axios = require('axios').default;

const PAYMENT_URL = "http://localhost:9090/"
const zbc = new ZBClient({
	camundaCloud: {
		clusterId: 'de121ba4-f76d-49ad-8cd5-842e653be639',
		clientId: '9vdtiexfQ8Ae4pIi7w.nSSxI6ELp76yJ',
		clientSecret: 'LJripeKNmFKOMI7Ps.cFeFRW3fiRW5gJ8mNTihMcAf6SqA8zlHUXroOghOB4xaJo',
    clusterRegion: 'bru-2'
	},
})

async function startInstances() {
  console.log("Starting process instances...")
  for (let index = 0; index < 500; index++) {
      // do this in a blocking call to avoidd backpressure (good topic for the next blog post :-))
      await zbc.createProcessInstance('rest')
  }
  console.log("...done")
}

function startNonBlockingWorker() {
  zbc.createWorker({
    taskType: 'rest',
    taskHandler: (job, _, worker) => {
      console.log("Invoke REST call...");
      axios.get(PAYMENT_URL)
          .then(response => {
            console.log("...finished. Complete Job...")
            job.complete().then( result => {
              incCounter()              
            })
          })
          .catch(error => {
            job.fail("Could not invoke REST API: " + error.message)
          });
      
    }
  })
}

function startBlockingWorker() {
  zbc.createWorker({
    taskType: 'rest',
    taskHandler: async (job, _, worker) => {
      console.log("Invoke REST call...");
      await axios.get(PAYMENT_URL);
      console.log("...finished. Complete Job...")
      await job.complete();
      incCounter();
    }
  })
}

void (async () => {
  await startInstances()
  console.log("-----------------")
  initCounterStartTime()
  //startNonBlockingWorker()
  startBlockingWorker()
})()


var count = 0;
var startTimestamp = 0;
var endTimestamp = 0;
var throughputMax = 0;

function incCounter() {
    endTimestamp = Date.now();
    count++;
    console.log("...completed (" + count + "). " + getThroughputInfoFor(count));
}

function getThroughputInfoFor(currentCount) {

    var timeDiff = Math.ceil( (endTimestamp - startTimestamp) / 1000);

    //console.log("startTimestamp: " + startTimestamp + " endTimestamp: " + endTimestamp + " diff: " + timeDiff);

    if (timeDiff == 0) {
        return "Current throughput (jobs/s ): " + currentCount;
    } else {
        var throughput = Math.round(currentCount / timeDiff);
        if (throughput>throughputMax) {
            throughputMax = throughput;
        }
        return "Current throughput (jobs/s ): " + throughput + ", Max: " + throughputMax;
    }
}

function initCounterStartTime() {
    if (startTimestamp==0) {
        startTimestamp = Date.now();
    }
}