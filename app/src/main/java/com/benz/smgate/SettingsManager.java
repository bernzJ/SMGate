package com.benz.smgate;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

public class SettingsManager {
    public Context context;
    boolean running = false;
    boolean removeLimiter = false;
    String serverIP = "";
    int serverPort = 0;
    private SharedPreferences sharedPref;

    SettingsManager(Context ctx) {
        context = ctx;
        sharedPref = ctx.getSharedPreferences(context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        this.updateSettings();
    }

    private void updateSettings() {
        boolean defaultRunning = context.getResources().getBoolean(R.bool.preference_default_running);
        boolean defaultRemoveLimiter = context.getResources().getBoolean(R.bool.preference_default_use_intent);

        String defaultIP = context.getResources().getString(R.string.preference_default_server_ip);
        int defaultPort = context.getResources().getInteger(R.integer.preference_default_server_port);

        this.running = sharedPref.getBoolean(context.getString(R.string.preference_running), defaultRunning);
        this.removeLimiter = sharedPref.getBoolean(context.getString(R.string.preference_remove_limiter), defaultRemoveLimiter);
        this.serverIP = sharedPref.getString(context.getString(R.string.preference_server_ip), defaultIP);
        this.serverPort = sharedPref.getInt(context.getString(R.string.preference_server_port), defaultPort);
    }

    void setSettings(boolean running, boolean removeLimiter, String ip, int port) {
        this.removeLimiter = removeLimiter;
        this.running = running;
        this.serverIP = ip;
        this.serverPort = port;
        sharedPref.edit()
                .putBoolean(context.getString(R.string.preference_running), running)
                .putBoolean(context.getString(R.string.preference_remove_limiter), removeLimiter)
                .putString(context.getString(R.string.preference_server_ip), ip)
                .putInt(context.getString(R.string.preference_server_port), port)
                .apply();
        Toast.makeText(context, "Settings updated", Toast.LENGTH_SHORT).show();
        this.updateSettings();
    }
}
