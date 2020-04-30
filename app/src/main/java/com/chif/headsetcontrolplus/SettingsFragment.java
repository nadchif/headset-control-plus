/**
 * Houses the preferences that are shown on the main activity.
 */

package com.chif.headsetcontrolplus;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.widget.Toast;
import androidx.core.content.ContextCompat;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;
import com.chif.headsetcontrolplus.shared.ServiceBase;

public class SettingsFragment extends PreferenceFragmentCompat {

  private static final String APP_TAG = SettingsFragment.class.getSimpleName();
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

  private SwitchPreference mPrefForegroundSwitch;

  /*
   * Updates the summary of each preference, to match newly set value
   */
  private static void bindPreferenceSummaryToValue(final Preference preference) {
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
  public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
    setPreferencesFromResource(R.xml.main_preferences, rootKey);
    bindPreferenceSummaryToValue(findPreference("hcp_mode"));
    bindPreferenceSummaryToValue(findPreference("hcp_gestures_long_press"));
    bindPreferenceSummaryToValue(findPreference("hcp_gestures_single_press"));
    bindPreferenceSummaryToValue(findPreference("hcp_gestures_double_press"));
    mPrefForegroundSwitch = findPreference("enable_hcp_foreground_service");
    setAccessibilityServiceText();
    setForegroundServiceSwitchListener();
  }

  private void setForegroundServiceSwitchListener() {
    final SwitchPreference preferenceSwitch = findPreference("enable_hcp_foreground_service");
    preferenceSwitch.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
      @Override
      public boolean onPreferenceChange(final Preference preference, final Object newValue) {
        final boolean isEnabled = (Boolean) newValue;
        if (isEnabled) {
          if (!ServiceBase.isAccessibilityServiceEnabled(getActivity(),
                  HeadsetControlPlusService.class)) {
            Toast.makeText(getContext(), getString(R.string.err_require_access),
                    Toast.LENGTH_SHORT).show();
            stopService();
            return false;
          }
          startService();
          return true;
        }
        stopService();
        return true;
      }

    });
  }

  /**
   * Updates the status in the settings page.
   */
  private void setAccessibilityServiceText() {
    Preference enableHeadsetControlPlusService = findPreference("enable_hcp");
    Spannable summary;
    if (ServiceBase.isAccessibilityServiceEnabled(getActivity(), HeadsetControlPlusService.class)) {
      summary = new SpannableString(getString(R.string.pref_status_enabled_hcp));
      summary.setSpan(new ForegroundColorSpan(Color.parseColor("#6ab04c")), 0, summary.length(), 0);
      enableHeadsetControlPlusService.setSummary(summary);
    } else {
      mPrefForegroundSwitch.getSharedPreferences().edit()
              .putBoolean("enable_hcp_foreground_service", true).commit();
      summary = new SpannableString(getString(R.string.pref_status_disabled_hcp));
      summary.setSpan(new ForegroundColorSpan(Color.parseColor("#f0932b")),
              0, summary.length(), 0);
      enableHeadsetControlPlusService.setSummary(summary);
      stopService();
      mPrefForegroundSwitch.setChecked(false);
      //refresh page

    }
  }

  private void startService() {
    mPrefForegroundSwitch.getSharedPreferences().edit()
            .putBoolean("enable_hcp_foreground_service", true).commit();
    Intent serviceIntent = new Intent(getActivity(), ForegroundService.class);
    ContextCompat.startForegroundService(getActivity(), serviceIntent);
  }

  private void stopService() {
    mPrefForegroundSwitch.getSharedPreferences().edit()
            .putBoolean("enable_hcp_foreground_service", false).commit();
    Intent serviceIntent = new Intent(getActivity(), ForegroundService.class);
    getActivity().stopService(serviceIntent);
  }
}
