package com.rocklee.videolivedemo;

import android.hardware.display.DisplayManager;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.Display;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class VideoCodec extends Thread {
    private static final String TAG = "VideoCodec";
    private MediaProjection mediaProjection;
    MediaCodec mediaCodec;
    private String filePath;
    private boolean isLiving;
    private long startTime;
    private ScreenLive screenLive;

    public VideoCodec(String path, ScreenLive screenLive) {
        this.filePath = path;
        this.screenLive = screenLive;
    }

    private static boolean isRecognizedFormat(int colorFormat) {
        switch (colorFormat) {
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface :
                return true;
            default:
                return false;
        }
    }

    private void getMediaCodecInfo() {
        MediaCodecList list = new MediaCodecList(MediaCodecList.REGULAR_CODECS);
        MediaCodecInfo[] supportCodes = list.getCodecInfos();
        Log.i(TAG, "解码器列表：");
        for (MediaCodecInfo codec : supportCodes) {
            if (!codec.isEncoder()) {
                String name = codec.getName();
                if (name.startsWith("OMX.google")) {
                    Log.i(TAG, "软解->" + name);
                }
            }
        }
        for (MediaCodecInfo codec : supportCodes) {
            if (!codec.isEncoder()) {
                String name = codec.getName();
                if (!name.startsWith("OMX.google")) {
                    Log.i(TAG, "硬解->" + name);
                }
            }
        }
        Log.i(TAG, "编码器列表：");
        for (MediaCodecInfo codec : supportCodes) {
            if (codec.isEncoder()) {
                String name = codec.getName();
                if (name.startsWith("OMX.google")) {
                    Log.i(TAG, "软编->" + name);
                }
            }
        }
        for (MediaCodecInfo codec : supportCodes) {
            if (codec.isEncoder()) {
                String name = codec.getName();
                if (!name.startsWith("OMX.google")) {
                    Log.i(TAG, "硬编->" + name);
                }
                for (String type : codec.getSupportedTypes()) {
                    Log.i(TAG, "type->" + type);
                    if (type.equalsIgnoreCase(MediaFormat.MIMETYPE_VIDEO_HEVC)) {
                        MediaCodecInfo.CodecCapabilities cap = codec.getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_HEVC);
                        MediaCodecInfo.VideoCapabilities vCap = cap.getVideoCapabilities();
                        Size supportedSize = new Size(vCap.getSupportedWidths().getUpper(), vCap.getSupportedHeights().getUpper());
                        Log.i(TAG, "HEVC encoder=\"" + codec.getName() + "\""
                                + " supported-size=" + supportedSize
                                + " color-formats=" + Arrays.toString(cap.colorFormats)
                        );
                        for (int i = 0; i < cap.colorFormats.length; i++) {
                            int colorFormat = cap.colorFormats[i];
                            if (isRecognizedFormat(colorFormat)) {
                                Log.i(TAG,"supported color");
                            }
                        }
                    }
                    if (type.equalsIgnoreCase(MediaFormat.MIMETYPE_VIDEO_AVC)) {
                        MediaCodecInfo.CodecCapabilities cap = codec.getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_AVC);
                        MediaCodecInfo.VideoCapabilities vCap = cap.getVideoCapabilities();
                        Size supportedSize = new Size(vCap.getSupportedWidths().getUpper(), vCap.getSupportedHeights().getUpper());
                        Log.i(TAG, "AVC encoder=\"" + codec.getName() + "\""
                                + " supported-size=" + supportedSize
                                + " color-formats=" + Arrays.toString(cap.colorFormats)
                        );
                        for (int i = 0; i < cap.colorFormats.length; i++) {
                            int colorFormat = cap.colorFormats[i];
                            if (isRecognizedFormat(colorFormat)) {
                                Log.i(TAG,"supported color");
                            }
                        }
                    }
                }
            }
        }
    }

    public void startLive(MediaProjection mediaProjection) {
        //getMediaCodecInfo();
        //5
        this.mediaProjection = mediaProjection;
        try {
            //6
            mediaCodec = MediaCodec.createEncoderByType("video/avc");
            MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC,
                    720,
                    1280);
            //7
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            format.setInteger(MediaFormat.KEY_BIT_RATE, 400_000);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, 15);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2);

            //8
            mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            //9
            Surface surface = mediaCodec.createInputSurface();
            //10
            mediaProjection.createVirtualDisplay("screenCodec", 720, 1280, 1,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC, surface, null, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //11
        isLiving = true;
        start();
    }

    private long timeStamp;
    @Override
    public void run() {
        //12
        mediaCodec.start();
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        Log.i(TAG, "filePath is " + filePath);
        while (isLiving) {
            //手动触发I帧
            if (System.currentTimeMillis() - timeStamp >= 2000) {
                Bundle params = new Bundle();
                params.putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0);
                mediaCodec.setParameters(params);
                timeStamp = System.currentTimeMillis();
            }

            //13
            ByteBuffer[] byteBuffer = mediaCodec.getOutputBuffers();
            int index = mediaCodec.dequeueOutputBuffer(bufferInfo, 100000);
            if (index >=0) {
                if (startTime == 0) {
                    startTime = bufferInfo.presentationTimeUs / 1000;
                }
                ByteBuffer buffer = byteBuffer[index];
                //14
                byte[] outData = new byte[bufferInfo.size];
                buffer.get(outData);
                //15 check h264 data
                //FileUtils.writeBytes(filePath, outData);
                //FileUtils.writeContent(filePath, outData);
                RTMPPackage rtmpPackage = new RTMPPackage(outData, (bufferInfo.presentationTimeUs/1000)-startTime);
                screenLive.addPackage(rtmpPackage);

                mediaCodec.releaseOutputBuffer(index, false);
            }
        }
    }

    public void stopLive() {
        isLiving = false;
    }
}
