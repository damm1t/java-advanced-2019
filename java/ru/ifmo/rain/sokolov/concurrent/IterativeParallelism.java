package ru.ifmo.rain.sokolov.concurrent;

import info.kgeorgiy.java.advanced.concurrent.ListIP;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IterativeParallelism implements ListIP {

    private <T> List<T> merge(Stream<? extends Stream<? extends T>> list) {
        return list.flatMap(Function.identity()).collect(Collectors.toList());
    }

    private <T> List<Stream<? extends T>> partition(int threads, List<? extends T> values) {
        var partitionList = new ArrayList<Stream<? extends T>>();
        var blockSize = (values.size() + threads - 1) / threads;
        for (int left = 0; left < values.size(); left += blockSize) {
            var right = Math.min(left + blockSize, values.size());
            partitionList.add(values.subList(left, right).stream());
        }
        return partitionList;
    }

    private <T, M> Stream<M> mapStream(List<Stream<? extends T>> valuesStream,
                                       Function<Stream<? extends T>, M> mapper) throws InterruptedException {
        var intermediateValues = new ArrayList<M>(Collections.nCopies(valuesStream.size(), null));
        var worker = new ArrayList<Thread>();
        for (int i = 0; i < valuesStream.size(); ++i) {
            final int index = i;
            var thread = new Thread(() -> intermediateValues.set(index, mapper.apply(valuesStream.get(index))));
            worker.add(thread);
            thread.start();
        }
        for (var thread : worker)
            thread.join();

        return intermediateValues.stream();
    }

    private <T, M, R> R runInParallel(int threads, List<? extends T> values,
                                      Function<Stream<? extends T>, M> mapper,
                                      Function<? super Stream<M>, R> reducer) throws InterruptedException {
        return reducer.apply(mapStream(partition(threads, values), mapper));
    }

    //ListIP
    @Override
    public String join(int threads, List<?> values) throws InterruptedException {
        return runInParallel(threads, values,
                list -> list.map(Object::toString).collect(Collectors.joining()),
                list -> list.collect(Collectors.joining()));
    }

    @Override
    public <T> List<T> filter(int threads,
                              List<? extends T> values,
                              Predicate<? super T> predicate) throws InterruptedException {
        return runInParallel(threads, values, list -> list.filter(predicate), this::merge);
    }

    @Override
    public <T, U> List<U> map(int threads,
                              List<? extends T> values,
                              Function<? super T, ? extends U> f) throws InterruptedException {
        return runInParallel(threads, values, list -> list.map(f), this::merge);
    }

    //ScalarIP
    @Override
    public <T> T minimum(int threads,
                         List<? extends T> values,
                         Comparator<? super T> comparator) throws InterruptedException {
        if (values.isEmpty()) {
            throw new NoSuchElementException("Values are not given");
        }
        Function<Stream<? extends T>, T> getMin = stream -> stream.min(comparator).get();
        return runInParallel(threads, values, getMin, getMin);
    }

    @Override
    public <T> T maximum(int threads,
                         List<? extends T> values,
                         Comparator<? super T> comparator) throws InterruptedException {
        return minimum(threads, values, comparator.reversed());
    }

    @Override
    public <T> boolean all(int threads,
                           List<? extends T> values,
                           Predicate<? super T> predicate) throws InterruptedException {
        return runInParallel(threads, values,
                list -> list.allMatch(predicate),
                list -> list.allMatch(Boolean::booleanValue));
    }

    @Override
    public <T> boolean any(int threads,
                           List<? extends T> values,
                           Predicate<? super T> predicate) throws InterruptedException {
        return runInParallel(threads, values,
                list -> list.anyMatch(predicate),
                list -> list.anyMatch(Boolean::booleanValue));
    }
}