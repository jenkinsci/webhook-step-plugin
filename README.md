Webhook Step Plugin
===================

This pipeline plugin provides an easy way to block a build pipeline until an
external system posts to a webhook. 
It can be used to integrate long running tasks into a pipeline, without busy waiting. 

A typical example is lauching a performance test on a dedicated hardware/configuration. 
When the task completes, it can easily notify the waiting pipeline. 
The webhook payload can be used to provide information like results or other useful data.


See the original discussion ["Waiting for Remote Systems in a Jenkins Pipeline"](https://cpitman.github.io/jenkins/cicd/2017/03/16/waiting-for-remote-systems-in-a-jenkins-pipeline.html).
It explains the context that triggered writing this plugin.

Usage
-----

Using this plugin will usually require 3 steps:

1. Register a webhook
2. Start a long running task while providing the webhook url for callback
3. Wait for the webhook to be executed

For example, the following pipeline script writes out the webhook url to the log
and waits for a user to call it:

```
hook = registerWebhook()

echo "Waiting for POST to ${hook.getURL()}"

data = waitForWebhook hook
echo "Webhook called with data: ${data}"
```

When this job is executed, something like the following log is printed:

```
Waiting for POST to http://localhost:8080/webhook-step/bef13807-a161-4193-ab95-6cb974afc71d
```

To continue the pipeline, we can post to this url. To do this with curl, execute
`curl -X POST -d 'OK' http://localhost:8080/webhook-step/bef13807-a161-4193-ab95-6cb974afc71d`.

The blocking `waitForWebhook` call will then complete. 
The returned data is the posted JSON payload.
With the above curl example it would be `OK`. 
The log file will thus show `Webhook called with data: OK`.

See example pipelines in the `examples` directory.

CAVEAT
------

* There is no secret associated with the webhook.
It is thus sufficient to know the full webhook URL to trigger it.