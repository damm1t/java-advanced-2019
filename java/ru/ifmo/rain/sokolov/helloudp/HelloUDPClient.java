package ru.ifmo.rain.sokolov.helloudp;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

public class HelloUDPClient implements HelloClient {

    public static void main(String[] args) {
        if (args == null || args.length != 5) {
            throw new IllegalArgumentException("Expected exactly 5 args : host, port, queryPrefix, threadsCount, queriesPerThread");
        }
        var host = args[0];
        var queryPrefix = args[2];
        int port;
        int threadsCount;
        int queriesPerThread;
        try {
            port = Integer.parseInt(args[1]);
            threadsCount = Integer.parseInt(args[3]);
            queriesPerThread = Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("port, threadsCount and queriesPerThread should be a integers");
        }
        System.out.println("running client... host = " + host + " , port = " + port + " , prefix = " + queryPrefix);
        try {
            new HelloUDPClient().run(host, port, queryPrefix, threadsCount, queriesPerThread);
        } catch (Exception e) {
            System.err.println("Failed to connect " + e.getMessage());
        }
    }

    private String readMessageWithCheck(HelloUDPClientStreams streams, String query) throws IOException {
        while (true) {
            streams.sendString(query);
            try {
                var response = streams.readString();
                if (MessageHelper.check(response, query)) {
                    return response;
                }
            } catch (IOException e) {
                System.out.println("received broken UDP packet");
            }
        }
    }

    /**
     * Runs Hello client.
     *
     * @param host     server host
     * @param port     server port
     * @param prefix   request prefix
     * @param threads  number of request threads
     * @param requests number of requests per thread.
     */
    @Override
    public void run(String host, int port, String prefix, int threads, int requests) {
        ExecutorService threadPool = Executors.newFixedThreadPool(threads);
        IntFunction<Callable<Void>> callTasks = threadId -> () -> {
            try (HelloUDPClientStreams streams = new HelloUDPClientStreams(
                    InetAddress.getByName(host),
                    port,
                    new DatagramSocket()
            )) {
                for (int i = 0; i < requests; i++) {
                    try {
                        var query = MessageHelper.createMessage(prefix, threadId, i);
                        var response = readMessageWithCheck(streams, query);
                        System.out.println("Received response : " + response);
                    } catch (IOException e) {
                        System.err.println("Failed to send request in thread " + threadId);
                    }
                }
            } catch (UnknownHostException | SocketException e) {
                System.err.println("Failed to connect to server");
            }
            return null;
        };
        IntStream.range(0, threads)
                .mapToObj(callTasks)
                .forEach(threadPool::submit);
        ShutdownHelper.shutdownAndAwaitTermination(threadPool);
    }
}
