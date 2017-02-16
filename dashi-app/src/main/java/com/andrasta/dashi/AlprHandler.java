package com.andrasta.dashi;

import android.graphics.Bitmap;
import android.media.Image;
import android.support.annotation.NonNull;
import android.util.Log;

import com.andrasta.dashi.camera.ImageUtil;
import com.andrasta.dashi.utils.Callback;
import com.andrasta.dashi.utils.Preconditions;

import java.util.concurrent.ArrayBlockingQueue;

public class AlprHandler extends Thread {
    private static final String TAG = "AlprHandler";

    private final ArrayBlockingQueue<Image> imageQueue = new ArrayBlockingQueue<Image>(10);
    private final Callback<AlprResponse, Exception> callback;

    public AlprHandler(@NonNull Callback<AlprResponse, Exception> callback) {
        super(TAG);
        Preconditions.assertParameterNotNull(callback, "callback");
        this.callback = callback;
        start();
    }

    public void request(Image image) {
        imageQueue.add(image);
    }

    @Override
    @SuppressWarnings("InfiniteLoopStatement")
    public void run() {
        Image image = null;
        try {
            for (;;) {
                image = imageQueue.take();

                Bitmap b = ImageUtil.imageToBitmap(image);
                b.recycle();

                // call alpr

                callback.onComplete(new AlprResponse(image, "?", 0));
                image.close();
            }
        } catch (InterruptedException e) {
            Log.d(TAG, "Interrupted", e);
        } catch (Exception e) {
            callback.onFailure(e);
        } finally {
            if (image != null) {
                image.close();
            }
        }
    }

    public class AlprResponse {
        public final Image image;
        public final String licensePlate;
        public final float probability;

        public AlprResponse(Image image, String licensePlate, float probability) {
            this.image = image;
            this.licensePlate = licensePlate;
            this.probability = probability;
        }
    }
}