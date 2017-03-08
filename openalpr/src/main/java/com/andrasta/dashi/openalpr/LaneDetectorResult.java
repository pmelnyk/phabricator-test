package com.andrasta.dashi.openalpr;

import android.graphics.Point;
import android.support.annotation.NonNull;

import com.andrasta.dashi.utils.NativeCallback;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by breh on 2/16/17.
 */

public final class LaneDetectorResult {

    private final float thickness;
    private final Point[] plateCoordinates;

    @NativeCallback
    public LaneDetectorResult(float thickness, @NonNull Point[] plateCoordinates) {
        this.thickness = thickness;
        this.plateCoordinates = plateCoordinates;
    }

    public float getThickness() {
        return thickness;
    }

    public @NonNull List<Point> getPlateCoordinates() {
        return Collections.unmodifiableList(Arrays.asList(plateCoordinates));
    }

    @Override
    public String toString() {
        return "LaneDetectorResult{" +
                "thickness=" + thickness +
                ", plateCoordinates=" + Arrays.toString(plateCoordinates) +
                '}';
    }
}
