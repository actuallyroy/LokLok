package com.elselse.loklok;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class LockScreenService extends Service {
    BroadcastReceiver mReceiver;
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {return null;}

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        mReceiver = new Receiver();
        registerReceiver(mReceiver, filter);
        Notification notification = new NotificationCompat.Builder(this,"ChannelID1")
                .build();

        startForeground(1,notification);
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public void createNotificationChannel() {
        NotificationChannel notificationChannel = new NotificationChannel(
                "ChannelID1", "Foreground notification", NotificationManager.IMPORTANCE_DEFAULT);
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(notificationChannel);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }
}
