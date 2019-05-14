package ru.ifmo.rain.sokolov.helloudp;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

public class HelloUDPClient implements HelloClient {

    private static final int MAX_PORT = 65536;

    public static void main(String[] args) {
        if (args == null || args.length != 5) {
            System.out.println("expected exactly 5 args : host, port, queryPrefix, threadsCount, queriesPerThread");
            return;
        }
        String host = args[0];
        String queryPrefix = args[2];
        int port;
        int threadsCount;
        int queriesPerThread;
        try {
            port = Integer.parseInt(args[1]);
            threadsCount = Integer.parseInt(args[3]);
            queriesPerThread = Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
            System.out.println("port, threadsCount and queriesPerThread should be a integers");
            return;
        }
        if (port < 0 || port > MAX_PORT) {
            System.out.println("port should be in [1..65536]");
            return;
        }
        if (threadsCount < 0) {
            System.out.println("threadsCount should be positive number");
            return;
        }
        System.out.println("running client... host = " + host + " , port = " + port + " , prefix = " + queryPrefix);
        try {
            new HelloUDPClient().run(
                    host,
                    port,
                    queryPrefix,
                    threadsCount,
                    queriesPerThread
            );
        } catch (Exception e) {
            System.out.println("failed to connect " + e.getMessage());
        }
    }

    private String readCheckedMessage(HelloUDPClientStreams streams, String query) throws IOException {
        while (true) {
            streams.sendString(query);
            try {
                String response = streams.readString();
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
        try {
            final SocketAddress address = new InetSocketAddress(InetAddress.getByName(host), port);
        } catch (UnknownHostException e) {
            System.err.println("Unable to reach specified host: " + e.getMessage());
        }
        ExecutorService threadPool = Executors.newFixedThreadPool(threads);
        IntFunction<Callable<Void>> taskGen = threadId -> () -> {
            try (HelloUDPClientStreams streams = new HelloUDPClientStreams(
                    InetAddress.getByName(host),
                    port,
                    new DatagramSocket()
            )) {
                for (int i = 0; i < requests; i++) {
                    try {
                        String query = MessageHelper.createMessage(prefix, threadId, i);
                        String response = readCheckedMessage(streams, query);
                        System.out.println("received response : " + response);
                    } catch (IOException e) {
                        System.out.println("failed to send request in thread " + threadId);
                    }
                }
            } catch (UnknownHostException | SocketException e) {
                System.out.println("failed to connect to server");
            }
            return null;
        };
        IntStream.range(0, threads)
                .mapToObj(taskGen)
                .forEach(threadPool::submit);
        ShutdownHelper.shutdownAndAwaitTermination(threadPool);
    }
}
