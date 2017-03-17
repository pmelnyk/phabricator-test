package com.andrasta.dashi.openalpr;

/**
 * Created by breh on 2/16/17.
 */

public final class RegionOfInterest {

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
}
