package com.andrasta.dashi.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CyclicBuffer<E> {
    private Object[] data;
    private int size = 0;
    private int last = 0;

    public CyclicBuffer(int size) {
        this.data = new Object[size];
    }

    @SuppressWarnings("unchecked")
    public E add(E item) {
        E e = (E) (data[last]);
        data[last] = item;
        last = (last + 1) % data.length;
        if (size < data.length) {
            size++;
        }
        return e;
    }

    @SuppressWarnings("unchecked")
    public List<E> asList() {
        if (size == 0) {
            return Collections.emptyList();
        }
        ArrayList<E> al = new ArrayList<>();
        for (int i = last - 1, j = 0; j < size; i--, j++) {
            if (i == -1) {
                i = size - 1;
            }
            al.add((E) data[i]);
        }
        return al;
    }

    public void reset() {
        size = 0;
        last = 0;
    }
}