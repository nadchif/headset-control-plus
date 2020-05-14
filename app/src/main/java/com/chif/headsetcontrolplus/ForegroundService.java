/**
 * Foreground service that provides headset gesture support while the screen is off. Uses the
 * following tactic to work.
 *
 * <p>1. Register a Media Session
 * 2. Listen for screen on or off events
 * a. when the screen is turned off, it silently plays a music file so that it can receive
 * headset/media commands from the system even while screen is off. During this time if
 * it intercepts a media button gesture, it schedules it for processing once the screen is on.
 * Then it lights up the screen briefly, for user feedback and for processing the gesture action
 * b. when screen is turned on, stops playing the file, to save battery consumption. It also
 * executes the actions for the headset gestures that we detected when screen was off
 *
 * <p>ISSUES:
 * now and then another media player may steal focus (this has happened when Samsung music player
 * changes to next track on its own, in the background) and the service stops receiving the
 * Media button press broadcast. However when the screen turns off again, the foreground service
 * receives the commands as expected.
 */

package com.chif.headsetcontrolplus;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.media.session.MediaButtonReceiver;
import androidx.preference.PreferenceManager;
import com.chif.headsetcontrolplus.shared.ServiceBase;

public class ForegroundService extends Service {
  public static final String CHANNEL_ID = "ForegroundServiceChannel";
  private static final String APP_TAG = HeadsetControlPlusService.class.getSimpleName();
  private static final Handler S_HANDLER = new Handler();
  private static AudioManager sAudioManager;

  private static int sKeyDownCount = 0;
  private final Handler mHandler = new Handler();
  private MediaSessionCompat mMediaSessionCompat;
  private MediaPlayer mMediaPlayer;
  private ScreenOnOffReceiver mScreenOnOffReceiver;
  private String mGestureMode = "unknown";
  private Runnable mGestureLongPressed;
  private Runnable mGestureSinglePressed;
  private Context mContext;
  private PlaybackStateCompat.Builder mStateBuilder;


  @Override
  public void onCreate() {
    super.onCreate();
    mContext = this;

    sAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

    mMediaSessionCompat = new MediaSessionCompat(this, "HCPMediaSessionCompat");

    mStateBuilder = new PlaybackStateCompat.Builder()
            .setActions(
                    PlaybackStateCompat.ACTION_PLAY
                            | PlaybackStateCompat.ACTION_PAUSE
                            | PlaybackStateCompat.ACTION_PLAY_PAUSE);

    mMediaSessionCompat.setPlaybackState(mStateBuilder.build());
    mMediaSessionCompat.setCallback(new MediaSessionCompat.Callback() {
      @Override
      public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
        if (mScreenOnOffReceiver.isScreenOn()) {
          return super.onMediaButtonEvent(mediaButtonEvent);
        }
        return handleMediaButton(mediaButtonEvent);
      }
    });
  }

  @Override
  public int onStartCommand(final Intent intent, final int flags, final int startId) {
    createNotificationChannel();
    Intent notificationIntent = new Intent(this, MainActivity.class);
    PendingIntent pendingIntent = PendingIntent.getActivity(this,
            0, notificationIntent, 0);
    Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Headset Control Plus Gestures")
            .setContentText("Enable gesture support when screen is off")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .build();
    startForeground(1, notification);

    confirmAccessibilityEnabled();
    confirmForegroundEnabled();

    registerScreenStatusReceiver();

    return START_STICKY;
  }

  @Override
  public void onDestroy() {
    unregisterScreenStatusReceiver();
    try {
      mMediaPlayer.reset();
      mMediaPlayer.release();
    } catch (Exception e) {
      Log.i(APP_TAG, "Failed to release MediaPlayer");
    }
    mMediaSessionCompat.release();
    if (!ServiceBase.isAccessibilityServiceEnabled(mContext, HeadsetControlPlusService.class)) {
      Toast.makeText(mContext, getString(R.string.err_require_access),
              Toast.LENGTH_SHORT).show();
      PreferenceManager.getDefaultSharedPreferences(mContext).edit()
              .putBoolean("enable_hcp_foreground_service", true).commit();
    }
    super.onDestroy();
  }

  @Nullable
  @Override
  public IBinder onBind(final Intent intent) {
    return null;
  }

  private void createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      NotificationChannel serviceChannel = new NotificationChannel(
              CHANNEL_ID,
              "Headset Control Plus Gesture Support",
              NotificationManager.IMPORTANCE_LOW
      );
      NotificationManager manager = getSystemService(NotificationManager.class);
      manager.createNotificationChannel(serviceChannel);
    }
  }

  private void confirmAccessibilityEnabled() {
    if (!ServiceBase.isAccessibilityServiceEnabled(mContext, HeadsetControlPlusService.class)) {
      stopSelf();
    }
  }

  private void confirmForegroundEnabled() {
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
    boolean runAutoStart = preferences
            .getBoolean("enable_hcp_foreground_service", false);
    if (!runAutoStart) {
      stopSelf();
    }
  }

  private boolean handleMediaButton(final Intent mediaButtonEvent) {
    KeyEvent event = (KeyEvent) mediaButtonEvent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
    if (event != null) {
      int keycode = event.getKeyCode();
      int action = event.getAction();

      if (action != KeyEvent.ACTION_UP && action != KeyEvent.ACTION_DOWN || event.isCanceled()) {
        return false;
      }

      if (keycode != KeyEvent.KEYCODE_HEADSETHOOK && keycode != KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
        // Not interested in any other key
        Log.i(APP_TAG, "Ignored " + keycode);
        return false;
      }

      SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);

      // Long Press.
      if (action == KeyEvent.ACTION_DOWN) {
        mGestureLongPressed = new Runnable() {
          public void run() {
            mGestureMode = "long_press";
            HeadsetControlPlusService.handleGesture("long");
            Log.w(APP_TAG, "Executed long press Action");
          }
        };
        // Start tracking long press. If no action up is detected after 950ms,
        // consider ut as long press.
        mHandler.postDelayed(mGestureLongPressed, 900);
      }


      // Single and Double Click.
      if (action == KeyEvent.ACTION_UP) {
        sKeyDownCount++;
        mHandler.removeCallbacks(mGestureLongPressed);
        mGestureSinglePressed = new Runnable() {
          public void run() {
            // Single press.
            if (sKeyDownCount == 1) {
              // Check if this keyup event is not following a long press event.
              if (mGestureMode != "long_press") {
                HeadsetControlPlusService.handleGesture("single");
                Log.w(APP_TAG, "Executed single press Action");
              }
              mGestureMode = "unknown";
            }
            // Double press.
            if (sKeyDownCount == 2) {
              if (mGestureMode != "long_press") {
                HeadsetControlPlusService.handleGesture("double");
                Log.w(APP_TAG, "Executed double press Action");
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
    }
    return true;
  }

  private void registerScreenStatusReceiver() {
    mScreenOnOffReceiver = new ScreenOnOffReceiver(this);
    IntentFilter filter = new IntentFilter();
    filter.addAction(Intent.ACTION_SCREEN_OFF);
    filter.addAction(Intent.ACTION_SCREEN_ON);
    registerReceiver(mScreenOnOffReceiver, filter);
  }

  private void unregisterScreenStatusReceiver() {
    try {
      if (mScreenOnOffReceiver != null) {
        unregisterReceiver(mScreenOnOffReceiver);
      }
    } catch (IllegalArgumentException e) {
      Log.d(APP_TAG, "Error unregistering receiver");
    }
  }

  public class MediaButtonService extends Service {
    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
      MediaButtonReceiver.handleIntent(mMediaSessionCompat, intent);
      return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(final Intent intent) {
      return null;
    }

  }

  public class ScreenOnOffReceiver extends BroadcastReceiver {
    private Context mServiceContext;
    private boolean mIsScreenOn = true;

    ScreenOnOffReceiver(final Context context) {
      this.mServiceContext = context;
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
      if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
        mIsScreenOn = false;
        mMediaSessionCompat.setCallback(new MediaSessionCompat.Callback() {
          @Override
          public boolean onMediaButtonEvent(final Intent mediaButtonEvent) {
            if (mScreenOnOffReceiver.isScreenOn()) {
              return super.onMediaButtonEvent(mediaButtonEvent);
            }
            return handleMediaButton(mediaButtonEvent);
          }
        });

        Log.i(APP_TAG, "Screen Off...launched silent media clip");

        // get focus already (by calling forceActivatePlayer), then start loop
        startMediaPlayerLoop(true);

      } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
        mIsScreenOn = true;

        mMediaSessionCompat.setActive(false);
        if (!ServiceBase.isAccessibilityServiceEnabled(context, HeadsetControlPlusService.class)) {
          Intent serviceIntent = new Intent(context, ForegroundService.class);
          context.stopService(serviceIntent);
        }
        Log.i(APP_TAG, "Screen On...turned off media clip");
      }
    }

    private void startMediaPlayerLoop(boolean forceActivatePlayer) {
      // check if audio is currently playing;
      if (sAudioManager.isMusicActive() || forceActivatePlayer) {
        //there's a music player which could steal focus. regain focus by playing a silent clip
        mMediaPlayer = MediaPlayer.create(this.mServiceContext, R.raw.soundclip);
        mMediaPlayer.setVolume(0, 0);
        //mMediaPlayer.setLooping(true);
        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
          @Override
          public void onCompletion(MediaPlayer mediaPlayer) {
            mediaPlayer.reset();
            mediaPlayer.release();
            if (!mIsScreenOn) {
              startMediaPlayerLoop(false);
            }
          }
        });
        mMediaPlayer.start();
        mMediaSessionCompat.setActive(true);
      } else {
        //No media player is playing, so no need to steal back focus
        Runnable mediaPlayerLoopCheck = new Runnable() {
          public void run() {
            if (!mIsScreenOn) {
              startMediaPlayerLoop(false);
            }
          }
        };
        S_HANDLER.postDelayed(mediaPlayerLoopCheck, 1000);
      }
    }

    public boolean isScreenOn() {
      return mIsScreenOn;
    }
  }

}
