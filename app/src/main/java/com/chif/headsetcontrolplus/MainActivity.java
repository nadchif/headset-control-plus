/**
 * This is Main Activity that is launched by default when a user opens HCP.
 */

package com.chif.headsetcontrolplus;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

public class MainActivity extends AppCompatActivity implements
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

  private static final String TITLE_TAG = "settingsActivityTitle";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    if (savedInstanceState == null) {
      getSupportFragmentManager()
              .beginTransaction()
              .replace(R.id.app_main_settings, new SettingsFragment())
              .commit();
    } else {
      setTitle(savedInstanceState.getCharSequence(TITLE_TAG));
    }

    // Check if Intro slider already ran, if not launch it
    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
    if (!sp.getBoolean("first", false)) {
      Intent intent = new Intent(this, IntroActivity.class);
      startActivity(intent);
    }

    getSupportFragmentManager().addOnBackStackChangedListener(
        new FragmentManager.OnBackStackChangedListener() {
          @Override
          public void onBackStackChanged() {
            ActionBar actionBar = getSupportActionBar();
            if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
              setTitle(R.string.app_name);
              if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(false);
              }
              return;
            }
            if (actionBar != null) {
              actionBar.setDisplayHomeAsUpEnabled(true);
            }
          }
        });
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    // Save current activity title so we can set it again after a configuration change
    outState.putCharSequence(TITLE_TAG, getTitle());
  }

  @Override
  public boolean onSupportNavigateUp() {
    if (getSupportFragmentManager().popBackStackImmediate()) {
      return true;
    }
    return super.onSupportNavigateUp();
  }

  @Override
  public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
    // Instantiate the new Fragment
    final Bundle args = pref.getExtras();
    final Fragment fragment = getSupportFragmentManager().getFragmentFactory().instantiate(
            getClassLoader(),
            pref.getFragment());
    fragment.setArguments(args);
    fragment.setTargetFragment(caller, 0);
    // Replace the existing Fragment with the new Fragment
    getSupportFragmentManager().beginTransaction()
            .replace(R.id.app_main_settings, fragment)
            .addToBackStack(null)
            .commit();
    setTitle(pref.getTitle());
    return true;
  }

  public static class AppInfoFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
      setPreferencesFromResource(R.xml.appinfo_preferences, rootKey);
    }
  }
}
