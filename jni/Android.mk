LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := record-jni
LOCAL_SRC_FILES := opensl_example.c \
opensl_io.c 
# for native audio
LOCAL_LDLIBS    += -lOpenSLES 
# for logging
LOCAL_LDLIBS    += -llog
# for native asset manager
LOCAL_LDLIBS    += -landroid  

include $(BUILD_SHARED_LIBRARY)



