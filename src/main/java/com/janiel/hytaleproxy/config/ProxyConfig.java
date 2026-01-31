package com.janiel.hytaleproxy.config;

import java.util.Map;

public final class ProxyConfig {
    public String defaultServer;
    public Map<String, ServerConfig> servers;
    public ServerConfig proxyListen;
    public java.util.Map<String, ServerConfig> listeners;


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

        // New: listeners is required (replaces proxyListen)
        if (listeners == null || listeners.isEmpty()) {
            throw new IllegalArgumentException("config.yaml: 'listeners' is required and must contain at least one listener.");
        }
        if (!listeners.containsKey(defaultServer)) {
            throw new IllegalArgumentException("config.yaml: default listener '" + defaultServer + "' not found in listeners: " + listeners.keySet());
        }

        // Validate all servers
        for (Map.Entry<String, ServerConfig> e : servers.entrySet()) {
            String id = e.getKey();
            ServerConfig sc = e.getValue();
            if (sc == null) {
                throw new IllegalArgumentException("config.yaml: servers." + id + " is null.");
            }
            sc.validateOrThrow("servers." + id);
        }

        // Validate listeners + ensure backend exists for each listener id
        for (Map.Entry<String, ServerConfig> e : listeners.entrySet()) {
            String id = e.getKey();
            ServerConfig listen = e.getValue();
            if (listen == null) {
                throw new IllegalArgumentException("config.yaml: listeners." + id + " is null.");
            }
            listen.validateOrThrow("listeners." + id);

            if (!servers.containsKey(id)) {
                throw new IllegalArgumentException("config.yaml: listeners." + id + " has no matching servers." + id);
            }
        }
    }
}
