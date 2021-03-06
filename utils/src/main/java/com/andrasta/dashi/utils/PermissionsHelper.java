package com.andrasta.dashi.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v13.app.FragmentCompat;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

/**
 * Simplifies work with android runtime permissions.
 */
public class PermissionsHelper {
    private static final String TAG = "PermissionsHelper";

    private static final Handler handler = new Handler(Looper.getMainLooper());
    private static int requestId = 1000;

    public static boolean hasPermission(@NonNull Context context, @NonNull String permission) {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean hasPermission(@NonNull Context context, @NonNull String... permissions) {
        for (String permission : permissions) {
            if (!hasPermission(context, permission)) {
                return false;
            }
        }
        return true;
    }

    public static int requestPermission(@NonNull Fragment fragment, @NonNull String permission, @StringRes int rationaleText) {
        return requestPermission(fragment.getActivity(), permission, rationaleText);
    }

    public static int requestPermission(@NonNull Fragment fragment, @NonNull String permission, String rationaleText) {
        if (fragment instanceof FragmentCompat.OnRequestPermissionsResultCallback) {
            requestId++;
            if (fragment.shouldShowRequestPermissionRationale(permission)) {
                showDialog(fragment, permission, rationaleText, requestId);
            } else {
                fragment.requestPermissions(new String[]{permission}, requestId);
            }
            return requestId;
        } else {
            throw new IllegalArgumentException("Fragment must implement FragmentCompat.OnRequestPermissionsResultCallback");
        }
    }

    public static int requestPermission(@NonNull Activity activity, @NonNull String permission, @StringRes int rationaleText) {
        return requestPermission(activity, permission, activity.getString(rationaleText));
    }

    public static int requestPermission(final @NonNull Activity activity, final @NonNull String permission, String rationaleText) {
        if (activity instanceof ActivityCompat.OnRequestPermissionsResultCallback) {
            requestId++;
            if (activity.shouldShowRequestPermissionRationale(permission)) {
                showDialog(activity, permission, rationaleText, requestId);
            } else {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        activity.requestPermissions(new String[]{permission}, requestId);
                    }
                });
            }
            return requestId;
        } else {
            throw new IllegalArgumentException("Activity must implement ActivityCompat.OnRequestPermissionsResultCallback");
        }
    }

    private static void showDialog(final @NonNull Activity activity, final @NonNull String permission, final @NonNull String text, final int requestId) {
        new AlertDialog.Builder(activity)
                .setMessage(text)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        activity.requestPermissions(new String[]{permission}, requestId);
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        activity.onRequestPermissionsResult(requestId, new String[]{permission}, new int[]{PackageManager.PERMISSION_DENIED});
                    }
                })
                .create()
                .show();
    }

    private static void showDialog(final @NonNull Fragment fragment, final @NonNull String permission, final @NonNull String text, final int requestId) {
        new AlertDialog.Builder(fragment.getActivity())
                .setMessage(text)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        fragment.requestPermissions(new String[]{permission}, requestId);
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        fragment.onRequestPermissionsResult(requestId, new String[]{permission}, new int[]{PackageManager.PERMISSION_DENIED});
                    }
                })
                .create()
                .show();
    }
}
