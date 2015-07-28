#include <jni.h>
#ifdef __cplusplus
extern "C" {
#endif
  void start_process(JNIEnv *env,
			jclass clazz);
  void stop_process();
#ifdef __cplusplus
};
#endif
