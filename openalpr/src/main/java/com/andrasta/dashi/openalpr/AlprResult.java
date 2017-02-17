package com.andrasta.dashi.openalpr;

import android.support.annotation.NonNull;

import com.andrasta.dashi.utils.NativeCallback;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by breh on 2/16/17.
 */

public final class AlprResult {

    private final int totalProcessingTime;
    private final int sourceWidth;
    private final int sourceHeight;
    private final List<PlateResult> plates;

    @NativeCallback
    AlprResult(@NonNull PlateResult[] plateResults, int totalProcessingTime, int sourceWidth, int sourceHeight) {
        if (plateResults != null) {
            this.plates = Collections.unmodifiableList(Arrays.asList(plateResults));
        } else {
            this.plates = Collections.emptyList();
        }
        this.totalProcessingTime = totalProcessingTime;
        this.sourceWidth = sourceWidth;
        this.sourceHeight = sourceHeight;
    }

    public int getTotalProcessingTime() {
        return totalProcessingTime;
    }

    public int getSourceWidth() {
        return sourceWidth;
    }

    public int getSourceHeight() {
        return sourceHeight;
    }

    public @NonNull List<PlateResult> getPlates() {
        return plates;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("AlprResult:{ ");

        sb.append("totalProcessingTime: ").append(totalProcessingTime);
        sb.append(", sourceWidth: ").append(sourceWidth);
        sb.append(", sourceHeight: ").append(sourceHeight);

        sb.append(", plates: [");
        for (PlateResult plate: plates) {
            sb.append(plate).append(", ");
        }
        sb.append("]");

        return sb.toString();
    }
}
