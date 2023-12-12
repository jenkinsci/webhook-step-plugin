package org.jenkinsci.plugins.webhookstep;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import java.io.Serializable;
import java.util.Collections;
import java.util.Set;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

public class WaitForWebhookStep extends Step implements Serializable {

    private static final long serialVersionUID = -667001655472658819L;

    private final String token;
    private boolean withHeaders;

    @DataBoundConstructor
    public WaitForWebhookStep(WebhookToken webhookToken) {
        this.token = webhookToken.getToken();
    }

    public String getToken() {
        return token;
    }

    public boolean isWithHeaders() {
        return withHeaders;
    }

    @DataBoundSetter
    public void setWithHeaders(boolean withHeaders) {
        this.withHeaders = withHeaders;
    }

    @Override
    public StepExecution start(StepContext context) {
        return new WaitForWebhookExecution(context, this);
    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {

        @Override
        public String getFunctionName() {
            return "waitForWebhook";
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return "Wait for webhook to be POSTed to by external system";
        }

        @Override
        public Set<Class<?>> getRequiredContext() {
            return Collections.emptySet();
        }
    }
}
