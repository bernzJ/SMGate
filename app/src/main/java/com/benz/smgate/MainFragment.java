package com.benz.smgate;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.os.IBinder;
import android.provider.Settings;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class MainFragment extends Fragment {
    private SettingsManager settingsManager;
    private Server serverService = null;
    private boolean serviceBound = false;
    private boolean shouldUpdate = false;

    private String getSMSLimiters(ContentResolver cr) {
        StringBuilder sb = new StringBuilder();
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
            sb.append("sms_outgoing_check_max_count (System):")
                    .append(Settings.System.getInt(cr, "sms_outgoing_check_max_count", -1))
                    .append("\n")
                    .append("sms_outgoing_check_max_count (Secure):")
                    .append(Settings.Secure.getInt(cr, "sms_outgoing_check_max_count", -1))
                    .append("\n");
            sb.append("sms_outgoing_check_interval_ms (global):").append(Settings.Global.getInt(cr, "sms_outgoing_check_interval_ms", -1)).append("\n");
        } else {
            sb.append("sms_outgoing_check_max_count (System):")
                    .append(Settings.System.getInt(cr, "sms_outgoing_check_max_count", -1))
                    .append("\n")
                    .append("sms_outgoing_check_max_count (Secure):")
                    .append(Settings.Secure.getInt(cr, "sms_outgoing_check_max_count", -1))
                    .append("\n");
        }
        return sb.toString();
    }

    private String setSMSLimiters() {
        StringBuilder sb = new StringBuilder();
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
            sb.append(ExecCommands.sudoForResult("settings put system sms_outgoing_check_max_count 99999", "settings put secure sms_outgoing_check_max_count 99999", "settings put global sms_outgoing_check_interval_ms 0")).append("\n");
        } else {
            sb.append(ExecCommands.sudoForResult("settings put system sms_outgoing_check_max_count 99999", "settings put secure sms_outgoing_check_max_count 99999")).append("\n");
        }
        return sb.toString();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        Context context = getContext();
        settingsManager = new SettingsManager(context);

        Intent serverIntent = new Intent(context, Server.class);
        ContextCompat.startForegroundService(context, serverIntent);
        context.bindService(serverIntent, serverConnection, context.BIND_AUTO_CREATE);

        if (getArguments() != null) {
            shouldUpdate = getArguments().getBoolean("shouldUpdate", false);
        }

        View view = inflater.inflate(R.layout.main_fragment, container, false);
        TextView textMainLog = view.findViewById(R.id.textMainLog);
        textMainLog.setMovementMethod(new ScrollingMovementMethod());

        // @TODO: may not be needed to call every time. also may require reboot ?
        boolean removeLimiter = settingsManager.removeLimiter;
        if (removeLimiter) {
            textMainLog.setText(setSMSLimiters());
        } else {
            textMainLog.setText(getSMSLimiters(context.getContentResolver()));
        }
        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (serverService != null) {
            serverService.stopServer();
        }
        if (serviceBound) {
            getContext().unbindService(serverConnection);
            serviceBound = false;
        }
    }

    private ServiceConnection serverConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            Server.ServerBinder serviceBinder = (Server.ServerBinder) binder;
            serverService = serviceBinder.getServerInstance();
            serverService
                    .setPreferences(
                            settingsManager
                    );
            serverService.setContext(getContext());
            serviceBound = true;

            if (serviceBound) {
                if (shouldUpdate) {
                    serverService.stopServer();
                }
                if (!settingsManager.running && serverService.isAlive()) {
                    serverService.stopServer();
                }
                if (settingsManager.running && !serverService.isAlive()) {
                    serverService.startServer();
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serverService = null;
            serviceBound = false;
        }

    };
}
