package com.benz.smgate;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import android.provider.Telephony;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import java.util.List;

public class SettingsFragment extends Fragment {

    private SettingsManager settingsManager;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        settingsManager = new SettingsManager(getActivity());
        View view = inflater.inflate(R.layout.settings_fragment, container, false);
        Button btnSave = view.findViewById(R.id.btnSave);

        Switch switchIsEnabled = view.findViewById(R.id.switchIsEnabled);
        Switch switchRemoveSMSLimit = view.findViewById(R.id.switchRemoveSMSLimit);
        Switch switchReplaceApp = view.findViewById(R.id.switchReplaceApp);
        EditText textServerIP = view.findViewById(R.id.textServerIP);
        EditText textServerPort = view.findViewById(R.id.textServerPort);

        Context context = getContext();
        String pckg = context.getPackageName();

        switchIsEnabled.setChecked(settingsManager.running);
        switchRemoveSMSLimit.setChecked(settingsManager.removeLimiter);
        textServerIP.setText(settingsManager.serverIP);
        textServerPort.setText(String.valueOf(settingsManager.serverPort));


        if (pckg.equals(getDefaultSmsAppPackageName(context))) {
            switchReplaceApp.setChecked(true);
        }
        // Click handlers
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                View view = v.getRootView();
                if (view != null) {
                    Switch switchIsEnabled = view.findViewById(R.id.switchIsEnabled);
                    Switch switchRemoveSMSLimit = view.findViewById(R.id.switchRemoveSMSLimit);
                    EditText textServerIP = view.findViewById(R.id.textServerIP);
                    EditText textServerPort = view.findViewById(R.id.textServerPort);

                    Bundle args = new Bundle();
                    args.putBoolean("shouldUpdate", true);

                    settingsManager.setSettings(switchIsEnabled.isChecked(), switchRemoveSMSLimit.isChecked(), textServerIP.getText().toString(), Integer.parseInt(textServerPort.getText().toString()));
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
                    Switch switchIsEnabled = (Switch) v;
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

        switchReplaceApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                View view = v.getRootView();
                Context context = getContext();
                PackageManager pm = context.getPackageManager();
                Switch switchReplaceApp = (Switch) v;
                if (view != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        String pckg = context.getPackageName();
                        if (pckg.equals(getDefaultSmsAppPackageName(context)) && !switchReplaceApp.isChecked()) {
                            toggleComponentEnabled(pm, context);
                        } else {
                            toggleComponentEnabled(pm, context);
                            Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
                            intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, pckg);
                            startActivityForResult(intent, 1);
                        }
                    } else {
                        Toast.makeText(context, "Not supported, set default via settings", Toast.LENGTH_LONG).show();
                        switchReplaceApp.setChecked(false);
                    }
                }
            }
        });

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Switch switchReplaceApp = getView().findViewById(R.id.switchReplaceApp);
        if (requestCode == 1) {
            if (resultCode == Activity.RESULT_OK) {
                switchReplaceApp.setChecked(true);
            } else {
                switchReplaceApp.setChecked(false);
            }
        }
    }

    public static String getDefaultSmsAppPackageName(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            return Telephony.Sms.getDefaultSmsPackage(context);
        else {
            Intent intent = new Intent(Intent.ACTION_VIEW)
                    .addCategory(Intent.CATEGORY_DEFAULT).setType("vnd.android-dir/mms-sms");
            final List<ResolveInfo> resolveInfos = context.getPackageManager().queryIntentActivities(intent, 0);
            if (!resolveInfos.isEmpty())
                return resolveInfos.get(0).activityInfo.packageName;
            return "-1";
        }
    }

    private void toggleComponentEnabled(PackageManager pm, Context context) {
        ComponentName componentName = new ComponentName(context.getApplicationContext(), HeadlessSmsSendService.class);
        int flag = ((pm.getComponentEnabledSetting(componentName) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) ? PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                : PackageManager.COMPONENT_ENABLED_STATE_ENABLED);
        pm.setComponentEnabledSetting(componentName, flag, PackageManager.DONT_KILL_APP);
    }

}
