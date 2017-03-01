#include "utils_jni.h"
#include <stdio.h>
#include <sys/types.h>
#include <unistd.h>
#include <android/log.h>
#include <pthread.h>
#include <iostream>

//
// Created by Martin B on 2/14/17.
//


std::string string_from_jstring(JNIEnv* env, jstring java_string) {
    //foo();
    if (java_string) {
        const char *chars = env->GetStringUTFChars(java_string, NULL);
        std::string string(chars);
        env->ReleaseStringUTFChars(java_string, chars);
        return string;
    } else {
        return std::string("");
    }
}

jstring jstring_from_string(JNIEnv* env, std::string string) {
    return env->NewStringUTF(string.c_str());
}



void throwRuntimeException(JNIEnv *env, std::string message) {
    jclass exClass;
    const char *className = "java/lang/RuntimeException";
    jclass clazz = env->FindClass(className);
    if (! clazz) {
        env->ThrowNew(clazz, message.c_str());
    }
}


jclass find_class(JNIEnv* env, const char* className) {
    jclass clazz = env->FindClass(className);
    if (! clazz) {
        std::string message = std::string("Cannot find class: ") + className;
        throwRuntimeException(env, message);
    }
    return clazz;
}




jmethodID find_method(JNIEnv* env, jclass clazz, const char* methodName, const char* signature) {
    jmethodID methodID = 0;
    if (! env->ExceptionCheck()) {
        methodID = env->GetMethodID(clazz, methodName, signature);
        if (! methodID) {
            std::string message = std::string("Cannot find constructor: ") + signature;
            throwRuntimeException(env, message);
        }
    }
    return methodID;
}



jmethodID find_constructor(JNIEnv* env, jclass clazz, const char* signature) {
    return find_method(env, clazz, "<init>", signature);
}


jobject create_object(JNIEnv *env, jclass clazz, jmethodID constructor, ...) {
    va_list args;
    va_start(args, constructor);
    jobject createdObject = env->NewObjectV(clazz, constructor, args);
    va_end(args);
    return createdObject;
}


static const char* TO_STRING_METHOD_NAME = "toString";
static const char* TO_STRING_METHOD_SIGNATURE = "()Ljava/lang/String;";
std::string call_toString(JNIEnv *env, jobject object) {
    jclass clazz = env->GetObjectClass(object);
    jmethodID methodId = find_method(env, clazz, TO_STRING_METHOD_NAME, TO_STRING_METHOD_SIGNATURE);
    jstring result = (jstring)env->CallObjectMethod(object, methodId);
    return string_from_jstring(env, result);
}

// constructor class


JConstructor::JConstructor(const char* className, const char* signature) {
    _className = className;
    _signature = signature;
    _methodId = 0;
    _clazz = 0;
}

jclass JConstructor::getClass(JNIEnv* env) {
    if (! _clazz) {
        jclass clazz = find_class(env, _className);
        JEXCEPTION_CHECK(env);
        _clazz = (jclass)env->NewGlobalRef(clazz);
    }
    return _clazz;
}


jobject JConstructor::newObject(JNIEnv* env, ...) {
    jclass clazz = getClass(env);
    JEXCEPTION_CHECK(env);
    if (! _methodId) {
        _methodId = find_constructor(env, _clazz, _signature);
        JEXCEPTION_CHECK(env);
    }
    va_list args;
    va_start(args, env);
    jobject createdObject = env->NewObjectV(_clazz, _methodId, args);
    va_end(args);
    return createdObject;
}



///

static int pfd[2];
static pthread_t thr;
static const char *tag = "dashi-native";

static void *thread_func(void*)
{
    ssize_t rdsz;
    char buf[128];
    while((rdsz = read(pfd[0], buf, sizeof buf - 1)) > 0) {
        if(buf[rdsz - 1] == '\n') --rdsz;
        buf[rdsz] = 0;  /* add null-terminator */
        __android_log_write(ANDROID_LOG_WARN, tag, buf);
    }
    return 0;
}


int start_logger()
{

    /* make stdout line-buffered and stderr unbuffered */
    setvbuf(stdout, 0, _IOLBF, 0);
    setvbuf(stderr, 0, _IONBF, 0);

    /* create the pipe and redirect stdout and stderr */
    pipe(pfd);
    dup2(pfd[1], 1);
    dup2(pfd[1], 2);

    /* spawn the logging thread */
    if(pthread_create(&thr, 0, thread_func, 0) == -1)
        return -1;
    pthread_detach(thr);
    return 0;
}

