package com.rocklee.videolivedemo;

import android.hardware.display.DisplayManager;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.util.Log;
import android.view.Display;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

public class VideoCodec extends Thread {

    private MediaProjection mediaProjection;
    MediaCodec mediaCodec;
    private boolean isLiving;
    public void startLive(MediaProjection mediaProjection) {
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
        start();
    }

    @Override
    public void run() {
        //12
        mediaCodec.start();
        isLiving = true;
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        while (true) {
            //13
            ByteBuffer[] byteBuffer = mediaCodec.getOutputBuffers();
            int index = mediaCodec.dequeueOutputBuffer(bufferInfo, 100000);
            if (index >=0) {
                ByteBuffer buffer = byteBuffer[index];
                //14
                byte[] outData = new byte[bufferInfo.size];
                buffer.get(outData);
                //15
                FileUtils.writeBytes(outData);
                FileUtils.writeContent(outData);
                mediaCodec.releaseOutputBuffer(index, false);
            }
        }
    }
}
