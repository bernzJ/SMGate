package com.benz.smgate;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.os.IBinder;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

public class MainFragment extends Fragment {
    private SettingsManager settingsManager;
    private Server serverService = null;
    private boolean serviceBound = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Context context = getContext();
        settingsManager = new SettingsManager(context);

        Intent serverIntent = new Intent(context, Server.class);
        ContextCompat.startForegroundService(context, serverIntent);
        context.bindService(serverIntent, serverConnection, context.BIND_AUTO_CREATE);


        View view = inflater.inflate(R.layout.main_fragment, container, false);
        TextView textMainLog = view.findViewById(R.id.textMainLog);
        textMainLog.setMovementMethod(new ScrollingMovementMethod());

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

            // TextView textMainLog = getView().findViewById(R.id.textMainLog);
            // textMainLog.setText(serverService.getLogs());

            if (serviceBound) {
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
