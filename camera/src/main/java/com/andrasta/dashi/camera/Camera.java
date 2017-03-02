package com.andrasta.dashi.camera;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.util.Log;

import com.andrasta.dashi.utils.Preconditions;

import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class Camera {
    private static final String TAG = "Camera";

    private Semaphore cameraOpenCloseLock = new Semaphore(1);
    private CameraCaptureSession captureSession;
    private final CameraListener cameraListener;
    private final CameraManager manager;
    private CameraDevice cameraDevice;
    private CameraConfig cameraConfig;

    private HandlerThread backgroundThread;
    private Handler handler;

    public Camera(@NonNull Context context, @NonNull CameraListener listener) {
        Preconditions.assertReturnNotNull(context, "context");
        Preconditions.assertReturnNotNull(listener, "listener");
        manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        cameraListener = listener;
    }

    @UiThread
    @SuppressWarnings("MissingPermission")
    public void open(CameraConfig config) {
        Preconditions.assertUiThread();
        if (cameraConfig != null) {
            Log.w(TAG, "Camera already started. Close it first.");
            return;
        }

        cameraConfig = config;
        initHandler();

        try {
            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            manager.openCamera(config.getCameraId(), cameraStateCallback, handler);
        } catch (CameraAccessException e) {
            cameraListener.onError(false, e);
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
    }

    @UiThread
    public void close() {
        Preconditions.assertUiThread();
        if (cameraConfig != null) {
            closeCamera();
            closeHandler();
            cameraConfig = null;
        } else {
            Log.d(TAG, "Camera isn't started");
        }
    }

    private void closeCamera() {
        try {
            cameraOpenCloseLock.acquire();
            if (null != captureSession) {
                captureSession.close();
                captureSession = null;
            }
            if (null != cameraDevice) {
                cameraDevice.close();
                cameraDevice = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            cameraOpenCloseLock.release();
        }
    }

    private void initHandler() {
        if (cameraConfig.getUseUiThread()) {
            handler = new Handler(Looper.getMainLooper());
        } else {
            backgroundThread = new HandlerThread("CameraHandlerThread");
            backgroundThread.start();
            handler = new Handler(backgroundThread.getLooper());
        }
    }

    private void closeHandler() {
        if (cameraConfig.getUseUiThread()) {
            handler = null;
        } else {
            backgroundThread.quitSafely();
            try {
                backgroundThread.join();
                backgroundThread = null;
                handler = null;
            } catch (InterruptedException e) {
                cameraListener.onError(false, e);
            }
        }
    }

    private final CameraDevice.StateCallback cameraStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            // This method is called when the camera is opened.  We start camera preview here.
            cameraOpenCloseLock.release();
            Camera.this.cameraDevice = cameraDevice;
            try {
                cameraDevice.createCaptureSession(cameraConfig.getSurfaces(), captureStateCallback, handler);
            } catch (CameraAccessException e) {
                cameraListener.onError(false, e);
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            cameraOpenCloseLock.release();
            cameraDevice.close();
            Camera.this.cameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            cameraOpenCloseLock.release();
            cameraDevice.close();
            Camera.this.cameraDevice = null;
            cameraListener.onError(true, null);
        }
    };

    private final CameraCaptureSession.StateCallback captureStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
            // The camera is already closed
            if (null == cameraDevice) {
                return;
            }

            // When the session is ready, we start displaying the preview.
            captureSession = cameraCaptureSession;
            try {
                List<CaptureRequest> requests = cameraConfig.getRequests(cameraDevice);
                if (requests.size() == 1) {
                    captureSession.setRepeatingRequest(requests.get(0), captureCallback, handler);
                } else {
                    captureSession.setRepeatingBurst(requests, captureCallback, handler);
                }
            } catch (CameraAccessException e) {
                cameraListener.onError(false, e);
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
            cameraListener.onError(false, null);
        }
    };

    private final CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request, CaptureFailure failure) {
            Log.w(TAG, "CaptureFailed. Reason:" + failure.getReason());
        }
    };

    public interface CameraListener extends ImageReader.OnImageAvailableListener {
        void onError(boolean critical, @Nullable Exception exception);
    }
}