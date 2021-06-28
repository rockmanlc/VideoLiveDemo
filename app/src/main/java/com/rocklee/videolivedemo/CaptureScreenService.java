package com.rocklee.videolivedemo;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class CaptureScreenService extends Service {
    private int mResultCode;
    private Intent mResultData;
    private MediaProjectionManager mediaProjectionManager;
    private MediaProjection mediaProjection;
    ScreenLive screenLive;
    String url = "rtmp://live-push.bilivideo.com/live-bvc/?streamname=live_347244613_47623249&key=367c3ef3c521d04318b0fbe164272037&schedule=rtmp&pflag=1";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotification();
        mResultCode = intent.getIntExtra("code", -1);
        mResultData = intent.getParcelableExtra("data");
        this.mediaProjectionManager = (MediaProjectionManager)getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        this.mediaProjection = mediaProjectionManager.getMediaProjection(mResultCode, mResultData);
        screenLive = new ScreenLive(getApplicationContext().getFilesDir().getAbsolutePath());
        screenLive.startLive(url, mediaProjection);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (screenLive != null) {
            screenLive.stopLive();
        }
        stopForeground(true);
    }

    private void createNotification() {
        NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = new NotificationCompat.Builder(this, "110")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("capture screen")
                .setContentText(getString(R.string.app_name) + " capturing")
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("110", "notification_name", NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }
        startForeground(110, notification);
    }
}
