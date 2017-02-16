package com.andrasta.dashi.utils;

import android.support.annotation.NonNull;

public interface Callback<T, E> {
    void onComplete(@NonNull T t);

    void onFailure(@NonNull E e);
}