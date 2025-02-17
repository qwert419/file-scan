package org.example;

import org.apache.http.HttpHost;

public class ProxyConfig {
    private String host;
    private int port;
    private boolean enabled;

    public ProxyConfig(String host, int port, boolean enabled) {
        this.host = host;
        this.port = port;
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public HttpHost getHttpHost() {
        return new HttpHost(host, port);
    }
}