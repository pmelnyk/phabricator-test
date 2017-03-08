# Sets the minimum version of CMake required to build the native
# library. You should either keep the default value or only pass a
# value of 3.4.0 or lower.

cmake_minimum_required(VERSION 3.4.1)

SET(CMAKE_VERBOSE_MAKEFILE "ON")

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds it for you.
# Gradle automatically packages shared libraries with your APK.

set(openalpr_dir ${CMAKE_SOURCE_DIR}/jni_openalpr/${ANDROID_ABI})


# alpr
add_library(lib_openalpr SHARED IMPORTED)
set_target_properties(lib_openalpr PROPERTIES IMPORTED_LOCATION ${openalpr_dir}/libopenalpr.so)

find_library(libz z)
find_library(liblog log)


set(src src/main/cpp/)

add_library( # Sets the name of the library.
             openalpr_jni

             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
             # Associated headers in the same location as their source
             # file are automatically included.
             ${src}/utils_jni.cpp
             ${src}/openalpr_jni.cpp
             ${src}/lanedetector_jni.cpp
             ${src}/lanedetector.cpp
             ${src}/jobject_helper.cpp)


# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in the
# build script, prebuilt third-party libraries, or system libraries.


target_include_directories(openalpr_jni PRIVATE
                           ${CMAKE_SOURCE_DIR}/include/openalpr
                           ${CMAKE_SOURCE_DIR}/include/tesseract
                           ${CMAKE_SOURCE_DIR}/include/opencv)

target_link_libraries( # Specifies the target library.
                       openalpr_jni
                       lib_openalpr
                       ${libz}
                       ${liblog}
                       )
