package ru.ifmo.rain.sokolov.mapper;

import info.kgeorgiy.java.advanced.concurrent.ListIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IterativeParallelism implements ListIP {

    private ParallelMapper mapper;

    public IterativeParallelism(ParallelMapper mapper) {
        this.mapper = mapper;
    }

    public IterativeParallelism() {
        this(null);
    }

    private <T> List<T> merge(Stream<? extends Stream<? extends T>> list) {
        return list.flatMap(Function.identity()).collect(Collectors.toList());
    }

    private <T> List<Stream<? extends T>> partition(int threads, List<? extends T> values) {
        var partitionList = new ArrayList<Stream<? extends T>>();
        var add = values.size() / threads;
        var numberAddSize = values.size() % threads;
        var left = 0;
        while (left < values.size()) {
            var d = add + (numberAddSize-- > 0 ? 1 : 0);
            var right = left + d;
            partitionList.add(values.subList(left, right).stream());
            left += d;
        }
        return partitionList;
    }

    private <T, R> List<R> map(List<Stream<? extends T>> valuesStream,
                               Function<Stream<? extends T>, R> mapper) throws InterruptedException {
        var intermediateValues = new ArrayList<R>(Collections.nCopies(valuesStream.size(), null));
        var worker = new ArrayList<Thread>();
        ParallelMapperImpl.startThreads(valuesStream.size(), worker,
                index -> () -> intermediateValues.set(index, mapper.apply(valuesStream.get(index))));
        ParallelMapperImpl.endThreads(worker);
        return intermediateValues;
    }

    private <T, M, R> R parallelRun(int threads, List<? extends T> values,
                                    Function<Stream<? extends T>, M> mapFunction,
                                    Function<? super Stream<M>, R> reducer) throws InterruptedException {
        return reducer.apply((mapper != null ?
                mapper.map(mapFunction, partition(threads, values))
                : map(partition(threads, values), mapFunction)).stream());
    }

    //ListIP

    /**
     * Join values to string.
     *
     * @param threads number or concurrent threads.
     * @param values  values to join.
     * @return list of joined result of {@link #toString()} call on each value.
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public String join(int threads, List<?> values) throws InterruptedException {
        return parallelRun(threads, values,
                list -> list.map(Object::toString).collect(Collectors.joining()),
                list -> list.collect(Collectors.joining()));
    }

    /**
     * Filters values by predicate.
     *
     * @param threads   number or concurrent threads.
     * @param values    values to filter.
     * @param predicate filter predicate.
     * @return list of values satisfying given predicated. Order of values is preserved.
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public <T> List<T> filter(int threads,
                              List<? extends T> values,
                              Predicate<? super T> predicate) throws InterruptedException {
        return parallelRun(threads, values, list -> list.filter(predicate), this::merge);
    }

    /**
     * Map values.
     *
     * @param threads number or concurrent threads.
     * @param values  values to filter.
     * @param f       mapper function.
     * @return list of values mapped by given function.
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public <T, U> List<U> map(int threads,
                              List<? extends T> values,
                              Function<? super T, ? extends U> f) throws InterruptedException {
        return parallelRun(threads, values, list -> list.map(f), this::merge);
    }

    //ScalarIP

    /**
     * Returns minimum value.
     *
     * @param threads    number or concurrent threads.
     * @param values     values to get minimum of.
     * @param comparator value comparator.
     * @param <T>        value type.
     * @return minimum of given values
     * @throws InterruptedException             if executing thread was interrupted.
     * @throws java.util.NoSuchElementException if not values are given.
     */
    @Override
    public <T> T minimum(int threads,
                         List<? extends T> values,
                         Comparator<? super T> comparator) throws InterruptedException {
        if (values.isEmpty()) {
            throw new NoSuchElementException("Values are not given");
        }
        Function<Stream<? extends T>, T> getMin = stream -> stream.min(comparator).get();
        return parallelRun(threads, values, getMin, getMin);
    }

    /**
     * Returns maximum value.
     *
     * @param threads    number or concurrent threads.
     * @param values     values to get maximum of.
     * @param comparator value comparator.
     * @param <T>        value type.
     * @return maximum of given values
     * @throws InterruptedException             if executing thread was interrupted.
     * @throws java.util.NoSuchElementException if not values are given.
     */
    @Override
    public <T> T maximum(int threads,
                         List<? extends T> values,
                         Comparator<? super T> comparator) throws InterruptedException {
        return minimum(threads, values, comparator.reversed());
    }

    /**
     * Returns whether all values satisfies predicate.
     *
     * @param threads   number or concurrent threads.
     * @param values    values to test.
     * @param predicate test predicate.
     * @param <T>       value type.
     * @return whether all values satisfies predicate or {@code true}, if no values are given.
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public <T> boolean all(int threads,
                           List<? extends T> values,
                           Predicate<? super T> predicate) throws InterruptedException {
        return parallelRun(threads, values,
                list -> list.allMatch(predicate),
                list -> list.allMatch(Boolean::booleanValue));
    }

    /**
     * Returns whether any of values satisfies predicate.
     *
     * @param threads   number or concurrent threads.
     * @param values    values to test.
     * @param predicate test predicate.
     * @param <T>       value type.
     * @return whether any value satisfies predicate or {@code false}, if no values are given.
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public <T> boolean any(int threads,
                           List<? extends T> values,
                           Predicate<? super T> predicate) throws InterruptedException {
        return !all(threads, values, predicate.negate());
    }
}