#include "lanedetector.h"
#include <stdio.h>
#include "opencv2/opencv.hpp"
#include "android/log.h"
#include <numeric>

using namespace cv;
using namespace vision;
using namespace std;

const char *const TAG = "LaneDetector";

static const int GAUSSIAN_KERNEL_SIZE = 5;
static const int GAUSSIAN_KERNEL_DEVIATION_X = 0;
static const int CANNY_THRESHOLD_1 = 50;
static const int CANNY_THRESHOLD_2 = 150;
static const int POLYGON_COLOR = 255;

static const double MASK_CONSTANT_1 = 0.48;
static const double MAST_CONSTANT_2 = 0.58;
static const double MAST_CONSTANT_3 = 0.52;

static const int RHO_CONST = 2;
static const int THRESHOLD1 = 50;
static const int LINE_LENGTH = 120;
static const int LINE_GAP = 100;

static const int LANE_THICKNESS = 5;

float LaneDetector::average(const vector<float>& items) {

    return accumulate(items.begin(), items.end(), 0.0f) / items.size();
}

Point LaneDetector::center(const vector<Point>& points) {

    int lSum = 0;
    int rSum = 0;

    for(auto p : points) {
        lSum += p.x;
        rSum += p.y;
    }

    return Point(lSum/points.size(), rSum/points.size());
}

LaneDetectorResult LaneDetector::recognize(unsigned char *pixelData, int bytesPerPixel,
                                           int imgWidth, int imgHeight) {

    auto arraySize = imgWidth * imgHeight * bytesPerPixel;

    Mat imageData = Mat(arraySize, 1, CV_8U, pixelData);
    Mat image = imageData.reshape(bytesPerPixel, imgHeight);

    if (!image.data) {
        __android_log_print(ANDROID_LOG_INFO, TAG, "No Image data");
        return LaneDetectorResult();
    }

    Mat blurImg, cannyImg;

    // we already get grayscale image. need not convert again.
    // cvtColor(image, retImg, COLOR_RGB2GRAY, 0);

    GaussianBlur(image, blurImg, Size(GAUSSIAN_KERNEL_SIZE, GAUSSIAN_KERNEL_SIZE), GAUSSIAN_KERNEL_DEVIATION_X);
    imageData.release();
    image.release();

    Canny(blurImg, cannyImg, CANNY_THRESHOLD_1, CANNY_THRESHOLD_2);
    blurImg.release();

    // preparing the mask
    Mat mask = Mat::zeros(cannyImg.size(), cannyImg.type());

    Point points[1][4];
    points[0][0] = Point(0, cannyImg.rows);
    points[0][1] = Point(cannyImg.cols * MASK_CONSTANT_1, cannyImg.rows * MAST_CONSTANT_2);
    points[0][2] = Point(cannyImg.cols * MAST_CONSTANT_3, cannyImg.rows * MAST_CONSTANT_2);
    points[0][3] = Point(cannyImg.cols, cannyImg.rows);

    const Point *ppt[1] = {points[0]};
    int npt[] = {4};

    fillPoly(mask, ppt, npt, 1, Scalar(POLYGON_COLOR));

    // apply the mask
    Mat maskedImage;
    bitwise_and(cannyImg, mask, maskedImage);

    cannyImg.release();
    mask.release();

    vector<Vec4i> lines;
    HoughLinesP(maskedImage, lines, RHO_CONST, CV_PI / 180, THRESHOLD1, LINE_LENGTH, LINE_GAP);

    // calculate the slope of the lane and center point of lanes for extrapolation
    vector<float> rSlopes;
    vector<float> lSlopes;
    vector<Point> rightLines;
    vector<Point> leftLines;

    for(auto l: lines) {

        auto slope = ((float) (l[3] - l[1]) / (float) (l[2] - l[0]));
        auto center = Point((l[0] + l[2]) / 2, (l[1] + l[3]) / 2);

        if (slope > 0.01 && slope < 1) {

            rSlopes.push_back(slope);
            rightLines.push_back(center);

        } else if (slope < -0.01 && slope > -1) {

            lSlopes.push_back(slope);
            leftLines.push_back(center);

        }
    }

    auto rSlope = average(rSlopes);
    auto lSlope = average(lSlopes);

    if ( lines.size() > 0 && rSlope == rSlope && lSlope == lSlope) { // == checks for nan references
        Point rCenter = center(rightLines);
        Point lCenter = center(leftLines);

        // extrapolate center and slope to determine lane coordinates

        int y2 = maskedImage.rows;
        int x2 = (y2 - rCenter.y) / rSlope + rCenter.x;
        int y1 = maskedImage.rows * 3 / 5;
        int x1 = (y1 - rCenter.y) / rSlope + rCenter.x;

        int y12 = maskedImage.rows;
        int x12 = (y12 - lCenter.y) / lSlope + lCenter.x;
        int y11 = maskedImage.rows * 3 / 5;
        int x11 = (y11 - lCenter.y) / lSlope + lCenter.x;

        // fill the detector result
        LaneDetectorResult detectorResult = LaneDetectorResult();

        detectorResult.coordinates[0] = Point(x1, y1);
        detectorResult.coordinates[1] = Point(x2, y2);
        detectorResult.coordinates[2] = Point(x11, y11);
        detectorResult.coordinates[3] = Point(x12, y12);

        detectorResult.thickness = LANE_THICKNESS;
        return detectorResult;
    } else {
        return LaneDetectorResult();
    }
}

