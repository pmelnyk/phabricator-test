package com.andrasta.dashi.openalpr;

import android.graphics.Point;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.andrasta.dashi.utils.NativeCallback;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by breh on 2/16/17.
 */


public final class PlateResult {

    private final Plate bestPlate;
    private final List<Plate> otherCandidates;
    private final int processingTimeInMs;
    private final Point[] plateCoordinates;
    private final int plateIndex;

    @NativeCallback
    PlateResult(@Nullable Plate bestPlate, @Nullable Plate[] otherCandidates, int processingTimeInMs,
                @Nullable Point[] plateCoordinates, int plateIndex) {
        this.bestPlate = bestPlate;
        if (otherCandidates != null) {
            this.otherCandidates = Collections.unmodifiableList(Arrays.asList(otherCandidates));
        } else {
            this.otherCandidates = Collections.emptyList();
        }
        this.processingTimeInMs = processingTimeInMs;
        this.plateCoordinates = plateCoordinates;
        this.plateIndex = plateIndex;
    }

    public @Nullable Plate getBestPlate() {
        return bestPlate;
    }

    public @NonNull List<Plate> getOtherCandidates() {
        return otherCandidates;
    }

    public int getProcessingTimeInMs() {
        return processingTimeInMs;
    }

    public Point[] getPlateCoordinates() {
        return plateCoordinates;
    }

    public int getPlateIndex() {
        return plateIndex;
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder("PlateResult:{ ");
        if (bestPlate != null) {
            sb.append("bestPlate: ").append(bestPlate).append(", ");
        }
        sb.append("processingTime: ").append(processingTimeInMs);
        sb.append(", plateIndex: ").append(plateIndex);
        if (plateCoordinates != null) {
            sb.append(", plateCoordinates: [");
            for (int i = 0; i < plateCoordinates.length; i++) {
                sb.append(plateCoordinates[i]).append(", ");
            }
            sb.append("]");
        }
        sb.append(", otherCandidates: [");
        for (Plate plate: otherCandidates) {
            sb.append(plate).append(", ");
        }
        sb.append("]");

        return sb.toString();

    }

}
