#include <jni.h>
#include <string>
#include <functional>
#include "alpr.h"
#include "utils_jni.h"
#include "opencv/cv.h"
#include "jobject_helper.h"

using namespace alpr;
using namespace std;
using namespace dashi::jni;

static Alpr* getAlpr(JNIEnv *env, jlong nativeReference) {
    if (nativeReference) {
        return (Alpr *) nativeReference;
    } else {
        JUtils::throwRuntimeException(env, "alpr native reference == 0");
        return 0;
    }
}


template<typename T>
static T withAlpr(JNIEnv *env, const jlong nativeReference, const std::function <T (Alpr*)>& f) {
    auto alpr = getAlpr(env,nativeReference);
    if (alpr) {
        return f(alpr);
    } else {
        return reinterpret_cast<T>(0);
    }
}

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_andrasta_dashi_openalpr_Alpr_nCreate(JNIEnv *env, jclass type, jstring country_jstring,
                                              jstring config_jstring,
                                              jstring runtimeDir_jstring) {
    ALog::startLogger();
    auto country = JUtils::stringFromJstring(env, country_jstring);
    if (country.empty()) {
        country = "us";
    }
    auto config = JUtils::stringFromJstring(env, config_jstring);
    auto runtimeDir = JUtils::stringFromJstring(env, runtimeDir_jstring);
    auto alpr = new Alpr(country, config, runtimeDir);
    return (jlong) alpr;
}


JNIEXPORT void JNICALL
Java_com_andrasta_dashi_openalpr_Alpr_nDelete(JNIEnv *env, jclass type, jlong nativeReference) {
    auto alpr = getAlpr(env, nativeReference);
    if (alpr) {
        delete alpr;
    }
}

JNIEXPORT jstring JNICALL
Java_com_andrasta_dashi_openalpr_Alpr_nGetVersion(JNIEnv *env, jclass type, jlong nativeReference) {
    return withAlpr<jstring>(env, nativeReference, [&](auto alpr) {
        auto versionString = alpr->getVersion();
        return JUtils::jstringFromString(env, versionString);
    });
}

static const char* PLATE_CLASS_NAME = "com/andrasta/dashi/openalpr/Plate";
static const char* PLATE_CONSTRUCTOR_SIG = "(Ljava/lang/String;F)V";
static JConstructor plateConstructor = JConstructor(PLATE_CLASS_NAME, PLATE_CONSTRUCTOR_SIG);

static jobject createJPlate(JNIEnv *env, const AlprPlate& plate) {
    auto plate_jstring = JUtils::jstringFromString(env, plate.characters);
    return plateConstructor.newObject(env, plate_jstring, (jfloat)plate.overall_confidence);
}

static const char* PLATE_RESULT_CLASS_NAME = "com/andrasta/dashi/openalpr/PlateResult";
static const char* PLATE_RESULT_CONSTRUCTOR_SIG = "(Lcom/andrasta/dashi/openalpr/Plate;[Lcom/andrasta/dashi/openalpr/Plate;I[Landroid/graphics/Point;I)V";
static JConstructor plateResultConstructor = JConstructor(PLATE_RESULT_CLASS_NAME, PLATE_RESULT_CONSTRUCTOR_SIG);

static jobject createJPlateResult(JNIEnv *env, const AlprPlateResult& alprPlateResults) {
    auto jplate = createJPlate(env, alprPlateResults.bestPlate);
    JEXCEPTION_CHECK(env);

    auto topNplates = alprPlateResults.topNPlates;
    auto topNplatesSize = topNplates.size();
    auto plateClass = plateConstructor.getClass(env);
    JEXCEPTION_CHECK(env);

    auto jtopNplates = env->NewObjectArray(topNplatesSize, plateClass, NULL);
    JEXCEPTION_CHECK(env);
    for (int i=0; i < topNplatesSize; i++) {
        auto plate = topNplates.at(i);
        auto jplate = createJPlate(env, plate);
        JEXCEPTION_CHECK(env);
        env->SetObjectArrayElement(jtopNplates, i, jplate);
        JEXCEPTION_CHECK(env);
        env->DeleteLocalRef(jplate);
    }

    auto jCoordinates = JObjectHelper::createJPointArray(env, 4);

    for (int i=0; i < 4; i++) {
        auto coordinate = alprPlateResults.plate_points[i];
        auto jpoint = JObjectHelper::createJPoint(env, coordinate.x, coordinate.y);
        JEXCEPTION_CHECK(env);
        env->SetObjectArrayElement(jCoordinates, i, jpoint);
        JEXCEPTION_CHECK(env);
        env->DeleteLocalRef(jpoint);
    }

    return plateResultConstructor.newObject(env, jplate, jtopNplates,
                                            (jint)alprPlateResults.processing_time_ms, jCoordinates,
                                            (jint)alprPlateResults.plate_index);

}

void sigsegvHandler(int signo) {
    if (signo == SIGSEGV) {
        printf("SIGSEGV received: %d\nExit alpr", signo);
        exit(0);
    }
}

static const char* ALPR_RESULT_CLASS_NAME = "com/andrasta/dashi/openalpr/AlprResult";
static const char* ALPR_RESULT_CONSTRUCTOR_SIG = "([Lcom/andrasta/dashi/openalpr/PlateResult;III)V";
static JConstructor alprResultsConstructor = JConstructor(ALPR_RESULT_CLASS_NAME, ALPR_RESULT_CONSTRUCTOR_SIG);

static jobject createJAlprResult(JNIEnv *env, const AlprResults& alprResults) {

    auto plateResults = alprResults.plates;
    auto plateResultsSize = plateResults.size();
    auto plateResultClass = plateResultConstructor.getClass(env);
    JEXCEPTION_CHECK(env);
    auto jplateResults = env->NewObjectArray(plateResultsSize, plateResultClass, NULL);
    JEXCEPTION_CHECK(env);
    for (int i=0; i < plateResultsSize; i++) {
        auto plateResult = plateResults.at(i);
        auto jplateResult = createJPlateResult(env, plateResult);
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
    return withAlpr<jobject>(env, nativeReference, [&](auto alpr) {
        auto filePath = JUtils::stringFromJstring(env, filePath_jstring);
        auto results = alpr->recognize(filePath);
        return createJAlprResult(env, results);
    });
}


JNIEXPORT jobject JNICALL
Java_com_andrasta_dashi_openalpr_Alpr_nRecognizeFileData(JNIEnv *env, jclass type, jlong nativeReference,
                                                          jbyteArray fileData) {
    return withAlpr<jobject>(env, nativeReference, [&](auto alpr) {
        auto bufferPtr = env->GetByteArrayElements(fileData, NULL);
        auto data = reinterpret_cast<char*>(bufferPtr);
        auto size = env->GetArrayLength(fileData);
        auto imageBytes = vector<char>(data, data+ size);
        auto results = alpr->recognize(imageBytes);
        env->ReleaseByteArrayElements(fileData,bufferPtr, JNI_ABORT);
        return createJAlprResult(env, results);
    });
}

JNIEXPORT jobject JNICALL
Java_com_andrasta_dashi_openalpr_Alpr_nRecognizeByteArray(JNIEnv *env, jclass type, jlong nativeReference,
                                                          jbyteArray pixelData, jint width, jint height) {
    return withAlpr<jobject>(env, nativeReference, [&](auto alpr) {
        auto bufferPtr = env->GetByteArrayElements(pixelData, NULL);
        auto pixels = reinterpret_cast<unsigned char*>(bufferPtr);
        auto regionsOfInterest = vector<AlprRegionOfInterest>();
        auto results = alpr->recognize(pixels, 4, width, height, regionsOfInterest);
        env->ReleaseByteArrayElements(pixelData,bufferPtr, JNI_ABORT);
        return createJAlprResult(env, results);
    });
}




JNIEXPORT jobject JNICALL
Java_com_andrasta_dashi_openalpr_Alpr_nRecognizeByteBuffer(JNIEnv *env, jclass type,
                                                           jlong nativeReference, jobject byteBuffer,
                                                           jint pixelSize, jint width, jint height) {
    signal(SIGSEGV, sigsegvHandler);
    return withAlpr<jobject>(env, nativeReference, [&](auto alpr) {
        auto directBuffer = env->GetDirectBufferAddress(byteBuffer);
        if (directBuffer) {
            auto pixels = reinterpret_cast<unsigned char*>(directBuffer);
            auto regionsOfInterest = vector<AlprRegionOfInterest>();
            auto results = alpr->recognize(pixels, pixelSize, width, height, regionsOfInterest);
            return createJAlprResult(env, results);
        } else {
            return (jobject)0;
        }
    });
}


}