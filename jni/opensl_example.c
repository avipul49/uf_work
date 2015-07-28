#include "opensl_example.h"
#include <android/log.h>
#include "opensl_io.h"

#include <sys/types.h>
#include <sys/socket.h>

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


void start_process(JNIEnv *env,
		jclass clazz) {
	__android_log_print(ANDROID_LOG_DEBUG, "", "NDK:LC: [ve]");
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

void stop_process() {
	on = 0;
}
