/**
 * Checks preferences to see if the foreground service was last enabled, and then starts
 * up the service again on android boot up.
 */

package com.chif.headsetcontrolplus;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import androidx.preference.PreferenceManager;

public class AutoStart extends BroadcastReceiver {
  private static final String APP_TAG = AutoStart.class.getSimpleName();

  @Override
  public void onReceive(final Context context, final Intent arg) {

    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
    boolean runAutoStart = preferences
            .getBoolean("enable_hcp_foreground_service", false);

    if (runAutoStart) {
      try {
        Intent intent = new Intent(context, ForegroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          context.startForegroundService(intent);
        } else {
          context.startService(intent);
        }
        Log.i(APP_TAG, "Auto started Headset Control Plus");
      } catch (Exception ex) {
        Log.e(APP_TAG, ex.getMessage());
      }
      return;
    }
    Log.i(APP_TAG, "Did not run auto start Headset Control Plus");
  }
}
