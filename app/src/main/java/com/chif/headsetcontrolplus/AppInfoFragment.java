/**
 * This fragment provides the App Information/About screen.
 */

package com.chif.headsetcontrolplus;

import android.os.Bundle;
import androidx.preference.PreferenceFragmentCompat;

public class AppInfoFragment extends PreferenceFragmentCompat {
  //required empty constructor to prevent app crash
  public AppInfoFragment() {
  }

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    setPreferencesFromResource(R.xml.appinfo_preferences, rootKey);
  }
}
