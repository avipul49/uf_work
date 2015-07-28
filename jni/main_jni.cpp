#ifdef ANDROID
#include <jni.h>
#include "helper.h"
#include "h264encoder.h"
extern "C" {
	#include "opensl_example.h"
}

#undef JNIEXPORT
#define JNIEXPORT __attribute__((visibility("default")))
#define JOW(rettype, name) extern "C" rettype JNIEXPORT JNICALL \
		Java_com_s3lab_guoguo_v1_DataService_##name

H264Encoder* myAVC = NULL;

JOW(void, nativeInitMediaEncoder)(JNIEnv* env, jclass, jint width, jint height) {
    myAVC = new H264Encoder(width, height);
    return;
};

JOW(void, nativeReleaseMediaEncoder)(JNIEnv* env, jclass) {
    delete myAVC;

    return;
};

JOW(void, startProcess)(JNIEnv* env, jclass clazz) {
	start_process(env,clazz);
}

JOW(void, stopProcess)(JNIEnv* env, jclass clazz) {
	stop_process();
}


JOW(int, nativeDoVideoEncode)(JNIEnv* env, jclass, jbyteArray _yuvData, jbyteArray _nalsData, jint flag) {
    jboolean isCopy = JNI_TRUE;
    jbyte* yuvData = env->GetByteArrayElements(_yuvData, NULL);
    jbyte* nalsData = env->GetByteArrayElements(_nalsData, &isCopy);

    int ret = myAVC->doEncode((unsigned char*)yuvData, (unsigned char*)nalsData, flag);

    env->ReleaseByteArrayElements(_nalsData, nalsData, 0);
    env->ReleaseByteArrayElements(_yuvData, yuvData, JNI_ABORT);

    return ret;
}



extern "C" jint JNIEXPORT JNICALL JNI_OnLoad(JavaVM *jvm, void *reserved) {
    JNIEnv* jni;
    if (jvm->GetEnv(reinterpret_cast<void**>(&jni), JNI_VERSION_1_6) != JNI_OK)
        return -1;
    return JNI_VERSION_1_6;
}

extern "C" jint JNIEXPORT JNICALL JNI_OnUnLoad(JavaVM *jvm, void *reserved) {
    return 0;
}

#endif
