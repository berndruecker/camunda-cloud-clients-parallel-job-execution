package main

import (
	"context"
	"fmt"
	"github.com/camunda-cloud/zeebe/clients/go/pkg/entities"
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
	startWorker(zbClient, handlerNonBlocking)
	//startWorker(zbClient, handlerBlocking)
}

func startInstances(zbClient zbc.Client) {
	ctx := context.Background()
	log.Println("Start process instances...")
	var wg = sync.WaitGroup{}
	for i := 0; i < 10000; i++ {
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

func startWorker(zbClient zbc.Client, handler worker.JobHandler) {
	w := zbClient.NewJobWorker().JobType("rest").Handler(handler).Open()
	defer w.Close()
	w.AwaitClose()
}

type nonBlocking struct {
	Response *http.Response
	Error    error
}

func paymentRequest(nb chan nonBlocking) {
	log.Println("Invoke REST call...")
	resp, err := http.Get(PaymentUrl)
	nb <- nonBlocking{
		Response: resp,
		Error:    err,
	}
}

func handleResponse(nb chan nonBlocking, client worker.JobClient, job entities.Job) {
	get := <-nb
	if get.Error != nil {
		ctx := context.Background()
		_, err := client.NewFailJobCommand().
			JobKey(job.Key).
			Retries(1).
			ErrorMessage("Could not invoke REST API: " + get.Error.Error()).
			Send(ctx)
		if err != nil {
			log.Printf("Could not fail job: %s\n", err.Error())
		}
		log.Println("Could not invoke REST API " + get.Error.Error())
	} else {
		log.Println("...finished. Complete Job...")
		go handleComplete(client, job)
	}
}

func handleComplete(client worker.JobClient, job entities.Job) {
	ctx := context.Background()
	result, err := client.NewCompleteJobCommand().
		JobKey(job.Key).
		Send(ctx)
	if err != nil {
		log.Printf("Could not complete job: %s\n", err.Error())
	}
	if result != nil {
		incCounter()
	}
}

func handlerNonBlocking(client worker.JobClient, job entities.Job) {
	nb := make(chan nonBlocking)
	go paymentRequest(nb)
	go handleResponse(nb, client, job)
}

func handlerBlocking(client worker.JobClient, job entities.Job) {
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
}

func incCounter() {
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
