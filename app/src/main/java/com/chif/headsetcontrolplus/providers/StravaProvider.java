/*
 * StravaProvider.java
 * Handles interactions between Strava and the app
 */

package com.chif.headsetcontrolplus.providers;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;


public class StravaProvider {

  private Context mContext;

  /** Strava Provider.
   * Handles interactions between Strava and the app
   */
  public StravaProvider(Context context) {
    this.mContext = context;
  }

  /** Toggle Record.
   * Starts an activity if its not recording. Stops an activity if its already recording.
   */
  public void toggleRecord() {
    Intent intent = new Intent(Intent.ACTION_RUN);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    intent.setData(Uri.parse("http://strava.com/nfc/record/toggle"));
    mContext.startActivity(intent);
  }
}
