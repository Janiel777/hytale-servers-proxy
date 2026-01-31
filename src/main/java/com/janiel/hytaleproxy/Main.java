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

            System.out.println("Bootstrap OK. Starting UDP forwarders...");

            for (Map.Entry<String, ServerConfig> e : cfg.listeners.entrySet()) {
                String id = e.getKey();
                ServerConfig listen = e.getValue();
                ServerConfig backend = cfg.servers.get(id);

                Thread t = new Thread(() -> {
                    try {
                        UdpForwarder.run(listen.host, listen.port, backend.host, backend.port);
                    } catch (Exception ex) {
                        System.err.println("[Main] Forwarder '" + id + "' failed: " + ex.getMessage());
                        ex.printStackTrace(System.err);
                    }
                }, "forwarder-" + id);

                t.setDaemon(false);
                t.start();

                System.out.println("  - listener '" + id + "' udp://" + listen.host + ":" + listen.port
                                   + " -> backend udp://" + backend.host + ":" + backend.port);
            }
        } catch (Exception ex) {
            System.err.println("Bootstrap FAILED: " + ex.getMessage());
            ex.printStackTrace(System.err);
            System.exit(1);
        }
    }
}
