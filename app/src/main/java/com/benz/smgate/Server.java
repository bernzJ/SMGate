package com.benz.smgate;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import java.io.IOException;


public class Server extends Service {
    private IBinder binder = new ServerBinder();
    private WebServer webServer;
    private SettingsManager preferences;
    private Context context;

    private void internalStop() {
        if (webServer == null) {
            return;
        }
        if (webServer.isAlive())
            webServer.stop();
        webServer = null;
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void startMyOwnForeground() {
        String NOTIFICATION_CHANNEL_ID = getText(R.string.app_name).toString();
        CharSequence headerText = getText(R.string.server_running_text);

        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "1337", NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(R.color.colorPrimary);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle(getText(R.string.app_name))
                .setTicker(headerText)
                .setContentText(headerText)
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        startForeground(2, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startMyOwnForeground();
        else
            startForeground(1, new Notification());
    }

    @Override
    public void onDestroy() {
        internalStop();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void setPreferences(SettingsManager preferences) {
        this.preferences = preferences;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public void startServer() {
        if (webServer == null) {
            webServer = new WebServer(preferences.serverIP, preferences.serverPort, preferences.useIntent);
            webServer.setContext(context);
        }
        if (preferences.running) {
            try {
                webServer.start();
                Toast.makeText(context, "SMGate server started.", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isAlive() {
        return webServer != null && webServer.isAlive();
    }

    public void stopServer() {
        internalStop();
        stopSelf();
        Toast.makeText(context, "SMGate server stopped.", Toast.LENGTH_SHORT).show();
    }

    class ServerBinder extends Binder {
        Server getServerInstance() {
            return Server.this;
        }
    }
}
