package org.jenkinsci.plugins.webhookstep;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import hudson.Extension;
import hudson.model.UnprotectedRootAction;
import hudson.security.csrf.CrumbExclusion;
import org.kohsuke.stapler.verb.POST;

import java.io.BufferedReader;
import java.nio.CharBuffer;
import java.util.Map;
import java.util.logging.Logger;

@Extension
public class WebhookRootAction extends CrumbExclusion implements UnprotectedRootAction {

    private static final HashMap<String, WaitForWebhookExecution> webhooks = new HashMap<>();
    private static final HashMap<String, WebhookResponse> alreadyPosted = new HashMap<>();

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public String getIconFileName() {
        return null;
    }

    @Override
    public String getUrlName() {
        return "webhook-step";
    }

    @POST
    public void doDynamic(StaplerRequest request, StaplerResponse response) {
        String token = request.getOriginalRestOfPath().substring(1); //Strip leading slash

        CharBuffer dest = CharBuffer.allocate(1024);
        StringBuilder content = new StringBuilder();
        try {
            BufferedReader reader = request.getReader();
            int len;

            while ((len = reader.read(dest)) > 0) {
                dest.rewind();
                dest.limit(len);
                content.append(dest);
                dest.limit(dest.capacity());
            }
        } catch (IOException e) {
            response.setStatus(400);
            return;
        }

        Map<String, String> headers = new HashMap<>();
        Enumeration<String> iter = request.getHeaderNames();
        while(iter.hasMoreElements()) {
            String header = iter.nextElement();
            headers.put(header, request.getHeader(header));
        }

        WebhookResponse whResponse = new WebhookResponse(content.toString(), headers);

        Logger.getLogger(WebhookRootAction.class.getName())
                .info("Webhook called with " + token);

        WaitForWebhookExecution exec;
        synchronized (webhooks) {
            exec = webhooks.remove(token);
            if (exec == null) {
                //pipeline has not yet waited on webhook, add an entry to track
                //that it was already triggered
                alreadyPosted.put(token, whResponse);
            }
        }

        if (exec != null) {
            exec.onTriggered(whResponse);
            response.setHeader("Result", "WebhookTriggered");
            response.setStatus(200);
        } else {
            response.setStatus(202);
        }

    }

    //Returns null when the webhook has been registered, the content when the webhook has already been called
    public static WebhookResponse registerWebhook(WaitForWebhookExecution exec) {
        Logger.getLogger(WebhookRootAction.class.getName())
                .info("Registering webhook with token " + exec.getToken());
        synchronized (webhooks) {
            if (alreadyPosted.containsKey(exec.getToken())) {
                return alreadyPosted.remove(exec.getToken());
            }

            webhooks.put(exec.getToken(), exec);
        }
        return null;
    }

    public static void deregisterWebhook(WaitForWebhookExecution exec) {
        Logger.getLogger(WebhookRootAction.class.getName())
                .info("Deregistering webhook with token " + exec.getToken());
        synchronized (webhooks) {
            webhooks.remove(exec.getToken());
        }
    }

    @Override
    public boolean process(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        String pathInfo = req.getPathInfo();
        if (pathInfo != null && pathInfo.startsWith("/webhook-step/")) {
            chain.doFilter(req, resp);
            return true;
        }

        return false;
    }

}
