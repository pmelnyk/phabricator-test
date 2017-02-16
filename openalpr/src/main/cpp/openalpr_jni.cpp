#include <jni.h>
#include <string>
#include "alpr.h"
#include "utils.h"
#include "opencv/cv.h"
#include "opencv/highgui.h"
#include "opencv2/highgui/highgui.hpp"
#include "opencv2/imgproc/imgproc.hpp"
#include <math.h>

using namespace alpr;
using namespace std;

static Alpr* getAlpr(JNIEnv *env, jlong nativeReference) {
    if (nativeReference) {
        return (Alpr *) nativeReference;
    } else {
        throwRuntimeException(env, "alpr native reference == 0");
        return 0;
    }
}




extern "C" {

JNIEXPORT jlong JNICALL
Java_com_andrasta_dashi_openalpr_Alpr_nCreate(JNIEnv *env, jclass type, jstring country_jstring,
                                              jstring config_jstring,
                                              jstring runtimeDir_jstring) {
    start_logger();
    std::string country = string_from_jstring(env, country_jstring);
    std::string config = string_from_jstring(env, config_jstring);
    std::string runtimeDir = string_from_jstring(env, runtimeDir_jstring);
    cout << "sending conf:" << config << ", runtime data: " << runtimeDir << endl;
    Alpr *alpr = new Alpr("us", config, runtimeDir);
    return (jlong) alpr;
}


JNIEXPORT void JNICALL
Java_com_andrasta_dashi_openalpr_Alpr_nDelete(JNIEnv *env, jclass type, jlong nativeReference) {
    Alpr *alpr = getAlpr(env, nativeReference);
    if (alpr) {
        delete alpr;
    }
}

JNIEXPORT jstring JNICALL
Java_com_andrasta_dashi_openalpr_Alpr_nGetVersion(JNIEnv *env, jclass type, jlong nativeReference) {
    Alpr *alpr = getAlpr(env, nativeReference);
    if (alpr) {
        //auto
        std::string versionString = alpr->getVersion();
        return jstring_from_string(env, versionString);
    } // else
    return 0;
}

JNIEXPORT jstring JNICALL
Java_com_andrasta_dashi_openalpr_Alpr_nRecognizeFilePath(JNIEnv *env, jclass type, jlong nativeReference,
                                                 jstring filePath_jstring) {
    Alpr *alpr = getAlpr(env, nativeReference);
    if (alpr) {
        std::string filePath = string_from_jstring(env, filePath_jstring);
        cv::Mat frame;
        cout << "image filepath : '" << filePath << "'" << endl;
        frame = cv::imread(filePath);
        //IplImage* img  = cvLoadImage(filePath.c_str());  //jpg - bmp
        //frame = cv::cvarrToMat(img);

        cout << "image read : " << frame.rows << "/" << frame.cols << endl;
        std::vector<unsigned char> buffer;
        cv::imencode(".bmp", frame, buffer);
        char* data = reinterpret_cast<char*>(buffer.data());
        vector<char> b2 = vector<char>(data, data + buffer.size());
        cout << "here - b2 size: " << b2.size() << endl;
        AlprResults results = alpr->recognize(b2);
        std::string jsonResults = Alpr::toJson(results);
        return jstring_from_string(env, jsonResults);
        //return 0;
    } // else
    return 0;
}


JNIEXPORT jstring JNICALL
Java_com_andrasta_dashi_openalpr_Alpr_nRecognizeFileData(JNIEnv *env, jclass type, jlong nativeReference,
                                                          jbyteArray fileData) {

    Alpr *alpr = getAlpr(env, nativeReference);
    if (alpr) {
        jbyte *bufferPtr = env->GetByteArrayElements(fileData, NULL);
        char* data = reinterpret_cast<char*>(bufferPtr);
        int size = env->GetArrayLength(fileData);
        std::vector<char> imageBytes(data, data+ size);
        AlprResults results = alpr->recognize(imageBytes);
        std::string jsonResults = Alpr::toJson(results);
        return jstring_from_string(env, jsonResults);
    }// else
    return 0;
}

JNIEXPORT jstring JNICALL
Java_com_andrasta_dashi_openalpr_Alpr_nRecognizeByteArray(JNIEnv *env, jclass type, jlong nativeReference,
                                                          jbyteArray pixelData, jint width, jint height) {

    Alpr *alpr = getAlpr(env, nativeReference);
    if (alpr) {
        jbyte *bufferPtr = env->GetByteArrayElements(pixelData, NULL);
        int size = env->GetArrayLength(pixelData);
        unsigned char* pixels = reinterpret_cast<unsigned char*>(bufferPtr);
        std::vector<AlprRegionOfInterest> regionsOfInterest;
        cout << "processing buffer size: " << size << " w/h: " << width << "/" << height << endl;
        AlprResults results = alpr->recognize(pixels, 4, width, height, regionsOfInterest);
        std::string jsonResults = Alpr::toJson(results);
        return jstring_from_string(env, jsonResults);
    }// else
    return 0;
}


JNIEXPORT jstring JNICALL
Java_com_andrasta_dashi_openalpr_Alpr_nRecognizeByteBuffer(JNIEnv *env, jclass type,
                                                           jlong nativeReference, jobject byteBuffer,
                                                           jint pixelSize, jint width, jint height) {

    Alpr *alpr = getAlpr(env, nativeReference);
    if (alpr) {
        void* directBuffer = env->GetDirectBufferAddress(byteBuffer);
        if (directBuffer) {
            unsigned char* pixels = reinterpret_cast<unsigned char*>(directBuffer);
            std::vector<AlprRegionOfInterest> regionsOfInterest;
            AlprResults results = alpr->recognize(pixels, pixelSize, width, height, regionsOfInterest);
            std::string jsonResults = Alpr::toJson(results);
            return jstring_from_string(env, jsonResults);
        }
    } // else
    return 0;

}


}