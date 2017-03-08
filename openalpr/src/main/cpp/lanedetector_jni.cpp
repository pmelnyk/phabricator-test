#include <jni.h>
#include <string>
#include <functional>
#include "alpr.h"
#include "lanedetector.h"
#include "utils_jni.h"
#include "opencv/cv.h"
#include "jobject_helper.h"

using namespace alpr;
using namespace std;
using namespace dashi::jni;
using namespace vision;

static LaneDetector* getLaneDetector(JNIEnv *env, jlong nativeReference) {
    if (nativeReference) {
        return (LaneDetector *) nativeReference;
    } else {
        JUtils::throwRuntimeException(env, "lane detector native reference == 0");
        return 0;
    }
}


template<typename T>
static T withLaneDetector(JNIEnv* env, const jlong nativeReference,
                          const std::function<T(LaneDetector *)> &f) {
    auto laneDetector = getLaneDetector(env, nativeReference);
    if (laneDetector) {
        return f(laneDetector);
    } else {
        return reinterpret_cast<T>(0);
    }
}

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_andrasta_dashi_openalpr_LaneDetector_nCreate(JNIEnv* env, jclass type) {

    ALog::startLogger();
    auto laneDetector = new LaneDetector();
    return (jlong) laneDetector;

}

static const char *LANE_RESULT_CLASS_NAME = "com/andrasta/dashi/openalpr/LaneDetectorResult";
static const char *LANE_RESULT_CONSTRUCTOR_SIG = "(F[Landroid/graphics/Point;)V";
static JConstructor laneResultsConstructor = JConstructor(LANE_RESULT_CLASS_NAME,
                                                              LANE_RESULT_CONSTRUCTOR_SIG);


jobject createJLaneDetectorResult(JNIEnv *env, const LaneDetectorResult &laneDetectorResult) {

    jobjectArray jCoordinates = JObjectHelper::createJPointArray(env, 4);

    for (int i = 0; i < 4; i++) {
        cv::Point coordinate = laneDetectorResult.coordinates[i];
        jobject jpoint = JObjectHelper::createJPoint(env, coordinate.x, coordinate.y);
        JEXCEPTION_CHECK(env);
        env->SetObjectArrayElement(jCoordinates, i, jpoint);
        JEXCEPTION_CHECK(env);
        env->DeleteLocalRef(jpoint);
    }

    return laneResultsConstructor.newObject(env, (jfloat) laneDetectorResult.thickness,
                                                jCoordinates);
}

JNIEXPORT jobject JNICALL
Java_com_andrasta_dashi_openalpr_LaneDetector_nRecognizeLaneByteBuffer(JNIEnv *env, jclass type,
                                                                       jlong nativeReference,
                                                                       jobject byteBuffer,
                                                                       jint pixelSize, jint width,
                                                                       jint height) {

    return withLaneDetector<jobject>(env, nativeReference, [&](auto laneDetector) {
        auto directBuffer = env->GetDirectBufferAddress(byteBuffer);
        if (directBuffer) {
            auto pixels = reinterpret_cast<unsigned char *>(directBuffer);

            LaneDetectorResult laneDetectorResult = laneDetector->recognize(pixels, pixelSize,
                                                                              width, height);
            return createJLaneDetectorResult(env, laneDetectorResult);
        } else {
            return (jobject) 0;
        }
    });

}


JNIEXPORT void JNICALL
Java_com_andrasta_dashi_openalpr_LaneDetector_nDelete(JNIEnv *env, jclass type,
                                                      jlong nativeReference) {

    auto laneDetector = getLaneDetector(env, nativeReference);

    if (laneDetector) {
        delete laneDetector;
    }

}
}