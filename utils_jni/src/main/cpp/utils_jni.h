//
// Created by Martin B on 2/28/17.
//

#ifndef DASH_I_UTILS_JNI_H
#define DASH_I_UTILS_JNI_H

#include <string>
#include <jni.h>

#define JEXCEPTION_CHECK(E) \
if (env->ExceptionCheck()) { \
    return 0;\
}

#define JEXCEPTION_CHECKR(E,R) \
if (env->ExceptionCheck()) { \
    return R;\
}

void foo();

std::string string_from_jstring(JNIEnv* env, jstring java_string);
jstring jstring_from_string(JNIEnv* env, std::string string);

void throwRuntimeException(JNIEnv *env, std::string message);

JNIEXPORT int start_logger();

jclass find_class(JNIEnv* env, const char* className);
jmethodID find_method(JNIEnv* env, jclass clazz, const char* methodName, const char* signature);
jmethodID find_constructor(JNIEnv* env, jclass clazz, const char* signature);
jobject create_object(JNIEnv *env, jclass clazz, jmethodID constructor, ...);
std::string call_toString(JNIEnv *env, jobject object);


class JConstructor {

public:
    JConstructor(const char* className, const char* signature);
    virtual ~JConstructor() {}

    jobject newObject(JNIEnv* env, ...);
    jclass  getClass(JNIEnv* env);

private:
    const char* _className;
    const char* _signature;
    jmethodID _methodId;
    jclass _clazz; // FIXME _clazz is currently leaking ... should be removed in destructor

};




#endif //DASH_I_UTILS_JNI_H
