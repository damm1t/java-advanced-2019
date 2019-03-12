package ru.ifmo.rain.sokolov.arrayset;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;

public class ArraySet<T> extends AbstractSet<T> implements NavigableSet<T> {

    private final List<T> data;
    private final Comparator<? super T> comparator;

    public ArraySet() {
        this(Collections.emptyList(), null);
    }

    public ArraySet(Comparator<? super T> cmp) {
        this(Collections.emptyList(), cmp);
    }

    public ArraySet(Collection<? extends T> data) {
        this(data, null, true);
    }

    public ArraySet(Collection<? extends T> data, Comparator<? super T> cmp) {
        this(data, cmp, true);
    }

    private ArraySet(List<T> data, Comparator<? super T> cmp) {
        this.data = data;
        this.comparator = cmp;
    }

    private ArraySet(Collection<? extends T> data, Comparator<? super T> cmp, boolean doSort) {
        List<T> sortedData;
        if (doSort) {
            var s = new TreeSet<T>(cmp);
            s.addAll(data);
            sortedData = new ArrayList<>(s);
        } else {
            sortedData = new ArrayList<>(data);
        }
        this.data = Collections.unmodifiableList(sortedData);
        this.comparator = cmp;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean contains(Object o) {
        return Collections.binarySearch(data, o, (Comparator<Object>) comparator) >= 0;
    }

    private T get(int index) {
        return ((index < 0 || index >= size()) ? null : data.get(index));
    }

    private int abstractFind(T t, int inclusive, int lower) {
        int pos = Collections.binarySearch(data, t, comparator);
        return (pos >= 0 ? pos + inclusive : ~pos + lower);
    }

    private T findLower(T t, boolean inclusive) {
        return get(abstractFind(t, inclusive ? 0 : -1, -1));
    }

    private T findUpper(T t, boolean inclusive) {
        return get(abstractFind(t, inclusive ? 0 : 1, 0));
    }

    @Override
    public T lower(T t) {
        return findLower(t, false);
    }

    @Override
    public T floor(T t) {
        return findLower(t, true);
    }

    @Override
    public T ceiling(T t) {
        return findUpper(t, true);
    }

    @Override
    public T higher(T t) {
        return findUpper(t, false);
    }

    @Override
    public T pollFirst() {
        throw new UnsupportedOperationException("Error: pollFirst is unsupported");
    }

    @Override
    public T pollLast() {
        throw new UnsupportedOperationException("Error: pollLast is unsupported");
    }

    @Override
    public Iterator<T> iterator() {
        return data.iterator();
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        data.forEach(action);
    }

    @Override
    public <T1> T1[] toArray(IntFunction<T1[]> generator) {
        return data.toArray(generator);
    }

    @Override
    public boolean removeIf(Predicate<? super T> filter) {
        throw new UnsupportedOperationException("Error: removeIf is unsupported");
    }

    @Override
    public NavigableSet<T> descendingSet() {
        return new ArraySet<>(new ReversedList<>(data), Collections.reverseOrder(comparator));
    }

    @Override
    public Iterator<T> descendingIterator() {
        return descendingSet().iterator();
    }

    @SuppressWarnings("unchecked")
    private void checkOnException(T fromElement, T toElement) throws IllegalArgumentException {
        if (comparator != null) {
            if (comparator.compare(fromElement, toElement) > 0) {
                throw new IllegalArgumentException();
            }
        } else if (fromElement instanceof Comparable) {
            if (((Comparable) fromElement).compareTo(toElement) > 0)
                throw new IllegalArgumentException();
        }
    }

    private NavigableSet<T> subSet(T fromElement, boolean fromInclusive, T toElement, boolean toInclusive,
                                   boolean enableExceptions) {
        if (enableExceptions)
            checkOnException(fromElement, toElement);
        int left = abstractFind(fromElement, fromInclusive ? 0 : 1, 0);
        int right = abstractFind(toElement, toInclusive ? 0 : -1, -1) + 1;
        return new ArraySet<>((right <= left ? Collections.emptyList() : data.subList(left, right)), comparator);
    }

    @Override
    public NavigableSet<T> subSet(T fromElement, boolean fromInclusive, T toElement, boolean toInclusive) {
        return subSet(fromElement, fromInclusive, toElement, toInclusive, true);
    }

    @Override
    public NavigableSet<T> headSet(T toElement, boolean inclusive) {
        if (isEmpty()) return this;
        return subSet(first(), true, toElement, inclusive, false);
    }

    @Override
    public NavigableSet<T> tailSet(T fromElement, boolean inclusive) {
        if (isEmpty()) return this;
        return subSet(fromElement, inclusive, last(), true, false);
    }

    @Override
    public Comparator<? super T> comparator() {
        return comparator;
    }

    @Override
    public SortedSet<T> subSet(T fromElement, T toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    @Override
    public SortedSet<T> headSet(T toElement) {
        return headSet(toElement, false);
    }

    @Override
    public SortedSet<T> tailSet(T fromElement) {
        return tailSet(fromElement, true);
    }

    private void emptyCheck() {
        if (isEmpty()) throw new NoSuchElementException("Try to access a non-existent element");
    }

    @Override
    public T first() {
        emptyCheck();
        return data.get(0);
    }

    @Override
    public T last() {
        emptyCheck();
        return data.get(data.size() - 1);
    }

    @Override
    public int size() {
        return data.size();
    }
}