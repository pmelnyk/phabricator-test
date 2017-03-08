#include "utils_jni.h"

namespace dashi {
    namespace jni {
        class JObjectHelper {
        public:

            static jobject createJPoint(JNIEnv *env, int x, int y);
            static jobjectArray createJPointArray(JNIEnv *env, jsize size);

        };
    }
}

