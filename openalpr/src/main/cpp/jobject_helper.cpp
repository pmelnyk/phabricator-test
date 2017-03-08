#include <jni.h>
#include "jobject_helper.h"
#include "utils_jni.h"

using namespace dashi::jni;

const char* POINT_CLASS_NAME = "android/graphics/Point";
const char* POINT_CONSTRUCTOR_SIG = "(II)V";
JConstructor pointConstructor = JConstructor(POINT_CLASS_NAME, POINT_CONSTRUCTOR_SIG);

jobject JObjectHelper::createJPoint(JNIEnv *env, int x, int y) {
    return pointConstructor.newObject(env, (jint)x, (jint)y);
}

jobjectArray JObjectHelper::createJPointArray(JNIEnv *env, jsize size) {
    auto pointClass = pointConstructor.getClass(env);
    JEXCEPTION_CHECK(env);

    auto jCoordinates = env->NewObjectArray(size, pointClass, NULL);
    JEXCEPTION_CHECK(env);

    return jCoordinates;
}



