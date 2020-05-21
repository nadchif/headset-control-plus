/**
 * Handles interactions between Strava and the app.
 *
 * <p>ISSUES:
 * Sometimes it just brings the strava app to the foreground, instead of start/stop.
 * see: https://groups.google.com/d/msg/strava-api/Uywi_830YWE/aqWmzyr8CwAJ
 */

package com.chif.headsetcontrolplus.providers;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;


public class StravaProvider {

  private static final String APP_TAG = StravaProvider.class.getSimpleName();
  private Context mContext;

  /**
   * Strava Provider.
   * Handles interactions between Strava and the app
   *
   * @param context - the Context
   */
  public StravaProvider(final Context context) {
    this.mContext = context;

  }

  /**
   * Toggle Record.
   * Starts an activity if its not recording.
   * Stops an activity if its already recording.
   */
  public void toggleRecord() {
    Intent intent = new Intent(Intent.ACTION_RUN);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    intent.setData(Uri.parse("http://strava.com/nfc/record/toggle"));
    try {
      mContext.startActivity(intent);
    } catch (Exception ex) {
      Log.e(APP_TAG, ex.getMessage());
    }
  }
}
