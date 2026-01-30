package com.janiel.hytaleproxy.config;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;

public final class YamlConfigLoader {

    private YamlConfigLoader() {
    }

    public static ProxyConfig loadFromResource(String resourceName) {
        InputStream in = YamlConfigLoader.class.getClassLoader().getResourceAsStream(resourceName);
        if (in == null) {
            throw new IllegalStateException("Resource not found on classpath: " + resourceName);
        }

        // Load raw map first so we can map YAML key "default" -> defaultServer cleanly.
        Yaml yaml = new Yaml();
        Object rawObj = yaml.load(in);
        if (!(rawObj instanceof Map<?, ?> raw)) {
            throw new IllegalStateException("config.yaml must be a YAML map/object at the root.");
        }

        ProxyConfig cfg = new ProxyConfig();

        Object def = raw.get("default");
        if (def instanceof String s) {
            cfg.defaultServer = s;
        }

        Object serversObj = raw.get("servers");
        if (serversObj instanceof Map<?, ?> serversMap) {
            // Convert each entry to ServerConfig
            cfg.servers = new java.util.LinkedHashMap<>();
            for (Map.Entry<?, ?> e : serversMap.entrySet()) {
                String id = String.valueOf(e.getKey());
                Object val = e.getValue();
                if (!(val instanceof Map<?, ?> serverFields)) {
                    throw new IllegalArgumentException("config.yaml: servers." + id + " must be a map with host/port.");
                }
                ServerConfig sc = new ServerConfig();
                Object host = serverFields.get("host");
                if (host != null) sc.host = String.valueOf(host);

                Object port = serverFields.get("port");
                if (port instanceof Number n) {
                    sc.port = n.intValue();
                } else if (port instanceof String ps) {
                    try {
                        sc.port = Integer.parseInt(ps);
                    } catch (NumberFormatException ignored) {
                        // validate will throw later
                    }
                }

                cfg.servers.put(id, sc);
            }
        }

        return cfg;
    }
}
