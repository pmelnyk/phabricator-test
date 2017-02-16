package com.andrasta.dashi;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v13.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.andrasta.dashi.camera.Camera;
import com.andrasta.dashi.camera.ImageSaver;
import com.andrasta.dashi.utils.Callback;
import com.andrasta.dashi.utils.PermissionsHelper;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback, Camera.CameraListener {
    private static final String TAG = "MainActivity";

    private AtomicBoolean requestImage = new AtomicBoolean();
    private AlprHandler alprHandler;
    private Camera camera;
    private int requestId;
    private File file;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        file = new File(Environment.getExternalStorageDirectory(), "pic.jpg");
        alprHandler = new AlprHandler(alprCallback);

        if (!PermissionsHelper.hasPermission(this, Manifest.permission.CAMERA)) {
            requestId = PermissionsHelper.requestPermission(this, Manifest.permission.CAMERA, R.string.camera_permission_rationale);
            return;
        }
        if (!PermissionsHelper.hasPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            requestId = PermissionsHelper.requestPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE, R.string.storage_permission_rationale);
            return;
        }

        camera = new Camera(this, this);
    }


    @Override
    public void onResume() {
        super.onResume();
        if (camera != null) {
            camera.open();
        }
    }

    @Override
    public void onPause() {
        if (camera != null) {
            camera.close();
        }
        super.onPause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == requestId) {
            if (grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                ExitDialog.newInstance(getString(R.string.permission_discard))
                        .show(getFragmentManager(), "Dialog");
            } else {
                camera = new Camera(this, this);
                camera.open();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onImageAvailable(ImageReader reader) {
        if (requestImage.getAndSet(false)) {
            ImageSaver imageSaver = new ImageSaver(reader.acquireNextImage(), file);
            imageSaver.save();
            Toast.makeText(MainActivity.this, "Image saved to " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
        }

        try {
            alprHandler.request(reader.acquireNextImage());
        } catch (IllegalStateException e) {
            //expected
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

    private final Callback<AlprHandler.AlprResponse, Exception> alprCallback = new Callback<AlprHandler.AlprResponse, Exception>() {
        @Override
        public void onComplete(@NonNull AlprHandler.AlprResponse alprResponse) {
            Log.d(TAG, "License recognized: " + alprResponse.licensePlate);
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
