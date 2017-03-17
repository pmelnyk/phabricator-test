package com.andrasta.dashi.openalpr;

/**
 * Created by breh on 2/16/17.
 */

public final class RegionOfInterest {
    private static final float IMAGE_RECOGNITION_REGION_HEIGHT = 2f / 3;

    private final int x, y, width, height;

    public RegionOfInterest(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public static float getImageRecognitionRegionHeight() {
        return IMAGE_RECOGNITION_REGION_HEIGHT;
    }

    public static RegionOfInterest calculateRecognitionRegion(int width, int height) {
        int size = (int) (height * IMAGE_RECOGNITION_REGION_HEIGHT);
        int shift = (height - size) / 2;
        return new RegionOfInterest(0, shift, width, size);
    }

    @Override
    public String toString() {
        return "RegionOfInterest{" + "x=" + x + ", y=" + y +
                ", width=" + width + ", height=" + height + '}';
    }
}
