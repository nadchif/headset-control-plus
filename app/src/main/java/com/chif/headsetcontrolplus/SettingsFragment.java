/*
 * SettingsFragment.java
 * Houses the preferences that are shown on the main activity.
 */

package com.chif.headsetcontrolplus;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

public class SettingsFragment extends PreferenceFragmentCompat {

  private static Preference.OnPreferenceChangeListener
          sBindPreferenceSummaryToValueListener;

  static {
    sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
      @Override
      public boolean onPreferenceChange(Preference preference, Object value) {
        String stringValue = value.toString();

        if (preference instanceof ListPreference) {
          // For list preferences, look up the correct display value in
          // the preference's 'entries' list.
          ListPreference listPreference = (ListPreference) preference;
          int index = listPreference.findIndexOfValue(stringValue);

          // Set the summary to reflect the new value.
          preference.setSummary(index >= 0 ? listPreference.getEntries()[index] : null);

        } else {
          // For all other preferences, set the summary to the value's
          // simple string representation.
          preference.setSummary(stringValue);
        }
        return true;
      }
    };
  }

  /*
   * Updates the summary of each preference, to match newly set value
   */
  private static void bindPreferenceSummaryToValue(Preference preference) {
    // Set the listener to watch for value changes.
    preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
    // Trigger the listener immediately with the preference's
    // current value.
    sBindPreferenceSummaryToValueListener.onPreferenceChange(
            preference,
            PreferenceManager.getDefaultSharedPreferences(preference.getContext())
                    .getString(preference.getKey(), ""));
  }

  @Override
  public void onResume() {
    super.onResume();
    setAccessibilityServiceText();
  }

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    setPreferencesFromResource(R.xml.main_preferences, rootKey);
    bindPreferenceSummaryToValue(findPreference("hcp_mode"));
    bindPreferenceSummaryToValue(findPreference("hcp_gestures_long_press"));
    bindPreferenceSummaryToValue(findPreference("hcp_gestures_single_press"));
    bindPreferenceSummaryToValue(findPreference("hcp_gestures_double_press"));
    setAccessibilityServiceText();
  }

  /*
   * Updates the status in the settings page
   */
  private void setAccessibilityServiceText() {
    Preference enableHeadsetControlPlusService = findPreference("enable_hcp");
    Spannable summary;
    if (isAccessibilityServiceEnabled(getActivity(), HeadsetControlPlusService.class)) {
      summary = new SpannableString(getString(R.string.pref_status_enabled_hcp));
      summary.setSpan(new ForegroundColorSpan(Color.parseColor("#6ab04c")), 0, summary.length(), 0);
      enableHeadsetControlPlusService.setSummary(summary);
    } else {
      summary = new SpannableString(getString(R.string.pref_status_disabled_hcp));
      summary.setSpan(new ForegroundColorSpan(Color.parseColor("#f0932b")), 0, summary.length(), 0);
      enableHeadsetControlPlusService.setSummary(summary);
    }
  }

  /*
   * Checks if the Accessibility Service is enabled. Returns true if it is
   */
  private static boolean isAccessibilityServiceEnabled(Context context,
                                                       Class<?> accessibilityService) {
    ComponentName expectedComponentName = new ComponentName(context, accessibilityService);

    String enabledServicesSetting = Settings.Secure.getString(context.getContentResolver(),
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
    if (enabledServicesSetting == null) {
      return false;
    }

    TextUtils.SimpleStringSplitter colonSplitter = new TextUtils.SimpleStringSplitter(':');
    colonSplitter.setString(enabledServicesSetting);

    while (colonSplitter.hasNext()) {
      String componentNameString = colonSplitter.next();
      ComponentName enabledService = ComponentName.unflattenFromString(componentNameString);

      if (enabledService != null && enabledService.equals(expectedComponentName)) {
        return true;
      }
    }
    return false;
  }
}