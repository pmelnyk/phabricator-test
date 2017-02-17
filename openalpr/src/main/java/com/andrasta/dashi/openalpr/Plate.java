package com.andrasta.dashi.openalpr;

import android.support.annotation.NonNull;

import com.andrasta.dashi.utils.NativeCallback;

/**
 * Created by breh on 2/16/17.
 */

public final class Plate {

    private final String plate;
    private final float confidence;

    @NativeCallback
    Plate(@NonNull String plate, float confidence) {
        this.plate = plate;
        this.confidence = confidence;
    }

    public @NonNull String getPlate() {
        return plate;
    }

    public float getConfidence() {
        return confidence;
    }

    @Override
    public String toString() {
        return new StringBuilder("Plate:{ plate: '")
                .append(plate).append("', confidence: ")
                .append(confidence).append("}").toString();
    }
}
