/*
 * IntroSlideSetup.java
 * Tests if the current setup (Phone + Headset) is supported
 */

package com.chif.headsetcontrolplus.slides;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import com.chif.headsetcontrolplus.R;
import com.github.paolorotolo.appintro.ISlidePolicy;

public class IntroSlideSetup extends Fragment implements ISlidePolicy {

  private static final String APP_TAG = "HeadsetControlPlus";
  private static final String DOCS_TROUBLESHOOT_MD = "https://github.com/nadchif/headset-control-plus/blob/master/docs/TROUBLESHOOT.md";
  private Button mStatusMessageBtn;
  private boolean mSignalReceived = false;
  private BroadcastReceiver mBroadcastReceiver;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_intro_slide_setup, container, false);

    mStatusMessageBtn = view.findViewById(R.id.txt_status);
    mStatusMessageBtn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        getActivity()
                .startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(DOCS_TROUBLESHOOT_MD)));
      }
    });
    mBroadcastReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        SharedPreferences.Editor editor = sp.edit();
        mStatusMessageBtn.setText(R.string.status_headset_setup_success);
        mSignalReceived = true;
        editor.putBoolean("first", true);
        editor.apply();
        // Unregister the broadcast receiver after first successful reception
        getActivity().unregisterReceiver(mBroadcastReceiver);
      }
    };

    // This receiver will wait to hear from the accessibility service that,
    // the headset button has worked
    getActivity().registerReceiver(mBroadcastReceiver,
            new IntentFilter(getActivity().getPackageName()));
    return view;
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
  }

  @Override
  public boolean isPolicyRespected() {
    // Check if signal is received. If false is returned, user cannot advance to next slide
    return mSignalReceived;
  }

  @Override
  public void onUserIllegallyRequestedNextPage() {
    mStatusMessageBtn.setText(R.string.status_headset_setup_trouble);
    Toast.makeText(getContext(), getString(R.string.err_require_headset_success),
            Toast.LENGTH_SHORT).show();
  }

}

