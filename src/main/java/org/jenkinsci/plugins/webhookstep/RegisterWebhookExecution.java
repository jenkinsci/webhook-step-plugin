package org.jenkinsci.plugins.webhookstep;

import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContext;

public class RegisterWebhookExecution extends AbstractSynchronousStepExecution<WebhookToken> {

    private static final long serialVersionUID = -6718328636399912927L;
    private final String authToken;

    public RegisterWebhookExecution(StepContext context, String authToken) {
        super(context);
        this.authToken = authToken;
    }

    public String getAuthToken() {
        return this.authToken;
    }

    @Override
    public WebhookToken run() throws Exception {
        String token = java.util.UUID.randomUUID().toString();
        String jenkinsUrl = getContext().get(hudson.EnvVars.class).get("JENKINS_URL");
        if (jenkinsUrl == null || jenkinsUrl.isEmpty()) {
            throw new RuntimeException("JENKINS_URL must be set in the Manage Jenkins console");
        }
        java.net.URI baseUri = new java.net.URI(jenkinsUrl);
        java.net.URI relative = new java.net.URI("webhook-step/" + token);
        java.net.URI path = baseUri.resolve(relative);

        WebhookToken hook = new WebhookToken(
            token, path.toString(), this.authToken);
        WebhookRootAction.registerAuthToken(hook);
        return hook;
    }
}
