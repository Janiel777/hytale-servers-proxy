package com.janiel.hytaleproxy;

import com.janiel.hytaleproxy.config.ProxyConfig;
import com.janiel.hytaleproxy.config.ServerConfig;
import com.janiel.hytaleproxy.config.YamlConfigLoader;
import com.janiel.hytaleproxy.quic.UdpForwarder;

import java.time.Instant;
import java.util.Map;

public final class Main {

    public static void main(String[] args) {
        try {
            System.out.println("=== Hytale Proxy Bootstrap ===");
            System.out.println("Started at: " + Instant.now());

            ProxyConfig cfg = YamlConfigLoader.loadFromResource("config.yaml");
            cfg.validateOrThrow();

            System.out.println("Default server: " + cfg.defaultServer);
            System.out.println("Servers:");
            for (Map.Entry<String, ServerConfig> e : cfg.servers.entrySet()) {
                String id = e.getKey();
                ServerConfig s = e.getValue();
                String suffix = id.equals(cfg.defaultServer) ? " (default)" : "";
                System.out.println("  - " + id + " -> " + s.host + ":" + s.port + suffix);
            }

            String bindHost = cfg.proxyListen.host;
            int bindPort = cfg.proxyListen.port;

            System.out.println("Bootstrap OK. Starting UDP forwarder...");

            String serverHost = cfg.servers.get(cfg.defaultServer).host;
            int serverPort = cfg.servers.get(cfg.defaultServer).port;

            UdpForwarder.run(bindHost, bindPort, serverHost, serverPort);
        } catch (Exception ex) {
            System.err.println("Bootstrap FAILED: " + ex.getMessage());
            ex.printStackTrace(System.err);
            System.exit(1);
        }
    }
}
