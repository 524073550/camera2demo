#include "../jniLibs/include/com_ke_zhu_camerademo_JniUtils.h"
#include "../jniLibs/include/libyuv.h"
#include "DebugLog.h"
extern "C"{
#include "libavformat/avformat.h"
#include "libavcodec/avcodec.h"
#include "libavfilter/avfilter.h"
#include "libswscale/swscale.h"
#include "libavutil/avutil.h"
#include "libpostproc/postprocess.h"
#include "libswresample/swresample.h"
}


void rotateI420(jbyte *src_i420_data, jint width, jint height, jbyte *dst_i420_data, jint degree) {
    jint src_i420_y_size = width * height;
    jint src_i420_u_size = (width >> 1) * (height >> 1);

    jbyte *src_i420_y_data = src_i420_data;
    jbyte *src_i420_u_data = src_i420_data + src_i420_y_size;
    jbyte *src_i420_v_data = src_i420_data + src_i420_y_size + src_i420_u_size;

    jbyte *dst_i420_y_data = dst_i420_data;
    jbyte *dst_i420_u_data = dst_i420_data + src_i420_y_size;
    jbyte *dst_i420_v_data = dst_i420_data + src_i420_y_size + src_i420_u_size;

    //要注意这里的width和height在旋转之后是相反的
    if (degree == libyuv::kRotate90 || degree == libyuv::kRotate270) {
        libyuv::I420Rotate((const uint8_t *) src_i420_y_data, width,
                           (const uint8_t *) src_i420_u_data, width >> 1,
                           (const uint8_t *) src_i420_v_data, width >> 1,
                           (uint8_t *) dst_i420_y_data, height,
                           (uint8_t *) dst_i420_u_data, height >> 1,
                           (uint8_t *) dst_i420_v_data, height >> 1,
                           width, height,
                           (libyuv::RotationMode) degree);
    } else {
        libyuv::I420Rotate((const uint8_t *) src_i420_y_data, width,
                           (const uint8_t *) src_i420_u_data, width >> 1,
                           (const uint8_t *) src_i420_v_data, width >> 1,
                           (uint8_t *) dst_i420_y_data, width,
                           (uint8_t *) dst_i420_u_data, width >> 1,
                           (uint8_t *) dst_i420_v_data, width >> 1,
                           width, height,
                           (libyuv::RotationMode) degree);
    }
}

void mirrorI420(jbyte *src_i420_data, jint width, jint height, jbyte *dst_i420_data) {

    jint i420_src_y_size = width * height;
    jint i420_src_u_size = (width >> 1) * (height >> 1);

    jbyte *i420_src_y_data = src_i420_data;
    jbyte *i420_src_u_data = src_i420_data + i420_src_y_size;
    jbyte *i420_src_v_data = src_i420_data + i420_src_y_size + i420_src_u_size;

    jbyte *i420_dst_y_data = dst_i420_data;
    jbyte *i420_dst_u_data = dst_i420_data + i420_src_y_size;
    jbyte *i420_dst_v_data = dst_i420_data + i420_src_y_size + i420_src_u_size;

    libyuv::I420Mirror((const uint8_t *) i420_src_y_data, width,
                       (const uint8_t *) i420_src_u_data, width >> 1,
                       (const uint8_t *) i420_src_v_data, width >> 1,
                       (uint8_t *) i420_dst_y_data, width,
                       (uint8_t *) i420_dst_u_data, width >> 1,
                       (uint8_t *) i420_dst_v_data, width >> 1,
                       width, height);

}


void I420ToNV21(jbyte *src_i420_data, jint width, jint height, jbyte *dst_nv21_data) {


    jint src_y_size = width * height;
    jint src_u_size = (width >> 1) * (height >> 1);

    jbyte *src_i420_y_data = src_i420_data;
    jbyte *src_i420_u_data = src_i420_data + src_y_size;
    jbyte *src_i420_v_data = src_i420_data + src_y_size + src_u_size;

    jbyte *src_nv21_y_data = dst_nv21_data;
    jbyte *src_nv21_vu_data = dst_nv21_data + src_y_size;

    libyuv::I420ToNV21(
            (const uint8_t *) src_i420_y_data, width,
            (const uint8_t *) src_i420_u_data, width >> 1,
            (const uint8_t *) src_i420_v_data, width >> 1,
            (uint8_t *) src_nv21_y_data, width,
            (uint8_t *) src_nv21_vu_data, width,
            width, height);
}




extern "C"
JNIEXPORT void JNICALL
Java_com_ke_zhu_camerademo_JniUtils_nv21ToI420(JNIEnv *env, jclass clazz, jbyteArray srcY,
                                               jbyteArray srcVU, jint width, jint height,
                                               jbyteArray dst) {
    jint src_y_size = width * height;
    jint src_u_size = (width >> 1) * (height >> 1);
    jbyte *src_i420_data = env->GetByteArrayElements(dst, NULL);

    jbyte *srcYData = env->GetByteArrayElements(srcY, NULL);
    jbyte *srcUVData = env->GetByteArrayElements(srcVU, NULL);

    jbyte *src_i420_y_data = src_i420_data;
    jbyte *src_i420_u_data = src_i420_data + src_y_size;
    jbyte *src_i420_v_data = src_i420_data + src_y_size + src_u_size;

    libyuv::NV21ToI420((const uint8_t *) srcYData, width,
                       (const uint8_t *) srcUVData, width,
                       (uint8_t *) src_i420_y_data, width,
                       (uint8_t *) src_i420_u_data, width >> 1,
                       (uint8_t *) src_i420_v_data, width >> 1,
                       width, height);

    env->ReleaseByteArrayElements(srcY, srcYData, 0);
    env->ReleaseByteArrayElements(srcVU, srcUVData, 0);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_ke_zhu_camerademo_JniUtils_compressYUV(JNIEnv *env, jclass clazz, jbyteArray src,
                                                jint width, jint height, jbyteArray dst,
                                                jint dst_width, jint dst_height, jint mode,
                                                jint degree, jboolean is_mirror) {
    // TODO: implement compressYUV()
}

extern "C"
JNIEXPORT void JNICALL
Java_com_ke_zhu_camerademo_JniUtils_cropYUV(JNIEnv *env, jclass clazz, jbyteArray src, jint width,
                                            jint height, jbyteArray dst, jint dst_width,
                                            jint dst_height, jint left, jint top) {
    // TODO: implement cropYUV()
}

extern "C"
JNIEXPORT void JNICALL
Java_com_ke_zhu_camerademo_JniUtils_yuvI420ToNV21(JNIEnv *env, jclass clazz, jbyteArray i420_src,
                                                  jbyteArray nv21_src, jint width, jint height) {
    jbyte *src_i420 = env->GetByteArrayElements(i420_src, NULL);
    jbyte *dst_nv21 = env->GetByteArrayElements(nv21_src, NULL);
    I420ToNV21(src_i420, width, height, dst_nv21);
    env->ReleaseByteArrayElements(i420_src, src_i420, 0);
    env->ReleaseByteArrayElements(nv21_src, dst_nv21, 0);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_ke_zhu_camerademo_JniUtils_scale(JNIEnv *env, jclass clazz, jbyteArray src, jint width,
                                          jint height, jbyteArray dst, jint dst_width,
                                          jint dst_height) {
    // TODO: implement scale()
}



extern "C"
JNIEXPORT void JNICALL
Java_com_ke_zhu_camerademo_JniUtils_I420Rotate(JNIEnv *env, jclass clazz, jbyteArray src,
                                               jint width, jint height, jbyteArray dst,
                                               jint degree, jboolean isMirror) {


    jbyte *srcI420Data = env->GetByteArrayElements(src, NULL);
    jbyte *dstI420Data = env->GetByteArrayElements(dst, NULL);
    if (isMirror) {
        jbyte *Src_i420_data_rotate = (jbyte *) malloc(sizeof(jbyte) * width * height * 3 / 2);
        mirrorI420(srcI420Data, width, height, Src_i420_data_rotate);
        rotateI420(Src_i420_data_rotate, width, height, dstI420Data, degree);
        free(Src_i420_data_rotate);
    } else {
        rotateI420(srcI420Data, width, height, dstI420Data, degree);
    }

    env->ReleaseByteArrayElements(dst, dstI420Data, 0);
}
void I420ToNV12(jbyte *src_i420_data, jint width, jint height, jbyte *dst_nv12_data) {


    jint src_y_size = width * height;
    jint src_u_size = (width >> 1) * (height >> 1);

    jbyte *src_i420_y_data = src_i420_data;
    jbyte *src_i420_u_data = src_i420_data + src_y_size;
    jbyte *src_i420_v_data = src_i420_data + src_y_size + src_u_size;

    jbyte *src_nv21_y_data = dst_nv12_data;
    jbyte *src_nv21_uv_data = dst_nv12_data + src_y_size;

    libyuv::I420ToNV12(
            (const uint8_t *) src_i420_y_data, width,
            (const uint8_t *) src_i420_u_data, width >> 1,
            (const uint8_t *) src_i420_v_data, width >> 1,
            (uint8_t *) src_nv21_y_data, width,
            (uint8_t *) src_nv21_uv_data, width,
            width, height);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_ke_zhu_camerademo_JniUtils_I420ToNV12(JNIEnv *env, jclass clazz, jbyteArray src,
                                               jint width, jint height, jbyteArray dst) {
    jint src_i420_y_size = width*height;
    jint src_i420_u_size = (width>>1)*(height>>1);


    jbyte *src_i420_data = env->GetByteArrayElements(src, NULL);
    jbyte *src_nv12_data = env->GetByteArrayElements(dst, NULL);

    jbyte *src_i420_y_data = src_i420_data;
    jbyte *src_i420_u_data = src_i420_data+src_i420_y_size;
    jbyte *src_i420_v_data = src_i420_data+src_i420_y_size+src_i420_u_size;


    jbyte *src_nv12_y_data = src_nv12_data;
    jbyte *src_nv12_uv_data = src_nv12_data+src_i420_y_size;

    libyuv::I420ToNV12((const uint8_t*)src_i420_y_data,width,
                       (const uint8_t*)src_i420_u_data,width>>1,
                       (const uint8_t*)src_i420_v_data,width>>1,
                       ( uint8_t*)src_nv12_y_data,width,
                       ( uint8_t*)src_nv12_uv_data,width,
                       width,height);

//    I420ToNV12(src_i420_data, width, height, src_nv12_data);
    env->ReleaseByteArrayElements(src, src_i420_data, 0);
    env->ReleaseByteArrayElements(dst, src_nv12_data, 0);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_ke_zhu_camerademo_JniUtils_getFfmpegInfo(JNIEnv *env, jclass clazz) {

    av_register_all();
    AVInputFormat *if_temp = av_iformat_next(NULL);
    AVOutputFormat *of_temp = av_oformat_next(NULL);
    while (if_temp != NULL) {
        LOGE( "%sInput: ", if_temp->name);
        if_temp = if_temp->next;
    }
    while (of_temp != NULL) {
        LOGE( "%sOutput:", of_temp->name);
        of_temp = of_temp->next;
    }
}


