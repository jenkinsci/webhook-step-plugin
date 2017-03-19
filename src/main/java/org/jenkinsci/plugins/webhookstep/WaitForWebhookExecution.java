package org.jenkinsci.plugins.webhookstep;

import org.jenkinsci.plugins.workflow.steps.AbstractStepExecutionImpl;
import org.jenkinsci.plugins.workflow.steps.StepContext;

public class WaitForWebhookExecution extends AbstractStepExecutionImpl {

    private static final long serialVersionUID = -148119134567863021L;

    WaitForWebhookStep step;

    public WaitForWebhookExecution(StepContext context, WaitForWebhookStep step) {
        super(context);
        this.step = step;
    }

    public String getToken() {
        return step.getToken();
    }

    @Override
    public boolean start() {
        String content = WebhookRootAction.registerWebhook(this);

        if (content != null) {
            getContext().onSuccess(content);
            return true;
        }

        return false;
    }

    @Override
    public void stop(Throwable cause) throws Exception {
        WebhookRootAction.deregisterWebhook(this);
        getContext().onFailure(cause);
    }

    @Override
    public void onResume() {
        super.onResume();
        start();
    }

    public void onTriggered(String content) {
        getContext().onSuccess(content);
    }

}
