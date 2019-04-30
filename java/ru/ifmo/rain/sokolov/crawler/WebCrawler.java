package ru.ifmo.rain.sokolov.crawler;


import info.kgeorgiy.java.advanced.crawler.Crawler;
import info.kgeorgiy.java.advanced.crawler.Downloader;
import info.kgeorgiy.java.advanced.crawler.Result;
import info.kgeorgiy.java.advanced.crawler.URLUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;

public class WebCrawler implements Crawler {

    private final Downloader downloader;
    private final ExecutorService downloadersPool;
    private final ExecutorService extractorsPool;
    private final int perHost;
    private final Map<String, TaskPoolPerHost> mapPerHost = new ConcurrentHashMap<>();

    @Override
    public Result download(String url, int depth) {
        final Set<String> downloaded = ConcurrentHashMap.newKeySet();
        final Map<String, IOException> errors = new ConcurrentHashMap<>();
        var phaser = new Phaser(1);
        downloadImplDfs(url, depth, downloaded, errors, phaser, site -> true);
        phaser.arriveAndAwaitAdvance();
        downloaded.removeAll(errors.keySet());
        return new Result(new ArrayList<>(downloaded), new HashMap<>(errors));
    }

    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        this.downloader = downloader;
        this.downloadersPool = Executors.newFixedThreadPool(downloaders);
        this.extractorsPool = Executors.newFixedThreadPool(extractors);
        this.perHost = perHost;
    }

    private void downloadImplDfs(String url, int depth, final Set<String> downloaded, final Map<String, IOException> errors,
                                 Phaser phaser, Predicate<String> filter) {

        if (depth <= 0 || !filter.test(url) || !downloaded.add(url)) {
            return;
        }

        try {
            final String host = URLUtils.getHost(url);
            final TaskPoolPerHost data = mapPerHost.computeIfAbsent(host, s -> new TaskPoolPerHost());

            phaser.register();

            data.addTask(() -> {
                try {
                    var document = downloader.download(url);
                    downloaded.add(url);
                    phaser.register();
                    extractorsPool.submit(() -> {
                        try {
                            document.extractLinks()
                                    .forEach(link ->
                                            downloadImplDfs(link, depth - 1, downloaded, errors, phaser, filter));
                        } catch (IOException e) {
                            errors.put(url, e);
                        } finally {
                            phaser.arrive();
                        }
                    });

                } catch (IOException e) {
                    errors.put(url, e);
                } finally {
                    phaser.arrive();
                    data.nextTask();
                }
            });
        } catch (MalformedURLException e) {
            errors.put(url, e);
        }
    }

    private void shutDownHelper(ExecutorService pool) {
        try {
            pool.shutdown();
            pool.awaitTermination(1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException("Method close was not completed correctly: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        shutDownHelper(extractorsPool);
        shutDownHelper(downloadersPool);
    }

    private class TaskPoolPerHost {
        final Queue<Runnable> queueTasks = new ArrayDeque<>();
        int threadCount = 0;

        private synchronized void addTask(Runnable task) {
            if (threadCount < perHost) {
                ++threadCount;
                downloadersPool.submit(task);
            } else {
                queueTasks.add(task);
            }
        }

        private synchronized void nextTask() {
            var task = queueTasks.poll();
            if (task != null) {
                downloadersPool.submit(task);
            } else {
                --threadCount;
            }
        }
    }

}
