package com.andrasta.dashi.alpr;

import android.media.Image;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.util.Log;

import com.andrasta.dashi.camera.ImageUtil;
import com.andrasta.dashi.openalpr.Alpr;
import com.andrasta.dashi.openalpr.AlprResult;
import com.andrasta.dashi.service.LicensePlateMatcher;
import com.andrasta.dashi.utils.Preconditions;

import java.io.File;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class AlprHandler {
    private static final String TAG = "AlprHandler";

    private static final DecimalFormat decimalFormat = new DecimalFormat("0.##");
    private static final String CONFIG_FILE_NAME = "openalpr.conf";
    private static final String RUNTIME_DIR = "runtime_data";
    private static final int IMAGE_WAIT_TIMEOUT = 100;

    private final ArrayBlockingQueue<Image> imageQueue = new ArrayBlockingQueue<Image>(10);
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AlprCallback callback;
    @NonNull
    private final LicensePlateMatcher licensePlateMatcher;
    private volatile ImageHandler imageHandler;
    private final Handler callbackHandler;
    private final Alpr alpr;
    private long handledImageCounter;
    private long handlingStartTime;
    private float bestPace = Float.MIN_VALUE;
    private float worstPace = Float.MAX_VALUE;
    private float avgPace;

    @UiThread
    public AlprHandler(@NonNull File configDir, @NonNull AlprCallback callback, @NonNull LicensePlateMatcher licensePlateMatcher) {
        this(configDir, callback, licensePlateMatcher, null);
    }

    public AlprHandler(@NonNull File configDir, @NonNull AlprCallback callback, @NonNull LicensePlateMatcher licensePlateMatcher, @Nullable Handler callbackHandler) {
        Preconditions.assertParameterNotNull(configDir, "configDir");
        Preconditions.assertParameterNotNull(callback, "callback");
        Preconditions.assertParameterNotNull(licensePlateMatcher, "licensePlateMatcher");
        File configFile = new File(configDir, CONFIG_FILE_NAME);
        File runtimeDir = new File(configDir, RUNTIME_DIR);
        checkAlprConfiguration(configFile, runtimeDir);
        this.alpr = new Alpr(null, configFile.getAbsolutePath(), runtimeDir.getAbsolutePath());
        this.callbackHandler = callbackHandler;
        this.licensePlateMatcher = licensePlateMatcher;
        this.callback = callback;
    }

    private void checkAlprConfiguration(File configFile, File runtimeDir) {
        if (!configFile.exists() || !configFile.isFile()) {
            throw new RuntimeException("No alpr config file " + configFile.getAbsolutePath());
        }
        if (!runtimeDir.exists() || !runtimeDir.isDirectory()) {
            throw new RuntimeException("No alpr runtime dir " + runtimeDir.getAbsolutePath());
        }
    }

    public void recognize(@NonNull Image image) {
        Preconditions.assertParameterNotNull(image, "image");
        if (imageHandler == null) {
            Log.e(TAG, "Not started. Skip image.");
            image.close();
            return;
        }
        imageQueue.add(image);
        Log.d(TAG, "New image; Queue size " + imageQueue.size());
    }

    @UiThread
    public void start() {
        if (imageHandler == null) {
            Log.d(TAG, "Handler started.");
            handledImageCounter = 0;
            handlingStartTime = 0;
            imageHandler = new ImageHandler();
            executor.execute(imageHandler);
        }
    }

    @UiThread
    public void stop() {
        if (imageHandler == null) {
            return;
        }

        imageHandler.stop();
        imageHandler = null;
        Log.d(TAG, "Handler stopped. Queue size " + imageQueue.size());
        Image img = imageQueue.poll();
        while (img != null) {
            img.close();
            img = imageQueue.poll();
        }
    }

    @SuppressWarnings("FieldCanBeLocal")
    private final class ImageHandler implements Runnable {
        private final AtomicBoolean stop = new AtomicBoolean(false);

        @Override
        @SuppressWarnings("InfiniteLoopStatement")
        public void run() {
            Image image = null;
            try {
                for (; ; ) {
                    image = imageQueue.poll(IMAGE_WAIT_TIMEOUT, TimeUnit.MILLISECONDS);
                    if (stop.get()) {
                        break;
                    } else if (image == null) {
                        continue;
                    }

                    ByteBuffer yBuffer = image.getPlanes()[0].getBuffer(); // Y channel
                    long timestamp = System.currentTimeMillis();
                    final AlprResult result = alpr.recognizeFromByteBuffer(yBuffer, 1, image.getWidth(), image.getHeight());
                    logStats(System.currentTimeMillis() - timestamp);

                    byte[] bytes = null;
                    if (!licensePlateMatcher.findMatches(result).isEmpty()) {
                        bytes = ImageUtil.imageToJpeg(image);
                    }

                    final byte[] jpeg = bytes;
                    image.close();
                    if (callbackHandler == null) {
                        callback.onComplete(jpeg, result);
                    } else {
                        callbackHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onComplete(jpeg, result);
                            }
                        });
                    }

                    if (stop.get()) {
                        break;
                    }
                }
            } catch (InterruptedException e) {
                Log.e(TAG, "Interrupted", e);
            } catch (Exception e) {
                callback.onFailure(e);
            } finally {
                if (image != null) {
                    image.close();
                }
            }
            Log.d(TAG, "ImageHandler terminated.");
        }

        private void logStats(long imgHandlingTime) {
            Log.d(TAG, "Alpr recognition time: " + imgHandlingTime);

            if (handlingStartTime == 0) {
                handlingStartTime = System.currentTimeMillis();
            } else {
                handledImageCounter++;
                float pace = handledImageCounter / ((System.currentTimeMillis() - handlingStartTime) / 1000f);
                Log.d(TAG, "Image handling pace (img/sec): " + decimalFormat.format(pace));

                if (pace > bestPace) {
                    bestPace = pace;
                }
                if (pace < worstPace) {
                    worstPace = pace;
                }

                if (handledImageCounter > 3) { // Skip first 3 frames, cos first frames have large deviation
                    avgPace = movingAvg(pace, avgPace, handledImageCounter - 3);
                }

                String statMsg = String.format("Best\\Worst\\Avg pace: %s\\%s\\%s", decimalFormat.format(bestPace), decimalFormat.format(worstPace), decimalFormat.format(avgPace));
                Log.d(TAG, statMsg);

            }
        }

        void stop() {
            this.stop.set(true);
        }
    }

    private static float movingAvg(float value, float avg, long counter) {
        return (Math.abs(value) + ((counter - 1) * avg)) / counter;
    }

    public static interface AlprCallback {
        void onFailure(Exception failure);

        void onComplete(byte[] bytes, AlprResult result);
    }
}