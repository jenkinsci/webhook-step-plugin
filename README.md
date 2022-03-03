Webhook Step Plugin
===================

This pipeline plugin provides an easy way to block a build pipeline until an
external system posts to a webhook.
It can be used to integrate long running tasks into a pipeline, without busy waiting.

A typical example is launching a performance test on a dedicated hardware/configuration.
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

```groovy
hook = registerWebhook()

echo "Waiting for POST to ${hook.url}"

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

For illustration, see the [scripted pipeline example](examples/scripted_pipeline).


Accessing hook data
-------------------

- **Token:** `hook.token` / `hook.getToken()`
- **Url:** `hook.url` / `hook.getUrl()`

###### Deprecation notice:

`getURL()` is *deprecated* and will be removed in a future release.


Specifying a fixed webhook name
-------------------------------

Instead of letting the plugin generate a unique webhook ID, you can provide a name at creation.
Like this: `hook = registerWebhook(token: "my_webhook")`.

The [declarative pipeline example](examples/declarative_pipeline) illustrates this.


**caveat**: if several job instances use the same token, only the most recent job will trigger.


Securing the webhook with an authentication token
-------------------------------------------------
It is possible to specify an authentication token.
This token has to be provided in the header of the webhook HTTP POST for the wait to complete.

To avoid secret leakage in the pipeline source code or logfiles, it is strongly advised to use a "secret text" credential.
The [declarative_withAuthToken](examples/declarative_withAuthToken) example illustrates how to use a webhook step authentication token stored as a secret (`webhook_secret`).

To trigger that webhook, the `curl` command would look like: `curl -X POST -d 'OK' -H "Authorization: 123" <JENKINS_URL>/webhook-step/test-webhook`
