package com.andrasta.dashi;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.andrasta.dashi.openalpr.Alpr;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;

public class MainActivity extends AppCompatActivity {

    private Alpr alpr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView tv = (TextView)findViewById(R.id.hello);
        Alpr alpr = new Alpr("us", "/data/local/tmp/openalpr.conf","/data/local/tmp/runtime_data");
        tv.setText("Version: "+alpr.getVersion());

        String result;

        /*
        String result = alpr.recognizeFromFilePath("/data/local/tmp/IMG_9383-copy.jpgs");
        Log.w("BLA", "result : "+result);



        try {

            RandomAccessFile f = new RandomAccessFile("/data/local/tmp/IMG_9383-copy.jpg", "r");
            byte[] b = new byte[(int) f.length()];
            Log.w("BLA", "byte size: "+b.length);
            f.readFully(b);
            result = alpr.recognizeFromFileData(b);
            Log.w("BLA", "result3 : "+result);
        } catch (IOException ioe) {
            Log.w("XXX", "", ioe);
        }
        */


        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;

        Bitmap bitmap = BitmapFactory.decodeFile("/data/local/tmp/IMG_9383-copy.jpg", options);


        int size = bitmap.getByteCount();
        ByteBuffer buf = ByteBuffer.allocateDirect(size);
        bitmap.copyPixelsToBuffer(buf);


        byte[] grayScale = toGrayScale(bitmap);
        ByteBuffer grayBuffer = ByteBuffer.allocateDirect(grayScale.length);
        grayBuffer.put(grayScale);

        for (int i=0; i < 1000; i++) {
            long timestamp = System.currentTimeMillis();
            result = alpr.recognizeFromByteBuffer(grayBuffer, 1, bitmap.getWidth(), bitmap.getHeight());
            long delta = System.currentTimeMillis() - timestamp;
            Log.w("BLA", "result2 : " + result);
            Log.w("BLA", "delta : " + delta);

        }


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


}
