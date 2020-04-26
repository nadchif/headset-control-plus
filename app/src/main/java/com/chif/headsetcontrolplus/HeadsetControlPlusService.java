/*
 * HeadsetControlPlusService.java
 *
 *
 * Copyright 2020 github.com/nadchif
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */


package com.chif.headsetcontrolplus;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityEvent;
import androidx.preference.PreferenceManager;
import com.chif.headsetcontrolplus.providers.FlashlightProvider;
import com.chif.headsetcontrolplus.providers.StravaProvider;

public class HeadsetControlPlusService extends AccessibilityService {
  private static final String APP_TAG = HeadsetControlPlusService.class.getSimpleName();
  static int d = 0;
  final Handler handler = new Handler();
  private static String actionsDefault;
  private static String actionsPlayPause;
  private static String actionsNext;
  private static String actionsPrevious;
  private static String actionsVolumeUp;
  private static String actionsVolumeDown;
  private static String actionsVolumeMute;
  private static String actionsFlashlightToggle;
  private static String actionsStravaToggle;
  private String gestureMode = "unknown";
  private static boolean isPlaying = false;
  private boolean isSimulation = false;
  private Runnable gestureLongPressed;
  private Runnable gestureSinglePressed;
  private AudioManager audioManager;
  private FlashlightProvider flashlightProvider;
  private StravaProvider stravaProvider;

  @Override
  protected void onServiceConnected() {
    audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
    actionsDefault = getString(R.string.pref_button_actions_default);
    actionsPlayPause = getString(R.string.pref_button_actions_playpause);
    actionsNext = getString(R.string.pref_button_actions_next);
    actionsPrevious = getString(R.string.pref_button_actions_previous);
    actionsVolumeUp = getString(R.string.pref_button_actions_volume_up);
    actionsVolumeDown = getString(R.string.pref_button_actions_volume_down);
    actionsVolumeMute = getString(R.string.pref_button_actions_volume_mute);
    actionsFlashlightToggle = getString(R.string.pref_button_actions_flashlight_toggle);
    actionsStravaToggle = getString(R.string.pref_button_actions_strava_toggle);
    flashlightProvider = new FlashlightProvider(this);
    stravaProvider = new StravaProvider(this);
    Log.i(APP_TAG, "Service COnnected");
  }

  @Override
  public void onAccessibilityEvent(AccessibilityEvent event) {

  }

  @Override
  public boolean onKeyEvent(KeyEvent event) {
    int keycode = event.getKeyCode();
    int action = event.getAction();

    if (action != KeyEvent.ACTION_UP && action != KeyEvent.ACTION_DOWN || event.isCanceled()) {
      return false;
    }

    if (keycode != KeyEvent.KEYCODE_HEADSETHOOK) {
      // Not interested in any other keys
      Log.i(APP_TAG, "Ignored " + keycode);
      return false;
    }

    // allow simulated keyEvents from this service to go through
    if (isSimulation) {
      isSimulation = false; // reset simulation mode for next event
      return false;
    }

    SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);

    Log.d(APP_TAG, ("Broadcast Key " + keycode));
    Intent intent = new Intent(getPackageName());
    sendBroadcast(intent);


    final String singlePressAction = pref.getString("hcp_gestures_single_press",
            actionsPlayPause);
    final String doublePressAction = pref.getString("hcp_gestures_double_press",
            actionsNext);
    final String longPressAction = pref.getString("hcp_gestures_long_press",
            actionsPrevious);

    // Long Press
    if (action == KeyEvent.ACTION_DOWN) {
      gestureLongPressed = new Runnable() {
        public void run() {
          gestureMode = "long_press";
          Log.i(APP_TAG, "Exec Long Press Action");
          if (longPressAction.equals(actionsDefault)) {
            simulateLongPress();
          } else {
            execAction(longPressAction);
          }
        }
      };
      // start tracking long press. If no action up is detected after 1100ms,
      // consider ut as long press
      handler.postDelayed(gestureLongPressed, 1100);
    }

    //Single and Double Click
    if (action == KeyEvent.ACTION_UP) {
      d++;
      handler.removeCallbacks(gestureLongPressed);
      gestureSinglePressed = new Runnable() {
        public void run() {
          // single press
          if (d == 1) {
            //check if this keyup event is not following a long press event
            if (gestureMode != "long_press") {
              Log.i(APP_TAG, "Exec Single Press Action");
              if (singlePressAction.equals(actionsDefault)) {
                // simulate the original event;
                simulateSinglePress();
              } else {
                execAction(singlePressAction);
              }
            }
            gestureMode = "unknown";
          }
          // double press
          if (d == 2) {
            if (gestureMode != "long_press") {
              Log.i(APP_TAG, "Exec Double Press Action");
              if (doublePressAction.equals(actionsDefault)) {
                // simulate the original event;
                simulateDoublePress();
              } else {
                execAction(doublePressAction);
              }
            }
            gestureMode = "unknown";
          }
          d = 0;
        }
      };
      if (d == 1) {
        handler.postDelayed(gestureSinglePressed, 400);
      }
    }
    return true;
  }

  @Override
  public void onInterrupt() {
  }

  private void execAction(String action) {
    isPlaying = audioManager.isMusicActive();
    if (action.equals(actionsPlayPause)) {
      playPause();
      return;
    }
    if (action.equals(actionsNext)) {
      nextTrack();
      return;
    }
    if (action.equals(actionsPrevious)) {
      previousTrack();
      return;
    }
    if (action.equals(actionsVolumeDown)) {
      decreaseVolume();
      return;
    }
    if (action.equals(actionsVolumeUp)) {
      increaseVolume();
      return;
    }
    if (action.equals(actionsVolumeMute)) {
      muteVolume();
      return;
    }
    if (action.equals(actionsFlashlightToggle)) {
      toggleFlashlight();
      return;
    }
    if (action.equals(actionsStravaToggle)) {
      toggleStrava();
      return;
    }
  }

  private void simulateSinglePress() {
    isSimulation = true; // set to true each time, to allow it go through and be handled by system
    long eventtime = SystemClock.uptimeMillis();
    KeyEvent downEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_DOWN,
            KeyEvent.KEYCODE_HEADSETHOOK, 0);
    audioManager.dispatchMediaKeyEvent(downEvent);

    isSimulation = true; // set to true each time, to allow it go through and be handled by system
    KeyEvent upEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_UP,
            KeyEvent.KEYCODE_HEADSETHOOK, 0);
    audioManager.dispatchMediaKeyEvent(upEvent);

    Log.i(APP_TAG, "simulated single press");
  }

  private void simulateDoublePress() {

    isSimulation = true; // set to true each time, to allow it go through and be handled by system
    long eventtime = SystemClock.uptimeMillis();
    KeyEvent downEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_DOWN,
            KeyEvent.KEYCODE_HEADSETHOOK, 0);
    audioManager.dispatchMediaKeyEvent(downEvent);

    isSimulation = true; // set to true each time, to allow it go through and be handled by system
    KeyEvent upEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_UP,
            KeyEvent.KEYCODE_HEADSETHOOK, 0);
    audioManager.dispatchMediaKeyEvent(upEvent);

    isSimulation = true; // set to true each time, to allow it go through and be handled by system
    KeyEvent downEvent2 = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_DOWN,
            KeyEvent.KEYCODE_HEADSETHOOK, 0);
    audioManager.dispatchMediaKeyEvent(downEvent2);

    isSimulation = true; // set to true each time, to allow it go through and be handled by system
    KeyEvent upEvent2 = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_UP,
            KeyEvent.KEYCODE_HEADSETHOOK, 0);
    audioManager.dispatchMediaKeyEvent(upEvent2);

    Log.i(APP_TAG, "simulated double press");

  }

  private void simulateLongPress() {
    isSimulation = true; // set to true each time, to allow it go through and be handled by system

    audioManager.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,
            KeyEvent.KEYCODE_HEADSETHOOK));
    gestureLongPressed = new Runnable() {
      public void run() {
        audioManager.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,
                KeyEvent.KEYCODE_HEADSETHOOK));
      }
    };
    //schedule keyup event after longpress timeout
    handler.postDelayed(gestureLongPressed, ViewConfiguration.get(this).getLongPressTimeout());

    Log.i(APP_TAG, "simulated long press");
  }

  private void playPause() {
    long eventtime = SystemClock.uptimeMillis();

    KeyEvent downEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_DOWN,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, 0);
    audioManager.dispatchMediaKeyEvent(downEvent);

    KeyEvent upEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_UP,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, 0);
    audioManager.dispatchMediaKeyEvent(upEvent);

    Log.i(APP_TAG, "Play Pause");
  }

  private void nextTrack() {
    long eventtime = SystemClock.uptimeMillis();
    KeyEvent downEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_DOWN,
            KeyEvent.KEYCODE_MEDIA_NEXT, 0);
    audioManager.dispatchMediaKeyEvent(downEvent);
    if (isPlaying) {
      KeyEvent upEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_UP,
              KeyEvent.KEYCODE_MEDIA_PLAY, 0);
      audioManager.dispatchMediaKeyEvent(upEvent);
    }
    Log.i(APP_TAG, "Next Track");
  }

  private void previousTrack() {
    long eventtime = SystemClock.uptimeMillis();
    KeyEvent downEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_DOWN,
            KeyEvent.KEYCODE_MEDIA_PREVIOUS, 0);
    audioManager.dispatchMediaKeyEvent(downEvent);
    if (isPlaying) {
      KeyEvent upEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_UP,
              KeyEvent.KEYCODE_MEDIA_PLAY, 0);
      audioManager.dispatchMediaKeyEvent(upEvent);
    }
    Log.i(APP_TAG, "Prev Track");
  }

  private void increaseVolume() {
    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE,
            AudioManager.FLAG_SHOW_UI);
    Log.i(APP_TAG, "Inc Vol");
  }

  private void decreaseVolume() {
    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER,
            AudioManager.FLAG_SHOW_UI);
    Log.i(APP_TAG, "Dec Vol");
  }

  private void muteVolume() {
    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE,
            AudioManager.FLAG_SHOW_UI);
    Log.i(APP_TAG, "Mut Vol");
  }

  private void toggleFlashlight() {
    flashlightProvider.toggleFlashLight();
    Log.i(APP_TAG, "Toggle Flashlight");
  }

  private void toggleStrava() {
    stravaProvider.toggleRecord();
    Log.i(APP_TAG, "Toggle Strava");
  }
}