package com.andrasta.dashi;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.location.Location;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.andrasta.dashi.alpr.AlprHandler;
import com.andrasta.dashi.camera.Camera;
import com.andrasta.dashi.camera.Camera.CameraListener;
import com.andrasta.dashi.camera.CameraConfig;
import com.andrasta.dashi.camera.CameraUtils;
import com.andrasta.dashi.camera.ImageSaver;
import com.andrasta.dashi.location.LocationHelper;
import com.andrasta.dashi.openalpr.AlprResult;
import com.andrasta.dashi.openalpr.Plate;
import com.andrasta.dashi.openalpr.PlateResult;
import com.andrasta.dashi.service.LicensePlateMatcher;
import com.andrasta.dashi.utils.CyclicBuffer;
import com.andrasta.dashi.utils.Preconditions;
import com.andrasta.dashi.utils.SharedPreferencesHelper;
import com.andrasta.dashi.view.AutoFitTextureView;
import com.andrasta.dashi.view.PolygonView;
import com.andrasta.dashiclient.LicensePlate;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.andrasta.dashi.utils.SharedPreferencesHelper.KEY_CAMERA_ROTATION;

public class MainActivity extends AppCompatActivity implements CameraListener {
    private static final String TAG = "MainActivity";
    private static final String CONFIG_ZIP_FILE_NAME = "alpr_config.zip";
    private static final int RECOGNITION_HISTORY_SIZE = 10;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault());
    private final File imageDestination = new File(Environment.getExternalStorageDirectory(), "pic.jpg");
    private CyclicBuffer<String> resultsBuffer = new CyclicBuffer<>(RECOGNITION_HISTORY_SIZE);
    private final AtomicBoolean saveImageOnDisk = new AtomicBoolean();
    private LocationHelper locationHelper = new LocationHelper();
    private LicensePlateMatcher licensePlateMatcher;
    private SharedPreferencesHelper prefs;
    private AlprHandler alprHandler;

    private ImageSaver imageSaver = new ImageSaver();
    private CameraConfig.Builder configBuilder;
    private Display display;
    private Camera camera;

    private AutoFitTextureView textureView;
    private TextView recognitionResult;
    private PolygonView polygonView;
    private ImageReader imageReader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs = new SharedPreferencesHelper(this);
        setupOrientation(prefs);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        display = getWindowManager().getDefaultDisplay();

        ((DrawerLayout) findViewById(R.id.drawer)).openDrawer(Gravity.LEFT);
        polygonView = (PolygonView) findViewById(R.id.plate_polygon);
        recognitionResult = (TextView) findViewById(R.id.recognition_result);
        recognitionResult.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resultsBuffer.reset();
                recognitionResult.setText("");
            }
        });
        textureView = (AutoFitTextureView) findViewById(R.id.texture);
        textureView.setSurfaceTextureListener(textureListener);
        textureView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveImageOnDisk.set(true);
            }
        });

        licensePlateMatcher = LicensePlateMatcher.getInstance(prefs);

        alprHandler = new AlprHandler(getFilesDir(), alprCallback, licensePlateMatcher, new Handler());
        camera = new Camera(this, this);

        try {
            locationHelper.start(this);
        } catch (IOException ioe) {
            Log.d(TAG, "Cannot start location", ioe);
        }
    }

    private void setupOrientation(@NonNull SharedPreferencesHelper prefs) {
        int orientation = this.prefs.getInt(KEY_CAMERA_ROTATION, 90);
        if (orientation == 270) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
        } else if (orientation == 90) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else {
            throw new RuntimeException("Camera orientation not supported: " + orientation);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        alprHandler.start();
        if (textureView.isAvailable()) {
            openCamera(textureView.getWidth(), textureView.getHeight());
        }
    }

    @Override
    public void onStop() {
        alprHandler.stop();
        super.onStop();
    }

    @SuppressWarnings("MissingPermission")
    private void openCamera(int width, int height) {
        try {
            configBuilder = CameraUtils.initCameraConfig(this, display, width, height);
            onCameraOrientationSet(configBuilder.getCameraOrientation());

            int cameraWidth = configBuilder.getSize().getWidth();
            int cameraHeight = configBuilder.getSize().getHeight();

            // We fit the aspect ratio of TextureView to the size of preview we picked.
            int orientation = this.getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                textureView.setAspectRatio(cameraWidth, cameraHeight);
            } else {
                textureView.setAspectRatio(cameraHeight, cameraWidth);
            }
            Matrix matrix = CameraUtils.configureTransform(display.getRotation(), width, height, cameraWidth, cameraHeight);
            textureView.setTransform(matrix);
            SurfaceTexture texture = textureView.getSurfaceTexture();
            // We configure the size of default buffer to be the size of camera preview we want.
            texture.setDefaultBufferSize(cameraWidth, cameraHeight);

            CameraConfig.Request request = new CameraConfig.Request(CameraDevice.TEMPLATE_PREVIEW, new Surface(texture));
            request.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            configBuilder.addRequest(request);

            imageReader = ImageReader.newInstance(cameraWidth, cameraHeight, ImageFormat.YUV_420_888, /*maxImages*/ 1);
            imageReader.setOnImageAvailableListener(this, null);
            request = new CameraConfig.Request(CameraDevice.TEMPLATE_PREVIEW, imageReader.getSurface());
            configBuilder.addRequest(request);

            Log.d(TAG, "Resolution selected " + cameraWidth + 'x' + cameraHeight);
            camera.open(configBuilder.build());
            Log.d(TAG, "Camera opened: " + configBuilder.getCameraId());
        } catch (CameraAccessException e) {
            onError(false, e);
        } catch (NullPointerException e) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the device
            onError(true, e);
        }
    }

    public void onCameraOrientationSet(int orientation) {
        if (prefs.getInt(KEY_CAMERA_ROTATION, 90) != orientation) {
            prefs.setInt(KEY_CAMERA_ROTATION, orientation);
            finish();
            startActivity(getIntent());
        }
    }

    @Override
    public void onImageAvailable(@NonNull ImageReader reader) {
        Preconditions.assertParameterNotNull(reader, "reader");
        if (saveImageOnDisk.getAndSet(false)) {
            imageSaver.saveToFile(reader.acquireNextImage(), imageDestination);
            Toast.makeText(this, "Image saved to " + imageDestination.getAbsolutePath(), Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            alprHandler.recognize(reader.acquireNextImage());
        } catch (IllegalStateException e) {
            Log.w(TAG, e);
        }
    }

    @Override
    public void onError(boolean critical, @Nullable Exception exception) {
        Log.e(TAG, "Camera error", exception);
        if (critical) {
            finish();
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, "Fail", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private final AlprHandler.AlprCallback alprCallback = new AlprHandler.AlprCallback() {
        private final Date date = new Date();

        @Override
        public void onComplete(byte[] imageAsJpeg, AlprResult alprResult) {
            Log.d(TAG, "AlprResult: " + alprResult);

            List<Pair<Plate, LicensePlate>> matches = licensePlateMatcher.findMatches(alprResult);
            Log.d(TAG, "Matches found : " + matches.size());

            PlateResult bestResult = getFirstBestPlate(alprResult);
            showResult(alprResult.getSourceWidth(), alprResult.getSourceHeight(), bestResult);

            Location lastKnownLocation = locationHelper.getLastKnownLocation();

            for (Pair<Plate, LicensePlate> match : matches) {
                licensePlateMatcher.sendMatch(match, imageAsJpeg, lastKnownLocation);
            }
        }

        private PlateResult getFirstBestPlate(AlprResult alprResult) {
            List<PlateResult> results = alprResult.getPlates();
            if (results.size() > 0) {
                PlateResult plate = results.get(0);
                if (plate != null && plate.getBestPlate() != null) {
                    return plate;
                }
            }
            return null;
        }

        private void showResult(final int sourceWidth, final int sourceHeight, final PlateResult plate) {
            if (plate != null) {
                Log.d(TAG, "Best result: " + plate.getBestPlate().getPlate());
                resultsBuffer.add(getResultLine(plate.getBestPlate()));
                polygonView.setAspectRatio(sourceWidth, sourceHeight);
                polygonView.setPolygon(sourceWidth, sourceHeight, plate.getPlateCoordinates());
            } else {
                resultsBuffer.add("");
                polygonView.setAspectRatio(sourceWidth, sourceHeight);
                polygonView.clear();
            }

            final StringBuilder sb = new StringBuilder();
            for (String pl : resultsBuffer.asList()) {
                if (pl != null) {
                    sb.append(pl).append("<br>");
                }
            }
            recognitionResult.setText(Html.fromHtml(sb.toString()));
        }

        private String getResultLine(Plate plate) {
            final String template = "<font color='#FFFFFF'>%s conf: %s%%&nbsp;&nbsp;&nbsp;&nbsp;</font><big><font color='#BBBBFF'>%s</font></big>";
            date.setTime(System.currentTimeMillis());
            return String.format(template, dateFormat.format(date), Math.round(plate.getConfidence()), plate.getPlate());
        }

        @Override
        public void onFailure(@NonNull Exception e) {
            throw new RuntimeException(e);
        }
    };

    public static class ExitDialog extends DialogFragment {
        private static final String ARG_MESSAGE = "message";

        public static ExitDialog newInstance(String message) {
            ExitDialog dialog = new ExitDialog();
            Bundle args = new Bundle();
            args.putString(ARG_MESSAGE, message);
            dialog.setArguments(args);
            return dialog;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Activity activity = getActivity();
            return new AlertDialog.Builder(activity)
                    .setMessage(getArguments().getString(ARG_MESSAGE))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            activity.finish();
                        }
                    })
                    .create();
        }
    }

    @SuppressWarnings("FieldCanBeLocal")
    private final TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            openCamera(textureView.getWidth(), textureView.getHeight());
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
            Matrix matrix = CameraUtils.configureTransform(display.getRotation(), width, height,
                    configBuilder.getSize().getWidth(), configBuilder.getSize().getHeight());
            textureView.setTransform(matrix);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            Log.d(TAG, "SurfaceTexture Destroyed. Stop camera.");
            texture.release();
            imageReader.close();
            camera.close();
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {
        }
    };
}
