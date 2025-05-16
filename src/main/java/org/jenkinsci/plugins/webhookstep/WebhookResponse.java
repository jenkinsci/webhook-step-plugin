package org.jenkinsci.plugins.webhookstep;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;

public class WebhookResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1;

    private final String content;
    private final Map<String, String> headers;

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
