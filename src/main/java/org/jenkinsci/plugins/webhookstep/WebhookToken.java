package org.jenkinsci.plugins.webhookstep;

import java.io.Serializable;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;

public class WebhookToken implements Serializable {

    private static final long serialVersionUID = 1;

    private final String token;
    private final String url;
    private final String authToken;

    public WebhookToken(String token, String url, String authToken) {
        this.token = token;
        this.url = url;
        this.authToken = authToken;
    }

    @Whitelisted
    public String getToken() {
        return token;
    }

    @Whitelisted
    public String getURL() {
        return url;
    }

    public String getAuthToken() {
        return this.authToken;
    }
}
