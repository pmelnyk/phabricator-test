package com.andrasta.dashi.camera;

import android.media.Image;
import android.support.annotation.NonNull;

import com.andrasta.dashi.utils.Preconditions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Help to save an {@link Image} into the specified {@link File}.
 */
public class ImageSaver {
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public void saveToFile(@NonNull Image image, @NonNull File file) {
        Preconditions.assertParameterNotNull(image, "image");
        Preconditions.assertParameterNotNull(file, "file");
        executor.execute(new SaveImageTask(image, file));
    }

    private static class SaveImageTask implements Runnable {
        private final Image image;
        private final File file;

        SaveImageTask(Image image, File file) {
            this.image = image;
            this.file = file;
        }

        @Override
        public void run() {
            byte[] bytes = ImageUtil.imageToJpeg(image);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(bytes);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                image.close();
            }
        }
    }
}