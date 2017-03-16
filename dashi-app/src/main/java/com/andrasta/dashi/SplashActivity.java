package com.andrasta.dashi;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat.OnRequestPermissionsResultCallback;
import android.util.Log;

import com.andrasta.dashi.service.LicensePlateMatcher;
import com.andrasta.dashi.tensorflow.DetectorActivity;
import com.andrasta.dashi.utils.FileUtils;
import com.andrasta.dashi.utils.PermissionsHelper;
import com.andrasta.dashi.utils.SharedPreferencesHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static com.andrasta.dashi.utils.SharedPreferencesHelper.KEY_ALPR_CONFIG_COPIED;
import static com.andrasta.dashi.utils.SharedPreferencesHelper.KEY_ALPR_CONFIG_DIR;
import static com.andrasta.dashi.utils.SharedPreferencesHelper.KEY_APP_INITIALIZED;

public class SplashActivity extends Activity implements OnRequestPermissionsResultCallback {
    private static final String TAG = "SplashActivity";

    private SharedPreferencesHelper prefs;
    private int requestId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        prefs = new SharedPreferencesHelper(getApplicationContext());
        if (checkPermissions()) {
            onAllPermissionsGranted();
        } else {
            Log.d(TAG, "Request permissions");
        }
    }

    private void onAllPermissionsGranted() {
        Log.d(TAG, "All permissions granted");

        if (prefs.getBoolean(KEY_APP_INITIALIZED, false)) {
            Log.d(TAG, "App initialized already");
            startMainActivity();
            return;
        }

        Log.d(TAG, "App isn't initialized. Start initialization.");
        LicensePlateMatcher.getInstance(prefs).initialize();

        File configDir = getFilesDir();
        prefs.setString(KEY_ALPR_CONFIG_DIR, configDir.getAbsolutePath());
        new AlprConfigCopierTask(this, configDir, prefs).execute();
    }

    private void startMainActivity() {
        Log.d(TAG, "Start main activity");
        prefs.setBoolean(KEY_APP_INITIALIZED, true);
        startActivity(new Intent(this, DetectorActivity.class));
//        finish();
    }

    private boolean checkPermissions() {
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == requestId) {
            if (!checkPermissions()) {
                return;
            }

            if (grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                MainActivity.ExitDialog.newInstance(getString(R.string.permission_discard)).show(getFragmentManager(), "Dialog");
            } else {
                onAllPermissionsGranted();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private static final class AlprConfigCopierTask extends AsyncTask<Void, Void, Void> {
        private static final String CONFIG_ZIP_FILE_NAME = "alpr_config.zip";
        private final AssetManager assetManager;
        private final File configDir;
        private final SharedPreferencesHelper prefs;
        private SplashActivity activity;

        AlprConfigCopierTask(@NonNull SplashActivity activity, @NonNull File configDir, @NonNull SharedPreferencesHelper prefs) {
            this.assetManager = activity.getAssets();
            this.activity = activity;
            this.configDir = configDir;
            this.prefs = prefs;
        }

        @Override
        protected Void doInBackground(Void... params) {

            if (!prefs.getBoolean(KEY_ALPR_CONFIG_COPIED, false)) {

                try {
                    InputStream open = assetManager.open(CONFIG_ZIP_FILE_NAME);
                    File file = new File(configDir, CONFIG_ZIP_FILE_NAME);
                    FileOutputStream fileOutputStream = new FileOutputStream(file);

                    FileUtils.copyFile(open, fileOutputStream);

                    FileUtils.unzip(file.getAbsolutePath(), file.getParentFile().getAbsolutePath());

                    //noinspection ResultOfMethodCallIgnored
                    file.delete();

                    prefs.setBoolean(KEY_ALPR_CONFIG_COPIED, true);

                } catch (IOException e) {
                    Log.e(TAG, "Error copying alpr config", e);
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (activity != null) {
                Log.d(TAG, "Alpr config copied");
                activity.startMainActivity();
            }
        }
    }
}
