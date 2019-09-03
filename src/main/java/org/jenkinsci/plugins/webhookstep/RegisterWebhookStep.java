package org.jenkinsci.plugins.webhookstep;

import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;

public class RegisterWebhookStep extends Step {

    private final String authToken;

    @DataBoundConstructor
    public RegisterWebhookStep(String authToken) {
        this.authToken = authToken;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new RegisterWebhookExecution(context, this.authToken);
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
