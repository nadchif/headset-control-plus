/*
 * IntroSlideSetup.java
 * Tests if the current setup (Phone + Headset) is supported
 */
package com.chif.headsetcontrolplus;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.github.paolorotolo.appintro.ISlidePolicy;

public class IntroSlideSetup extends Fragment implements ISlidePolicy {

    private static final String APP_TAG = "HeadsetControlPlus";
    private final String troubleShootUrl = "https://github.com/nadchif/headset-control-plus/blob/master/docs/TROUBLESHOOT.md";
    private Button statusMessage;
    private boolean signalReceived = false;
    BroadcastReceiver br;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_intro_slide_setup, container, false);

        statusMessage = (Button) view.findViewById(R.id.txt_status);
        statusMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getActivity().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(troubleShootUrl)));
            }
        });
        br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
                SharedPreferences.Editor editor = sp.edit();
                statusMessage.setText(R.string.status_headset_setup_success);
                signalReceived = true;
                editor.putBoolean("first", true);
                editor.apply();
                // unregister the broadcast receiver after first successful reception
                getActivity().unregisterReceiver(br);
            }
        };

        // this receiver will wait to hear from the accessibility service that the headset button has worked
        getActivity().registerReceiver(br, new IntentFilter(getActivity().getPackageName()));
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public boolean isPolicyRespected() {
        //check if signal is received. if false is returned, user cannot advance to next slide
        return signalReceived;
    }

    @Override
    public void onUserIllegallyRequestedNextPage() {
        statusMessage.setText(R.string.status_headset_setup_trouble);
        Toast.makeText(getContext(), getString(R.string.err_require_headset_success), Toast.LENGTH_SHORT).show();
    }

}

