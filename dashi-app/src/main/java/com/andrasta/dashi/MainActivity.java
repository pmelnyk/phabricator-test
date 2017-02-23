package com.andrasta.dashi;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat.OnRequestPermissionsResultCallback;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.andrasta.dashi.alpr.AlprHandler;
import com.andrasta.dashi.camera.Camera;
import com.andrasta.dashi.camera.Camera.CameraListener;
import com.andrasta.dashi.camera.ImageSaver;
import com.andrasta.dashi.location.LocationHelper;
import com.andrasta.dashi.openalpr.AlprResult;
import com.andrasta.dashi.openalpr.PlateResult;
import com.andrasta.dashi.utils.Callback;
import com.andrasta.dashi.utils.CyclicBuffer;
import com.andrasta.dashi.utils.PermissionsHelper;
import com.andrasta.dashi.utils.Preconditions;
import com.andrasta.dashi.utils.SharedPreferencesHelper;
import com.andrasta.dashi.view.AutoFitTextureView;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.andrasta.dashi.utils.SharedPreferencesHelper.KEY_CAMERA_ROTATION;

public class MainActivity extends AppCompatActivity implements OnRequestPermissionsResultCallback, CameraListener {
    private static final String TAG = "MainActivity";

    private final File imageDestination = new File(Environment.getExternalStorageDirectory(), "pic.jpg");
    private final AtomicBoolean requestImage = new AtomicBoolean();
    private final File configDir = new File("/data/local/tmp/");
    private LocationHelper locationHelper = new LocationHelper();
    private CyclicBuffer<String> resultsBuffer = new CyclicBuffer<>(10);
    private ImageSaver imageSaver = new ImageSaver();
    private SharedPreferencesHelper prefs;
    private AutoFitTextureView textureView;
    private TextView recognitionResult;
    private AlprHandler alprHandler;
    private Camera camera;
    private int requestId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setupOrientation();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recognitionResult = (TextView) findViewById(R.id.recognition_result);
        recognitionResult.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resultsBuffer.reset();
                recognitionResult.setText("");
            }
        });
        textureView = (AutoFitTextureView) findViewById(R.id.texture);
        textureView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestImage.set(true);
            }
        });

        alprHandler = new AlprHandler(configDir, alprCallback);
        if (askForPermissions()) {
            afterPermissionsGranted();
        }
    }

    private void setupOrientation() {
        prefs = new SharedPreferencesHelper(this);
        int orientation = prefs.getInt(KEY_CAMERA_ROTATION, 90);
        if (orientation == 270) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
        } else if (orientation == 90) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else {
            throw new RuntimeException("Camera orientation not supported: " + orientation);
        }
    }

    private boolean askForPermissions() {
        if (!PermissionsHelper.hasPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            requestId = PermissionsHelper.requestPermission(this, Manifest.permission.ACCESS_FINE_LOCATION, R.string.location_permission_rationale);
            return false;
        }
        if (!PermissionsHelper.hasPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
            requestId = PermissionsHelper.requestPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION, R.string.location_permission_rationale);
            return false;
        }
        if (!PermissionsHelper.hasPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            requestId = PermissionsHelper.requestPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE, R.string.storage_permission_rationale);
            return false;
        }
        if (!PermissionsHelper.hasPermission(this, Manifest.permission.CAMERA)) {
            requestId = PermissionsHelper.requestPermission(this, Manifest.permission.CAMERA, R.string.camera_permission_rationale);
            return false;
        }
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        alprHandler.start();
        if (camera != null) {
            camera.open();
        }
    }

    @Override
    public void onPause() {
        alprHandler.stop();
        if (camera != null) {
            camera.close();
        }
        super.onPause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == requestId) {
            if (!askForPermissions()) {
                return;
            }

            if (grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                ExitDialog.newInstance(getString(R.string.permission_discard)).show(getFragmentManager(), "Dialog");
            } else {
                afterPermissionsGranted();
                camera.open();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void afterPermissionsGranted() {
        camera = new Camera(textureView, this);
        try {
            locationHelper.start(this);
        } catch (IOException ioe) {
            Log.d(TAG, "Cannot start location", ioe);
        }
    }

    @Override
    public void onImageAvailable(@NonNull ImageReader reader) {
        Preconditions.assertParameterNotNull(reader, "reader");
        if (requestImage.getAndSet(false)) {
            imageSaver.saveToFile(reader.acquireNextImage(), imageDestination);
            Toast.makeText(this, "Image saved to " + imageDestination.getAbsolutePath(), Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            alprHandler.recognize(reader.acquireNextImage());
        } catch (IllegalStateException e) {
            //expected
        }
    }

    @Override
    public void onCameraOrientationSet(int orientation) {
        if (prefs.getInt(KEY_CAMERA_ROTATION, 90) != orientation) {
            prefs.setInt(KEY_CAMERA_ROTATION, orientation);
            finish();
            startActivity(getIntent());
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

    private final Callback<AlprResult, Exception> alprCallback = new Callback<AlprResult, Exception>() {
        @Override
        public void onComplete(@NonNull AlprResult alprResult) {
            Log.d(TAG, "AlprResult: " + alprResult);
            List<PlateResult> results = alprResult.getPlates();
            if (results.size() > 0) {
                PlateResult plate = results.get(0);
                if (plate != null && plate.getBestPlate() != null) {
                    String result = plate.getBestPlate().getPlate();
                    Log.d(TAG, "Best result: " + result);
                    showResult(result);
                }
            }
        }

        private void showResult(String licensePlate) {
            resultsBuffer.add(licensePlate);
            final StringBuilder sb = new StringBuilder();
            for (String pl : resultsBuffer.asList()) {
                if (pl != null) {
                    sb.append(pl).append("\n");
                }
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    recognitionResult.setText(sb.toString());
                }
            });
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
}
