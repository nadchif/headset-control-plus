package com.chif.headsetcontrolplus;


import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
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

import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;

import com.github.paolorotolo.appintro.ISlidePolicy;

public class IntroSlideSetup extends Fragment implements ISlidePolicy {

    private static final String APP_TAG = "HeadsetControlPlus";
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
                Intent intent = new Intent(getActivity().getPackageName());
                getActivity().sendBroadcast(intent);

                Log.e(APP_TAG, ("Broadcast Key "));
            }
        });
        br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
                SharedPreferences.Editor editor = sp.edit();

                Log.e("Broadcast", "Received");
                statusMessage.setText(R.string.status_headset_setup_success);
                signalReceived = true;

                editor.putBoolean("first", true);
                editor.apply();

                getActivity().unregisterReceiver(br);
            }
        };

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

