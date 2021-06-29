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
    //sps
    int16_t sps_len;
    int8_t *sps;
    //pps
    int16_t pps_len;
    int8_t *pps;
}Live;
Live *live = NULL;

void prepareVideo(int8_t *data, int len, Live *live) {
    for (int i = 0; i < len; ++i) {
        if (i + 4 < len) {
            if (data[i] == 0x00 && data[i + 1] == 0x00
                && data[i + 2] == 0x00
                && data[i + 3] == 0x01) {
                if (data[i + 4] == 0x68) {
                    live->sps_len = i - 4;
                    live->sps = static_cast<int8_t *>(malloc(live->sps_len));
                    memcpy(live->sps, data + 4, live->sps_len);
                    live->pps_len = len - (4 + live->sps_len) - 4;
                    live->pps = static_cast<int8_t *>(malloc(live->pps_len));
                    memcpy(live->pps, data + 4 + live->sps_len + 4, live->pps_len);
                    LogD("sps:%d pps:%d", live->sps_len, live->pps_len);
                    break;
                }
            }
        }
    }
}

int sendPacket(RTMPPacket *packet) {
    int r = RTMP_SendPacket(live->rtmp, packet, 1);
    RTMPPacket_Free(packet);
    free(packet);
    return r;
}

RTMPPacket *createVideoPackage(int8_t *buf, int len, const long tms, Live *live) {
    buf += 4;
    RTMPPacket *packet = (RTMPPacket *)malloc(sizeof(RTMPPacket));
    int body_size = len + 9;
    RTMPPacket_Alloc(packet, body_size);

    if (buf[0] == 0x65) {
        packet->m_body[0] = 0x17;
        LogI("send IDR");
    } else {
        packet->m_body[0] = 0x27;
        LogI("send non IDR");
    }
    packet->m_body[1] = 0x01;
    packet->m_body[2] = 0x00;
    packet->m_body[3] = 0x00;
    packet->m_body[4] = 0x00;
    packet->m_body[5] = (len >> 24) & 0xFF;
    packet->m_body[6] = (len >> 16) & 0xFF;
    packet->m_body[7] = (len >> 8) & 0xFF;
    packet->m_body[8] = len & 0xFF;
    memcpy(&packet->m_body[9], buf, len);
    packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
    packet->m_nBodySize = body_size;
    packet->m_nChannel = 0x04;
    packet->m_nTimeStamp = tms;
    packet->m_hasAbsTimestamp = 0;
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
    packet->m_nInfoField2 = live->rtmp->m_stream_id;
    return packet;
}

RTMPPacket *createVideoPackage(Live *live) {
    int body_size = 16 + live->sps_len + live->pps_len;
    RTMPPacket *packet = (RTMPPacket *)malloc(sizeof(RTMPPacket));
    RTMPPacket_Alloc(packet, body_size);
    int i = 0;
    packet->m_body[i++] = 0x17;
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x01;
    packet->m_body[i++] = live->sps[1];
    packet->m_body[i++] = live->sps[2];
    packet->m_body[i++] = live->sps[3];
    packet->m_body[i++] = 0xFF;
    packet->m_body[i++] = 0xE1;
    //sps len
    packet->m_body[i++] = (live->sps_len >> 8) & 0xFF;
    packet->m_body[i++] = live->sps_len & 0xFF;
    //sps
    memcpy(&packet->m_body[i], live->sps, live->sps_len);
    i += live->sps_len;
    packet->m_body[i++] = 0x01;
    //pps len
    packet->m_body[i++] = (live->pps_len >> 8) & 0xFF;
    packet->m_body[i++] = live->pps_len & 0xFF;
    //pps
    memcpy(&packet->m_body[i], live->pps, live->pps_len);

    packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
    packet->m_nBodySize = body_size;
    packet->m_nChannel = 0x04;
    packet->m_nTimeStamp = 0;
    packet->m_hasAbsTimestamp = 0;
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
    packet->m_nInfoField2 = live->rtmp->m_stream_id;
    return packet;
}

int sendVideo(int8_t *buf, int len, long tms) {
    int ret = 0;
    int type = buf[4] & 0x1F;
    if (buf[4] == 0x67) {
        if (live && (!live->sps || !live->pps)) {
            prepareVideo(buf, len, live);
        }
        return ret;
    }
    if (buf[4] == 0x65) {
        RTMPPacket *packet = createVideoPackage(live);
        sendPacket(packet);
        LogD("send sps&pps");
    }
    RTMPPacket *packet2 = createVideoPackage(buf, len, tms, live);
    ret = sendPacket(packet2);
    return ret;
}

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
}extern "C"
JNIEXPORT jboolean JNICALL
Java_com_rocklee_videolivedemo_ScreenLive_sendData(JNIEnv *env, jobject thiz, jbyteArray data_,
                                                   jint len, jlong tms) {
    int ret;
    jbyte *data = env->GetByteArrayElements(data_, 0);
    ret = sendVideo(data, len, tms);
    env->ReleaseByteArrayElements(data_, data, 0);
    return ret;
}