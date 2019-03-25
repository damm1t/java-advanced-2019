package ru.ifmo.rain.sokolov.mapper;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;

public class ParallelMapperImpl implements ParallelMapper {

    private List<Thread> workers = new ArrayList<>();
    private final Queue<Runnable> tasks = new ArrayDeque<>();

    static void startThreads(int threadCount, List<Thread> threads, Function<Integer, Runnable> taskGen) {
        for (int i = 0; i < threadCount; i++) {
            Thread thread = new Thread(taskGen.apply(i));
            threads.add(thread);
            thread.start();
        }
    }

    static void endThreads(List<Thread> threads) throws InterruptedException {
        InterruptedException exeption = null;
        for (var thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                if (exeption == null) exeption = new InterruptedException("Interrupted while joining");
                else exeption.addSuppressed(e);
            }
        }
        if (exeption != null)
            throw exeption;
    }

    public static class ReversedCounter {
        private int value;

        ReversedCounter(int value) {
            this.value = value;
        }

        void dec() {
            value--;
        }

        boolean isZero() {
            return value == 0;
        }

        boolean decIsZero() {
            dec();
            return isZero();
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

    /**
     * Maps function {@code f} over specified {@code args}.
     * Mapping for each element performs in parallel.
     *
     * @throws InterruptedException if calling thread was interrupted
     */
    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> f, List<? extends T> args) throws InterruptedException {
        final List<R> mapValues = new ArrayList<>(Collections.nCopies(args.size(), null));
        final var cnt = new ReversedCounter(args.size());
        synchronized (tasks) {
            for (int i = 0; i < args.size(); ++i) {
                final int index = i;
                tasks.add(() -> {
                            mapValues.set(index, f.apply(args.get(index)));
                            synchronized (cnt) {
                                if (cnt.decIsZero()) {
                                    cnt.notify();
                                }
                            }
                        }
                );
                tasks.notify();
            }
        }
        synchronized (cnt) {
            while (!cnt.isZero()) {
                cnt.wait();
            }
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
            endThreads(workers);
        } catch (InterruptedException ignored) {
        }
    }
}
