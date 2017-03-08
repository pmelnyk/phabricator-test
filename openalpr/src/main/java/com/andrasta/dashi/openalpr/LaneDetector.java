package com.andrasta.dashi.openalpr;

import android.support.annotation.NonNull;

import com.andrasta.dashi.utils.Preconditions;

import java.nio.ByteBuffer;

public final class LaneDetector {
    private final long nativeReference;

    public LaneDetector() {
        nativeReference = nCreate();
    }

    public @NonNull LaneDetectorResult recognizeLaneFromByteBuffer(@NonNull ByteBuffer byteBuffer,  int pixelSize, int width, int height) {
        Preconditions.assertParameterNotNull(byteBuffer,"byteBuffer");
        return nRecognizeLaneByteBuffer(nativeReference, byteBuffer, pixelSize, width, height);
    }


    @Override
    protected void finalize() throws Throwable {
        nDelete(nativeReference);
    }

    // native calls
    private static native long nCreate();
    private static native LaneDetectorResult nRecognizeLaneByteBuffer(long nativeReference, @NonNull ByteBuffer byteBuffer, int pixelSize, int width, int height);
    private static native void nDelete(long nativeReference);

}
