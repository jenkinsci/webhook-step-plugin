package org.jenkinsci.plugins.webhookstep;

import hudson.Extension;
import hudson.model.UnprotectedRootAction;
import hudson.security.csrf.CrumbExclusion;
import hudson.util.Secret;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.CharBuffer;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;
import org.kohsuke.stapler.verb.POST;

@Extension
public class WebhookRootAction extends CrumbExclusion implements UnprotectedRootAction {

    private static final HashMap<String, WaitForWebhookExecution> webhooks = new HashMap<>();
    private static final HashMap<String, WebhookResponse> alreadyPosted = new HashMap<>();
    private static final HashMap<String, Secret> authTokens = new HashMap<>();

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
    public void doDynamic(StaplerRequest2 request, StaplerResponse2 response) {
        String token = request.getOriginalRestOfPath().substring(1); // Strip leading slash
        String authHeader = request.getHeader("Authorization");
        Secret secretAuthToken;

        synchronized (authTokens) {
            secretAuthToken = authTokens.get(token);
        }

        if (secretAuthToken != null) {
            // Decrypt the stored AuthToken with the one received in the header
            if (!secretAuthToken.getPlainText().equals(authHeader)) {
                response.setHeader("Result", "Unauthorized");
                response.setStatus(403);
                return;
            }
        } else if (authHeader != null) {
            Logger.getLogger(WebhookRootAction.class.getName())
                    .warning("Unexpected Authorization header for Webhook " + token);
        }

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
        while (iter.hasMoreElements()) {
            String header = iter.nextElement();
            headers.put(header, request.getHeader(header));
        }

        WebhookResponse whResponse = new WebhookResponse(content.toString(), headers);

        Logger.getLogger(WebhookRootAction.class.getName()).fine("Webhook called with " + token);

        WaitForWebhookExecution exec;
        synchronized (webhooks) {
            exec = webhooks.remove(token);
            if (exec == null) {
                // pipeline has not yet waited on webhook, add an entry to track
                // that it was already triggered
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

    public static void registerAuthToken(WebhookToken hook) {
        synchronized (authTokens) {
            authTokens.put(hook.getToken(), hook.getSecretAuthToken());
        }
    }

    // Returns null when the webhook has been registered, the content when the webhook has already been called
    public static WebhookResponse registerWebhook(WaitForWebhookExecution exec) {
        Logger.getLogger(WebhookRootAction.class.getName()).fine("Registering webhook with token " + exec.getToken());
        synchronized (webhooks) {
            if (alreadyPosted.containsKey(exec.getToken())) {
                return alreadyPosted.remove(exec.getToken());
            }

            webhooks.put(exec.getToken(), exec);
        }
        return null;
    }

    public static void deregisterWebhook(WaitForWebhookExecution exec) {
        Logger.getLogger(WebhookRootAction.class.getName()).fine("Deregistering webhook with token " + exec.getToken());
        synchronized (webhooks) {
            webhooks.remove(exec.getToken());
        }
        synchronized (authTokens) {
            authTokens.remove(exec.getToken());
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
