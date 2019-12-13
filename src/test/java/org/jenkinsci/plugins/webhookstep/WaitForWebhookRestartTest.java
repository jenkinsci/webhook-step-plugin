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
import org.jenkinsci.plugins.workflow.test.steps.SemaphoreStep;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.model.Statement;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.RestartableJenkinsRule;

import java.io.File;
import java.net.URL;
import java.util.List;

import static net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals;
import static net.javacrumbs.jsonunit.JsonAssert.when;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class WaitForWebhookRestartTest {

    @Rule
    public RestartableJenkinsRule rr = new RestartableJenkinsRule();

    @Test
    public void testWaitHook() throws Exception {
        URL url = this.getClass().getResource("/simple.json");
        String content = FileUtils.readFileToString(new File(url.getFile()));

        rr.addStep(new Statement() {
            @Override
            public void evaluate() throws Throwable {
                WorkflowJob p = rr.j.jenkins.createProject(WorkflowJob.class, "prj");
                p.setDefinition(new CpsFlowDefinition(
                        "node {\n" +
                               "  def hook = registerWebhook(token: \"test-token\")\n" +
                               "  echo \"token=${hook.token}\"\n" +
                               "  semaphore 'started'\n" +
                               "  def data = waitForWebhook(hook)\n" +
                               "  echo \"${data}\"" +
                               "}"));

                WorkflowRun b = p.scheduleBuild2(0).getStartCondition().get();
                SemaphoreStep.waitForStart("started/1", b);
                assertTrue(JenkinsRule.getLog(b), b.isBuilding());
            }
        });

        rr.addStep(new Statement() {
            @Override
            public void evaluate() throws Throwable {
                WorkflowJob job = rr.j.jenkins.getItemByFullName("prj", WorkflowJob.class);
                assertNotNull(job);
                WorkflowRun run = job.getBuildByNumber(1);
                assertNotNull(run);

                SemaphoreStep.success("started/1", null);

                rr.j.postJSON("webhook-step/test-token", content);

                rr.j.waitForCompletion(run);
                rr.j.assertBuildStatus(Result.SUCCESS, run);
                rr.j.assertLogContains("token=test-token", run);
                rr.j.assertLogContains("\"action\":\"done\"", run);
            }
        });
    }
}
