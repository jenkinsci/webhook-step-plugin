package org.jenkinsci.plugins.webhookstep;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import hudson.FilePath;
import hudson.model.Result;
import java.io.File;
import java.net.URL;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.test.steps.SemaphoreStep;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.RestartableJenkinsRule;

public class WaitForWebhookRestartTest {

    @Rule
    public RestartableJenkinsRule rr = new RestartableJenkinsRule();

    @Test
    public void testWaitHook() throws Exception {
        URL url = this.getClass().getResource("/simple.json");

        FilePath contentFilePath = new FilePath(new File(url.getFile()));
        String content = contentFilePath.readToString();

        rr.then(rr -> {
            WorkflowJob p = rr.jenkins.createProject(WorkflowJob.class, "prj");
            p.setDefinition(new CpsFlowDefinition(
                    "node {\n" + "  def hook = registerWebhook(token: \"test-token\")\n"
                            + "  echo \"token=${hook.token}\"\n"
                            + "  semaphore 'started'\n"
                            + "  def data = waitForWebhook(hook)\n"
                            + "  echo \"${data}\""
                            + "}",
                    true));

            WorkflowRun b = p.scheduleBuild2(0).getStartCondition().get();
            SemaphoreStep.waitForStart("started/1", b);
            assertTrue(JenkinsRule.getLog(b), b.isBuilding());
        });

        rr.then(rr -> {
            WorkflowJob job = rr.jenkins.getItemByFullName("prj", WorkflowJob.class);
            assertNotNull(job);
            WorkflowRun run = job.getBuildByNumber(1);
            assertNotNull(run);

            SemaphoreStep.success("started/1", null);

            rr.postJSON("webhook-step/test-token", content);

            rr.waitForCompletion(run);
            rr.assertBuildStatus(Result.SUCCESS, run);
            rr.assertLogContains("token=test-token", run);
            rr.assertLogContains("\"action\":\"done\"", run);
        });
    }

    @Test
    public void testSuspendAfterWebhookResponse() throws Exception {
        URL url = this.getClass().getResource("/simple.json");

        FilePath contentFilePath = new FilePath(new File(url.getFile()));
        String content = contentFilePath.readToString();

        rr.then(rr -> {
            WorkflowJob p = rr.jenkins.createProject(WorkflowJob.class, "prj");
            p.setDefinition(new CpsFlowDefinition(
                    "node {\n" + "  def hook = registerWebhook(token: \"test-token\")\n"
                            + "  echo \"token=${hook.token}\"\n"
                            + "  def data = waitForWebhook(hook)\n"
                            + "  semaphore 'complete'\n"
                            + "  echo \"${data}\""
                            + "}",
                    true));

            WorkflowRun b = p.scheduleBuild2(0).getStartCondition().get();

            rr.postJSON("webhook-step/test-token", content);

            SemaphoreStep.waitForStart("complete/1", b);
            assertTrue(JenkinsRule.getLog(b), b.isBuilding());
        });

        rr.then(rr -> {
            WorkflowJob job = rr.jenkins.getItemByFullName("prj", WorkflowJob.class);
            assertNotNull(job);
            WorkflowRun run = job.getBuildByNumber(1);
            assertNotNull(run);

            SemaphoreStep.success("complete/1", null);

            rr.waitForCompletion(run);
            rr.assertBuildStatus(Result.SUCCESS, run);
            rr.assertLogContains("token=test-token", run);
            rr.assertLogContains("\"action\":\"done\"", run);
        });
    }

    @Test
    public void testSuspendAfterWebhookResponseWithHeaders() throws Exception {
        URL url = this.getClass().getResource("/simple.json");

        FilePath contentFilePath = new FilePath(new File(url.getFile()));
        String content = contentFilePath.readToString();

        rr.then(rr -> {
            WorkflowJob p = rr.jenkins.createProject(WorkflowJob.class, "prj");
            p.setDefinition(new CpsFlowDefinition(
                    "node {\n" + "  def hook = registerWebhook(token: \"test-token\")\n"
                            + "  echo \"token=${hook.token}\"\n"
                            + "  def response = waitForWebhook(webhookToken: hook, withHeaders: true)\n"
                            + "  semaphore 'complete'\n"
                            + "  echo \"${response.content}\""
                            + "}",
                    true));

            WorkflowRun b = p.scheduleBuild2(0).getStartCondition().get();

            rr.postJSON("webhook-step/test-token", content);

            SemaphoreStep.waitForStart("complete/1", b);
            assertTrue(JenkinsRule.getLog(b), b.isBuilding());
        });

        rr.then(rr -> {
            WorkflowJob job = rr.jenkins.getItemByFullName("prj", WorkflowJob.class);
            assertNotNull(job);
            WorkflowRun run = job.getBuildByNumber(1);
            assertNotNull(run);

            SemaphoreStep.success("complete/1", null);

            rr.waitForCompletion(run);
            rr.assertBuildStatus(Result.SUCCESS, run);
            rr.assertLogContains("token=test-token", run);
            rr.assertLogContains("\"action\":\"done\"", run);
        });
    }
}
