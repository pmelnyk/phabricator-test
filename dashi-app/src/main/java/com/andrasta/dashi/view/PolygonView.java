package com.andrasta.dashi.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.support.annotation.UiThread;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

import com.andrasta.dashi.R;

/**
 * Draw specified polygon on drawing surface.
 */
@SuppressLint("AppCompatCustomView")
public class PolygonView extends ImageView {
    private static final String TAG = "PolygonView";
    private static final float FRAME_LINE_WIDTH = 10f;

    private final Paint polygonPaint = new Paint();
    private final Paint framePaint = new Paint();
    private float widthDivider, heightDivider;
    private int width, height;
    private Point[] polygon;

    public PolygonView(Context context) {
        this(context, null);
    }

    public PolygonView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PolygonView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        polygonPaint.setColor(getContext().getColor(R.color.colorPrimaryDark));
        framePaint.setColor(getContext().getColor(R.color.lightBlue));
        framePaint.setStyle(Paint.Style.STROKE);
        framePaint.setStrokeWidth(FRAME_LINE_WIDTH * 2);
        polygonPaint.setStrokeWidth(FRAME_LINE_WIDTH);
    }

    @UiThread
    public void setViewSize(int width, int height) {
        this.width = width;
        this.height = height;
        Log.d(TAG, "Size set: " + width + 'x' + height);
    }

    @UiThread
    public void setPolygon(int sourceWidth, int sourceHeight, Point[] polygon) {
        this.polygon = polygon;
        this.widthDivider = 1f * width / sourceWidth;
        this.heightDivider = 1f * height / sourceHeight;
        Log.d(TAG, "New source resolution: " + sourceWidth + 'x' + sourceHeight);
        Log.d(TAG, "Ratio set: " + widthDivider + 'x' + heightDivider);
        invalidate();
    }

    @UiThread
    public void clear() {
        if (polygon != null) {
            polygon = null;
            invalidate();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (width == 0 || height == 0) {
            setMeasuredDimension(width, height);
        } else {
            if (width > height) {
                setMeasuredDimension(width, height);
            } else {
                setMeasuredDimension(height, width);
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (polygon == null || polygon.length < 2 || widthDivider == 0 || heightDivider == 0) {
            return;
        }

        int i;
        for (i = 0; i < polygon.length - 1; i++) {
            canvas.drawLine(polygon[i].x * widthDivider, polygon[i].y * heightDivider, polygon[i + 1].x * widthDivider, polygon[i + 1].y * heightDivider, polygonPaint);
        }
        i = polygon.length - 1;
        canvas.drawLine(polygon[i].x * widthDivider, polygon[i].y * heightDivider, polygon[0].x * widthDivider, polygon[0].y * heightDivider, polygonPaint);
        canvas.drawRect(0, 0, getWidth(), getHeight(), framePaint);
    }
}