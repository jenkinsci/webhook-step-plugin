package org.jenkinsci.plugins.webhookstep;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.util.FormValidation;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RegisterWebhookStep extends Step {

    // Token identifies the webhook
    String token; 

    // authToken is the secret associated with the webHook
    private final String authToken;

    @DataBoundConstructor
    public RegisterWebhookStep() {
        this.token = null;
        this.authToken = null;
    }

    public RegisterWebhookStep(String authToken) {
        this.token = null;
        this.authToken = authToken;
    }

    public String getToken() {
        return this.token;
    }

    @DataBoundSetter
    public void setToken(String token) {
        this.token = token;
    }

    public FormValidation doCheckToken(@QueryParameter String value) {
        try {
            if (StringUtils.isEmpty(value) || token.equals(URLEncoder.encode(token, "UTF-8"))) {
                return FormValidation.ok();
            }
        } catch (UnsupportedEncodingException e) {
            Logger.getLogger(this.getClass().getName())
                    .log(Level.FINE, String.format("bad token: %s", token), e);
            return FormValidation.warning(String.format(
                    "bad encoding for token [%s]: %s", token, e.getLocalizedMessage()));
        }
        return FormValidation.warning(String.format("bad token [%s], it should be passed in urlencoded format", token));
    }

    @Override
    public StepExecution start(StepContext context) {
        return new RegisterWebhookExecution(this, context, this.authToken);
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
