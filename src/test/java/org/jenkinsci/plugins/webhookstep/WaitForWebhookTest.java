package org.jenkinsci.plugins.webhookstep;

import com.gargoylesoftware.htmlunit.WebClient;
import hudson.FilePath;
import hudson.model.Result;
import jdk.nashorn.internal.parser.JSONParser;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.File;
import java.net.URL;
import java.util.List;

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

        p.setDefinition(new CpsFlowDefinition("def hook = registerWebhook()\necho \"hookurl=${hook.url}\""));
        WorkflowRun b = p.scheduleBuild2(0).waitForStart();

        j.waitForCompletion(b);
        j.assertBuildStatus(Result.SUCCESS, b);
        j.assertLogContains("hookurl=", b);
    }

    @Test
    public void testUseCustomToken() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "prj");

        p.setDefinition(new CpsFlowDefinition("def hook = registerWebhook(token: \"test-token\")\necho \"token=${hook.token}\""));
        WorkflowRun b = p.scheduleBuild2(0).waitForStart();

        j.waitForCompletion(b);
        j.assertBuildStatus(Result.SUCCESS, b);
        j.assertLogContains("token=test-token", b);
    }

    @Test
    public void testWaitHook() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "prj");
        URL url = this.getClass().getResource("/simple.json");
        String content = FileUtils.readFileToString(new File(url.getFile()));

        p.setDefinition(new CpsFlowDefinition("def hook = registerWebhook(token: \"test-token\")\necho \"token=${hook.token}\"\ndef data = waitForWebhook(webhookToken: hook)\necho \"${data}\""));
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
        String content = FileUtils.readFileToString(new File(url.getFile()));

        p.setDefinition(new CpsFlowDefinition("def hook = registerWebhook(token: \"test-token\")\necho \"token=${hook.token}\"\ndef data = waitForWebhook(webhookToken: hook, withHeaders: true)\necho \"${data.content}\"\necho \"${data.headers.size()}\"\nfor(k in data.headers.keySet()) {  echo \"${k} -> ${data.headers[k]}\"}"));
        WorkflowRun r = p.scheduleBuild2(0).waitForStart();

        j.assertBuildStatus(null, r);

        j.postJSON("webhook-step/test-token", content);

        j.waitForCompletion(r);
        j.assertBuildStatus(Result.SUCCESS, r);
        j.assertLogContains("token=test-token", r);
        j.assertLogContains("\"action\":\"done\"", r);
        j.assertLogContains("Cache-Control -> no-cache", r);
    }

    @Test
    public void testLargeDataMessage() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "prj");
        URL url = this.getClass().getResource("/large.json");
        String content = FileUtils.readFileToString(new File(url.getFile()));

        p.setDefinition(new CpsFlowDefinition("node {\ndef hook = registerWebhook(token: \"test-token\")\necho \"token=${hook.token}\"\ndef data = waitForWebhook(hook)\nwriteFile(file: 'large.json', text: data)\n}", true));
        WorkflowRun r = p.scheduleBuild2(0).waitForStart();

        j.assertBuildStatus(null, r);

        j.postJSON("webhook-step/test-token", content);

        j.waitForCompletion(r);
        j.assertBuildStatus(Result.SUCCESS, r);

        FilePath output = j.jenkins.getWorkspaceFor(p).child("large.json");
        assertTrue(output.exists());
        j.assertLogContains("token=test-token", r);

        assertJsonEquals(content, IOUtils.toString(output.read()), when(IGNORING_ARRAY_ORDER));
    }
}
