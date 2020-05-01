package com.chif.headsetcontrolplus;

import android.os.Bundle;
import androidx.preference.PreferenceFragmentCompat;

public class AppInfoFragment extends PreferenceFragmentCompat {
  public AppInfoFragment(){

  };
  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    setPreferencesFromResource(R.xml.appinfo_preferences, rootKey);
  }
}