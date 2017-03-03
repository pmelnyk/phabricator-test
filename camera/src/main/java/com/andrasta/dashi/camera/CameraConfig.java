package com.andrasta.dashi.camera;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.support.annotation.NonNull;
import android.util.Size;
import android.view.Surface;

import com.andrasta.dashi.utils.Preconditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class CameraConfig {
    private final String cameraId;
    private final boolean useUiThread;
    private final ArrayList<Surface> surfaces;
    private final ArrayList<Request> requests;

    private CameraConfig(String cameraId, boolean useUiThread, ArrayList<Surface> surfaces, ArrayList<Request> requests) {
        this.cameraId = cameraId;
        this.surfaces = surfaces;
        this.requests = requests;
        this.useUiThread = useUiThread;
    }

    @NonNull
    String getCameraId() {
        return cameraId;
    }

    boolean getUseUiThread() {
        return useUiThread;
    }

    @NonNull
    List<Surface> getSurfaces() {
        return Collections.unmodifiableList(surfaces);
    }

    @NonNull
    List<CaptureRequest> getRequests(CameraDevice cameraDevice) throws CameraAccessException {
        if (requests.size() == 1) {
            return Collections.singletonList(requests.get(0).getCaptureRequest(cameraDevice));
        } else if (requests.size() > 1) {
            ArrayList<CaptureRequest> res = new ArrayList<>(requests.size());
            for (Request r : requests) {
                res.add(r.getCaptureRequest(cameraDevice));
            }
            return res;
        } else {
            throw new RuntimeException("No requests");
        }
    }

    public static class Builder {
        private Size size;
        private int imageFormat;
        private final String cameraId;
        private int cameraOrientation;
        private boolean useUiThread;
        private final ArrayList<Surface> surfaces = new ArrayList<>();
        private final ArrayList<Request> requests = new ArrayList<>();

        public Builder(@NonNull String cameraId) {
            Preconditions.assertStringNotEmpty(cameraId, "cameraId");
            this.cameraId = cameraId;
        }

        public String getCameraId() {
            return cameraId;
        }

        public int getCameraOrientation() {
            return cameraOrientation;
        }

        public Builder setCameraOrientation(int cameraOrientation) {
            this.cameraOrientation = cameraOrientation;
            return this;
        }

        public boolean getUseUiThread() {
            return useUiThread;
        }

        public Builder setUseUiThread(boolean useUiThread) {
            this.useUiThread = useUiThread;
            return this;
        }

        public Size getSize() {
            return size;
        }

        public Builder setSize(Size size) {
            this.size = size;
            return this;
        }

        public Builder setResolution(@NonNull Size size) {
            Preconditions.assertParameterNotNull(size, "size");
            this.size = size;
            return this;
        }

        public Builder setImageFormat(int imageFormat) {
            this.imageFormat = imageFormat;
            return this;
        }

        public Builder addRequest(@NonNull Request request) {
            Preconditions.assertParameterNotNull(request, "request");
            requests.add(request);
            surfaces.add(request.surface);
            return this;
        }

        public CameraConfig build() {
            return new CameraConfig(cameraId, useUiThread, surfaces, requests);
        }
    }

    public static class Request {
        private final Surface surface;
        private final int requestTemplate;
        private final HashMap<CaptureRequest.Key, Object> requestKeys = new HashMap<>();

        public Request(int requestTemplate, @NonNull Surface surface) {
            Preconditions.assertReturnNotNull(surface, "surface");
            this.requestTemplate = requestTemplate;
            this.surface = surface;
        }

        public <T> Request set(@NonNull CaptureRequest.Key<T> key, @NonNull T value) {
            Preconditions.assertReturnNotNull(key, "key");
            Preconditions.assertReturnNotNull(value, "value");
            requestKeys.put(key, value);
            return this;
        }

        CaptureRequest getCaptureRequest(@NonNull CameraDevice cameraDevice) throws CameraAccessException {
            Preconditions.assertReturnNotNull(cameraDevice, "cameraDevice");
            CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(requestTemplate);
            builder.addTarget(surface);
            for (CaptureRequest.Key rk : requestKeys.keySet()) {
                builder.set(rk, requestKeys.get(rk));
            }
            return builder.build();
        }
    }
}