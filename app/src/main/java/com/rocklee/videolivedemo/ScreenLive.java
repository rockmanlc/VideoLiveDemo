package com.rocklee.videolivedemo;

import android.media.projection.MediaProjection;
import android.util.Log;

public class ScreenLive extends Thread {
    private static final String TAG = "ScreenLive";
    VideoCodec videoCodec;
    private String url;
    private String videoFilePath;
    private MediaProjection mediaProjection;
    public void startLive(String url, MediaProjection mediaProjection) {
        this.url = url;
        this.mediaProjection = mediaProjection;
        //3
        start();
    }

    public ScreenLive(String path) {
        this.videoFilePath = path;
    }

    @Override
    public void run() {
        //16
        if (!connect(url)) {
            Log.e(TAG, "run: ---->fail!");
        }

        videoCodec = new VideoCodec(videoFilePath);
        //4
        videoCodec.startLive(mediaProjection);
    }

    public void stopLive() {
        if (videoCodec != null) {
            videoCodec.stopLive();
        }
    }

    private native boolean connect(String url);
}
