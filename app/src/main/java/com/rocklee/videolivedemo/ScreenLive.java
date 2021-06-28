package com.rocklee.videolivedemo;

import android.media.projection.MediaProjection;
import android.util.Log;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;

public class ScreenLive extends Thread {
    private static final String TAG = "ScreenLive";
    VideoCodec videoCodec;
    private String url;
    private String videoFilePath;
    private MediaProjection mediaProjection;
    private boolean isLiving;
    private LinkedBlockingQueue<RTMPPackage> queue = new LinkedBlockingQueue<>();
    public void startLive(String url, MediaProjection mediaProjection) {
        this.url = url;
        this.mediaProjection = mediaProjection;
        //3
        start();
    }

    public ScreenLive(String path) {
        this.videoFilePath = path;
    }

    public void addPackage(RTMPPackage rtmpPackage) {
        if (!isLiving) {
            return;
        }
        queue.add(rtmpPackage);
    }

    @Override
    public void run() {
        //16
        if (!connect(url)) {
            Log.e(TAG, "run: ---->fail!");
        }
        isLiving = true;

        videoCodec = new VideoCodec(videoFilePath, this);
        //4
        videoCodec.startLive(mediaProjection);

        while (isLiving) {
            RTMPPackage rtmpPackage = null;
            try {
                rtmpPackage = queue.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (rtmpPackage.getBuffer() != null && rtmpPackage.getBuffer().length != 0) {
                sendData(rtmpPackage.getBuffer(), rtmpPackage.getBuffer().length,
                        rtmpPackage.getTms());
            }
        }
    }

    public void stopLive() {
        if (videoCodec != null) {
            videoCodec.stopLive();
        }
    }

    private native boolean connect(String url);
    private native boolean sendData(byte[] data, int len, long tms);
}
