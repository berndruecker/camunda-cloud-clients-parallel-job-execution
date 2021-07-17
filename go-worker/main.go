package main

import (
	"context"
	"fmt"
	"github.com/camunda-cloud/zeebe/clients/go/pkg/entities"
	"github.com/camunda-cloud/zeebe/clients/go/pkg/pb"
	"github.com/camunda-cloud/zeebe/clients/go/pkg/worker"
	"github.com/camunda-cloud/zeebe/clients/go/pkg/zbc"
	"log"
	"math"
	"net/http"
	"os"
	"sync"
	"time"
)
var PaymentUrl = "http://localhost:9090/"
var startTimestamp time.Time
var endTimestamp time.Time
var count = 0
var throughputMax = 0

func main() {
	gatewayAddr := os.Getenv("ZEEBE_ADDRESS")
	var plainText bool

	zbClient, err := zbc.NewClient(&zbc.ClientConfig{
		GatewayAddress:         gatewayAddr,
		UsePlaintextConnection: plainText,
	})
	if err != nil {
		panic(err)
	}
	startInstances(zbClient)
	startTimestamp = time.Now()
	startNonBlockingWorker(zbClient)
	//startBlockingWorker(zbClient)
}

func startInstances(zbClient zbc.Client) {
	ctx := context.Background()
	log.Println("Start process instances...");
	var wg = sync.WaitGroup{}
	for i := 0; i < 10; i++ {
		wg.Add(1)
		go func(index int) {
			defer wg.Done()
			log.Printf("Added process %d\n", index)
			zbClient.NewCreateInstanceCommand().BPMNProcessId("rest").LatestVersion().Send(ctx)
		}(i)
	}
	wg.Wait()
	log.Println("...done")
}

type nonBlocking struct {
	Response *http.Response
	Error    error
}

func PaymentRequest(nb chan nonBlocking) {
	log.Println("Invoke REST call...")
	resp, err := http.Get(PaymentUrl)
	nb <- nonBlocking{
		Response: resp,
		Error:    err,
	}
}

func HandleResponse(nb chan nonBlocking, client worker.JobClient, job entities.Job) {
	compH := make(chan *pb.CompleteJobResponse)
	wg := sync.WaitGroup{}
	for get := range nb {
		if get.Error != nil {
			client.NewFailJobCommand().
				JobKey(job.Key).
				Retries(0).
				ErrorMessage("Could not invoke REST API: " + get.Error.Error())
		} else {
			log.Println("...finished. Complete Job...")
			wg.Add(1)
			HandleComplete(client, job, &wg, compH)
		}
	}
}

func HandleComplete(client worker.JobClient, job entities.Job, wg *sync.WaitGroup, compH chan *pb.CompleteJobResponse){
	defer wg.Done()
	ctx := context.Background()
	result, _ := client.NewCompleteJobCommand().
		JobKey(job.Key).
		Send(ctx)
	if result != nil {
		incCounter()
	}
	compH <- result
}

func startNonBlockingWorker(zbClient zbc.Client) {
	nb := make(chan nonBlocking)
	zbClient.NewJobWorker().
		JobType("rest").
		Handler(func(client worker.JobClient, job entities.Job) {
			go PaymentRequest(nb)
			go HandleResponse(nb, client, job)
		}).Open().AwaitClose()
}

func startBlockingWorker(zbClient zbc.Client) {
	zbClient.NewJobWorker().
		JobType("rest").
		Handler(func(client worker.JobClient, job entities.Job) {
			log.Println("Invoke REST call...")
			resp, err := http.Get(PaymentUrl)
			if err != nil {
				log.Println("Error connecting to payment server")
			}
			if resp != nil {
				log.Println("...finished. Complete Job...")
				ctx := context.Background()
				result, _ := client.NewCompleteJobCommand().
					JobKey(job.Key).
					Send(ctx)
				if result != nil {
					incCounter()
				}
			}
		}).Open().AwaitClose()
	log.Println("worker done")
}

func incCounter(){
	endTimestamp = time.Now()
	count++
	log.Printf("...completed (%s). ", getThroughputInfoFor(count))
}

func getThroughputInfoFor(currentCount int) string {
	timeDiff := endTimestamp.Sub(startTimestamp)

	if timeDiff == 0 {
		return fmt.Sprintf("Current throughput (jobs/s): %d", currentCount)
	} else {
		throughput := int(math.Round(float64(currentCount) / timeDiff.Seconds()))
		if throughput > throughputMax {
			throughputMax = throughput
		}
		return fmt.Sprintf("Current throughput (jobs/s): %d, Max: %d", throughput, throughputMax)
	}
}