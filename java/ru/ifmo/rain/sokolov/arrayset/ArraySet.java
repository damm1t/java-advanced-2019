package ru.ifmo.rain.sokolov.arrayset;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;

public class ArraySet<T> extends AbstractSet<T> implements NavigableSet<T> {

    private final List<T> data;
    private Comparator<? super T> comparator;

    public ArraySet() {
        this(Collections.emptyList(), null, false);
    }

    public ArraySet(Comparator<? super T> cmp) {
        this(Collections.emptyList(), cmp, false);
    }

    public ArraySet(Collection<T> data) {
        this(data, null, true);
    }

    public ArraySet(Collection<T> data, Comparator<? super T> cmp) {
        this(data, cmp, true);
    }

    private ArraySet(Collection<T> data, Comparator<? super T> comparator, boolean doSort) {
        List<T> sortedData;
        if (doSort) {
            var s = new TreeSet<T>(comparator);
            s.addAll(data);
            sortedData = new ArrayList<T>(s);
        } else if (data instanceof List) {
            sortedData = (List<T>) data;
        } else {
            sortedData = new ArrayList<T>(data);
        }
        this.data = Collections.unmodifiableList(sortedData);
        this.comparator = comparator;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean contains(Object o) {
        return Collections.binarySearch(data, o, (Comparator<Object>) comparator) >= 0;
    }


    private int abstractFind(T t, int inclusive, int lower) {
        int pos = Collections.binarySearch(data, Objects.requireNonNull(t), comparator);
        return (pos >= 0 ? pos + inclusive : ~pos + lower);
    }

    private T findLower(T t, boolean inclusive) {
        int i = abstractFind(t, inclusive ? 0 : -1, -1);
        return i >= 0 ? data.get(i) : null;
    }

    private T findUpper(T t, boolean inclusive) {
        int i = abstractFind(t, inclusive ? 0 : 1, 0);
        return i < data.size() ? data.get(i) : null;
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

        return new ArraySet<>(new ReversedList<>(data), Collections.reverseOrder(comparator), false);
    }

    @Override
    public Iterator<T> descendingIterator() {
        return descendingSet().iterator();
    }

    @Override
    public NavigableSet<T> subSet(T fromElement, boolean fromInclusive, T toElement, boolean toInclusive) {
        int left = abstractFind(fromElement, fromInclusive ? 0 : 1, 0);
        int right = abstractFind(toElement, toInclusive ? 0 : -1, -1);
        return (right <= left ?
                Collections.emptyNavigableSet() :
                new ArraySet<>(data.subList(left, right + 1), comparator, false));
    }

    @Override
    public NavigableSet<T> headSet(T toElement, boolean inclusive) {
        if (isEmpty()) return this;
        return subSet(first(), true, toElement, inclusive);
    }

    @Override
    public NavigableSet<T> tailSet(T fromElement, boolean inclusive) {
        if (isEmpty()) return this;
        return subSet(fromElement, inclusive, last(), true);
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
        if (data.isEmpty())
            throw new NoSuchElementException();
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

    /*@Override
    public Spliterator<T> spliterator() {
        return null;
    }

    @Override
    public Stream<T> stream() {
        return null;
    }

    @Override
    public Stream<T> parallelStream() {
        return null;
    }*/

    @Override
    public int size() {
        return data.size();
    }
}