package com.andrasta.dashi.utils;

import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.util.Collection;

/**
 * Created by breh on 12/4/16.
 */

public final class Preconditions {

    private Preconditions() {}



    @NonNull
    public static <T> T assertParameterNotNull(@NonNull T parameter, @Nullable String parameterName) {
        if (parameter == null) {
            throw new IllegalArgumentException("Parameter  " + parameterName + " cannot be null!");
        }
        return parameter;
    }


    @NonNull
    public static <T> Collection<T> assertCollectionNotEmpty(@NonNull Collection<T> parameter,
                                                            @Nullable String parameterName) {
        if (parameter == null || parameter.isEmpty()) {
            throw new IllegalArgumentException("Parameter  " + parameterName + " cannot be empty!");
        }
        return parameter;
    }

    @NonNull
    public static String assertStringNotEmpty(@NonNull String parameter, @Nullable String parameterName) {
        if (TextUtils.isEmpty(parameter)) {
            throw new IllegalArgumentException("Parameter  " + parameterName + " cannot be empty!");
        }
        return parameter;
    }

    @NonNull
    public static <T> T assertReturnNotNull(@NonNull T returnVal, @Nullable String parameterName) {
        if (returnVal == null) {
            throw new IllegalArgumentException(
                    "Return value  " + parameterName + " cannot be null!");
        }
        return returnVal;
    }

    public static void assertUiThread() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new RuntimeException("Has to be called on the main thread!");
        }
    }

    public static void assertNonUiThread() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new RuntimeException("Cannot be called on the main thread!");
        }
    }

}
