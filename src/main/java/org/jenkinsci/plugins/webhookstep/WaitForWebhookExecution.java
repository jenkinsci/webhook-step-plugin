package org.jenkinsci.plugins.webhookstep;

import edu.umd.cs.findbugs.annotations.NonNull;
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
        WebhookResponse response = WebhookRootAction.registerWebhook(this);

        if (response != null) {
            if(step.isWithHeaders()) {
                getContext().onSuccess(response);
            } else {
                getContext().onSuccess(response.getContent());
            }
            return true;
        }

        return false;
    }

    @Override
    public void stop(@NonNull Throwable cause) {
        WebhookRootAction.deregisterWebhook(this);
        getContext().onFailure(cause);
    }

    @Override
    public void onResume() {
        super.onResume();
        start();
    }

    public void onTriggered(WebhookResponse response) {
        if(step.isWithHeaders()) {
            getContext().onSuccess(response);
        } else {
            getContext().onSuccess(response.getContent());
        }
    }

}
