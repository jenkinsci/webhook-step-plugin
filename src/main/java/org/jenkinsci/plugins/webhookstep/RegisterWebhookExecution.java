package org.jenkinsci.plugins.webhookstep;

import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContext;

import javax.inject.Inject;

public class RegisterWebhookExecution extends AbstractSynchronousStepExecution<WebhookToken> {

    private static final long serialVersionUID = -6718328636399912927L;

    @Inject
    private transient RegisterWebhookStep step;

    public RegisterWebhookExecution(RegisterWebhookStep step, StepContext context) {
        super(context);
        this.step = step;
    }

    @Override
    public WebhookToken run() throws Exception {
        String token = (step == null || step.token  == null )
                ? java.util.UUID.randomUUID().toString()
                : step.token;
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
