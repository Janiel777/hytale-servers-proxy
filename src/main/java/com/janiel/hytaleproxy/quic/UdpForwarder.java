package com.janiel.hytaleproxy.quic;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class UdpForwarder {

    private UdpForwarder() {
    }

    private static final class Session {
        final InetSocketAddress clientAddr;
        final DatagramSocket upstreamSocket; // proxy -> server (per client)
        volatile long lastSeenMs;

        Session(InetSocketAddress clientAddr, DatagramSocket upstreamSocket) {
            this.clientAddr = clientAddr;
            this.upstreamSocket = upstreamSocket;
            this.lastSeenMs = System.currentTimeMillis();
        }
    }

    // client endpoint -> session
    private static final Map<String, Session> sessions = new ConcurrentHashMap<>();

    public static void run(String bindHost, int bindPort, String serverHost, int serverPort) throws Exception {
        InetSocketAddress bind = new InetSocketAddress(bindHost, bindPort);
        InetSocketAddress serverAddr = new InetSocketAddress(serverHost, serverPort);

        try (DatagramSocket downSocket = new DatagramSocket(bind)) {
            downSocket.setReuseAddress(true);

            System.out.println("[UdpForwarder] Listening on udp://" + bindHost + ":" + bindPort);
            System.out.println("[UdpForwarder] Forwarding to server udp://" + serverHost + ":" + serverPort);

            byte[] buf = new byte[2048];

            while (true) {
                DatagramPacket pkt = new DatagramPacket(buf, buf.length);
                downSocket.receive(pkt);

                InetSocketAddress client = new InetSocketAddress(pkt.getAddress(), pkt.getPort());
                String key = client.getAddress().getHostAddress() + ":" + client.getPort();

                int len = pkt.getLength();
                byte[] payload = Arrays.copyOf(pkt.getData(), len);

                Session s = sessions.get(key);
                if (s == null) {
                    DatagramSocket upstream = new DatagramSocket(); // ephemeral port
                    upstream.setReuseAddress(true);

                    Session created = new Session(client, upstream);
                    sessions.put(key, created);

                    System.out.println("[UdpForwarder] " + Instant.now()
                                       + " NEW session client=" + key
                                       + " upstreamLocalPort=" + upstream.getLocalPort());

                    startUpstreamReader(created, downSocket);
                    s = created;
                }

                s.lastSeenMs = System.currentTimeMillis();

                // Forward client -> server
                DatagramPacket toServer = new DatagramPacket(payload, payload.length, serverAddr);
                s.upstreamSocket.send(toServer);

                System.out.println("[UdpForwarder] " + Instant.now()
                                   + " C->S " + key
                                   + " len=" + len);
            }
        }
    }

    private static void startUpstreamReader(Session s, DatagramSocket downSocket) {
        Thread t = new Thread(() -> {
            byte[] buf = new byte[4096];

            while (true) {
                try {
                    DatagramPacket pkt = new DatagramPacket(buf, buf.length);
                    s.upstreamSocket.receive(pkt);

                    int len = pkt.getLength();
                    byte[] payload = Arrays.copyOf(pkt.getData(), len);

                    DatagramPacket toClient = new DatagramPacket(
                    payload, payload.length,
                    s.clientAddr.getAddress(), s.clientAddr.getPort()
                    );

                    downSocket.send(toClient);

                    String key = s.clientAddr.getAddress().getHostAddress() + ":" + s.clientAddr.getPort();

                    System.out.println("[UdpForwarder] " + Instant.now()
                                       + " S->C " + key
                                       + " len=" + len);
                } catch (Exception e) {
                    System.err.println("[UdpForwarder] upstream reader error: " + e.getMessage());
                    e.printStackTrace(System.err);
                    return;
                }
            }
        }, "udp-upstream-" + s.clientAddr.getPort());

        t.setDaemon(true);
        t.start();
    }
}
