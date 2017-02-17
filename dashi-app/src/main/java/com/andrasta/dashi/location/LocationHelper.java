package com.andrasta.dashi.location;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.andrasta.dashi.utils.Preconditions;

import java.io.IOException;

/**
 * Created by breh on 2/17/17.
 */

public class LocationHelper {

    private static final String TAG = "LocationHelper";

    private static final int GPS_UPDATE_TIME_FREQ = 10000;
    private static final float GPS_UPDATE_DISTANCE = 10;

    private LocationManager locationManager;
    private LocationProvider locationProvider;
    private LocationListenerImpl locationListener;
    private int gpsStatus = GpsStatus.GPS_EVENT_STOPPED;
    private Location lastKnownLocation;

    private static boolean permissionGranted(@NonNull Context context) {
        return Build.VERSION.SDK_INT < 23 ||
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                        ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }


    public boolean isLocationAvailable() {
        return gpsStatus != GpsStatus.GPS_EVENT_STOPPED;
    }


    public @Nullable Location getLastKnownLocation() {
        return lastKnownLocation;
    }


    public void start(@NonNull Context context) throws IOException {
        Preconditions.assertParameterNotNull(context, "context");
        if (Build.VERSION.SDK_INT < 23 ||
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            if (locationManager == null) {
                throw new IOException("Cannot obtain location manager");
            } else {
                locationProvider = locationManager.getProvider(LocationManager.GPS_PROVIDER);
                if (locationProvider == null) {
                    locationManager = null;
                    throw new IOException("Cannot obtain location povider");
                } else {
                    locationListener = new LocationListenerImpl();
                    locationManager.addGpsStatusListener(locationListener);
                    locationManager.requestLocationUpdates(locationProvider.getName(), GPS_UPDATE_TIME_FREQ, GPS_UPDATE_DISTANCE, locationListener);
                }
            }
        } else {
            throw new IOException("No permission to obtain location");
        }

    }

    public boolean stop(@NonNull Context context) {
        Preconditions.assertParameterNotNull(context, "context");
        if (locationManager != null) {
            if (Build.VERSION.SDK_INT >= 23 &&
                    ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return false;
            } else {

                locationManager.removeUpdates(locationListener);
            }
            locationManager.removeGpsStatusListener(locationListener);
            lastKnownLocation = null;
            gpsStatus = GpsStatus.GPS_EVENT_STOPPED;
            locationProvider = null;
            locationManager = null;
            return true;
        } //
        return false;
    }


    private class LocationListenerImpl  implements LocationListener, GpsStatus.Listener {


        public LocationListenerImpl() {

        }

        @Override
        public void onLocationChanged(Location location) {
            Log.d(TAG,"obtained location: "+location);
            LocationHelper.this.lastKnownLocation = location;
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }

        @Override
        public void onGpsStatusChanged(int event) {
            gpsStatus = event;

        }
    };



}



