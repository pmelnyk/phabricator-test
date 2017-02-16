//
// Created by Martin B on 2/14/17.
//

#ifndef DASH_I_UTILS_H
#define DASH_I_UTILS_H

#include <string>
#include <jni.h>

std::string string_from_jstring(JNIEnv* env, jstring java_string);
jstring jstring_from_string(JNIEnv* env, std::string string);

void throwRuntimeException(JNIEnv *env, std::string message);

int start_logger();


#endif //DASH_I_UTILS_H
