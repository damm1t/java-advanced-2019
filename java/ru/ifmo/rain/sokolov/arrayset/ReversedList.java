package ru.ifmo.rain.sokolov.arrayset;

import java.util.AbstractList;
import java.util.List;
import java.util.RandomAccess;

public class ReversedList<T> extends AbstractList<T> implements RandomAccess {

    private final List<T> data;
    private boolean isReversed;

    ReversedList(List<T> data) {
        if (!(data instanceof RandomAccess)) {
            throw new IllegalArgumentException("Error: expected random access list");
        }
        if (data instanceof ReversedList) {
            this.data = ((ReversedList<T>) data).data;
            isReversed = !((ReversedList<T>) data).isReversed;
        } else {
            this.data = data;
            isReversed = true;
        }
    }

    public void reverse() {
        isReversed = !isReversed;
    }

    @Override
    public T get(int index) {
        return isReversed ? data.get(size() - index - 1) : data.get(index);
    }

    @Override
    public int size() {
        return data.size();
    }
}
