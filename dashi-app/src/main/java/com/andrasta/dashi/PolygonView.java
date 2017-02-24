package com.andrasta.dashi;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.support.annotation.UiThread;
import android.util.AttributeSet;
import android.view.View;

/**
 * Draw specified polygon on drawing surface.
 */
public class PolygonView extends View {
    private static final float FRAME_LINE_WIDTH = 10f;

    private int ratioWidth = 0;
    private int ratioHeight = 0;

    private final Paint paint = new Paint();
    private Point[] polygon;
    private float wc, hc;

    public PolygonView(Context context) {
        this(context, null);
    }

    public PolygonView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PolygonView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        paint.setColor(Color.BLUE);
        paint.setStrokeWidth(FRAME_LINE_WIDTH);
    }

    @UiThread
    public void setPolygon(int sourceWidth, int sourceHeight, Point[] polygon) {
        this.polygon = polygon;
        this.wc = 1f * getWidth() / sourceWidth;
        this.hc = 1f * getHeight() / sourceHeight;
        invalidate();
    }

    @UiThread
    public void clear() {
        if (polygon != null) {
            polygon = null;
            invalidate();
        }
    }

    public void setAspectRatio(int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Size cannot be negative.");
        }
        ratioWidth = width;
        ratioHeight = height;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (0 == ratioWidth || 0 == ratioHeight) {
            setMeasuredDimension(width, height);
        } else {
            if (width < height * ratioWidth / ratioHeight) {
                setMeasuredDimension(width, width * ratioHeight / ratioWidth);
            } else {
                setMeasuredDimension(height * ratioWidth / ratioHeight, height);
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (polygon == null || polygon.length < 2 || wc == 0 || hc == 0) {
            return;
        }

        int i;
        for (i = 0; i < polygon.length - 1; i++) {
            canvas.drawLine(polygon[i].x * wc, polygon[i].y * hc, polygon[i + 1].x * wc, polygon[i + 1].y * hc, paint);
        }
        i = polygon.length - 1;
        canvas.drawLine(polygon[i].x * wc, polygon[i].y * hc, polygon[0].x * wc, polygon[0].y * hc, paint);
    }
}