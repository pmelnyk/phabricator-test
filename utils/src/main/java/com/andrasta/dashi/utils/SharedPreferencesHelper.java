package com.andrasta.dashi.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;

public class SharedPreferencesHelper {
    public static final String KEY_CAMERA_ROTATION = "key_camera_rotation";

    private final String PREF_NAME = "SharedPreference";
    private final Context context;

    public SharedPreferencesHelper(@NonNull Context context) {
        Preconditions.assertParameterNotNull(context, "context");
        this.context = context.getApplicationContext();
    }

    /**
     * Set a string shared preference
     *
     * @param key   - Key to set shared preference
     * @param value - Value for the key
     */
    public void setString(@NonNull String key, @NonNull String value) {
        SharedPreferences settings = context.getSharedPreferences(PREF_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(key, value);
        editor.apply();
    }

    /**
     * Set a integer shared preference
     *
     * @param key   - Key to set shared preference
     * @param value - Value for the key
     */
    public void setInt(@NonNull String key, int value) {
        SharedPreferences settings = context.getSharedPreferences(PREF_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    /**
     * Set a Boolean shared preference
     *
     * @param key   - Key to set shared preference
     * @param value - Value for the key
     */
    public void setBoolean(@NonNull String key, boolean value) {
        SharedPreferences settings = context.getSharedPreferences(PREF_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    /**
     * Get a string shared preference
     *
     * @param key      - Key to look up in shared preferences.
     * @param defValue - Default value to be returned if shared preference isn't found.
     * @return value - String containing value of the shared preference if found.
     */
    public @NonNull String getString(@NonNull String key, @NonNull String defValue) {
        SharedPreferences settings = context.getSharedPreferences(PREF_NAME, 0);
        return settings.getString(key, defValue);
    }

    /**
     * Get a integer shared preference
     *
     * @param key      - Key to look up in shared preferences.
     * @param defValue - Default value to be returned if shared preference isn't found.
     * @return value - String containing value of the shared preference if found.
     */
    public int getInt(@NonNull String key, int defValue) {
        SharedPreferences settings = context.getSharedPreferences(PREF_NAME, 0);
        return settings.getInt(key, defValue);
    }

    /**
     * Get a boolean shared preference
     *
     * @param key      - Key to look up in shared preferences.
     * @param defValue - Default value to be returned if shared preference isn't found.
     * @return value - String containing value of the shared preference if found.
     */
    public boolean getBoolean(@NonNull String key, boolean defValue) {
        SharedPreferences settings = context.getSharedPreferences(PREF_NAME, 0);
        return settings.getBoolean(key, defValue);
    }
}