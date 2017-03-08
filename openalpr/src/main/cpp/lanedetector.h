#include <vector>
#include <opencv2/core/types.hpp>

namespace vision {

    class LaneDetectorResult {
        public:
            cv::Point coordinates[4];
            float thickness;


        LaneDetectorResult() { }
    };

    class LaneDetector {

        public:
            LaneDetectorResult recognize(unsigned char* pixelData, int bytesPerPixel, int imgWidth, int imgHeight);

        private:
            static cv::Point center(const std::vector<cv::Point>& points);
            static float average(const std::vector<float>& items);
    };
}