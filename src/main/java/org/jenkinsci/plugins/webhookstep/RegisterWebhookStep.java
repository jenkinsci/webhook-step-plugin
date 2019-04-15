package org.jenkinsci.plugins.webhookstep;

import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class RegisterWebhookStep extends Step {

    final String token;

    @DataBoundConstructor
    public RegisterWebhookStep() {
        this.token = null;
    }

    public RegisterWebhookStep(String token) throws UnsupportedEncodingException {
        if(token != null && !token.equals(URLEncoder.encode(token, "UTF-8"))) {
            throw new IllegalArgumentException(
                    String.format("bad token [%s], it should be passed in urlencoded format [%s]",
                            token, URLEncoder.encode(token, "UTF-8")));
        }
        this.token = token;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new RegisterWebhookExecution(this, context);
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(RegisterWebhookExecution.class);
        }

        @Override
        public String getFunctionName() {
            return "registerWebhook";
        }

        @Override
        public String getDisplayName() {
            return "Creates and returns a webhook that can be used by an external system to notify a pipeline";
        }
    }
}
