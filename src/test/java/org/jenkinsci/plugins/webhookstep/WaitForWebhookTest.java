package org.jenkinsci.plugins.webhookstep;

import hudson.FilePath;
import hudson.model.Result;

import org.hamcrest.Matchers;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.jvnet.hudson.test.JenkinsRule;


import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;

import static net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals;
import static net.javacrumbs.jsonunit.JsonAssert.when;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

public class WaitForWebhookTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Rule
    public TestName name = new TestName();

    @Test
    public void testCreateSimpleWebhook() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "prj");

        StringBuilder pipelineCode = new StringBuilder();
        pipelineCode.append("def hook = registerWebhook()" + "\n");
        pipelineCode.append("echo \"hookurl=${hook.getURL()}\"" + "\n");

        p.setDefinition(new CpsFlowDefinition(pipelineCode.toString(), true));
        WorkflowRun b = p.scheduleBuild2(0).waitForStart();

        j.waitForCompletion(b);
        j.assertBuildStatus(Result.SUCCESS, b);
        j.assertLogContains("hookurl=", b);
    }

    @Test
    public void testUseCustomToken() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "prj");

        StringBuilder pipelineCode = new StringBuilder();
        pipelineCode.append("def hook = registerWebhook(token: \"test-token\") \n");
        pipelineCode.append("echo \"token=${hook.token}\" \n");

        p.setDefinition(new CpsFlowDefinition(pipelineCode.toString(), true));
        WorkflowRun b = p.scheduleBuild2(0).waitForStart();

        j.waitForCompletion(b);
        j.assertBuildStatus(Result.SUCCESS, b);
        j.assertLogContains("token=test-token", b);
    }

    @Test
    public void testWaitHook() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "prj");
        URL url = this.getClass().getResource("/simple.json");

        //build a webhook name that is unique for this test
        String webHook_ID = "test-token_" + name.getMethodName();

        StringBuilder pipelineCode = new StringBuilder();
        pipelineCode.append("def hook = registerWebhook(token: \"" + webHook_ID + "\")  \n");
        pipelineCode.append("echo \"token=${hook.token}\"  \n");
        pipelineCode.append("def data = waitForWebhook(webhookToken: hook)  \n");
        pipelineCode.append("echo \"${data}\"  \n");


        FilePath contentFilePath = new FilePath(new File(url.getFile()));
        String content = contentFilePath.readToString();

        //Tokens with the same custom token tend to overwrite themselves and give a 202 return instead of 200. Non deterministic behavior warning.
        p.setDefinition(new CpsFlowDefinition(pipelineCode.toString(), true));
        WorkflowRun r = p.scheduleBuild2(0).waitForStart();

        j.assertBuildStatus(null, r);

        //Use a WebCLient to send a json file to trigger the webhook
        WebResponse webResponse = trigger_webhook("webhook-step/" + webHook_ID, content);
        assertThat("Triggering the webhook should succeed", webResponse.getStatusCode(), Matchers.is(200));

        //wait for the pipeline to continue
        j.waitForCompletion(r);
        j.assertBuildStatus(Result.SUCCESS, r);
        j.assertLogContains("token=" + webHook_ID, r);
        j.assertLogContains("\"action\":\"done\"", r);
    }

    @Test
    public void testWaitHookWithHeaders() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "prj");
        URL url = this.getClass().getResource("/simple.json");

        //build a webhook name that is unique for this test
        String webHook_ID = "test-token_" + name.getMethodName();

        FilePath contentFilePath = new FilePath(new File(url.getFile()));
        String content = contentFilePath.readToString();

        StringBuilder pipelineCode = new StringBuilder();
        pipelineCode.append("def hook = registerWebhook(token: \"" + webHook_ID + "\")  \n");
        pipelineCode.append("echo \"token=${hook.token}\"  \n");
        pipelineCode.append("def data = waitForWebhook(webhookToken: hook, withHeaders: true)  \n");
        pipelineCode.append("echo \"${data.content}\"  \n");
        pipelineCode.append("echo \"${data.headers.size()}\"  \n");
        pipelineCode.append("for(k in data.headers.keySet()) {  echo \"${k} -> ${data.headers[k]}\"}  \n");

        p.setDefinition(new CpsFlowDefinition(pipelineCode.toString(), true));
        WorkflowRun r = p.scheduleBuild2(0).waitForStart();

        j.assertBuildStatus(null, r);

        //Use a WebCLient to send a json file to trigger the webhook
        WebResponse webResponse = trigger_webhook("webhook-step/" + webHook_ID, content);
        assertThat("Triggering the webhook should succeed", webResponse.getStatusCode(), Matchers.is(200));

        //wait for the pipeline to continue
        j.waitForCompletion(r);
        j.assertBuildStatus(Result.SUCCESS, r);
        j.assertLogContains("token=" + webHook_ID, r);
        j.assertLogContains("\"action\":\"done\"", r);
    }

    @Test
    public void testWaitAuthHook_sendNoAuthToken() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "prj");
        URL url = this.getClass().getResource("/simple.json");

        //build a webhook name that is unique for this test
        String webHook_ID = "test-token_" + name.getMethodName();     
        String testAuthToken = "123";  

        StringBuilder pipelineCode = new StringBuilder();
        pipelineCode.append("def hook = registerWebhook(token: \"" + webHook_ID + "\", authToken: \"" + testAuthToken + "\")  \n");
        pipelineCode.append("echo \"token=${hook.token}\"  \n");
        pipelineCode.append("def data = waitForWebhook(webhookToken: hook)  \n");
        pipelineCode.append("echo \"${data}\"  \n");

        FilePath contentFilePath = new FilePath(new File(url.getFile()));
        String content = contentFilePath.readToString();

        p.setDefinition(new CpsFlowDefinition(pipelineCode.toString(), true));
        WorkflowRun r = p.scheduleBuild2(0).waitForStart();

        j.assertBuildStatus(null, r);

        //Use a WebClient to send a json file to trigger the webhook
        try {
            trigger_webhook("webhook-step/" + webHook_ID, content);
        } catch(Exception ex) {
            if(ex.getClass() == FailingHttpStatusCodeException.class) {
                FailingHttpStatusCodeException httpException = (FailingHttpStatusCodeException) ex;
                assertThat("Triggering the webhook with wrong authentication should fail", httpException.getStatusCode(), Matchers.is(403));
            } 
            else {
                assertTrue("Unexpected exception occured.", false);
            }
        }

        //try again but with the correct webhook
        WebResponse webResponse = trigger_authenticated_webhook("webhook-step/" + webHook_ID, content, testAuthToken);
        assertThat("Triggering the webhook return a HTTP OK.", webResponse.getStatusCode(), Matchers.is(200));

        // //wait for the pipeline to continue
        j.waitForCompletion(r);
        j.assertBuildStatus(Result.SUCCESS, r);
    }

    @Test
    public void testWaitAuthHook_sendGoodAuthToken() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "prj");
        URL url = this.getClass().getResource("/simple.json");

        //build a webhook name that is unique for this test
        String webHook_ID = "test-token_" + name.getMethodName();      

        String testAuthToken = "123";  

        StringBuilder pipelineCode = new StringBuilder();
        pipelineCode.append("def hook = registerWebhook(token: \"" + webHook_ID + "\", authToken: \"" + testAuthToken + "\")  \n");
        pipelineCode.append("echo \"token=${hook.token}\"  \n");
        pipelineCode.append("def data = waitForWebhook(webhookToken: hook)  \n");
        pipelineCode.append("echo \"${data}\"  \n");

        FilePath contentFilePath = new FilePath(new File(url.getFile()));
        String content = contentFilePath.readToString();

        p.setDefinition(new CpsFlowDefinition(pipelineCode.toString(), true));
        WorkflowRun r = p.scheduleBuild2(0).waitForStart();

        j.assertBuildStatus(null, r);

        //Use a WebCLient to send a json file to trigger the webhook
        WebResponse webResponse = trigger_authenticated_webhook("webhook-step/" + webHook_ID, content, testAuthToken);
        assertThat("Triggering the webhook without authentication should fail", webResponse.getStatusCode(), Matchers.is(200));

        //wait for the pipeline to continue
        j.waitForCompletion(r);
        j.assertBuildStatus(Result.SUCCESS, r);
        j.assertLogContains("token=" + webHook_ID, r);
        j.assertLogContains("\"action\":\"done\"", r);
    }   

    @Test
    public void testWaitAuthHook_sendIncorrectAuthToken() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "prj");
        URL url = this.getClass().getResource("/simple.json");

        //build a webhook name that is unique for this test
        String webHook_ID = "test-token_" + name.getMethodName();      
    
        String testAuthToken = "123";  

        StringBuilder pipelineCode = new StringBuilder();
        pipelineCode.append("def hook = registerWebhook(token: \"" + webHook_ID + "\", authToken: \"" + testAuthToken + "\")  \n");
        pipelineCode.append("echo \"token=${hook.token}\"  \n");
        pipelineCode.append("def data = waitForWebhook(webhookToken: hook)  \n");
        pipelineCode.append("echo \"${data}\"  \n");

        FilePath contentFilePath = new FilePath(new File(url.getFile()));
        String content = contentFilePath.readToString();

        p.setDefinition(new CpsFlowDefinition(pipelineCode.toString(), true));
        WorkflowRun r = p.scheduleBuild2(0).waitForStart();

        j.assertBuildStatus(null, r);

        
        //Use a WebCLient to send a json file to trigger the webhook with a wrong password
        try {
            trigger_authenticated_webhook("webhook-step/" + webHook_ID, content, "badAuthToken");
        } catch(Exception ex) {
            if(ex.getClass() == FailingHttpStatusCodeException.class) {
                FailingHttpStatusCodeException httpException = (FailingHttpStatusCodeException) ex;
                assertThat("Triggering the webhook with wrong authentication should return a HTTP forbidden error", httpException.getStatusCode(), Matchers.is(403));
            } 
            else {
                assertTrue("Unexpected exception occured.", false);
            }
        }

        //try again but with the correct webhook
        WebResponse webResponse = trigger_authenticated_webhook("webhook-step/" + webHook_ID, content, "123");
        assertThat("Triggering the webhook return a HTTP OK.", webResponse.getStatusCode(), Matchers.is(200));

        // //wait for the pipeline to continue
        j.waitForCompletion(r);
        j.assertBuildStatus(Result.SUCCESS, r);
    }

    @Test
    public void testLargeDataMessage() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "prj");
        URL url = this.getClass().getResource("/large.json");

        //build a webhook name that is unique for this test
        String webHook_ID = "test-token_" + name.getMethodName();

        FilePath contentFilePath = new FilePath(new File(url.getFile()));
        String content = contentFilePath.readToString();

        StringBuilder pipelineCode = new StringBuilder();
        pipelineCode.append("node {  \n");
        pipelineCode.append("   def hook = registerWebhook(token: \"" + webHook_ID + "\")  \n");
        pipelineCode.append("   echo \"token=${hook.token}\"  \n");
        pipelineCode.append("   def data = waitForWebhook(hook)\n");
        pipelineCode.append("   writeFile(file: 'large.json', text: data)\n");
        pipelineCode.append("}  \n");

        p.setDefinition(new CpsFlowDefinition(pipelineCode.toString(), true));
        WorkflowRun r = p.scheduleBuild2(0).waitForStart();

        j.assertBuildStatus(null, r);

        //Use a WebCLient to send a json file to trigger the webhook
        WebResponse webResponse = trigger_webhook("webhook-step/" + webHook_ID, content);
        assertThat("Triggering the webhook should succeed", webResponse.getStatusCode(), Matchers.is(200));

        //wait for the pipeline to continue
        j.waitForCompletion(r);
        j.assertBuildStatus(Result.SUCCESS, r);

        FilePath outputFilePath = j.jenkins.getWorkspaceFor(p).child("large.json");
        assertTrue(outputFilePath.exists());
        j.assertLogContains("token=" + webHook_ID, r);

        String output = outputFilePath.readToString();
        assertJsonEquals(content, output, when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Sends a json file to the specified url to trigger the webhook
     * @param webhook_path the path to the webhook
     * @param content the json payload
     * @return the webResponse from the webhook
     */
    public WebResponse trigger_webhook(String webhook_path, String content) throws MalformedURLException, IOException{
        JenkinsRule.WebClient wc = j.createWebClient();
        URL URLtoCall = new URL(j.getURL(), webhook_path);
        wc.addRequestHeader("content-type", "application/json");
        WebRequest webRequest = new WebRequest(URLtoCall, HttpMethod.POST);
        webRequest.setRequestBody(content);
        return(wc.getPage(webRequest).getWebResponse());
    }

        /**
     * Sends a json file to the specified url to trigger the webhook
     * @param webhook_path the path to the webhook
     * @param content the json payload
     * @param authToken the autentication token
     * @return the webResponse from the webhook
     */
    public WebResponse trigger_authenticated_webhook(String webhook_path, String content, String authToken) throws MalformedURLException, IOException{
        JenkinsRule.WebClient wc = j.createWebClient();
        URL URLtoCall = new URL(j.getURL(), webhook_path);
        wc.addRequestHeader("content-type", "application/json");
        wc.addRequestHeader("Authorization", authToken);
        WebRequest webRequest = new WebRequest(URLtoCall, HttpMethod.POST);
        webRequest.setRequestBody(content);
        return(wc.getPage(webRequest).getWebResponse());
    }
}