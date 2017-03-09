package com.andrasta.dashi.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;
import android.util.AttributeSet;
import android.view.View;

import com.andrasta.dashi.R;
import com.andrasta.dashi.openalpr.LaneDetectorResult;
import com.andrasta.dashi.utils.Preconditions;

import java.util.List;

public class LaneView extends View {
    private static final float FRAME_LINE_WIDTH = 12f;

    private final Paint paint = new Paint();
    private LaneDetectorResult lanes;
    private int ratioHeight = 0;
    private int ratioWidth = 0;
    private float wc, hc;

    public LaneView(Context context) {
        this(context, null);
    }

    public LaneView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LaneView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        paint.setStrokeWidth(FRAME_LINE_WIDTH);
        paint.setColor(getContext().getColor(R.color.colorPrimaryDark));
    }

    @UiThread
    public void setLanes(int sourceWidth, int sourceHeight, @NonNull LaneDetectorResult lanes) {
        Preconditions.assertReturnNotNull(lanes, "lanes");
        this.lanes = lanes;
        this.wc = (1f * getWidth()) / sourceWidth;
        this.hc = 1f * getHeight() / sourceHeight;
        invalidate();
    }

    @UiThread
    public void clear() {
        if (lanes != null) {
            lanes = null;
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
        if (lanes == null || wc == 0 || hc == 0) {
            return;
        } else if (lanes.getPlateCoordinates().size() != 4) {
            throw new RuntimeException("Wrong LaneDetectorResult response");
        }

        List<Point> points = lanes.getPlateCoordinates();
        canvas.drawLine(points.get(0).x * wc, points.get(0).y * hc, points.get(1).x * wc, points.get(1).y * hc, paint);
        canvas.drawLine(points.get(2).x * wc, points.get(2).y * hc, points.get(3).x * wc, points.get(3).y * hc, paint);
    }
}