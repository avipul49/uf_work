#include <assert.h>
#include <jni.h>
#include <string.h>
#include <stdio.h>
#include <fcntl.h>
#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>

#include <sys/types.h>

#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <string.h>
#include <netdb.h>
#include <sys/types.h>
#include <netinet/in.h>
#include <sys/socket.h>
#include <unistd.h>
#include <strings.h>
#include <pthread.h>
#include <android/log.h>

static SLObjectItf engineObject = NULL;
static SLEngineItf engineEngine;

static SLObjectItf outputMixObject = NULL;
static SLEnvironmentalReverbItf outputMixEnvironmentalReverb = NULL;

static SLObjectItf bqPlayerObject = NULL;
static SLPlayItf bqPlayerPlay;
static SLAndroidSimpleBufferQueueItf bqPlayerBufferQueue;
static SLEffectSendItf bqPlayerEffectSend;
static SLMuteSoloItf bqPlayerMuteSolo;
static SLVolumeItf bqPlayerVolume;

static const SLEnvironmentalReverbSettings reverbSettings =
		SL_I3DL2_ENVIRONMENT_PRESET_STONECORRIDOR;

static SLObjectItf recorderObject = NULL;
static SLRecordItf recorderRecord;
static SLAndroidSimpleBufferQueueItf recorderBufferQueue;

#define RECORDER_FRAMES 44100 //change from 44100 to 3445, for now let's just record shorts while converting thme into floats in the cellphone -D 10/25
static short recorderBuffer[RECORDER_FRAMES];
static unsigned recorderSize = 0;
static SLmilliHertz recorderSR;

static short *nextBuffer;
static unsigned nextSize;
static int nextCount;
//static SLmillisecond limit=40000;
static short Buffer[RECORDER_FRAMES];
static short process_buffer[RECORDER_FRAMES];
static pthread_mutex_t mutex1 = PTHREAD_MUTEX_INITIALIZER;

JNIEnv* jniEnv;
static JavaVM* g_jvm;

//jclass MainActivity;
//jclass DataService;
jclass DataService;
jobject mRA;

jmethodID fromNative;

jshortArray out;

jint JNI_OnLoad(JavaVM* vm, void* reserved) {
	JNIEnv* env = 0;
	jint result = -1;

	result = JNI_VERSION_1_4;
	return result;
}

void bqRecorderCallback(SLAndroidSimpleBufferQueueItf bq, void *context) {
	assert(bq == bqRecorderBufferQueue);
	assert(NULL == context);

	//** write to local files ********************
//    int result_code=mkdir("/mnt/sdcard/Android/record",0770);
//    FILE *fp=NULL;
//    fp=fopen("/mnt/sdcard/Android/record/fromnative.txt","w+");
//    long size=fwrite(Buffer,sizeof(short),RECORDER_FRAMES,fp);
//    fclose(fp);
	//**********************

	// transfer_data(Buffer);
//    __android_log_print(ANDROID_LOG_VERBOSE,"native-activity","ready to invoke");

	int attach_result;
	attach_result = (*g_jvm)->AttachCurrentThread(g_jvm, &jniEnv, 0);
//	__android_log_print(ANDROID_LOG_VERBOSE,"native-activity","thread attached");

	out = (*jniEnv)->NewShortArray(jniEnv, RECORDER_FRAMES);
	__android_log_print(ANDROID_LOG_VERBOSE, "native-activity",
			"-------- Cannot allocate JNI Byte Array");
	if (out == NULL) {
		__android_log_print(ANDROID_LOG_VERBOSE, "native-activity",
				"Cannot allocate JNI Byte Array");
		return;
	}

	(*jniEnv)->SetShortArrayRegion(jniEnv, out, 0, RECORDER_FRAMES, Buffer);
//    __android_log_print(ANDROID_LOG_VERBOSE,"native-activity","buffer copy done");

	(*jniEnv)->CallStaticVoidMethod(jniEnv, mRA, fromNative, out);
//    __android_log_print(ANDROID_LOG_VERBOSE,"native-activity","invoke done");

	int detach_result;
	detach_result = (*g_jvm)->DetachCurrentThread(g_jvm);

	SLresult result;
	result = (*recorderBufferQueue)->Enqueue(recorderBufferQueue, Buffer,
			(RECORDER_FRAMES) * sizeof(short));
	assert(SL_RESULT_SUCCESS == result);

}

void Java_com_s3lab_guoguo_v1_DataService_createEngine(JNIEnv* env,
		jclass clazz) {
	SLresult result;

	// create engine
	result = slCreateEngine(&engineObject, 0, NULL, 0, NULL, NULL);
	assert(SL_RESULT_SUCCESS == result);

	// realize the engine
	result = (*engineObject)->Realize(engineObject, SL_BOOLEAN_FALSE); //实现这个engine将会一直实现直到最后被终结
	assert(SL_RESULT_SUCCESS == result);

	// get the engine interface, which is needed in order to create other objects
	result = (*engineObject)->GetInterface(engineObject, SL_IID_ENGINE,
			&engineEngine); //创作出了一个 engineEngine的实例这个用来创造出其他API的
	assert(SL_RESULT_SUCCESS == result);

	// create output mix, with environmental reverb specified as a non-required interface
	const SLInterfaceID ids[1] = { SL_IID_ENVIRONMENTALREVERB };
	const SLboolean req[1] = { SL_BOOLEAN_FALSE };
	result = (*engineEngine)->CreateOutputMix(engineEngine, &outputMixObject, 1,
			ids, req); //通过engineEngine 实现了一个outputMixObject.
	assert(SL_RESULT_SUCCESS == result);

	// realize the output mix
	result = (*outputMixObject)->Realize(outputMixObject, SL_BOOLEAN_FALSE); //实现这个engine 一直到最后。
	assert(SL_RESULT_SUCCESS == result);

	// get the environmental reverb interface
	// this could fail if the environmental reverb effect is not available,
	// either because the feature is not present, excessive CPU load, or
	// the required MODIFY_AUDIO_SETTINGS permission was not requested and granted
	result = (*outputMixObject)->GetInterface(outputMixObject,
			SL_IID_ENVIRONMENTALREVERB, &outputMixEnvironmentalReverb); //得到reverb的object 然后设定object.
	if (SL_RESULT_SUCCESS == result) {
		result =
				(*outputMixEnvironmentalReverb)->SetEnvironmentalReverbProperties(
						outputMixEnvironmentalReverb, &reverbSettings);
	}

}

jboolean Java_com_s3lab_guoguo_v1_DataService_createAudioRecorder(JNIEnv* env,
		jclass clazz) {
	SLresult result;

	// configure audio source
	SLDataLocator_IODevice loc_dev = { SL_DATALOCATOR_IODEVICE,
			SL_IODEVICE_AUDIOINPUT, SL_DEFAULTDEVICEID_AUDIOINPUT, NULL };
	SLDataSource audioSrc = { &loc_dev, NULL };

	// configure audio sink
	SLDataLocator_AndroidSimpleBufferQueue loc_bq = {
			SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE, 2 };
	SLDataFormat_PCM format_pcm = { SL_DATAFORMAT_PCM, 1, SL_SAMPLINGRATE_44_1,
			SL_PCMSAMPLEFORMAT_FIXED_16, SL_PCMSAMPLEFORMAT_FIXED_16,
			SL_SPEAKER_FRONT_CENTER, SL_BYTEORDER_LITTLEENDIAN };
	SLDataSink audioSnk = { &loc_bq, &format_pcm };

	// create audio recorder
	// (requires the RECORD_AUDIO permission)
	const SLInterfaceID id[1] = { SL_IID_ANDROIDSIMPLEBUFFERQUEUE };
	const SLboolean req[1] = { SL_BOOLEAN_TRUE };
	result = (*engineEngine)->CreateAudioRecorder(engineEngine, &recorderObject,
			&audioSrc, &audioSnk, 1, id, req);
	if (SL_RESULT_SUCCESS != result) {
		return JNI_FALSE;
	}
	result = (*recorderObject)->Realize(recorderObject, SL_BOOLEAN_FALSE);
	if (SL_RESULT_SUCCESS != result) {
		return JNI_FALSE;
	}

	// get the record interface
	result = (*recorderObject)->GetInterface(recorderObject, SL_IID_RECORD,
			&recorderRecord);
	assert(SL_RESULT_SUCCESS == result);

	// get the buffer queue interface
	result = (*recorderObject)->GetInterface(recorderObject,
			SL_IID_ANDROIDSIMPLEBUFFERQUEUE, &recorderBufferQueue);
	assert(SL_RESULT_SUCCESS == result);
	//result=(*recorderBufferQueue)->RegisterCallback(recorderBufferQueue,transfer_data,NULL);

	// register callback on the buffer queue
	result = (*recorderBufferQueue)->RegisterCallback(recorderBufferQueue,
			bqRecorderCallback, NULL);
	assert(SL_RESULT_SUCCESS == result);

	return JNI_TRUE;
}

void Java_com_s3lab_guoguo_v1_DataService_recordjni(JNIEnv* env, jclass clazz) {
	SLresult result;
	SLuint32 State;

//    jint rr = 100;

//    return rr;

//	char buffer[5];
//	bufffer = "test";
//	return (*env)->NewStringUTF(env,buffer);
//    return 88888;
	result = (*recorderRecord)->SetRecordState(recorderRecord,
			SL_RECORDSTATE_RECORDING);
	assert(SL_RESULT_SUCCESS == result);

}

void Java_com_s3lab_guoguo_v1_DataService_clear(JNIEnv* env, jclass clazz) {
	SLresult result;
	result = (*recorderRecord)->SetRecordState(recorderRecord,
			SL_RECORDSTATE_STOPPED);
	assert(SL_RESULT_SUCESS == result);
	result = (*recorderBufferQueue)->Clear(recorderBufferQueue);
	assert(SL_RESULT_SUCESS == result);
	// result = (*recorderBufferQueue)->Enqueue(recorderBufferQueue, recorderBuffer,RECORDER_FRAMES * sizeof(short));
	result = (*recorderBufferQueue)->Enqueue(recorderBufferQueue, Buffer,
			(RECORDER_FRAMES) * sizeof(short));
	assert(SL_RESULT_SUCESS == result);
	//result=(*recorderRecord)->SetDurationLimit(recorderRecord,1000);
	//assert(SL_RESULT_SUCESS==result);

}

void Java_com_s3lab_guoguo_v1_DataService_stop(JNIEnv* env, jclass clazz) {

	SLresult result;
	result = (*recorderRecord)->SetRecordState(recorderRecord,
			SL_RECORDSTATE_STOPPED);

	assert(SL_RESULT_SUCCESS == result);

}

void Java_com_s3lab_guoguo_v1_DataService_shutdown(JNIEnv* env, jclass clazz) {

	// destroy buffer queue audio player object, and invalidate all associated interfaces
	if (bqPlayerObject != NULL) {
		(*bqPlayerObject)->Destroy(bqPlayerObject);
		bqPlayerObject = NULL;
		bqPlayerPlay = NULL;
		bqPlayerBufferQueue = NULL;
		bqPlayerEffectSend = NULL;
		bqPlayerMuteSolo = NULL;
		bqPlayerVolume = NULL;
	}

	// destroy file descriptor audio player object, and invalidate all associated interfaces

	if (recorderObject != NULL) {
		(*recorderObject)->Destroy(recorderObject);
		recorderObject = NULL;
		recorderRecord = NULL;
		recorderBufferQueue = NULL;
	}

	// destroy output mix object, and invalidate all associated interfaces
	if (outputMixObject != NULL) {
		(*outputMixObject)->Destroy(outputMixObject);
		outputMixObject = NULL;
		outputMixEnvironmentalReverb = NULL;
	}

	// destroy engine object, and invalidate all associated interfaces
	if (engineObject != NULL) {
		(*engineObject)->Destroy(engineObject);
		engineObject = NULL;
		engineEngine = NULL;
	}

	(*jniEnv)->DeleteGlobalRef(jniEnv, out);
	(*jniEnv)->DeleteGlobalRef(jniEnv, DataService);

}

//void Java_com_pack_Record_RecordActivity_writefile(JNIEnv *env,jclass jclazz)
//{
//int result_code=mkdir("/mnt/sdcard/Android/record",0770);
//FILE *fp=NULL;
//fp=fopen("/mnt/sdcard/Android/record/record_4s.txt","a+");
//long size=fwrite(Buffer,sizeof(short),44100,fp);
//fclose(fp);

//}

jboolean Java_com_s3lab_guoguo_v1_DataService_init(JNIEnv* env, jclass clazz) {
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
				"fromNative", "([S)V");

		__android_log_print(ANDROID_LOG_VERBOSE, "native-activity",
				"method initialization succeed");
	} else {
		__android_log_print(ANDROID_LOG_VERBOSE, "native-activity",
				"method initialization failed");
		return JNI_FALSE;
	}

	if (out == 0) {
		out = (*jniEnv)->NewShortArray(jniEnv, RECORDER_FRAMES);
		out = (jshortArray)((*jniEnv)->NewGlobalRef(jniEnv, out));

		__android_log_print(ANDROID_LOG_VERBOSE, "native-activity",
				"out initialization succeed");
	} else {
		__android_log_print(ANDROID_LOG_VERBOSE, "native-activity",
				"out initialization failed");
		return JNI_FALSE;
	}
	return JNI_TRUE;
}
