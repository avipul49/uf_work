#include <android/log.h>
#include "opensl_io.h"
#include <jni.h>
#include <sys/types.h>
#include <sys/socket.h>
#include "g72x/g72x.h"


#define BUFFERFRAMES 2048
#define VECSAMPS_MONO 1024
#define SR 44100

static int on;

JNIEnv* jniEnv;
static JavaVM* g_jvm;

jclass DataService;
jobject mRA;

jmethodID fromNative;

jshortArray out;

g726_state   g726State;
jboolean init(JNIEnv* env, jclass clazz) {
	jint jvm_result;

	if (jniEnv == 0) {
		jniEnv = env;

		__android_log_print(ANDROID_LOG_VERBOSE, "native-activity",
				"ENV initialization succeed");

	} else {
		__android_log_print(ANDROID_LOG_VERBOSE, "native-activity",
				"ENV initialization failed");
		return JNI_FALSE;
	}

	if (g_jvm == 0) {
		jvm_result = (*jniEnv)->GetJavaVM(jniEnv, &g_jvm);

		__android_log_print(ANDROID_LOG_VERBOSE, "native-activity",
				"JVM initialization succeed");

	} else {
		__android_log_print(ANDROID_LOG_VERBOSE, "native-activity",
				"JVM initialization failed");
		return JNI_FALSE;
	}

	if (DataService == 0) {
		DataService = (*jniEnv)->FindClass(jniEnv,
				"com/s3lab/guoguo/v1/DataService");
		DataService = (jclass)((*jniEnv)->NewGlobalRef(jniEnv, DataService));

		__android_log_print(ANDROID_LOG_VERBOSE, "native-activity",
				"class initialization succeed");
	} else {
		__android_log_print(ANDROID_LOG_VERBOSE, "native-activity",
				"class initialization failed");
		return JNI_FALSE;
	}

	if (mRA == 0) {
		jmethodID construction_id = (*jniEnv)->GetMethodID(jniEnv, DataService,
				"<init>", "()V");
		mRA = (*jniEnv)->NewObject(jniEnv, DataService, construction_id);

		__android_log_print(ANDROID_LOG_VERBOSE, "native-activity",
				"object initialization succeed");
	} else {
		__android_log_print(ANDROID_LOG_VERBOSE, "native-activity",
				"object initialization failed");
		return JNI_FALSE;
	}

	if (fromNative == 0) {
		fromNative = (*jniEnv)->GetStaticMethodID(jniEnv, DataService,
				"callback", "([F)V");
		__android_log_print(ANDROID_LOG_VERBOSE, "native-activity",
				"method initialization succeed");
	} else {
		__android_log_print(ANDROID_LOG_VERBOSE, "native-activity",
				"method initialization failed");
		return JNI_FALSE;
	}

	return JNI_TRUE;
}

void Java_com_s3lab_guoguo_v1_DataService_startProcess(JNIEnv *env,
		jclass clazz) {
	__android_log_print(ANDROID_LOG_DEBUG, "", "NDK:LC: [ve]");
	init(env, clazz);
	OPENSL_STREAM *p;
	int samps, i, j;
	float inbuffer[VECSAMPS_MONO];
	jfloat outbuffer[VECSAMPS_MONO];
	p = android_OpenAudioDevice(SR, 1, 2, BUFFERFRAMES);
	if (p == NULL)
		return;
	on = 1;
	while (on) {
		samps = android_AudioIn(p, inbuffer, VECSAMPS_MONO);

		j = 0;
		for (i = 0; i < samps; i++) {
			outbuffer[i] = inbuffer[i];
		}

		if (samps != 0) {
			jfloatArray result;
			result = (*env)->NewFloatArray(env, samps);
			(*env)->SetFloatArrayRegion(env, result, 0, samps, outbuffer);
			fromNative = (*env)->GetStaticMethodID(env, clazz, "callback",
					"([F)V");
			(*env)->CallStaticVoidMethod(env, clazz, fromNative, result);
			(*env)->DeleteLocalRef(env, result);
		}

	}
	android_CloseAudioDevice(p);
}

void Java_com_s3lab_guoguo_v1_DataService_stopProcess() {
	on = 0;
}

int Java_com_s3lab_guoguo_v1_DataService_nativeDoAudioEncode(JNIEnv* env,
		jclass class, jbyteArray pcmData, jint length, jbyteArray g726Data) {
	g726_init_state(&g726State);
	jboolean isCopy = JNI_TRUE;
	jbyte* audioFrame = (*env)->GetByteArrayElements(env,pcmData, NULL);
	jbyte* audioPacket = (*env)->GetByteArrayElements(env,g726Data, &isCopy);

	int j = 0,i=0;
	unsigned char byteCode = 0x00;
	short* pcmBuffer = (short*) audioFrame;
	for ( i = 0; i < length; i++) {
		short pcm = pcmBuffer[i];
		unsigned char code = g726_32_encoder(pcm, AUDIO_ENCODING_LINEAR,
				&g726State);

		if (i & 0x01) {
			byteCode = (code << 4) | (byteCode & 0x0f);
			audioPacket[j] = byteCode;
			j++;
		} else {
			byteCode = code;
		}
	}

	(*env)->ReleaseByteArrayElements(env,g726Data, audioPacket, 0);
	(*env)->ReleaseByteArrayElements(env,pcmData, audioFrame, JNI_ABORT);

	return j;
}
