# Sets the minimum version of CMake required to build the native
# library. You should either keep the default value or only pass a
# value of 3.4.0 or lower.

cmake_minimum_required(VERSION 3.4.1)

SET(CMAKE_VERBOSE_MAKEFILE "ON")

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds it for you.
# Gradle automatically packages shared libraries with your APK.

find_library(libz z)
find_library(liblog log)

set(src src/main/cpp/)

add_library( # Sets the name of the library.
             utils_jni

             # Sets the library as a shared library.
             STATIC

             # Provides a relative path to your source file(s).
             # Associated headers in the same location as their source
             # file are automatically included.
             ${src}/utils_jni.cpp
             )


set_target_properties(utils_jni
                      PROPERTIES
                      LIBRARY_OUTPUT_DIRECTORY
                      "${distribution_DIR}/lib/${ANDROID_ABI}")

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in the
# build script, prebuilt third-party libraries, or system libraries.


target_link_libraries( # Specifies the target library.
                       utils_jni
                       ${liblog}
                       )
