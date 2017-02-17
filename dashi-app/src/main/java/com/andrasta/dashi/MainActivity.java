package com.andrasta.dashi;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.andrasta.dashi.location.LocationHelper;
import com.andrasta.dashi.openalpr.Alpr;
import com.andrasta.dashi.openalpr.AlprResult;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private LocationHelper locationHelper = new LocationHelper();
    private Alpr alpr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        askForPermissions();


        TextView tv = (TextView)findViewById(R.id.hello);
        Alpr alpr = new Alpr(null, "/data/local/tmp/openalpr.conf","/data/local/tmp/runtime_data");
        tv.setText("Version: "+alpr.getVersion());


        AlprResult result = alpr.recognizeFromFilePath("/data/local/tmp/IMG_9383-copy.jpg");
        Log.w("BLA", "result : "+result);



        try {
            RandomAccessFile f = new RandomAccessFile("/data/local/tmp/IMG_9383-copy.jpg", "r");
            byte[] b = new byte[(int) f.length()];
            Log.w("BLA", "byte size: "+b.length);
            f.readFully(b);
            result = alpr.recognizeFromFileData(b);
            Log.w("BLA", "result2 : "+result);
        } catch (IOException ioe) {
            Log.w("XXX", "", ioe);
        }



        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;

        Bitmap bitmap = BitmapFactory.decodeFile("/data/local/tmp/IMG_9383-copy.jpg", options);


        int size = bitmap.getByteCount();
        ByteBuffer buf = ByteBuffer.allocateDirect(size);
        bitmap.copyPixelsToBuffer(buf);


        byte[] grayScale = toGrayScale(bitmap);
        ByteBuffer grayBuffer = ByteBuffer.allocateDirect(grayScale.length);
        grayBuffer.put(grayScale);

        //for (int i=0; i < 1000; i++) {
            long timestamp = System.currentTimeMillis();
            AlprResult r = alpr.recognizeFromByteBuffer(grayBuffer, 1, bitmap.getWidth(), bitmap.getHeight());
            long delta = System.currentTimeMillis() - timestamp;
            Log.w("BLA", "result3 : " + r.toString());
            Log.w("BLA", "delta : " + delta);

        //}


    }



    private static byte[] toGrayScale(Bitmap bitmap) {

        final int width = bitmap.getWidth();
        final int height = bitmap.getHeight();
        byte[] grayScaleArray = new byte[width * height];
        double GS_RED = 0.299;
        double GS_GREEN = 0.587;
        double GS_BLUE = 0.114;
        int pixel;
        int R, G, B;

        // scan through every single pixel
        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                // get one pixel color
                pixel = bitmap.getPixel(x, y);

                // retrieve color of all channels
                R = Color.red(pixel);
                G = Color.green(pixel);
                B = Color.blue(pixel);

                // take conversion up to one single value
                pixel = (int) (GS_RED * R + GS_GREEN * G + GS_BLUE * B);
                // set new pixel color to output bitmap
                int index = width*y + x;
                grayScaleArray[index] = (byte)pixel;
                //Show grayscaled image
            }
        }
        return grayScaleArray;
    }

    // permissions
    private static final int TAG_CODE_PERMISSION_LOCATION = 0x99;

    private void startLocationHelper() {
        try {
            locationHelper.start(this);
        } catch (IOException ioe) {
            Log.d(TAG,"Cannot start location", ioe);
        }

    }

    private void askForPermissions() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED) {
            startLocationHelper();
        } else {
            ActivityCompat.requestPermissions(this, new String[] {
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION },
                    TAG_CODE_PERMISSION_LOCATION);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case TAG_CODE_PERMISSION_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    startLocationHelper();

                } else {
                    locationHelper.stop(this);
                }
                return;
            }
        }
    }



}
