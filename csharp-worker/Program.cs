using System;
using System.Collections.Generic;
using System.Net.Http;
using System.Threading.Tasks;
using Microsoft.Extensions.Logging;
using Newtonsoft.Json;
using NLog.Extensions.Logging;
using Zeebe.Client;
using Zeebe.Client.Api.Responses;
using Zeebe.Client.Api.Worker;
using Zeebe.Client.Impl.Builder;

namespace io.berndruecker.experiments
{
    class Program
    {
        private static readonly ILoggerFactory LoggerFactory = new NLogLoggerFactory();
        private static readonly ILogger<Program> Log = LoggerFactory.CreateLogger<Program>();

        private static HttpClient httpClient = new HttpClient();
        private static String PAYMENT_URL = "http://localhost:9090/";

        private static JobCounter counter = new JobCounter();
        static async Task Main(string[] _)
        {
            httpClient = new HttpClient();
            httpClient.BaseAddress = new Uri(PAYMENT_URL);

            var zeebeClient = CreateZeebeClient();

            Log.LogInformation("Start process instances...");
            for (int i = 0; i < 500; i++)
            {
                await zeebeClient.NewCreateProcessInstanceCommand()
                    .BpmnProcessId("rest").LatestVersion()
                    .Send();
            }
            Log.LogInformation("... done");

            //startWorker(zeebeClient, BlockingJobHandler);
            startWorker(zeebeClient, NonBlockingJobHandler);

            AwaitExitUserCmd();
        }

        private static IDisposable startWorker(IZeebeClient zeebeClient, JobHandler handler) {
            return zeebeClient.NewWorker()
                .JobType("rest")
                .Handler(handler)
                .MaxJobsActive(32)
                .Timeout(TimeSpan.FromSeconds(10))
                .Open();
        }


        private static async void BlockingJobHandler(IJobClient jobClient, IJob activatedJob)
        {
            Log.LogInformation("Invoke REST call...");
            var response = httpClient.GetAsync("/").Result;
            Log.LogInformation("...finished. Complete Job...");
            await jobClient.NewCompleteJobCommand(activatedJob).Send();
            counter.inc();
        }
        private static async void NonBlockingAwaitJobHandler(IJobClient jobClient, IJob activatedJob)
        {
            Log.LogInformation("Invoke REST call...");
            var response = await httpClient.GetAsync("/");
            Log.LogInformation("...finished. Complete Job...");
            var result = await jobClient.NewCompleteJobCommand(activatedJob).Send();
            counter.inc();
        }
        private static void NonBlockingJobHandler(IJobClient jobClient, IJob activatedJob)
        {
            Log.LogInformation("Invoke REST call...");
            var response = httpClient.GetAsync("/").ContinueWith( response => {                
                Log.LogInformation("...finished. Complete Job...");
                jobClient.NewCompleteJobCommand(activatedJob).Send().ContinueWith( result => {
                    if (result.Exception==null) {
                        counter.inc();
                    } else {
                         Log.LogInformation("...could not do REST call because of: " + result.Exception);
                    }
                });

            });
        }
        private static IZeebeClient CreateZeebeClient()
        {
            return CamundaCloudClientBuilder
                .Builder()
                .UseClientId("9vdtiexfQ8Ae4pIi7w.nSSxI6ELp76yJ")
                .UseClientSecret("LJripeKNmFKOMI7Ps.cFeFRW3fiRW5gJ8mNTihMcAf6SqA8zlHUXroOghOB4xaJo")
                .UseContactPoint("de121ba4-f76d-49ad-8cd5-842e653be639.bru-2.zeebe.camunda.io:443")
                .UseLoggerFactory(LoggerFactory) // optional
                .Build();
        }

        private static void AwaitExitUserCmd()
        {
            Console.Write("Type any key to stop");
            Console.ReadLine();
        }
    }
}