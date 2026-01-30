package com.janiel.hytaleproxy.config;

public final class ServerConfig {
    public String host;
    public Integer port;

    public ServerConfig() {
    }

    public void validateOrThrow(String path) {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("config.yaml: " + path + ".host is required and must be non-empty.");
        }
        if (port == null) {
            throw new IllegalArgumentException("config.yaml: " + path + ".port is required.");
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("config.yaml: " + path + ".port must be 1..65535, got " + port + ".");
        }
    }
}
