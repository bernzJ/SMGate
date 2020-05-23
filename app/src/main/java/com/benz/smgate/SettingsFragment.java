package com.benz.smgate;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

public class SettingsFragment extends Fragment {

    private SettingsManager settingsManager;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        settingsManager = new SettingsManager(getActivity());
        View view = inflater.inflate(R.layout.settings_fragment, container, false);
        Button btnSave = view.findViewById(R.id.btnSave);

        Switch switchIsEnabled = view.findViewById(R.id.switchIsEnabled);
        Switch switchUseIntent = view.findViewById(R.id.switchUseIntent);
        EditText textServerIP = view.findViewById(R.id.textServerIP);
        EditText textServerPort = view.findViewById(R.id.textServerPort);

        switchIsEnabled.setChecked(settingsManager.running);
        switchUseIntent.setChecked(settingsManager.useIntent);
        textServerIP.setText(settingsManager.serverIP);
        textServerPort.setText(String.valueOf(settingsManager.serverPort));

        // Click handlers
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Log.d("TAG", v.getParent().toString());
                View view = v.getRootView();
                if (view != null) {
                    Switch switchIsEnabled = view.findViewById(R.id.switchIsEnabled);
                    Switch switchUseIntent = view.findViewById(R.id.switchUseIntent);
                    EditText textServerIP = view.findViewById(R.id.textServerIP);
                    EditText textServerPort = view.findViewById(R.id.textServerPort);

                    Bundle args = new Bundle();
                    args.putBoolean("shouldUpdate", true);

                    settingsManager.setSettings(switchIsEnabled.isChecked(), switchUseIntent.isChecked(), textServerIP.getText().toString(), Integer.parseInt(textServerPort.getText().toString()));
                    NavHostFragment.findNavController(SettingsFragment.this)
                            .navigate(R.id.action_settingsFragment_to_mainFragment, args);
                }
            }
        });

        switchIsEnabled.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                View view = v.getRootView();
                if (view != null) {
                    Switch switchIsEnabled = view.findViewById(R.id.switchIsEnabled);
                    EditText textServerIP = view.findViewById(R.id.textServerIP);
                    EditText textServerPort = view.findViewById(R.id.textServerPort);

                    if (switchIsEnabled.isChecked()) {
                        if (
                                TextUtils.isEmpty(textServerIP.getText()) ||
                                        !TextUtils.isDigitsOnly(textServerPort.getText())
                        ) {
                            switchIsEnabled.setChecked(false);
                            Toast.makeText(getActivity().getApplicationContext(), "Please complete all fields", Toast.LENGTH_LONG).show();
                        }
                    }
                }
            }
        });
        return view;
    }

}
