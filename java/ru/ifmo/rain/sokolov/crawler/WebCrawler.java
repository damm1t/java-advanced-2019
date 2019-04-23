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

    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        this.downloader = downloader;
        this.downloadersPool = Executors.newFixedThreadPool(downloaders);
        this.extractorsPool = Executors.newFixedThreadPool(extractors);
        this.perHost = perHost;
    }

    private TaskPoolPerHost getTaskPool(String hostname) {
        synchronized (mapPerHost) {
            mapPerHost.putIfAbsent(hostname, new TaskPoolPerHost());
            return mapPerHost.get(hostname);
        }
    }

    private Result download(String url, int depth, Predicate<String> filter) {
        var result = new ResultWrapper();
        var barrier = new Phaser(1);
        downloadImplDfs(url, depth, result, barrier, filter);
        barrier.arriveAndAwaitAdvance();
        return result.toResult();
    }

    @Override
    public Result download(String url, int depth) {
        return download(url, depth, site -> true);
    }

    private void downloadImplDfs(String url, int depth, ResultWrapper wrapper,
                                 Phaser barrier, Predicate<String> filter) {
        if (depth <= 0 || !filter.test(url) || !wrapper.downloaded.add(url)) {
            return;
        }

        submitDownloader(url, wrapper, barrier, () -> {
            try {
                var document = downloader.download(url);
                barrier.register();
                extractorsPool.submit(() -> {
                    try {
                        document.extractLinks()
                                .forEach(link ->
                                        downloadImplDfs(link, depth - 1, wrapper, barrier, filter));
                    } catch (IOException e) {
                        wrapper.errors.put(url, e);
                    } finally {
                        barrier.arrive();
                    }
                });
            } catch (IOException e) {
                wrapper.errors.put(url, e);
            }
        });
    }

    private void submitDownloader(String url, ResultWrapper wrapper,
                                  Phaser barrier, Runnable task) {
        try {
            var host = URLUtils.getHost(url);
            barrier.register();
            var taskPoolPerHost = getTaskPool(host);
            taskPoolPerHost.submitDownloaderImpl(barrier, task, this);
        } catch (MalformedURLException e) {
            wrapper.errors.put(url, e);
        }
    }

    private Runnable transformTask(TaskPoolPerHost taskPool, Runnable task, Phaser barrier) {
        return () -> {
            task.run();
            synchronized (taskPool.queueTasks) {
                if (!taskPool.queueTasks.isEmpty()) {
                    downloadersPool.submit(
                            transformTask(taskPool, taskPool.queueTasks.poll(), barrier)
                    );
                } else {
                    taskPool.threadCount--;
                }
            }
            barrier.arrive();
        };
    }

    @Override
    public void close() {
        try {
            extractorsPool.shutdown();
            extractorsPool.awaitTermination(1000, TimeUnit.MILLISECONDS);
            downloadersPool.shutdown();
            downloadersPool.awaitTermination(1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            System.err.println("Method close was not completed correctly: " + e.getMessage());
        }
    }

    private static class ResultWrapper {
        final Set<String> downloaded = ConcurrentHashMap.newKeySet();
        final Map<String, IOException> errors = new ConcurrentHashMap<>();

        Result toResult() {
            downloaded.removeAll(errors.keySet());
            return new Result(new ArrayList<>(downloaded), new HashMap<>(errors));
        }
    }

    private static class TaskPoolPerHost {
        final Queue<Runnable> queueTasks = new ArrayDeque<>();
        int threadCount = 0;

        private void submitDownloaderImpl(Phaser barrier, Runnable task, WebCrawler crawler) {
            synchronized (queueTasks) {
                if (threadCount < crawler.perHost) {
                    threadCount++;
                    crawler.downloadersPool
                            .submit(crawler
                                    .transformTask(this, task, barrier));

                } else {
                    queueTasks.add(task);
                }
            }
        }
    }
}
