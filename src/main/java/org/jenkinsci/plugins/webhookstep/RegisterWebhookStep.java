package org.jenkinsci.plugins.webhookstep;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.Extension;
import hudson.util.FormValidation;
import hudson.util.Secret;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

public class RegisterWebhookStep extends Step {

    // Token identifies the webhook
    String token;

    // authToken is the secret associated with the webHook
    private Secret secretAuthToken;

    @DataBoundConstructor
    public RegisterWebhookStep() {
        this.token = null;
        this.secretAuthToken = null;
    }

    public String getToken() {
        return this.token;
    }

    @DataBoundSetter
    public void setToken(String token) {
        this.token = token;
    }

    @DataBoundSetter
    public void setAuthToken(String authToken) {
        // Encrypt the clear text
        this.secretAuthToken = Secret.fromString(authToken);
    }

    public FormValidation doCheckToken(@QueryParameter String value) {
        if (StringUtils.isEmpty(value) || token.equals(URLEncoder.encode(token, StandardCharsets.UTF_8))) {
            return FormValidation.ok();
        }
        return FormValidation.warning(String.format("bad token [%s], it should be passed in urlencoded format", token));
    }

    @Override
    public StepExecution start(StepContext context) {
        return new RegisterWebhookExecution(this, context, this.secretAuthToken);
    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {
        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return Collections.singleton(EnvVars.class);
        }

        @Override
        public String getFunctionName() {
            return "registerWebhook";
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return "Creates and returns a webhook that can be used by an external system to notify a pipeline";
        }
    }
}
