package ru.ifmo.rain.sokolov.mapper;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;

public class ParallelMapperImpl implements ParallelMapper {

    private List<Thread> workers = new ArrayList<>();
    private final Queue<Runnable> tasks = new ArrayDeque<>();
    private final static int MAX_SIZE = 1_000_000;

    public static class ReverseCounter {
        private int value;

        ReverseCounter(int value) {
            this.value = value;
        }

        synchronized boolean isZero() {
            return value == 0;
        }

        synchronized boolean decAndZeroCheck() {
            value--;
            return isZero();
        }
    }

    static void startThreads(int size, List<Thread> threads, Function<Integer, Runnable> runnableFunction) {
        for (int i = 0; i < size; i++) {
            var thread = new Thread(runnableFunction.apply(i));
            threads.add(thread);
            thread.start();
        }
    }

    static void stopThreads(List<Thread> threads) throws InterruptedException {
        InterruptedException exeption = null;
        for (var thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                if (exeption == null) {
                    exeption = new InterruptedException("Interrupted while joining");
                } else {
                    exeption.addSuppressed(e);
                }
            }
        }
        if (exeption != null) {
            throw exeption;
        }
    }

    public ParallelMapperImpl(int threads) {
        startThreads(threads, workers, (i) -> () -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Runnable task;
                    synchronized (tasks) {
                        while (tasks.isEmpty()) {
                            tasks.wait();
                        }
                        task = tasks.poll();
                    }
                    task.run();
                }
            } catch (InterruptedException ignored) {
            }
        });
    }

    private void addTask(final Runnable task) throws InterruptedException {
        synchronized (tasks) {
            while (tasks.size() == MAX_SIZE) {
                tasks.wait();
            }
            tasks.add(task);
            tasks.notifyAll();
        }
    }

    /**
     * Maps function {@code f} over specified {@code args}.
     * Mapping for each element performs in parallel.
     *
     * @throws InterruptedException if calling thread was interrupted
     */
    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> f, List<? extends T> args) throws InterruptedException {
        final List<R> mapValues = new ArrayList<>(Collections.nCopies(args.size(), null));
        final var counter = new ReverseCounter(args.size());
        synchronized (tasks) {
            for (int i = 0; i < args.size(); ++i) {
                final var index = i;
                addTask(() -> mapValues.set(index, f.apply(args.get(index))));
                if (counter.decAndZeroCheck()) {
                    counter.notify();
                }
                tasks.notify();
            }
        }
            while (!counter.isZero()) {
                counter.wait();
            }
        return mapValues;
    }

    /**
     * Stops all threads. All unfinished mappings leave in undefined state.
     */
    @Override
    public void close() {
        workers.forEach(Thread::interrupt);
        try {
            stopThreads(workers);
        } catch (InterruptedException ignored) {
        }
    }
}
