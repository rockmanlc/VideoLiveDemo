package com.rocklee.videolivedemo;

import android.media.projection.MediaProjection;

public class ScreenLive extends Thread {
    VideoCodec videoCodec;
    private String url;
    private MediaProjection mediaProjection;
    public void startLive(String url, MediaProjection mediaProjection) {
        this.url = url;
        this.mediaProjection = mediaProjection;
        //3
        start();
    }

    @Override
    public void run() {
        videoCodec = new VideoCodec();
        //4
        videoCodec.startLive(mediaProjection);
    }
}
