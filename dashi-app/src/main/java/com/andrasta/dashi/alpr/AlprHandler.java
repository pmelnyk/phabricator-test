package com.andrasta.dashi.alpr;

import android.media.Image;
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;
import android.util.Log;

import com.andrasta.dashi.openalpr.Alpr;
import com.andrasta.dashi.openalpr.AlprResult;
import com.andrasta.dashi.utils.Callback;
import com.andrasta.dashi.utils.Preconditions;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class AlprHandler {
    private static final String TAG = "AlprHandler";

    private static final String CONFIG_FILE_NAME = "openalpr.conf";
    private static final String RUNTIME_DIR = "runtime_data";
    private static final int IMAGE_WAIT_TIMEOUT = 100;

    private final ArrayBlockingQueue<Image> imageQueue = new ArrayBlockingQueue<Image>(10);
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Callback<AlprResult, Exception> callback;
    private volatile ImageHandler imageHandler;
    private final Alpr alpr;

    @UiThread
    public AlprHandler(@NonNull File configDir, @NonNull Callback<AlprResult, Exception> callback) {
        Preconditions.assertParameterNotNull(configDir, "configDir");
        Preconditions.assertParameterNotNull(callback, "callback");
        File configFile = new File(configDir, CONFIG_FILE_NAME);
        File runtimeDir = new File(configDir, RUNTIME_DIR);
        checkAlprConfiguration(configFile, runtimeDir);
        this.alpr = new Alpr(null, configFile.getAbsolutePath(), runtimeDir.getAbsolutePath());
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

                    ByteBuffer byteBuffer = image.getPlanes()[0].getBuffer(); // Y channel
                    long timestamp = System.currentTimeMillis();
                    AlprResult result = alpr.recognizeFromByteBuffer(byteBuffer, 1, image.getWidth(), image.getHeight());
                    long delta = System.currentTimeMillis() - timestamp;
                    Log.w(TAG, "Alpr recognition time: " + delta);
                    image.close();
                    callback.onComplete(result);

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

        void stop() {
            this.stop.set(true);
        }
    }
}