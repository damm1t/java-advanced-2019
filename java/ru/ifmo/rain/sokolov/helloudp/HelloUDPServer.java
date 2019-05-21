package ru.ifmo.rain.sokolov.helloudp;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static ru.ifmo.rain.sokolov.helloudp.MessageHelper.packetToString;

public class HelloUDPServer implements HelloServer {

    private ExecutorService threadPool;
    private HelloUDPStreams streams;
    private volatile boolean isRunning = true;

    public static void main(String[] args) {
        if (args == null || args.length != 2) {
            throw new IllegalArgumentException("Expected 2 not-null arguments");
        }
        try {
            int port = Integer.parseInt(args[0]);
            int threadsCount = Integer.parseInt(args[1]);
            new HelloUDPServer().start(port, threadsCount);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Expected integer arguments");
        }
    }

    private String process(String request) {
        return "Hello, " + request;
    }

    public void start(int port, int threads) {
        try {
            streams = new HelloUDPServerStreams(new DatagramSocket(port));
            threadPool = Executors.newFixedThreadPool(threads + 1);
            threadPool.submit(() -> {
                while (!streams.isClosed() && isRunning) {
                    try {
                        DatagramPacket curPacket = streams.readPacket();
                        threadPool.submit(() -> {
                            try {
                                curPacket.setData(process(packetToString(curPacket)).getBytes(StandardCharsets.UTF_8));
                                streams.socket.send(curPacket);
                            } catch (IOException e) {
                                System.err.println("Failed to send message" + e.getMessage());
                            }
                        });
                    } catch (IOException e) {
                        if (isRunning) {
                            System.err.println("Failed to receive message " + e.getMessage());
                        }
                    }
                }
            });
        } catch (SocketException e) {
            System.err.println("Failed to bind to address " + e.getMessage());
        }
    }

    @Override
    public void close() {
        streams.close();
        isRunning = false;
        ShutdownHelper.shutdownAndAwaitTermination(threadPool);
    }
}
