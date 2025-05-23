package org.jenkinsci.plugins.webhookstep;

import hudson.util.Secret;
import jakarta.inject.Inject;
import java.io.Serial;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.SynchronousStepExecution;

public class RegisterWebhookExecution extends SynchronousStepExecution<WebhookToken> {

    @Serial
    private static final long serialVersionUID = -6718328636399912927L;

    private final Secret secretAuthToken;

    @Inject
    private transient RegisterWebhookStep step;

    public RegisterWebhookExecution(RegisterWebhookStep step, StepContext context, Secret secretAuthToken) {
        super(context);
        this.step = step;
        this.secretAuthToken = secretAuthToken;
    }

    public Secret getSecretAuthToken() {
        return this.secretAuthToken;
    }

    @Override
    public WebhookToken run() throws Exception {
        String token = (step == null || StringUtils.isEmpty(step.token))
                ? java.util.UUID.randomUUID().toString()
                : URLEncoder.encode(step.token, StandardCharsets.UTF_8);
        String jenkinsUrl = getContext().get(hudson.EnvVars.class).get("JENKINS_URL");
        if (jenkinsUrl == null || jenkinsUrl.isEmpty()) {
            throw new RuntimeException("JENKINS_URL must be set in the Manage Jenkins console");
        }
        java.net.URI baseUri = new java.net.URI(jenkinsUrl);
        java.net.URI relative = new java.net.URI("webhook-step/" + token);
        java.net.URI path = baseUri.resolve(relative);

        WebhookToken hook = new WebhookToken(token, path.toString(), this.secretAuthToken);
        WebhookRootAction.registerAuthToken(hook);
        return hook;
    }
}
