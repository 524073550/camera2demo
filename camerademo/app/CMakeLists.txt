include_directories(src/main/jniLibs/include)

cmake_minimum_required(VERSION 3.4.1)

add_library(
        jni_demo
        SHARED
        src/main/jni/com_ke_zhu_camerademo_JniUtils.cpp)

add_library(
        yuv_static
        SHARED
        IMPORTED)
set_target_properties(
        yuv_static
        PROPERTIES IMPORTED_LOCATION
        ../../../../src/main/jniLibs/arm64-v8a/libyuv_static.so)

find_library(
        log-lib
        log)

target_link_libraries(
        jni_demo
        yuv_static

        ${log-lib})