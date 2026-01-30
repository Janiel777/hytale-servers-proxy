package com.janiel.hytaleproxy.quic;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.Arrays;

public final class UdpSniffer {

    private UdpSniffer() {
    }

    public static void run(String bindHost, int bindPort) throws Exception {

        int dumpLimit = 10;
        int dumped = 0;
        java.nio.file.Path dumpDir = java.nio.file.Path.of("dumps");
        java.nio.file.Files.createDirectories(dumpDir);

        InetSocketAddress bind = new InetSocketAddress(bindHost, bindPort);

        try (DatagramSocket socket = new DatagramSocket(bind)) {
            socket.setReuseAddress(true);

            System.out.println("[UdpSniffer] Listening on udp://" + bindHost + ":" + bindPort);

            byte[] buf = new byte[2048];

            while (true) {
                DatagramPacket pkt = new DatagramPacket(buf, buf.length);
                socket.receive(pkt);

                int len = pkt.getLength();
                String src = pkt.getAddress().getHostAddress() + ":" + pkt.getPort();

                byte[] payload = Arrays.copyOf(pkt.getData(), len);

                if (dumped < dumpLimit) {
                    String name = String.format("udp_%02d_len%d.bin", dumped, len);
                    java.nio.file.Files.write(dumpDir.resolve(name), payload);
                    dumped++;
                }

                System.out.println("[UdpSniffer] " + Instant.now()
                                   + " from " + src
                                   + " len=" + len
                                   + " firstBytes=" + toHexPrefix(payload, 16));
            }
        }
    }

    private static String toHexPrefix(byte[] data, int maxBytes) {
        int n = Math.min(data.length, maxBytes);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            if (i > 0) sb.append(' ');
            sb.append(String.format("%02x", data[i]));
        }
        if (data.length > n) sb.append(" ...");
        return sb.toString();
    }
}
