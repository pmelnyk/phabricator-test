//
// Created by Martin B on 2/14/17.
//

#ifndef DASH_I_UTILS_H
#define DASH_I_UTILS_H

#include <string>
#include <jni.h>

#define JEXCEPTION_CHECK(E) \
if (env->ExceptionCheck()) { \
    return 0;\
}

#define JEXCEPTION_CHECKR(E, R) \
if (env->ExceptionCheck()) { \
    return R;\
}


namespace dashi {
    namespace jni {

        /**
         * Basic JNI utils
         */
        class JUtils {
        public:
            /**
             * Converts Java string to std::string
             * @param env
             * @param java_string
             * @return
             */
            static std::string stringFromJstring(JNIEnv *env, jstring java_string);

            /**
             * Converts std::string to Java string
             * @param env
             * @param string
             * @return
             */
            static jstring jstringFromString(JNIEnv *env, std::string string);

            /**
             * Throws runtime exception with given message
             * @param env
             * @param message
             */
            static void throwRuntimeException(JNIEnv *env, std::string message);

            /**
             * Finds a class with given class name. If class not found, a runtime
             * exception is thrown and the function returns 0;
             * @param env
             * @param className
             * @return
             */
            static jclass findClass(JNIEnv *env, const char *className);

            /**
             * Finds a method in a class with given name and siganture. If not found, a runtime
             * exception is thrown and the function returns 0;
             * @param env
             * @param clazz
             * @param methodName
             * @param signature
             * @return
             */
            static jmethodID findMethod(JNIEnv *env, jclass clazz, const char *methodName,
                                        const char *signature);

            /**
             * Finds a constructor in a class with given signature. If not found, a runtime
             * exception is thrown and the function returns 0;
             * @param env
             * @param clazz
             * @param signature
             * @return
             */
            static jmethodID findConstructor(JNIEnv *env, jclass clazz, const char *signature);

            /**
             * Creates a new Java object from given constructor and given parameters.
             * @param env
             * @param clazz
             * @param constructor
             * @return
             */
            static jobject createObject(JNIEnv *env, jclass clazz, jmethodID constructor, ...);

            /**
             * Calls toString() method on given objects and returns the value as std::string
             * @param env
             * @param object
             * @return
             */
            static std::string callToString(JNIEnv *env, jobject object);

        private:
            JUtils() {}

        };

        /**
         * Logging utilities
         */
        class ALog {
        public:
            /**
             * starts background logger stdout / stderr is logged to android log
             * @return
             */
            static int startLogger();

        private:
            ALog() {};
        };

        /**
         * Class representing a Java constructor
         */
        class JConstructor {

        public:
            /**
             * Creates a new constructor with a classname and signature
             * @param className
             * @param signature
             * @return
             */
            JConstructor(const char *className, const char *signature);

            virtual ~JConstructor() {}

            /**
             * Creates a new object with given parameters
             * @param env
             * @return
             */
            jobject newObject(JNIEnv *env, ...);

            /**
             * Gets a class in which this constructor exists
             * @param env
             * @return
             */
            jclass getClass(JNIEnv *env);

        private:
            const char *_className;
            const char *_signature;
            jmethodID _methodId;
            jclass _clazz; // FIXME _clazz is currently leaking ... should be removed in destructor

        };

    }
}


#endif //DASH_I_UTILS_H
