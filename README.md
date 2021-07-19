# Camunda Cloud Workers And Parallelism - Sample Code

Service tasks within Camunda Cloud require you to set a task type and implement job workers who perform whatever needs to be performed. These workers can control the number of jobs retrieved at once and you will have a number of jobs in your local application that need to be processed.

![Workers in Camunda Cloud](https://cdn-images-1.medium.com/max/1200/0*5AeSSJuIZvZD3FCd)

Now you can write blocking or non-blockin workers which will heavily influence scalability of your worker.

The worst-case in terms of scalability is that you process the jobs sequentially one after the other. While this sounds bad, it is valid for many use cases. Most projects I know do not need any parallel processing in the worker code as they simply do not care whether a job is executed a second earlier or later. Think of a business process that is executed only some hundred times per day and includes mostly human tasks - a sequential worker is totally sufficient (congratulations, this means you can safely skip this section of the blog post).

However, you might need to do better and process jobs in parallel and utilize the full power of your worker's CPUs. In such a case, you should read on and understand the difference between writing blocking and non-blocking code.

# Background reading

The code in this repository was developed while writing this blog post: **[Writing Good Workers For Camunda Cloud](https://blog.bernd-ruecker.com/writing-good-workers-for-camunda-cloud-61d322cad862)**

# Walk through recording

<a href="http://www.youtube.com/watch?feature=player_embedded&v=ZHKz9l5yG3Q" target="_blank"><img src="http://img.youtube.com/vi/ZHKz9l5yG3Q/0.jpg" alt="Walkthrough" width="240" height="180" border="10" /></a>

# What you find here: Sample code and results

Now this repository contains

#1: [REST Server](rest-server-with-latency/) that responds to REST requests with a delay of 100ms to simulate latency of a REST request

#2: Sample code for blocking and non-blocking workers in different languages:

* [Java](java-worker/)
* [NodeJs](nodejs-worker/)
* [C#](csharp-worker/)
* [Go](go-worker/)

#3: [Log statements](results/) from my experiments where you can see how workers behave without running them.
