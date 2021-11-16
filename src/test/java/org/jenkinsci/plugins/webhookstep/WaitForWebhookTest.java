package org.jenkinsci.plugins.webhookstep;

import hudson.FilePath;
import hudson.model.Result;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.File;
import java.net.URL;

import static net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals;
import static net.javacrumbs.jsonunit.JsonAssert.when;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;
import static org.junit.Assert.assertTrue;

public class WaitForWebhookTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void testCreateSimpleWebhook() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "prj");

        p.setDefinition(new CpsFlowDefinition("def hook = registerWebhook()\necho \"hookurl=${hook.getURL()}\"", true));
        WorkflowRun b = p.scheduleBuild2(0).waitForStart();

        j.waitForCompletion(b);
        j.assertBuildStatus(Result.SUCCESS, b);
        j.assertLogContains("hookurl=", b);
    }

    @Test
    public void testUseCustomToken() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "prj");

        p.setDefinition(new CpsFlowDefinition("def hook = registerWebhook(token: \"test-token\")\necho \"token=${hook.token}\"", true));
        WorkflowRun b = p.scheduleBuild2(0).waitForStart();

        j.waitForCompletion(b);
        j.assertBuildStatus(Result.SUCCESS, b);
        j.assertLogContains("token=test-token", b);
    }

    @Test
    public void testWaitHook() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "prj");
        URL url = this.getClass().getResource("/simple.json");


        FilePath contentFilePath = new FilePath(new File(url.getFile()));
        String content = contentFilePath.readToString();

        p.setDefinition(new CpsFlowDefinition("def hook = registerWebhook(token: \"test-token\")\necho \"token=${hook.token}\"\ndef data = waitForWebhook(webhookToken: hook)\necho \"${data}\"", true));
        WorkflowRun r = p.scheduleBuild2(0).waitForStart();

        j.assertBuildStatus(null, r);

        j.postJSON("webhook-step/test-token", content);

        j.waitForCompletion(r);
        j.assertBuildStatus(Result.SUCCESS, r);
        j.assertLogContains("token=test-token", r);
        j.assertLogContains("\"action\":\"done\"", r);
    }

    @Test
    public void testWaitHookWithHeaders() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "prj");
        URL url = this.getClass().getResource("/simple.json");

        FilePath contentFilePath = new FilePath(new File(url.getFile()));
        String content = contentFilePath.readToString();

        p.setDefinition(new CpsFlowDefinition("def hook = registerWebhook(token: \"test-token\")\necho \"token=${hook.token}\"\ndef data = waitForWebhook(webhookToken: hook, withHeaders: true)\necho \"${data.content}\"\necho \"${data.headers.size()}\"\nfor(k in data.headers.keySet()) {  echo \"${k} -> ${data.headers[k]}\"}", true));
        WorkflowRun r = p.scheduleBuild2(0).waitForStart();

        j.assertBuildStatus(null, r);

        j.postJSON("webhook-step/test-token", content);

        j.waitForCompletion(r);
        j.assertBuildStatus(Result.SUCCESS, r);
        j.assertLogContains("token=test-token", r);
        j.assertLogContains("\"action\":\"done\"", r);
        j.assertLogContains("Jenkins-Crumb -> test", r);
    }

    @Test
    public void testLargeDataMessage() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "prj");
        URL url = this.getClass().getResource("/large.json");

        FilePath contentFilePath = new FilePath(new File(url.getFile()));
        String content = contentFilePath.readToString();

        p.setDefinition(new CpsFlowDefinition("node {\ndef hook = registerWebhook(token: \"test-token\")\necho \"token=${hook.token}\"\ndef data = waitForWebhook(hook)\nwriteFile(file: 'large.json', text: data)\n}", true));
        WorkflowRun r = p.scheduleBuild2(0).waitForStart();

        j.assertBuildStatus(null, r);

        j.postJSON("webhook-step/test-token", content);

        j.waitForCompletion(r);
        j.assertBuildStatus(Result.SUCCESS, r);

        FilePath outputFilePath = j.jenkins.getWorkspaceFor(p).child("large.json");
        assertTrue(outputFilePath.exists());
        j.assertLogContains("token=test-token", r);

        String output = outputFilePath.readToString();
        assertJsonEquals(content, output, when(IGNORING_ARRAY_ORDER));
    }
}
