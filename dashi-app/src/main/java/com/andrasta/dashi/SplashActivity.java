package com.andrasta.dashi;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import com.andrasta.dashi.service.LicensePlateMatcher;
import com.andrasta.dashi.utils.FileUtils;
import com.andrasta.dashi.utils.SharedPreferencesHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static com.andrasta.dashi.utils.SharedPreferencesHelper.KEY_ALPR_CONFIG_COPIED;

public class SplashActivity extends Activity {

    private static final String TAG = "SplashActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferencesHelper prefs = new SharedPreferencesHelper(getApplicationContext());
        new AlprConfigCopierTask(this, getFilesDir(), prefs).execute();

        LicensePlateMatcher.getInstance(prefs).initialize();
    }

    private static final class AlprConfigCopierTask extends AsyncTask<Void, Void, Void> {

        private static final String CONFIG_ZIP_FILE_NAME = "alpr_config.zip";
        private final AssetManager assetManager;
        private Activity activity;
        private final File configDir;
        private final SharedPreferencesHelper prefs;

        public AlprConfigCopierTask(@NonNull Activity activity, @NonNull File configDir, @NonNull SharedPreferencesHelper prefs) {
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

            if(activity != null) {
                activity.startActivity(new Intent(activity, MainActivity.class));
                activity.finish();
            }
        }

    }
}
