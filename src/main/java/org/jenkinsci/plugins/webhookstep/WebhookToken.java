package org.jenkinsci.plugins.webhookstep;

import java.io.Serializable;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;

import hudson.util.Secret;

public class WebhookToken implements Serializable {

    private static final long serialVersionUID = 1;

    private final String token;
    private final String url;
    private final Secret secretAuthToken;

    public WebhookToken(String token, String url, Secret secretAuthToken) {
        this.token = token;
        this.url = url;
        this.secretAuthToken = secretAuthToken;
    }

    @Whitelisted
    public String getToken() {
        return token;
    }

    /**
     * @deprecated Use {@link #getUrl()} instead.
     */
    @Deprecated
    @Whitelisted
    public String getURL() {
        return getUrl();
    }

    @Whitelisted
    public String getUrl() {
        return url;
    }

    public Secret getSecretAuthToken() {
        return this.secretAuthToken;
    }
}
