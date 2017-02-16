package com.andrasta.dashi.openalpr;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.andrasta.dashi.utils.Preconditions;

import java.nio.ByteBuffer;

/**
 * Created by breh on 2/14/17.
 */

public final class Alpr {

    private final long nativeRefernce;

    public Alpr(@Nullable String country, @Nullable String config, @Nullable String runtimeDir) {
        nativeRefernce = nCreate(config, config, runtimeDir);
    }

    public @NonNull String getVersion() {
        return nGetVersion(nativeRefernce);
    }

    public String recognizeFromFilePath(@NonNull String filePath) {
        Preconditions.assertParameterNotNull(filePath,"filePath");
        return nRecognizeFilePath(nativeRefernce, filePath);
    }

    public String recognizeFromByteArray(@NonNull byte[] pixelData, int width, int height) {
        Preconditions.assertParameterNotNull(pixelData,"pixelData");
        return nRecognizeByteArray(nativeRefernce, pixelData, width, height);
    }

    public String recognizeFromFileData(@NonNull byte[] fileData) {
        Preconditions.assertParameterNotNull(fileData,"fileData");
        return nRecognizeFileData(nativeRefernce, fileData);
    }

    public String recognizeFromByteBuffer(@NonNull ByteBuffer byteBuffer,  int pixelSize, int width, int height) {
        Preconditions.assertParameterNotNull(byteBuffer,"byteBuffer");
        return nRecognizeByteBuffer(nativeRefernce, byteBuffer, pixelSize, width, height);
    }

    @Override
    protected void finalize() throws Throwable {
        nDelete(nativeRefernce);
    }

    // native calls
    private static native long nCreate(@Nullable String country, @Nullable String config, @Nullable String runtimeDir);

    private static native void nDelete(long nativeReference);

    private static native String nGetVersion(long nativeReference);

    private static native String nRecognizeFilePath(long nativeReference, @NonNull String filePath);

    private static native String nRecognizeFileData(long nativeReference, @NonNull byte[] fileData);

    private static native String nRecognizeByteArray(long nativeReference, @NonNull byte[] pixelData, int width, int heigh);

    private static native String nRecognizeByteBuffer(long nativeReference, @NonNull ByteBuffer byteBuffer, int pixelSize, int width, int height);


}
