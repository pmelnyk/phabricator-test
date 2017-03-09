package com.andrasta.dashi.alpr;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.andrasta.dashi.camera.ImageUtil;
import com.andrasta.dashi.openalpr.Alpr;
import com.andrasta.dashi.openalpr.AlprResult;
import com.andrasta.dashi.openalpr.LaneDetector;
import com.andrasta.dashi.openalpr.LaneDetectorResult;
import com.andrasta.dashi.utils.Preconditions;

import java.io.File;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Creates number of threads specified in {@link ImageHandler#THREADS} to recognize
 * license plates on images posted through {@link ImageHandler#recognize(Image)} method.
 * <p>
 * Class takes care about closing {@link Image} after recognition.
 * Class is threadsafe.
 */
public class ImageHandler {
    private static final String TAG = "ImageHandler";

    private static final DecimalFormat decimalFormat = new DecimalFormat("0.##");
    private static final int THREADS = Runtime.getRuntime().availableProcessors();
    private static final long DEFAULT_IMAGE_QUEUE_TIMEOUT = 1000;
    private static final String CONFIG_FILE_NAME = "openalpr.conf";
    private static final String RUNTIME_DIR = "runtime_data";
    private static final int IMAGE_WAIT_TIMEOUT = 100;

    private final ArrayBlockingQueue<Image> imageQueue = new ArrayBlockingQueue<Image>(THREADS * 2);
    private final ExecutorService executor = Executors.newFixedThreadPool(THREADS);
    private final ImageHandlerThread[] imageHandlers = new ImageHandlerThread[THREADS];
    private final Semaphore recognitionSemaphore = new Semaphore(THREADS);
    private final LaneDetector laneDetector = new LaneDetector();
    private final ImageHandlerCallback callback;
    private final Handler callbackHandler;
    private final File configFile;
    private final File runtimeDir;

    private final BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
    private long imageQueueTimeout;
    private long lastImageQueueTime;
    private long handledImageCounter;
    private long handlingTime;
    private float worstPace;
    private float bestPace;
    private float avgPace;

    public ImageHandler(@NonNull File configDir, @NonNull ImageHandlerCallback callback) {
        this(configDir, callback, null);
    }

    public ImageHandler(@NonNull File configDir, @NonNull ImageHandlerCallback callback, @Nullable Handler callbackHandler) {
        Preconditions.assertParameterNotNull(configDir, "configDir");
        Preconditions.assertParameterNotNull(callback, "callback");
        configFile = new File(configDir, CONFIG_FILE_NAME);
        runtimeDir = new File(configDir, RUNTIME_DIR);
        checkAlprConfiguration(configFile, runtimeDir);
        this.callbackHandler = callbackHandler;
        this.callback = callback;
    }

    private void checkAlprConfiguration(@NonNull File configFile, @NonNull File runtimeDir) {
        if (!configFile.exists() || !configFile.isFile()) {
            throw new RuntimeException("No alpr config file " + configFile.getAbsolutePath());
        }
        if (!runtimeDir.exists() || !runtimeDir.isDirectory()) {
            throw new RuntimeException("No alpr runtime dir " + runtimeDir.getAbsolutePath());
        }
    }

    public int getThreadsNum() {
        return THREADS;
    }

    public void recognize(@NonNull Image image) {
        Preconditions.assertParameterNotNull(image, "image");
        synchronized (this) {
            if (imageHandlers[0] == null) {
                Log.e(TAG, "Not started. Skip image.");
                image.close();
                return;
            }
        }

        if (recognitionSemaphore.availablePermits() == 0) {
            Log.e(TAG, "No threads available. Skip image.");
            image.close();
            return;
        }

        if (System.currentTimeMillis() - lastImageQueueTime < imageQueueTimeout) {
            Log.e(TAG, "Processing timeout not passed. Skip image.");
            image.close();
            return;
        }

        lastImageQueueTime = System.currentTimeMillis();
        if (!imageQueue.offer(image)) {
            image.close();
        }
        Log.d(TAG, "New image; Queue size " + imageQueue.size());
    }

    public synchronized void start() {
        if (imageHandlers[0] == null) {
            Log.d(TAG, "Handler started. Threads num:" + THREADS);
            resetTimeoutsAndStatistics();
            for (int i = 0; i < THREADS; i++) {
                imageHandlers[i] = new ImageHandlerThread();
                executor.execute(imageHandlers[i]);
            }
        }
    }

    private void resetTimeoutsAndStatistics() {
        handledImageCounter = 0;
        handlingTime = 0;
        imageQueueTimeout = DEFAULT_IMAGE_QUEUE_TIMEOUT;
        lastImageQueueTime = 0;
        bestPace = Float.MIN_VALUE;
        worstPace = Float.MAX_VALUE;
        avgPace = 0;
    }

    public synchronized void stop() {
        if (imageHandlers[0] == null) {
            return;
        }

        for (int i = THREADS - 1; i >= 0; i--) {
            imageHandlers[i].stop();
            imageHandlers[i] = null;
        }

        Log.d(TAG, "Handler stopped. Queue size " + imageQueue.size());
        Image img = imageQueue.poll();
        while (img != null) {
            img.close();
            img = imageQueue.poll();
        }
    }

    public synchronized void setBitmapSize(int width, int height) {
        bitmapOptions.outHeight = height;
        bitmapOptions.outWidth = width;
    }

    @SuppressWarnings("FieldCanBeLocal")
    private final class ImageHandlerThread implements Runnable {
        private final AtomicBoolean stop = new AtomicBoolean(false);
        private String logTag;

        @Override
        @SuppressWarnings("InfiniteLoopStatement")
        public void run() {
            logTag = TAG + "#" + Thread.currentThread().getId();
            Alpr alpr = new Alpr(null, configFile.getAbsolutePath(), runtimeDir.getAbsolutePath());
            Image image = null;
            try {
                for (; ; ) {
                    image = imageQueue.poll(IMAGE_WAIT_TIMEOUT, TimeUnit.MILLISECONDS);
                    if (stop.get()) {
                        break;
                    } else if (image == null) {
                        continue;
                    }

                    try {
                        recognitionSemaphore.acquire();
                        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer(); // Y channel
                        if (yBuffer == null || yBuffer.remaining() == 0) {
                            Log.e(logTag, "No yBuffer for image " + image);
                            continue;
                        }
                        recognizeLicensePlate(alpr, image, yBuffer);
                        recognizeLanes(alpr, image, yBuffer);
                    } catch (IllegalStateException e) {
                        Log.w(logTag, "Image closed. Stop recognition.");
                    } finally {
                        recognitionSemaphore.release();
                        image.close();
                    }

                    if (stop.get()) {
                        break;
                    }
                }
            } catch (InterruptedException e) {
                Log.e(logTag, "Interrupted", e);
            } catch (Exception e) {
                callback.onFailure(e);
            } finally {
                if (image != null) {
                    image.close();
                }
                alpr.close();
            }
            Log.d(logTag, "ImageHandler terminated.");
        }

        private void recognizeLicensePlate(@NonNull Alpr alpr, @NonNull Image image, @NonNull ByteBuffer yBuffer) throws InterruptedException {
            final AlprResult result = alpr.recognizeFromByteBuffer(yBuffer, 1, image.getWidth(), image.getHeight());
            logStats(result.getTotalProcessingTime());

            Bitmap bitmap = null;
            if (!result.getPlates().isEmpty() && bitmapOptions.outWidth > 0) {
                bitmap = ImageUtil.imageToBitmap(image, bitmapOptions);
            }

            if (callbackHandler == null) {
                callback.onLicensePlateDetected(bitmap, result);
            } else {
                final Bitmap b = bitmap;
                callbackHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onLicensePlateDetected(b, result);
                    }
                });
            }
        }

        private void recognizeLanes(@NonNull Alpr alpr, @NonNull Image image, @NonNull ByteBuffer yBuffer) throws InterruptedException {
            long time = System.currentTimeMillis();
            final LaneDetectorResult lanes = laneDetector.recognizeLaneFromByteBuffer(yBuffer, 1, image.getWidth(), image.getHeight());
            time = System.currentTimeMillis() - time;
            Log.d(logTag, "Lane detection: " + time + '\t' + lanes);
            final int imageWidth = image.getWidth();
            final int imageHeight = image.getHeight();

            if (callbackHandler == null) {
                callback.onLaneDetected(imageWidth, imageHeight, lanes);
            } else {
                callbackHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onLaneDetected(imageWidth, imageHeight, lanes);
                    }
                });
            }
        }

        private void logStats(long imgHandlingTime) {
            Log.d(logTag, "Alpr recognition time: " + imgHandlingTime);
            synchronized (ImageHandler.this) {
                handledImageCounter++;
                handlingTime += imgHandlingTime;
                float pace = handledImageCounter / (handlingTime / 1000f);
                Log.d(logTag, "Image handling pace (img/sec): " + decimalFormat.format(pace));

                imageQueueTimeout = Math.round(THREADS / pace * 100);
                Log.d(logTag, "Image queue updated set: " + imageQueueTimeout);

                if (pace > bestPace) {
                    bestPace = pace;
                }
                if (pace < worstPace) {
                    worstPace = pace;
                }

                avgPace = movingAvg(pace, avgPace, handledImageCounter);
                String statMsg = String.format("Best\\Worst\\Avg pace: %s\\%s\\%s", decimalFormat.format(bestPace), decimalFormat.format(worstPace), decimalFormat.format(avgPace));
                Log.d(logTag, statMsg);
            }
        }

        void stop() {
            this.stop.set(true);
        }
    }

    private static float movingAvg(float value, float avg, long counter) {
        return (Math.abs(value) + ((counter - 1) * avg)) / counter;
    }

    public static interface ImageHandlerCallback {
        void onFailure(@NonNull Exception failure);

        void onLicensePlateDetected(@Nullable Bitmap bitmap, @NonNull AlprResult result);

        void onLaneDetected(int width, int height, @NonNull LaneDetectorResult lanes);
    }
}