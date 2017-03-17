package com.andrasta.dashi.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.util.AttributeSet;
import android.view.View;

import com.andrasta.dashi.R;
import com.andrasta.dashi.openalpr.LaneDetectorResult;
import com.andrasta.dashi.openalpr.RegionOfInterest;
import com.andrasta.dashi.utils.Preconditions;

import java.util.List;

public class LaneView extends View {
    private static final float LANE_LINE_WIDTH = 12f;
    private static final float REC_LINE_WIDTH = LANE_LINE_WIDTH / 2;

    private final Paint lanePaint = new Paint();
    private final Paint recPaint = new Paint();
    private RegionOfInterest regionOfInterest;
    private LaneDetectorResult lanes;
    private int ratioHeight = 0;
    private int ratioWidth = 0;
    private float wc, hc;

    public LaneView(@NonNull Context context) {
        this(context, null);
    }

    public LaneView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LaneView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        lanePaint.setStrokeWidth(LANE_LINE_WIDTH);
        lanePaint.setColor(getContext().getColor(R.color.colorPrimaryDark));
        recPaint.setStyle(Paint.Style.STROKE);
        recPaint.setStrokeWidth(REC_LINE_WIDTH);
        recPaint.setColor(getContext().getColor(R.color.lightYellow));
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
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        regionOfInterest = RegionOfInterest.calculateRecognitionRegion(w, h);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        if (regionOfInterest != null) {
            canvas.drawRect(regionOfInterest.getX() + REC_LINE_WIDTH, regionOfInterest.getY(), regionOfInterest.getWidth() - REC_LINE_WIDTH, regionOfInterest.getY() + regionOfInterest.getHeight(), recPaint);
        }

        super.onDraw(canvas);
        if (lanes == null || wc == 0 || hc == 0) {
            return;
        } else if (lanes.getPlateCoordinates().size() != 4) {
            throw new RuntimeException("Wrong LaneDetectorResult response");
        }

        List<Point> points = lanes.getPlateCoordinates();
        canvas.drawLine(points.get(0).x * wc, points.get(0).y * hc, points.get(1).x * wc, points.get(1).y * hc, lanePaint);
        canvas.drawLine(points.get(2).x * wc, points.get(2).y * hc, points.get(3).x * wc, points.get(3).y * hc, lanePaint);
    }
}