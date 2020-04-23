/*
 * IntroSlidePermissions.java
 * Explains to the user the permissions the app will require. In this case its just accessibility
 */
package com.chif.headsetcontrolplus;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.paolorotolo.appintro.ISlidePolicy;


public class IntroSlidePermissions extends Fragment implements ISlidePolicy {
    private LinearLayout layoutContainer;
    private Button launchSettingsBtn;
    private TextView successMessage;

    ContentObserver observer = new ContentObserver(new Handler(Looper.getMainLooper())) {
        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            boolean accessibilityServiceEnabled = isAccessibilityServiceEnabled(getActivity(), HeadsetControlPlusService.class);
            if (accessibilityServiceEnabled) {
                Toast.makeText(getContext(), getString(R.string.success_require_access), Toast.LENGTH_SHORT).show();
                launchSettingsBtn.setVisibility(View.GONE);
                successMessage.setVisibility(View.VISIBLE);
            } else {
                Toast.makeText(getContext(), getString(R.string.err_require_access), Toast.LENGTH_SHORT).show();
                successMessage.setVisibility(View.GONE);
                launchSettingsBtn.setVisibility(View.VISIBLE);
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_intro_slide_permissions, container, false);

        layoutContainer = (LinearLayout) view.findViewById(R.id.intro_slide_layout_permissions);
        launchSettingsBtn = (Button) view.findViewById(R.id.btn_settings);
        successMessage = (TextView) view.findViewById(R.id.txt_success);

        boolean serviceEnabled = isAccessibilityServiceEnabled(getActivity(), HeadsetControlPlusService.class);
        if (serviceEnabled) {
            Toast.makeText(getContext(), getString(R.string.success_require_access), Toast.LENGTH_SHORT).show();
            launchSettingsBtn.setVisibility(View.GONE);
            successMessage.setVisibility(View.VISIBLE);
        } else {
            Toast.makeText(getContext(), getString(R.string.err_require_access), Toast.LENGTH_SHORT).show();
            successMessage.setVisibility(View.GONE);
            launchSettingsBtn.setVisibility(View.VISIBLE);
        }

        launchSettingsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivity(intent);
            }
        });

        Uri uri = Settings.Secure.getUriFor(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        getActivity().getContentResolver().registerContentObserver(uri, false, observer);

        return view;
    }

    @Override
    public void onDestroyView() {
        getActivity().getContentResolver().unregisterContentObserver(observer);
        super.onDestroyView();
    }

    @Override
    public boolean isPolicyRespected() {
        // check if accessibity is enabled . return false if it is not
        boolean enabled = isAccessibilityServiceEnabled(getActivity(), HeadsetControlPlusService.class);
        return enabled;
    }

    @Override
    public void onUserIllegallyRequestedNextPage() {
        Toast.makeText(getContext(), getString(R.string.err_require_access), Toast.LENGTH_SHORT).show();
    }

    private static boolean isAccessibilityServiceEnabled(Context context, Class<?> accessibilityService) {
        ComponentName expectedComponentName = new ComponentName(context, accessibilityService);

        String enabledServicesSetting = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabledServicesSetting == null)
            return false;

        TextUtils.SimpleStringSplitter colonSplitter = new TextUtils.SimpleStringSplitter(':');
        colonSplitter.setString(enabledServicesSetting);

        while (colonSplitter.hasNext()) {
            String componentNameString = colonSplitter.next();
            ComponentName enabledService = ComponentName.unflattenFromString(componentNameString);
            if (enabledService != null && enabledService.equals(expectedComponentName))
                return true;
        }
        return false;
    }

}

