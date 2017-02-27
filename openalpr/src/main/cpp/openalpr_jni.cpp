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
    if (country.empty()) {
        country = "us";
    }
    std::string config = string_from_jstring(env, config_jstring);
    std::string runtimeDir = string_from_jstring(env, runtimeDir_jstring);
    Alpr *alpr = new Alpr(country, config, runtimeDir);
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



static const char* POINT_CLASS_NAME = "android/graphics/Point";
static const char* POINT_CONSTRUCTOR_SIG = "(II)V";
static JConstructor pointConstructor = JConstructor(POINT_CLASS_NAME, POINT_CONSTRUCTOR_SIG);

jobject createJPoint(JNIEnv *env, int x, int y) {
    return pointConstructor.newObject(env, (jint)x, (jint)y);
}



static const char* PLATE_CLASS_NAME = "com/andrasta/dashi/openalpr/Plate";
static const char* PLATE_CONSTRUCTOR_SIG = "(Ljava/lang/String;F)V";
static JConstructor plateConstructor = JConstructor(PLATE_CLASS_NAME, PLATE_CONSTRUCTOR_SIG);

jobject createJPlate(JNIEnv *env, const AlprPlate& plate) {
    jstring plate_jstring = jstring_from_string(env, plate.characters);
    return plateConstructor.newObject(env, plate_jstring, (jfloat)plate.overall_confidence);
}

static const char* PLATE_RESULT_CLASS_NAME = "com/andrasta/dashi/openalpr/PlateResult";
static const char* PLATE_RESULT_CONSTRUCTOR_SIG = "(Lcom/andrasta/dashi/openalpr/Plate;[Lcom/andrasta/dashi/openalpr/Plate;I[Landroid/graphics/Point;I)V";
static JConstructor plateResultConstructor = JConstructor(PLATE_RESULT_CLASS_NAME, PLATE_RESULT_CONSTRUCTOR_SIG);

jobject createJPlateResult(JNIEnv *env, const AlprPlateResult& alprPlateResults) {
    jobject jplate = createJPlate(env, alprPlateResults.bestPlate);
    JEXCEPTION_CHECK(env);

    vector<AlprPlate> topNplates = alprPlateResults.topNPlates;
    int topNplatesSize = topNplates.size();
    jclass plateClass = plateConstructor.getClass(env);
    JEXCEPTION_CHECK(env);

    jobjectArray jtopNplates = env->NewObjectArray(topNplatesSize, plateClass, NULL);
    JEXCEPTION_CHECK(env);
    for (int i=0; i < topNplatesSize; i++) {
        AlprPlate plate = topNplates.at(i);
        jobject jplate = createJPlate(env, plate);
        JEXCEPTION_CHECK(env);
        env->SetObjectArrayElement(jtopNplates, i, jplate);
        JEXCEPTION_CHECK(env);
        env->DeleteLocalRef(jplate);
    }

    jclass pointClass = pointConstructor.getClass(env);
    JEXCEPTION_CHECK(env);

    jobjectArray jCoordinates = env->NewObjectArray(4, pointClass, NULL);
    JEXCEPTION_CHECK(env);
    for (int i=0; i < 4; i++) {
        AlprCoordinate coordinate = alprPlateResults.plate_points[i];
        jobject jpoint = createJPoint(env, coordinate.x, coordinate.y);
        JEXCEPTION_CHECK(env);
        env->SetObjectArrayElement(jCoordinates, i, jpoint);
        JEXCEPTION_CHECK(env);
        env->DeleteLocalRef(jpoint);
    }

    return plateResultConstructor.newObject(env, jplate, jtopNplates,
                                            (jint)alprPlateResults.processing_time_ms, jCoordinates,
                                            (jint)alprPlateResults.plate_index);

}




static const char* ALPR_RESULT_CLASS_NAME = "com/andrasta/dashi/openalpr/AlprResult";
static const char* ALPR_RESULT_CONSTRUCTOR_SIG = "([Lcom/andrasta/dashi/openalpr/PlateResult;III)V";
static JConstructor alprResultsConstructor = JConstructor(ALPR_RESULT_CLASS_NAME, ALPR_RESULT_CONSTRUCTOR_SIG);

jobject createJAlprResult(JNIEnv *env, const AlprResults& alprResults) {

    vector<AlprPlateResult> plateResults = alprResults.plates;
    int plateResultsSize = plateResults.size();
    jclass plateResultClass = plateResultConstructor.getClass(env);
    JEXCEPTION_CHECK(env);
    jobjectArray jplateResults = env->NewObjectArray(plateResultsSize, plateResultClass, NULL);
    JEXCEPTION_CHECK(env);
    for (int i=0; i < plateResultsSize; i++) {
        AlprPlateResult plateResult = plateResults.at(i);
        jobject jplateResult = createJPlateResult(env, plateResult);
        JEXCEPTION_CHECK(env);
        env->SetObjectArrayElement(jplateResults, i, jplateResult);
        JEXCEPTION_CHECK(env);
    }

    return alprResultsConstructor.newObject(env, jplateResults, (jint) alprResults.total_processing_time_ms,
                                            (jint)alprResults.img_width, (jint)alprResults.img_height);
}




JNIEXPORT jobject JNICALL
Java_com_andrasta_dashi_openalpr_Alpr_nRecognizeFilePath(JNIEnv *env, jclass type, jlong nativeReference,
                                                 jstring filePath_jstring) {
    jobject jresult = 0;
    Alpr *alpr = getAlpr(env, nativeReference);
    if (alpr) {
        std::string filePath = string_from_jstring(env, filePath_jstring);
        AlprResults results = alpr->recognize(filePath);
        jresult = createJAlprResult(env, results);
    }
    return jresult;
}


JNIEXPORT jobject JNICALL
Java_com_andrasta_dashi_openalpr_Alpr_nRecognizeFileData(JNIEnv *env, jclass type, jlong nativeReference,
                                                          jbyteArray fileData) {

    jobject jresult = 0;
    Alpr *alpr = getAlpr(env, nativeReference);
    if (alpr) {
        jbyte *bufferPtr = env->GetByteArrayElements(fileData, NULL);
        char* data = reinterpret_cast<char*>(bufferPtr);
        int size = env->GetArrayLength(fileData);
        std::vector<char> imageBytes(data, data+ size);
        AlprResults results = alpr->recognize(imageBytes);
        jresult = createJAlprResult(env, results);
        env->ReleaseByteArrayElements(fileData,bufferPtr, JNI_ABORT);
    }
    return jresult;
}

JNIEXPORT jobject JNICALL
Java_com_andrasta_dashi_openalpr_Alpr_nRecognizeByteArray(JNIEnv *env, jclass type, jlong nativeReference,
                                                          jbyteArray pixelData, jint width, jint height) {
    jobject jresult = 0;
    Alpr *alpr = getAlpr(env, nativeReference);
    if (alpr) {
        jbyte *bufferPtr = env->GetByteArrayElements(pixelData, NULL);
        unsigned char* pixels = reinterpret_cast<unsigned char*>(bufferPtr);
        std::vector<AlprRegionOfInterest> regionsOfInterest;
        AlprResults results = alpr->recognize(pixels, 4, width, height, regionsOfInterest);
        jresult = createJAlprResult(env, results);
        env->ReleaseByteArrayElements(pixelData,bufferPtr, JNI_ABORT);

    }
    return jresult;
}




JNIEXPORT jobject JNICALL
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
            return createJAlprResult(env, results);
        }
    } // else
    return 0;

}


}