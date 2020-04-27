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
  private static int sKeyDownCount = 0;
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

  final Handler mHandler = new Handler();
  private String mGestureMode = "unknown";
  private boolean mIsSimulation = false;
  private Runnable mGestureLongPressed;
  private Runnable mGestureSinglePressed;
  private FlashlightProvider mFlashlightProvider;
  private StravaProvider mStravaProvider;

  @Override
  protected void onServiceConnected() {
    sAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
    sActionsDefault = getString(R.string.pref_button_actions_default);
    sActionsPlayPause = getString(R.string.pref_button_actions_playpause);
    sActionsNext = getString(R.string.pref_button_actions_next);
    sActionsPrevious = getString(R.string.pref_button_actions_previous);
    sActionsVolumeUp = getString(R.string.pref_button_actions_volume_up);
    sActionsVolumeDown = getString(R.string.pref_button_actions_volume_down);
    sActionsVolumeMute = getString(R.string.pref_button_actions_volume_mute);
    sActionsFlashlightToggle = getString(R.string.pref_button_actions_flashlight_toggle);
    sActionsStravaToggle = getString(R.string.pref_button_actions_strava_toggle);
    mFlashlightProvider = new FlashlightProvider(this);
    mStravaProvider = new StravaProvider(this);
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

    if (keycode != KeyEvent.KEYCODE_HEADSETHOOK && keycode != KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
      // Not interested in any other keys
      Log.i(APP_TAG, "Ignored " + keycode);
      return false;
    }

    // allow simulated keyEvents from this service to go through
    if (mIsSimulation) {
      mIsSimulation = false; // reset simulation mode for next event
      return false;
    }

    SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);

    Log.d(APP_TAG, ("Broadcast Key " + keycode));
    Intent intent = new Intent(getPackageName());
    sendBroadcast(intent);


    final String singlePressAction = pref.getString("hcp_gestures_single_press",
            sActionsPlayPause);
    final String doublePressAction = pref.getString("hcp_gestures_double_press",
            sActionsNext);
    final String longPressAction = pref.getString("hcp_gestures_long_press",
            sActionsPrevious);

    // Long Press
    if (action == KeyEvent.ACTION_DOWN) {
      mGestureLongPressed = new Runnable() {
        public void run() {
          mGestureMode = "long_press";
          Log.i(APP_TAG, "Exec Long Press Action");
          if (longPressAction.equals(sActionsDefault)) {
            simulateLongPress();
          } else {
            execAction(longPressAction);
          }
        }
      };
      // start tracking long press. If no action up is detected after 1100ms,
      // consider ut as long press
      mHandler.postDelayed(mGestureLongPressed, 1100);
    }

    //Single and Double Click
    if (action == KeyEvent.ACTION_UP) {
      sKeyDownCount++;
      mHandler.removeCallbacks(mGestureLongPressed);
      mGestureSinglePressed = new Runnable() {
        public void run() {
          // single press
          if (sKeyDownCount == 1) {
            //check if this keyup event is not following a long press event
            if (mGestureMode != "long_press") {
              Log.i(APP_TAG, "Exec Single Press Action");
              if (singlePressAction.equals(sActionsDefault)) {
                // simulate the original event;
                simulateSinglePress();
              } else {
                execAction(singlePressAction);
              }
            }
            mGestureMode = "unknown";
          }
          // double press
          if (sKeyDownCount == 2) {
            if (mGestureMode != "long_press") {
              Log.i(APP_TAG, "Exec Double Press Action");
              if (doublePressAction.equals(sActionsDefault)) {
                // simulate the original event;
                simulateDoublePress();
              } else {
                execAction(doublePressAction);
              }
            }
            mGestureMode = "unknown";
          }
          sKeyDownCount = 0;
        }
      };
      if (sKeyDownCount == 1) {
        mHandler.postDelayed(mGestureSinglePressed, 400);
      }
    }
    return true;
  }

  @Override
  public void onInterrupt() {
  }

  private void execAction(String action) {
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
      toggleFlashlight();
      return;
    }
    if (action.equals(sActionsStravaToggle)) {
      toggleStrava();
      return;
    }
  }

  private void simulateSinglePress() {
    mIsSimulation = true; // set to true each time, to allow it go through and be handled by system
    long eventtime = SystemClock.uptimeMillis();
    KeyEvent downEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_DOWN,
            KeyEvent.KEYCODE_HEADSETHOOK, 0);
    sAudioManager.dispatchMediaKeyEvent(downEvent);

    mIsSimulation = true; // set to true each time, to allow it go through and be handled by system
    KeyEvent upEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_UP,
            KeyEvent.KEYCODE_HEADSETHOOK, 0);
    sAudioManager.dispatchMediaKeyEvent(upEvent);

    Log.i(APP_TAG, "simulated single press");
  }

  private void simulateDoublePress() {

    mIsSimulation = true; // set to true each time, to allow it go through and be handled by system
    long eventtime = SystemClock.uptimeMillis();
    KeyEvent downEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_DOWN,
            KeyEvent.KEYCODE_HEADSETHOOK, 0);
    sAudioManager.dispatchMediaKeyEvent(downEvent);

    mIsSimulation = true; // set to true each time, to allow it go through and be handled by system
    KeyEvent upEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_UP,
            KeyEvent.KEYCODE_HEADSETHOOK, 0);
    sAudioManager.dispatchMediaKeyEvent(upEvent);

    mIsSimulation = true; // set to true each time, to allow it go through and be handled by system
    KeyEvent downEvent2 = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_DOWN,
            KeyEvent.KEYCODE_HEADSETHOOK, 0);
    sAudioManager.dispatchMediaKeyEvent(downEvent2);

    mIsSimulation = true; // set to true each time, to allow it go through and be handled by system
    KeyEvent upEvent2 = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_UP,
            KeyEvent.KEYCODE_HEADSETHOOK, 0);
    sAudioManager.dispatchMediaKeyEvent(upEvent2);

    Log.i(APP_TAG, "simulated double press");

  }

  private void simulateLongPress() {
    mIsSimulation = true; // set to true each time, to allow it go through and be handled by system

    sAudioManager.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,
            KeyEvent.KEYCODE_HEADSETHOOK));
    mGestureLongPressed = new Runnable() {
      public void run() {
        sAudioManager.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,
                KeyEvent.KEYCODE_HEADSETHOOK));
      }
    };
    //schedule keyup event after longpress timeout
    mHandler.postDelayed(mGestureLongPressed, ViewConfiguration.get(this).getLongPressTimeout());

    Log.i(APP_TAG, "simulated long press");
  }

  private void playPause() {
    long eventtime = SystemClock.uptimeMillis();

    KeyEvent downEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_DOWN,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, 0);
    sAudioManager.dispatchMediaKeyEvent(downEvent);

    KeyEvent upEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_UP,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, 0);
    sAudioManager.dispatchMediaKeyEvent(upEvent);

    Log.i(APP_TAG, "Play Pause");
  }

  private void nextTrack() {
    long eventtime = SystemClock.uptimeMillis();
    KeyEvent downEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_DOWN,
            KeyEvent.KEYCODE_MEDIA_NEXT, 0);
    sAudioManager.dispatchMediaKeyEvent(downEvent);
    if (isPlaying) {
      KeyEvent upEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_UP,
              KeyEvent.KEYCODE_MEDIA_PLAY, 0);
      sAudioManager.dispatchMediaKeyEvent(upEvent);
    }
    Log.i(APP_TAG, "Next Track");
  }

  private void previousTrack() {
    long eventtime = SystemClock.uptimeMillis();
    KeyEvent downEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_DOWN,
            KeyEvent.KEYCODE_MEDIA_PREVIOUS, 0);
    sAudioManager.dispatchMediaKeyEvent(downEvent);
    if (isPlaying) {
      KeyEvent upEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_UP,
              KeyEvent.KEYCODE_MEDIA_PLAY, 0);
      sAudioManager.dispatchMediaKeyEvent(upEvent);
    }
    Log.i(APP_TAG, "Prev Track");
  }

  private void increaseVolume() {
    sAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE,
            AudioManager.FLAG_SHOW_UI);
    Log.i(APP_TAG, "Inc Vol");
  }

  private void decreaseVolume() {
    sAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER,
            AudioManager.FLAG_SHOW_UI);
    Log.i(APP_TAG, "Dec Vol");
  }

  private void muteVolume() {
    sAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE,
            AudioManager.FLAG_SHOW_UI);
    Log.i(APP_TAG, "Mut Vol");
  }

  private void toggleFlashlight() {
    mFlashlightProvider.toggleFlashLight();
    Log.i(APP_TAG, "Toggle Flashlight");
  }

  private void toggleStrava() {
    mStravaProvider.toggleRecord();
    Log.i(APP_TAG, "Toggle Strava");
  }
}