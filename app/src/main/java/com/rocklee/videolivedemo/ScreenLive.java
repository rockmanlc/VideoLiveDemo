package com.rocklee.videolivedemo;

import android.media.projection.MediaProjection;

public class ScreenLive extends Thread {
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
        videoCodec = new VideoCodec(videoFilePath);
        //4
        videoCodec.startLive(mediaProjection);
    }

    public void stopLive() {
        if (videoCodec != null) {
            videoCodec.stopLive();
        }
    }
}
