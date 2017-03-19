Webhook Step Plugin
===================

This pipeline plugin provides an easy way to block a build pipeline until an
external system posts to a webhook. This can be used to integrate long running
tasks into a pipeline, without busy waiting.

It is already possible to wait for an external system to post [using the `input`
step](https://cpitman.github.io/jenkins/cicd/2017/03/16/waiting-for-remote-systems-in-a-jenkins-pipeline.html), 
but is more complex. To use `input`, an external system must authenticate 
to Jenkins, retrieve a Jenkins-Crumb for CSRF protection, then post data in an 
`input` specific format. This plugin uses unique tokens as an implicit form of
authentication and accepts any content that is posted.

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
Looking back at the Jenkins Job, it should now have completed and logged 
`Webhook called with data: OK`.