package com.andrasta.dashi.camera;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.Pair;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Display;
import android.view.Surface;

import com.andrasta.dashi.utils.Preconditions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CameraUtils {
    private static final String TAG = "CameraUtils";

    /**
     * Max preview width that is guaranteed by Camera API
     */
    private static final int MAX_PREVIEW_WIDTH = 1920;
    /**
     * Max preview height that is guaranteed by Camera API
     */
    private static final int MAX_PREVIEW_HEIGHT = 1080;

    /**
     * Conversion from screen rotation to JPEG orientation.
     */
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private static final Set<Size> BROKEN_RESOLUTIONS = new HashSet<>();

    static {
        BROKEN_RESOLUTIONS.add(new Size(1440, 1080));
        BROKEN_RESOLUTIONS.add(new Size(4048, 3036));
        BROKEN_RESOLUTIONS.add(new Size(4000, 3000));
    }

    /**
     * Retrieves the JPEG orientation from the specified screen rotation.
     *
     * @param rotation The screen rotation.
     * @return The JPEG orientation (one of 0, 90, 270, and 360)
     */
    static int getOrientation(int rotation, int orientation) {
        // Sensor orientation is 90 for most devices, or 270 for some devices (eg. Nexus 5X)
        // We have to take that into account and rotate JPEG properly.
        // For devices with orientation of 90, we simply return our mapping from ORIENTATIONS.
        // For devices with orientation of 270, we need to rotate the JPEG 180 degrees.
        return (ORIENTATIONS.get(rotation) + orientation + 270) % 360;
    }

    /**
     * Given {@code choices} of {@code Size}s supported by a camera, choose the smallest one that
     * is at least as large as the respective texture view size, and that is at most as large as the
     * respective max size, and whose aspect ratio matches with the specified value. If such size
     * doesn't exist, choose the largest one that is at most as large as the respective max size,
     * and whose aspect ratio matches with the specified value.
     *
     * @param choices           The list of sizes that the camera supports for the intended output
     *                          class
     * @param textureViewWidth  The width of the texture view relative to sensor coordinate
     * @param textureViewHeight The height of the texture view relative to sensor coordinate
     * @param maxWidth          The maximum width that can be chosen
     * @param maxHeight         The maximum height that can be chosen
     * @param aspectRatio       The aspect ratio
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     */
    private static Size chooseOptimalSize(Size[] choices, int textureViewWidth,
                                          int textureViewHeight, int maxWidth, int maxHeight, Size aspectRatio) {

        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (skipThisChoice(option)) {
                continue;
            }
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
                    option.getHeight() == option.getWidth() * h / w) {
                if (option.getWidth() >= textureViewWidth &&
                        option.getHeight() >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, SIZE_COMPARATOR);
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, SIZE_COMPARATOR);
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    // Workaround for Google Nexus 5x and Pixel devices
    // When format is YUV_420_888 and image resolution is inside brokenResolutions
    // array camera responds with trash instead of real image
    // TODO: Check if those size works in other Android OS versions
    private static boolean skipThisChoice(Size size) {
        return BROKEN_RESOLUTIONS.contains(size);
    }

    private static boolean isSwappedDimensions(int rotation, int orientation) {
        switch (rotation) {
            case Surface.ROTATION_0:
            case Surface.ROTATION_180:
                if (orientation == 90 || orientation == 270) {
                    return true;
                }
                break;
            case Surface.ROTATION_90:
            case Surface.ROTATION_270:
                if (orientation == 0 || orientation == 180) {
                    return true;
                }
                break;
            default:
                Log.e(TAG, "Display rotation is invalid: " + rotation);
        }
        return false;
    }

    /**
     * Configures the necessary {@link Matrix} transformation to `textureView`.
     * This method should be called after the camera preview size is determined in
     * setUpCameraOutputs and also the size of `textureView` is fixed.
     *
     * @param viewWidth  The width of `textureView`
     * @param viewHeight The height of `textureView`
     */
    public static Matrix configureTransform(int rotation, int viewWidth, int viewHeight, int cameraWidth, int cameraHeight) {
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, cameraHeight, cameraWidth);
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / cameraHeight,
                    (float) viewWidth / cameraWidth);
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        return matrix;
    }

    @Nullable
    @SuppressWarnings("SuspiciousNameCombination")
    public static CameraConfig.Builder initCameraConfig(@NonNull Context context, @NonNull Display display, int width, int height) throws CameraAccessException {
        Preconditions.assertParameterNotNull(context, "context");
        Preconditions.assertParameterNotNull(display, "display");
        CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        for (String cameraId : manager.getCameraIdList()) {
            CameraCharacteristics cc = manager.getCameraCharacteristics(cameraId);

            // Skip front facing camera
            Integer facing = cc.get(CameraCharacteristics.LENS_FACING);
            if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                continue;
            }

            StreamConfigurationMap map = cc.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if (map == null) {
                continue;
            }

            // For still image captures, we use the largest available size.
            Size largest = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.YUV_420_888)),
                    CameraUtils.SIZE_COMPARATOR);

            // Find out if we need to swap dimension to get the preview size relative to sensor
            // coordinate.
            int displayRotation = display.getRotation();
            //noinspection ConstantConditions
            int cameraOrientation = cc.get(CameraCharacteristics.SENSOR_ORIENTATION);
            boolean swappedDimensions = CameraUtils.isSwappedDimensions(displayRotation, cameraOrientation);

            Point displaySize = new Point();
            display.getSize(displaySize);
            int rotatedPreviewWidth = width;
            int rotatedPreviewHeight = height;
            int maxPreviewWidth = displaySize.x;
            int maxPreviewHeight = displaySize.y;

            if (swappedDimensions) {
                rotatedPreviewWidth = height;
                rotatedPreviewHeight = width;
                maxPreviewWidth = displaySize.y;
                maxPreviewHeight = displaySize.x;
            }

            if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
                maxPreviewWidth = MAX_PREVIEW_WIDTH;
            }

            if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
                maxPreviewHeight = MAX_PREVIEW_HEIGHT;
            }

            // Danger, W.R.! Attempting to use too large a preview size could  exceed the camera
            // bus' bandwidth limitation, resulting in gorgeous previews but the storage of
            // garbage capture data.
            Size size = CameraUtils.chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth, maxPreviewHeight, largest);
            if (size != null) {
                CameraConfig.Builder builder = new CameraConfig.Builder(cameraId);
                builder.setCameraOrientation(cameraOrientation);
                builder.setSize(size);
                return builder;
            }
        }
        return null;
    }

    /**
     * Compares two {@link Size} objects based on their areas.
     */
    private static Comparator<Size> SIZE_COMPARATOR = new Comparator<Size>() {
        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }
    };

    @Nullable
    public static Pair<String, List<Size>> getMainCameraImageSizes(@NonNull Context context, int format) {
        Preconditions.assertParameterNotNull(context, "context");
        CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

                // Skip front facing camera
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }

                StreamConfigurationMap map = characteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map == null) {
                    continue;
                }

                Size[] options = map.getOutputSizes(format);
                List<Size> sizes = new ArrayList<>(options.length);
                Collections.addAll(sizes, options);
                sizes.removeAll(BROKEN_RESOLUTIONS);

                // For still image captures, we use the largest available size.
                return new Pair<>(cameraId, sizes);
            }
        } catch (CameraAccessException e) {
            throw new RuntimeException(e);
        }
        return null;
    }
}