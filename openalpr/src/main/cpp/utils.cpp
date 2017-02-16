#include "utils.h"
#include <stdio.h>
#include <sys/types.h>
#include <unistd.h>
#include <android/log.h>
#include <pthread.h>

//
// Created by Martin B on 2/14/17.
//


std::string string_from_jstring(JNIEnv* env, jstring java_string) {
    if (java_string) {
        const char *chars = env->GetStringUTFChars(java_string, NULL);
        std::string string(chars);
        env->ReleaseStringUTFChars(java_string, chars);
        return string;
    } else {
        return std::string();
    }
}

jstring jstring_from_string(JNIEnv* env, std::string string) {
    return env->NewStringUTF(string.c_str());
}



void throwRuntimeException(JNIEnv *env, std::string message) {
    jclass exClass;
    char *className = "java/lang/RuntimeException";
    jclass clazz = env->FindClass(className);
    if (clazz) {
        env->ThrowNew(clazz, message.c_str());
    }
}


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

