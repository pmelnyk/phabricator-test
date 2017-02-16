package com.andrasta.dashi.camera;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

@SuppressWarnings("WeakerAccess")
public class ImageUtil {
    public static Bitmap imageToBitmap(Image image) {
        byte[] data = imageToJpeg(image);
        return BitmapFactory.decodeByteArray(data, 0, data.length);
    }

    public static byte[] imageToJpeg(Image image) {
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
//
//    private static byte[] convertYUV420ToN21(Image imgYUV420, boolean grayscale) {
//
//        Image.Plane yPlane = imgYUV420.getPlanes()[0];
//        byte[] yData = getRawCopy(yPlane.getBuffer());
//
//        Image.Plane uPlane = imgYUV420.getPlanes()[1];
//        byte[] uData = getRawCopy(uPlane.getBuffer());
//
//        Image.Plane vPlane = imgYUV420.getPlanes()[2];
//        byte[] vData = getRawCopy(vPlane.getBuffer());
//
//        // NV21 stores a full frame luma (y) and half frame chroma (u,v), so total size is
//        // size(y) + size(y) / 2 + size(y) / 2 = size(y) + size(y) / 2 * 2 = size(y) + size(y) = 2 * size(y)
//        int npix = imgYUV420.getWidth() * imgYUV420.getHeight();
//        byte[] nv21Image = new byte[npix * 2];
//        Arrays.fill(nv21Image, (byte) 127); // 127 -> 0 chroma (luma will be overwritten in either case)
//
//        // Copy the Y-plane
//        ByteBuffer nv21Buffer = ByteBuffer.wrap(nv21Image);
//        for (int i = 0; i < imgYUV420.getHeight(); i++) {
//            nv21Buffer.put(yData, i * yPlane.getRowStride(), imgYUV420.getWidth());
//        }
//
//        // Copy the u and v planes interlaced
//        if (!grayscale) {
//            for (int row = 0; row < imgYUV420.getHeight() / 2; row++) {
//                for (int cnt = 0, upix = 0, vpix = 0; cnt < imgYUV420.getWidth() / 2; upix += uPlane.getPixelStride(), vpix += vPlane.getPixelStride(), cnt++) {
//                    nv21Buffer.put(uData[row * uPlane.getRowStride() + upix]);
//                    nv21Buffer.put(vData[row * vPlane.getRowStride() + vpix]);
//                }
//            }
//
//            fastReverse(nv21Image, npix, npix);
//        }
//
//        fastReverse(nv21Image, 0, npix);
//
//        return nv21Buffer.array();
//    }
//
//    private static byte[] getRawCopy(ByteBuffer in) {
//        ByteBuffer rawCopy = ByteBuffer.allocate(in.capacity());
//        rawCopy.put(in);
//        return rawCopy.array();
//    }
//
//    private static void fastReverse(byte[] array, int offset, int length) {
//        int end = offset + length;
//        for (int i = offset; i < offset + (length / 2); i++) {
//            array[i] = (byte)(array[i] ^ array[end - i  - 1]);
//            array[end - i  - 1] = (byte)(array[i] ^ array[end - i  - 1]);
//            array[i] = (byte)(array[i] ^ array[end - i  - 1]);
//        }
//    }

    public static byte[] YUV420888toNV21(Image image) {
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

    public static byte[] NV21toJPEG(byte[] nv21, int width, int height) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        YuvImage yuv = new YuvImage(nv21, ImageFormat.NV21, width, height, null);
        yuv.compressToJpeg(new Rect(0, 0, width, height), 100, out);
        return out.toByteArray();
    }
}
