package org.jenkinsci.plugins.webhookstep;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.SynchronousStepExecution;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.net.URLEncoder;

public class RegisterWebhookExecution extends SynchronousStepExecution<WebhookToken> {

    private static final long serialVersionUID = -6718328636399912927L;

    //TODO: remove the FindBugs warning
    // From a discussion with Jesse Glick:
    //   The warning is valid though it could be suppressed. Basically you would need to 
    //   ensure that `step` is never used after a Jenkins restart. Since this is a 
    //   `SynchronousStepExecution`, and there is no support yet for retrying those after 
    //   restart, it would not be. But saving a `Step` as a field in a `StepExecution` is 
    //   generally a bad idea, even if you make it `non-transient` and `Serializable`. 
    //   Better to extract the primitive-like fields you will need in the execution and 
    //   save those.
    //
    //   In this case, make `token` a field rather than `step` and initialize it in 
    //   the constructor.
    //
    //   (Whether the `UUID.randomUUID()` fallback should be in the ctor or `run` does not 
    //   currently matter, but again could if we ever allow a `SynchronousStepExecution` 
    //   to opt into being marked idempotent and retrying.)

    @SuppressFBWarnings(value = "SE_TRANSIENT_FIELD_NOT_RESTORED")
    private transient RegisterWebhookStep step;

    public RegisterWebhookExecution(RegisterWebhookStep step, StepContext context) {
        super(context);
        this.step = step;
    }

    @Override
    public WebhookToken run() throws Exception {
        String token = (step == null || StringUtils.isEmpty(step.token))
                ? java.util.UUID.randomUUID().toString()
                : URLEncoder.encode(step.token, "UTF-8");
        String jenkinsUrl = getContext().get(hudson.EnvVars.class).get("JENKINS_URL");
        if (jenkinsUrl == null || jenkinsUrl.isEmpty()) {
            throw new RuntimeException("JENKINS_URL must be set in the Manage Jenkins console");
        }
        java.net.URI baseUri = new java.net.URI(jenkinsUrl);
        java.net.URI relative = new java.net.URI("webhook-step/" + token);
        java.net.URI path = baseUri.resolve(relative);

        return new WebhookToken(token, path.toString());
    }
}
