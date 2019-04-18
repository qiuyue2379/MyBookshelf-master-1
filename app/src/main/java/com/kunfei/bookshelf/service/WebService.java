package com.kunfei.bookshelf.service;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.kunfei.bookshelf.MApplication;
import com.kunfei.bookshelf.R;
import com.kunfei.bookshelf.utils.NetworkUtil;
import com.yanzhenjie.andserver.AndServer;
import com.yanzhenjie.andserver.Server;

import java.util.concurrent.TimeUnit;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import static com.kunfei.bookshelf.constant.AppConstant.ActionDoneService;
import static com.kunfei.bookshelf.constant.AppConstant.ActionStartService;

public class WebService extends Service {
    private Server server;

    public static void startThis(Activity activity) {
        Intent intent = new Intent(activity, WebService.class);
        intent.setAction(ActionStartService);
        activity.startService(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        updateNotification("正在启动服务");
        Server.ServerListener listener = new Server.ServerListener() {
            @Override
            public void onStarted() {
                updateNotification( getString(R.string.http_ip, server.getInetAddress().getHostAddress()));
            }

            @Override
            public void onStopped() {

            }

            @Override
            public void onException(Exception e) {

            }
        };
        server = AndServer.serverBuilder()
                .inetAddress(NetworkUtil.getLocalIPAddress())
                .port(1122)
                .timeout(10, TimeUnit.SECONDS)
                .listener(listener)
                .build();
        server.startup();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        if (action != null) {
            switch (action) {
                case ActionDoneService:
                    stopSelf();
                    break;
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (server != null) server.shutdown();
    }

    private PendingIntent getThisServicePendingIntent() {
        Intent intent = new Intent(this, this.getClass());
        intent.setAction(ActionDoneService);
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * 更新通知
     */
    private void updateNotification(String content) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, MApplication.channelIdWeb)
                .setSmallIcon(R.drawable.ic_speaker_phone_black_24dp)
                .setOngoing(true)
                .setContentTitle(getString(R.string.web_edit_source))
                .setContentText(content);
        builder.addAction(R.drawable.ic_stop_black_24dp, getString(R.string.cancel), getThisServicePendingIntent());
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        Notification notification = builder.build();
        int notificationId = 1122;
        startForeground(notificationId, notification);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}