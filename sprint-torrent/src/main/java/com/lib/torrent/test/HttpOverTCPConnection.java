package com.lib.torrent.test;

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class HttpOverTCPConnection {
    public static void main(String[] args) {
        try (SocketChannel socketChannel = SocketChannel.open();
             PrintStream printStream = new PrintStream(System.out)) {

            SocketAddress remoteAddress = new InetSocketAddress("localhost", 8080);

            socketChannel.connect(remoteAddress);

            ByteBuffer buffer = ByteBuffer.allocate(1024);

            buffer.put("GET /hello HTTP/1.1\n".getBytes(StandardCharsets.UTF_8));
            buffer.flip();
            socketChannel.write(buffer);

            buffer.clear();

            buffer.put("Host: 127.0.0.1\n\n".getBytes(StandardCharsets.UTF_8));
            buffer.flip();

            socketChannel.write(buffer);

            buffer.clear();
            ByteBuffer readbuffer = ByteBuffer.allocate(5);

            while (socketChannel.read(readbuffer) > 0) {
                readbuffer.flip();
                while (readbuffer.hasRemaining())
                    printStream.write(readbuffer.get());
                printStream.flush();
                readbuffer.clear();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
