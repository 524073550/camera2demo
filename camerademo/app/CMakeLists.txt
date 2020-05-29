include_directories(src/main/jniLibs/include)

cmake_minimum_required(VERSION 3.4.1)

set(distribution_DIR ${CMAKE_SOURCE_DIR}/src/main/jniLibs/${ANDROID_ABI})

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
        ${distribution_DIR}/libyuv_static.so)

add_library(
        libavcodec
        SHARED
        IMPORTED)
set_target_properties(
        libavcodec
        PROPERTIES IMPORTED_LOCATION
        ${distribution_DIR}/libavcodec.so)

add_library(
        libavfilter
        SHARED
        IMPORTED)
set_target_properties(
        libavfilter
        PROPERTIES IMPORTED_LOCATION
        ${distribution_DIR}/libavfilter.so)
add_library(
        libavformat
        SHARED
        IMPORTED)
set_target_properties(
        libavformat
        PROPERTIES IMPORTED_LOCATION
        ${distribution_DIR}/libavformat.so)
add_library(
        libavutil
        SHARED
        IMPORTED)
set_target_properties(
        libavutil
        PROPERTIES IMPORTED_LOCATION
        ${distribution_DIR}/libavutil.so)
add_library(
        libswresample
        SHARED
        IMPORTED)
set_target_properties(
        libswresample
        PROPERTIES IMPORTED_LOCATION
        ${distribution_DIR}/libswresample.so)

add_library(
        libswscale
        SHARED
        IMPORTED)
set_target_properties(
        libswscale
        PROPERTIES IMPORTED_LOCATION
        ${distribution_DIR}/libswscale.so)

add_library(
        libpostproc
        SHARED
        IMPORTED)
set_target_properties(
        libpostproc
        PROPERTIES IMPORTED_LOCATION
        ${distribution_DIR}/libpostproc.so)

find_library(
        log-lib
        log)

target_link_libraries(
        jni_demo
        yuv_static
        libavcodec
        libavfilter
        libavformat
        libavutil
        libswresample
        libswscale
        libpostproc
        ${log-lib})