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

public class HeadsetControlPlusService extends AccessibilityService {
    private static final String APP_TAG = "HeadsetControlPlus";
    private AudioManager audioManager;
    String actionsDefault;
    String actionsPlayPause;
    String actionsNext;
    String actionsPrevious;
    String actionsVolumeUp;
    String actionsVolumeDown;
    String actionsVolumeMute;
    String gestureMode = "unknown";
    boolean isPlaying = false;
    boolean isSimulation = false;
    final Handler handler = new Handler();
    Runnable mLongPressed;
    Runnable mSinglePressed;
    static int d = 0;

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

        /*
        Note: It is important that key events are handled in such a way that the event
        stream that would be passed to the rest of the system is well-formed. For example,
        handling the down event but not the up event and vice versa would generate an
        inconsistent event stream.
        source: https://developer.android.com/reference/android/accessibilityservice/AccessibilityService#onKeyEvent(android.view.KeyEvent)
        */

        // Long Press
        if (action == KeyEvent.ACTION_DOWN) {
            mLongPressed = new Runnable() {
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
            // start tracking long press. If no action up is detected after 1100ms, consider ut as long press
            handler.postDelayed(mLongPressed, 1100);
        }

        //Single and Double Click
        if (action == KeyEvent.ACTION_UP) {
            d++;
            handler.removeCallbacks(mLongPressed);
            mSinglePressed = new Runnable() {
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
                handler.postDelayed(mSinglePressed, 400);
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
        }
        if (action.equals(actionsNext)) {
            nextTrack();
        }
        if (action.equals(actionsPrevious)) {
            previousTrack();
        }
        if (action.equals(actionsVolumeDown)) {
            decreaseVolume();
        }
        if (action.equals(actionsVolumeUp)) {
            increaseVolume();
        }
        if (action.equals(actionsVolumeMute)) {
            muteVolume();
        }
    }

    private void simulateSinglePress() {
        isSimulation = true; // set to true each time, to allow it go through and be handled by system
        long eventtime = SystemClock.uptimeMillis();
        KeyEvent downEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_HEADSETHOOK, 0);
        audioManager.dispatchMediaKeyEvent(downEvent);

        isSimulation = true; // set to true each time, to allow it go through and be handled by system
        KeyEvent upEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_HEADSETHOOK, 0);
        audioManager.dispatchMediaKeyEvent(upEvent);

        Log.i(APP_TAG, "simulated single press");
    }

    private void simulateDoublePress() {

        isSimulation = true; // set to true each time, to allow it go through and be handled by system
        long eventtime = SystemClock.uptimeMillis();
        KeyEvent downEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_HEADSETHOOK, 0);
        audioManager.dispatchMediaKeyEvent(downEvent);

        isSimulation = true; // set to true each time, to allow it go through and be handled by system
        KeyEvent upEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_HEADSETHOOK, 0);
        audioManager.dispatchMediaKeyEvent(upEvent);

        isSimulation = true; // set to true each time, to allow it go through and be handled by system
        KeyEvent downEvent2 = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_HEADSETHOOK, 0);
        audioManager.dispatchMediaKeyEvent(downEvent2);

        isSimulation = true; // set to true each time, to allow it go through and be handled by system
        KeyEvent upEvent2 = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_HEADSETHOOK, 0);
        audioManager.dispatchMediaKeyEvent(upEvent2);

        Log.i(APP_TAG, "simulated double press");

    }

    private void simulateLongPress() {
        isSimulation = true; // set to true each time, to allow it go through and be handled by system

        audioManager.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_HEADSETHOOK));
        mLongPressed = new Runnable() {
            public void run() {
                audioManager.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_HEADSETHOOK));
            }
        };
        //schedule keyup event after longpress timeout
        handler.postDelayed(mLongPressed, ViewConfiguration.get(this).getLongPressTimeout());

        Log.i(APP_TAG, "simulated long press");
    }

    private void playPause() {
        long eventtime = SystemClock.uptimeMillis();

        KeyEvent downEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, 0);
        audioManager.dispatchMediaKeyEvent(downEvent);

        KeyEvent upEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, 0);
        audioManager.dispatchMediaKeyEvent(upEvent);

        Log.i(APP_TAG, "Play Pause");
    }

    private void nextTrack() {
        long eventtime = SystemClock.uptimeMillis();
        KeyEvent downEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT, 0);
        audioManager.dispatchMediaKeyEvent(downEvent);
        if (isPlaying) {
            KeyEvent upEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY, 0);
            audioManager.dispatchMediaKeyEvent(upEvent);
        }
        Log.i(APP_TAG, "Next Track");
    }

    private void previousTrack() {
        long eventtime = SystemClock.uptimeMillis();
        KeyEvent downEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PREVIOUS, 0);
        audioManager.dispatchMediaKeyEvent(downEvent);
        if (isPlaying) {
            KeyEvent upEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY, 0);
            audioManager.dispatchMediaKeyEvent(upEvent);
        }
        Log.i(APP_TAG, "Prev Track");
    }

    private void increaseVolume() {
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
        Log.i(APP_TAG, "Inc Vol");
    }

    private void decreaseVolume() {
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
        Log.i(APP_TAG, "Dec Vol");
    }

    private void muteVolume() {
        Log.i(APP_TAG, "Mut Vol");
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, AudioManager.FLAG_SHOW_UI);
    }
}