package com.andrasta.dashi;

import android.app.Application;
import android.util.Log;

/**
 * Created by breh on 2/14/17.
 */

public class DashiApplication extends Application {

    // load the JNI libraries

    private static final String TAG = "DashiApplication";

    private static final String[] NATIVE_LIBS = new String[] {
            "openalpr_jni"
    };

    private static void loadNativeLibaries() {
        Log.d(TAG,"Loading native libraries ...");
        for (String lib : NATIVE_LIBS) {
            Log.d(TAG," ... loading: "+lib);
            System.loadLibrary(lib);
        }
        Log.d(TAG,"Libraries loaded!");
    }

    static {
        loadNativeLibaries();
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }
}
