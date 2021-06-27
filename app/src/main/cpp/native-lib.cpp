#include <jni.h>
#include <string>
#include <android/log.h>
extern "C"{
#include "librtmp/rtmp.h"
}

#ifndef LOG_TAG
#define LOG_TAG "liveDemo"
#define LogI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LogD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LogE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#endif

typedef struct {
    RTMP *rtmp;
}Live;
Live *live = NULL;

extern "C" JNIEXPORT jstring JNICALL
Java_com_rocklee_videolivedemo_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}extern "C"
JNIEXPORT jboolean JNICALL
Java_com_rocklee_videolivedemo_ScreenLive_connect(JNIEnv *env, jobject thiz, jstring url_) {
    int ret = 0;
    const char *url = env->GetStringUTFChars(url_, 0);
    do {
        live = (Live*)malloc(sizeof(Live));
        memset(live, 0, sizeof(Live));
        live->rtmp = RTMP_Alloc();
        RTMP_Init(live->rtmp);
        live->rtmp->Link.timeout = 10;
        LogI("connect %s", url);
        if (!(ret = RTMP_SetupURL(live->rtmp, (char *)url))) {
            break;
        }
        RTMP_EnableWrite(live->rtmp);
        LogI("RTMP_Connect");
        if (!(ret = RTMP_Connect(live->rtmp, 0))) {
            break;
        }
        LogI("RTMP_ConnectStream");
        if (!(ret = RTMP_ConnectStream(live->rtmp, 0))) {
            break;
        }
        LogI("connect success");
    } while (0);

    if (!ret && live) {
        free(live);
        live = nullptr;
    }

    env->ReleaseStringUTFChars(url_, url);
    return ret;
}