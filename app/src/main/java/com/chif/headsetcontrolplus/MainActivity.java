/**
 * This is Main Activity that is launched by default when a user opens HCP.
 */

package com.chif.headsetcontrolplus;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

public class MainActivity extends AppCompatActivity {

  @Override
  protected void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    getSupportFragmentManager().beginTransaction().replace(R.id.app_main_settings,
            new SettingsFragment()).commit();

    /* Check if Intro slider already ran, if not launch it */
    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
    if (!sp.getBoolean("first", false)) {
      Intent intent = new Intent(this, IntroActivity.class);
      startActivity(intent);
    }
  }
}
