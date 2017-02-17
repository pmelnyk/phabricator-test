package com.andrasta.dashi.openalpr;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.andrasta.dashi.utils.Preconditions;

import java.nio.ByteBuffer;

/**
 * Created by breh on 2/14/17.
 */

public final class Alpr {

    private final long nativeReference;

    public Alpr(@Nullable String country, @Nullable String config, @Nullable String runtimeDir) {
        nativeReference = nCreate(country, config, runtimeDir);
    }

    public @NonNull String getVersion() {
        return nGetVersion(nativeReference);
    }

    public @NonNull AlprResult recognizeFromFilePath(@NonNull String filePath) {
        Preconditions.assertParameterNotNull(filePath,"filePath");
        return nRecognizeFilePath(nativeReference, filePath);
    }

    public @NonNull AlprResult recognizeFromByteArray(@NonNull byte[] pixelData, int width, int height) {
        Preconditions.assertParameterNotNull(pixelData,"pixelData");
        return nRecognizeByteArray(nativeReference, pixelData, width, height);
    }

    public  @NonNull AlprResult recognizeFromFileData(@NonNull byte[] fileData) {
        Preconditions.assertParameterNotNull(fileData,"fileData");
        return nRecognizeFileData(nativeReference, fileData);
    }

    public @NonNull AlprResult recognizeFromByteBuffer(@NonNull ByteBuffer byteBuffer,  int pixelSize, int width, int height) {
        Preconditions.assertParameterNotNull(byteBuffer,"byteBuffer");
        return nRecognizeByteBuffer(nativeReference, byteBuffer, pixelSize, width, height);
    }

    @Override
    protected void finalize() throws Throwable {
        nDelete(nativeReference);
    }

    // native calls
    private static native long nCreate(@Nullable String country, @Nullable String config, @Nullable String runtimeDir);

    private static native void nDelete(long nativeReference);

    private static native String nGetVersion(long nativeReference);

    private static native AlprResult nRecognizeFilePath(long nativeReference, @NonNull String filePath);

    private static native AlprResult nRecognizeFileData(long nativeReference, @NonNull byte[] fileData);

    private static native AlprResult nRecognizeByteArray(long nativeReference, @NonNull byte[] pixelData, int width, int heigh);

    private static native AlprResult nRecognizeByteBuffer(long nativeReference, @NonNull ByteBuffer byteBuffer, int pixelSize, int width, int height);


}
