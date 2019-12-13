package org.jenkinsci.plugins.webhookstep;

import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;

import java.util.Map;

public class WebhookResponse {
    private String content;
    private Map<String, String> headers;

    public WebhookResponse(String content, Map<String, String> headers) {
        this.content = content;
        this.headers = headers;
    }

    @Whitelisted
    public String getContent() {
        return content;
    }

    @Whitelisted
    public Map<String, String> getHeaders() {
        return headers;
    }
}
