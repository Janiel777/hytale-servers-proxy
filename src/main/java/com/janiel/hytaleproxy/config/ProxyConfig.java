package com.janiel.hytaleproxy.config;

import java.util.Map;

public final class ProxyConfig {
    public String defaultServer;
    public Map<String, ServerConfig> servers;
    public ServerConfig proxyListen;


    public ProxyConfig() {
    }

    public void validateOrThrow() {
        if (defaultServer == null || defaultServer.isBlank()) {
            throw new IllegalArgumentException("config.yaml: 'default' is required and must be non-empty.");
        }
        if (servers == null || servers.isEmpty()) {
            throw new IllegalArgumentException("config.yaml: 'servers' is required and must contain at least one server.");
        }
        if (!servers.containsKey(defaultServer)) {
            throw new IllegalArgumentException("config.yaml: default server '" + defaultServer + "' not found in servers: " + servers.keySet());
        }
        if (proxyListen == null) {
            throw new IllegalArgumentException("config.yaml: 'proxyListen' is required.");
        }
        proxyListen.validateOrThrow("proxyListen");

        for (Map.Entry<String, ServerConfig> e : servers.entrySet()) {
            String id = e.getKey();
            ServerConfig sc = e.getValue();
            if (sc == null) {
                throw new IllegalArgumentException("config.yaml: servers." + id + " is null.");
            }
            sc.validateOrThrow("servers." + id);
        }
    }
}
