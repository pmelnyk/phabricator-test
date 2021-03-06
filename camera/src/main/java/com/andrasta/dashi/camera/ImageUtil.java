package com.andrasta.dashi.camera;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.andrasta.dashi.utils.Preconditions;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

@SuppressWarnings("WeakerAccess")
public class ImageUtil {
    private static final int JPEG_QUALITY = 80;

    private ImageUtil() {
        super();
    }

    public static Bitmap imageToBitmap(@NonNull Image image) {
        return imageToBitmap(image, null);
    }

    public static Bitmap imageToBitmap(@NonNull Image image, @Nullable BitmapFactory.Options options) {
        byte[] data = imageToJpeg(image);
        return BitmapFactory.decodeByteArray(data, 0, data.length, options);
    }

    public static byte[] imageToJpeg(@NonNull Image image) {
        Preconditions.assertReturnNotNull(image, "image");
        byte[] data = null;
        if (image.getFormat() == ImageFormat.JPEG) {
            Image.Plane[] planes = image.getPlanes();
            ByteBuffer buffer = planes[0].getBuffer();
            data = new byte[buffer.capacity()];
            buffer.get(data);
            return data;
        } else if (image.getFormat() == ImageFormat.YUV_420_888) {
            data = NV21toJPEG(
                    YUV420888toNV21(image), image.getWidth(), image.getHeight());
        }
        return data;
    }

    public static byte[] YUV420888toNV21(@NonNull Image image) {
        Preconditions.assertReturnNotNull(image, "image");

        byte[] nv21;
        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
        ByteBuffer uBuffer = image.getPlanes()[1].getBuffer();
        ByteBuffer vBuffer = image.getPlanes()[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        nv21 = new byte[ySize + uSize + vSize];

        int vstat = ySize;
        int ustat = ySize + vSize;
        //U and V are swapped
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, vstat, vSize);
        uBuffer.get(nv21, ustat, uSize);

        return nv21;
    }

    public static byte[] NV21toJPEG(@NonNull byte[] nv21, int width, int height) {
        Preconditions.assertReturnNotNull(nv21, "nv21");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        YuvImage yuv = new YuvImage(nv21, ImageFormat.NV21, width, height, null);
        yuv.compressToJpeg(new Rect(0, 0, width, height), JPEG_QUALITY, out);
        return out.toByteArray();
    }

    public static byte[] bitmapToJpeg(@NonNull Bitmap bitmap) {
        Preconditions.assertReturnNotNull(bitmap, "bitmap");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out);
        return out.toByteArray();
    }

    public static byte[] toGrayScale(@NonNull Bitmap bitmap) {
        Preconditions.assertReturnNotNull(bitmap, "bitmap");

        final int width = bitmap.getWidth();
        final int height = bitmap.getHeight();
        byte[] grayScaleArray = new byte[width * height];
        double GS_RED = 0.299;
        double GS_GREEN = 0.587;
        double GS_BLUE = 0.114;
        int pixel;
        int R, G, B;

        // scan through every single pixel
        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                // get one pixel color
                pixel = bitmap.getPixel(x, y);

                // retrieve color of all channels
                R = Color.red(pixel);
                G = Color.green(pixel);
                B = Color.blue(pixel);

                // take conversion up to one single value
                pixel = (int) (GS_RED * R + GS_GREEN * G + GS_BLUE * B);
                // set new pixel color to output bitmap
                int index = width * y + x;
                grayScaleArray[index] = (byte) pixel;
                //Show grayscaled image
            }
        }
        return grayScaleArray;
    }
}