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
import android.view.View;
import android.widget.Toast;

import com.andrasta.dashi.camera.Camera;
import com.andrasta.dashi.camera.ImageSaver;
import com.andrasta.dashi.utils.PermissionsHelper;
import com.andrasta.dashi.view.AutoFitTextureView;

import java.io.File;

public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback, Camera.CameraListener {
    private static final String TAG = "MainActivity";

    private Camera camera;
    private int requestId;
    private File file;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextureView = (AutoFitTextureView) findViewById(R.id.texture);
        mTextureView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (camera != null) {
                    camera.requestImage();
                }
            }
        });
        file = new File(Environment.getExternalStorageDirectory(), "pic.jpg");

        if (!PermissionsHelper.hasPermission(this, Manifest.permission.CAMERA)) {
            requestId = PermissionsHelper.requestPermission(this, Manifest.permission.CAMERA, R.string.camera_permission_rationale);
            return;
        }
        if (!PermissionsHelper.hasPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            requestId = PermissionsHelper.requestPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE, R.string.storage_permission_rationale);
            return;
        }

        camera = new Camera(mTextureView, this);
    }

    /**
     * An {@link AutoFitTextureView} for camera preview.
     */
    private AutoFitTextureView mTextureView;

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
                camera = new Camera(mTextureView, this);
                camera.open();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onImageAvailable(ImageReader reader) {
        ImageSaver imageSaver = new ImageSaver(reader.acquireNextImage(), file);
        imageSaver.save();
        Toast.makeText(MainActivity.this, "Image saved to " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
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
