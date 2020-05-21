/**
 * HeadsetControlPlusService.java
 *
 * <p>Copyright 2020 github.com/nadchif
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.chif.headsetcontrolplus;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import androidx.preference.PreferenceManager;
import com.chif.headsetcontrolplus.providers.FlashlightProvider;
import com.chif.headsetcontrolplus.providers.StravaProvider;
import com.chif.headsetcontrolplus.shared.ServiceBase;

public class HeadsetControlPlusService extends AccessibilityService {
  private static final String APP_TAG = HeadsetControlPlusService.class.getSimpleName();
  private static final Handler S_HANDLER = new Handler();
  private static int sKeyUpCount = 0;
  private static String sActionsDefault;
  private static String sActionsPlayPause;
  private static String sActionsNext;
  private static String sActionsPrevious;
  private static String sActionsVolumeUp;
  private static String sActionsVolumeDown;
  private static String sActionsVolumeMute;
  private static String sActionsFlashlightToggle;
  private static String sActionsStravaToggle;
  private static AudioManager sAudioManager;
  private static boolean isPlaying = false;
  private static SharedPreferences pref;
  private static String mGestureMode = "unknown";
  private static boolean sIsSimulation = false;
  private static Runnable sGestureSinglePressed;
  private static Runnable sGestureDoublePressed;
  private static FlashlightProvider sFlashlightProvider;
  private static StravaProvider sStravaProvider;
  private static Context sContext;

  private static void execAction(final String action) {
    isPlaying = sAudioManager.isMusicActive();
    if (action.equals(sActionsPlayPause)) {
      playPause();
      return;
    }
    if (action.equals(sActionsNext)) {
      nextTrack();
      return;
    }
    if (action.equals(sActionsPrevious)) {
      previousTrack();
      return;
    }
    if (action.equals(sActionsVolumeDown)) {
      decreaseVolume();
      return;
    }
    if (action.equals(sActionsVolumeUp)) {
      increaseVolume();
      return;
    }
    if (action.equals(sActionsVolumeMute)) {
      muteVolume();
      return;
    }
    if (action.equals(sActionsFlashlightToggle)) {
      sFlashlightProvider.toggleFlashLight();
      return;
    }
    if (action.equals(sActionsStravaToggle)) {
      sStravaProvider.toggleRecord();
      return;
    }
  }

  /**
   * Handles gestures that were polled during screen off.
   * @param gesture - Accepts "single", "double", and "triple"
   */
  public static void handleGesture(final String gesture) {

    final String singlePressAction = pref.getString("hcp_gestures_single_press",
            sActionsPlayPause);
    final String doublePressAction = pref.getString("hcp_gestures_double_press",
            sActionsNext);
    final String triplePressAction = pref.getString("hcp_gestures_triple_press",
            sActionsPrevious);
    if (gesture == "single") {
      execAction(singlePressAction);
    } else if (gesture == "double") {
      execAction(doublePressAction);
    } else if (gesture == "triple") {
      execAction(triplePressAction);
    }
  }

  /**
   * Simulates a single press of the headset button. This is necessary for cases where after
   * catching the initial single press event and it is assigned to do default, you have to
   * re-stage it with the sIsSimulation set to true, to allow the event to go through this service
   * uninterrupted.
   */

  private static void simulateSinglePress() {
    sIsSimulation = true; // Set to true each time, to allow it go through and be handled by system.
    long eventtime = SystemClock.uptimeMillis();
    KeyEvent downEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_DOWN,
            KeyEvent.KEYCODE_HEADSETHOOK, 0);
    sAudioManager.dispatchMediaKeyEvent(downEvent);

    sIsSimulation = true; // Set to true each time, to allow it go through and be handled by system.
    KeyEvent upEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_UP,
            KeyEvent.KEYCODE_HEADSETHOOK, 0);
    sAudioManager.dispatchMediaKeyEvent(upEvent);

    Log.i(APP_TAG, "hcp simulated single press");
  }

  /**
   * Simulates double press of the headset button. This is necessary for cases where after
   * catching the initial double press event and it is assigned to do default, you have to
   * re-stage it with the sIsSimulation set to true, to allow the event to go through this service
   * uninterrupted.
   */
  private static void simulateDoublePress() {

    sIsSimulation = true; // Set to true each time, to allow it go through and be handled by system.
    long eventtime = SystemClock.uptimeMillis();
    KeyEvent downEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_DOWN,
            KeyEvent.KEYCODE_HEADSETHOOK, 0);
    sAudioManager.dispatchMediaKeyEvent(downEvent);

    sIsSimulation = true; // Set to true each time, to allow it go through and be handled by system.
    KeyEvent upEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_UP,
            KeyEvent.KEYCODE_HEADSETHOOK, 0);
    sAudioManager.dispatchMediaKeyEvent(upEvent);

    sIsSimulation = true; // Set to true each time, to allow it go through and be handled by system.
    KeyEvent downEvent2 = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_DOWN,
            KeyEvent.KEYCODE_HEADSETHOOK, 0);
    sAudioManager.dispatchMediaKeyEvent(downEvent2);

    sIsSimulation = true; // Set to true each time, to allow it go through and be handled by system.
    KeyEvent upEvent2 = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_UP,
            KeyEvent.KEYCODE_HEADSETHOOK, 0);
    sAudioManager.dispatchMediaKeyEvent(upEvent2);

    Log.i(APP_TAG, "hcp simulated double press");

  }

  /**
   * Simulates triple press of the headset button. This is necessary for cases where after
   * catching the initial triple press event and it is assigned to do default, you have to
   * re-stage it with the sIsSimulation set to true, to allow the event to go through this service
   * uninterrupted.
   * @todo write Triple Press Simulation function
   */
  private static void simulateTriplePress() {

  }

  /* Broadcast a togglepause intent */
  private static void playPause() {
    Intent i = new Intent("com.android.music.musicservicecommand");
    i.putExtra("command", "togglepause");
    sContext.sendBroadcast(i);

    Log.i(APP_TAG, "Play Pause");
  }

  /* Broadcast a next track intent */
  private static void nextTrack() {
    Intent i = new Intent("com.android.music.musicservicecommand");
    i.putExtra("command", "next");
    sContext.sendBroadcast(i);

    Log.i(APP_TAG, "Next Track");
  }

  /* Broadcast a previous track intent */
  private static void previousTrack() {
    Intent i = new Intent("com.android.music.musicservicecommand");
    i.putExtra("command", "previous");
    sContext.sendBroadcast(i);

    Log.i(APP_TAG, "Prev Track");
  }

  private static void increaseVolume() {
    sAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE,
            AudioManager.FLAG_SHOW_UI);
    Log.i(APP_TAG, "Inc Vol");
  }

  private static void decreaseVolume() {
    sAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER,
            AudioManager.FLAG_SHOW_UI);
    Log.i(APP_TAG, "Dec Vol");
  }

  private static void muteVolume() {
    sAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE,
            AudioManager.FLAG_SHOW_UI);
    Log.i(APP_TAG, "Mut Vol");
  }

  @Override
  protected void onServiceConnected() {
    sAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
    // sActionsDefault = getString(R.string.pref_button_actions_default);
    sActionsPlayPause = getString(R.string.pref_button_actions_playpause);
    sActionsNext = getString(R.string.pref_button_actions_next);
    sActionsPrevious = getString(R.string.pref_button_actions_previous);
    sActionsVolumeUp = getString(R.string.pref_button_actions_volume_up);
    sActionsVolumeDown = getString(R.string.pref_button_actions_volume_down);
    sActionsVolumeMute = getString(R.string.pref_button_actions_volume_mute);
    sActionsFlashlightToggle = getString(R.string.pref_button_actions_flashlight_toggle);
    sActionsStravaToggle = getString(R.string.pref_button_actions_strava_toggle);
    sFlashlightProvider = new FlashlightProvider(this);
    sStravaProvider = new StravaProvider(this);
    sContext = this;
    pref = PreferenceManager.getDefaultSharedPreferences(this);
    Log.i(APP_TAG, "Service Connected");
  }

  @Override
  public void onAccessibilityEvent(final AccessibilityEvent event) {

  }

  @Override
  public boolean onKeyEvent(final KeyEvent event) {
    int keycode = event.getKeyCode();
    int action = event.getAction();

    if (action != KeyEvent.ACTION_UP && action != KeyEvent.ACTION_DOWN || event.isCanceled()) {
      return false;
    }

    if (ServiceBase.isSupportedKey(keycode)) {
      // Not interested in any other keys
      Log.i(APP_TAG, "Ignored " + keycode);
      return false;
    }

    // Allow simulated keyEvents from this service to go through.
    if (sIsSimulation) {
      sIsSimulation = false; // Reset simulation mode for next event.
      return false;
    }

    pref = PreferenceManager.getDefaultSharedPreferences(this);

    Log.d(APP_TAG, ("Broadcast Key " + keycode));
    Intent intent = new Intent(getPackageName());
    intent.putExtra("pressed", keycode);
    sendBroadcast(intent);


    final String singlePressAction = pref.getString("hcp_gestures_single_press",
            sActionsPlayPause);
    final String doublePressAction = pref.getString("hcp_gestures_double_press",
            sActionsNext);
    final String triplePressAction = pref.getString("hcp_gestures_triple_press",
            sActionsPrevious);

    // Determin Single, Double or Click.
    if (action == KeyEvent.ACTION_UP) {
      sKeyUpCount++;

      // Single press.
      if (sKeyUpCount == 1) {
        sGestureSinglePressed = new Runnable() {
          public void run() {
            if (sKeyUpCount == 1) {
              sKeyUpCount = 0;
              Log.d(APP_TAG, "Exec Single Press Action");
              if (singlePressAction.equals(sActionsDefault)) {
                // Simulate the original event.
                simulateSinglePress();
              } else {
                execAction(singlePressAction);
              }
              mGestureMode = "unknown";
            }
          }
        };
        S_HANDLER.postDelayed(sGestureSinglePressed, 500);
      }

      // Double press.
      if (sKeyUpCount == 2) {
        S_HANDLER.removeCallbacks(sGestureSinglePressed);
        sGestureDoublePressed = new Runnable() {
          public void run() {
            if (sKeyUpCount == 2) {
              sKeyUpCount = 0;
              Log.d(APP_TAG, "Exec Double Press Action");
              if (doublePressAction.equals(sActionsDefault)) {
                // Simulate the original event.
                simulateDoublePress();
              } else {
                execAction(doublePressAction);
              }
              mGestureMode = "unknown";
            }

          }
        };
        S_HANDLER.postDelayed(sGestureDoublePressed, 500);
      }

      // Triple press.
      if (sKeyUpCount == 3) {
        S_HANDLER.removeCallbacks(sGestureDoublePressed);
        if (sKeyUpCount == 3) {
          sKeyUpCount = 0;
          Log.d(APP_TAG, "Exec Triple Press Action");
          if (doublePressAction.equals(sActionsDefault)) {
            // Simulate the original event.
            simulateTriplePress();
          } else {
            execAction(triplePressAction);
          }
          mGestureMode = "unknown";
        }
      }
    }
    return true;
  }

  @Override
  public void onInterrupt() {
  }
}
