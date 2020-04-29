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
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;
import android.view.KeyEvent;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.media.session.MediaButtonReceiver;
import androidx.preference.PreferenceManager;

public class ForegroundService extends Service {
  public static final String CHANNEL_ID = "ForegroundServiceChannel";
  private static final String APP_TAG = HeadsetControlPlusService.class.getSimpleName();

  private static boolean sScheduleSinglePress = false;
  private static boolean sScheduleDoublePress = false;
  private static boolean sScheduleLongPress = false;
  private static int sKeyDownCount = 0;
  private static PowerManager sPowerManager;
  private static PowerManager.WakeLock sWakeLock;
  private final Handler mHandler = new Handler();
  private MediaSessionCompat mMediaSessionCompat;
  private MediaPlayer mMediaPlayer;
  private ScreenOnOffReceiver mScreenOnOffReceiver;
  private String mGestureMode = "unknown";
  private Runnable mGestureLongPressed;
  private Runnable mGestureSinglePressed;


  @Override
  public void onCreate() {
    super.onCreate();
    sPowerManager = (PowerManager) this.getSystemService(this.POWER_SERVICE);
    sWakeLock = sPowerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK
            | PowerManager.ACQUIRE_CAUSES_WAKEUP
            | PowerManager.ON_AFTER_RELEASE, "hcp::WakeLock");

    mMediaSessionCompat = new MediaSessionCompat(this, "MediaSessionCompat");
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
    registerScreenStatusReceiver();
    return START_STICKY;
  }

  @Override
  public void onDestroy() {
    unregisterScreenStatusReceiver();
    if (mMediaPlayer != null) {
      mMediaPlayer.stop();
    }
    mMediaSessionCompat.release();
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
              NotificationManager.IMPORTANCE_HIGH
      );
      NotificationManager manager = getSystemService(NotificationManager.class);
      manager.createNotificationChannel(serviceChannel);
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
      Log.e(APP_TAG, "FG svc key " + keycode);
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
            sWakeLock.acquire();
            Log.w(APP_TAG, "Scheduled long press Action");
            sScheduleDoublePress = true;
            sWakeLock.release();
          }
        };
        // Start tracking long press. If no action up is detected after 950ms,
        // consider ut as long press.
        mHandler.postDelayed(mGestureLongPressed, 950);
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
                sWakeLock.acquire();
                Log.w(APP_TAG, "Scheduled single press Action");
                sScheduleSinglePress = true;
                sWakeLock.release();
              }
              mGestureMode = "unknown";
            }
            // Double press.
            if (sKeyDownCount == 2) {
              if (mGestureMode != "long_press") {
                sWakeLock.acquire();
                Log.w(APP_TAG, "Scheduled double press Action");
                sScheduleDoublePress = true;
                sWakeLock.release();
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
    public int onStartCommand(final Intent intent, int flags, int startId) {
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

        mMediaSessionCompat.setActive(true);
        Log.e(APP_TAG, "Screen Off...launched silent media clip");
        mMediaPlayer = MediaPlayer.create(this.mServiceContext, R.raw.sound);
        mMediaPlayer.setVolume(0, 0);
        mMediaPlayer.setLooping(true);
        mMediaPlayer.start();

      } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
        mIsScreenOn = true;
        mMediaPlayer.stop();
        mMediaSessionCompat.setActive(false);
        // handle scheduled events once screen is on
        if (sScheduleSinglePress) {
          sScheduleSinglePress = false;
          HeadsetControlPlusService.handleGesture("single");
        }
        // handle scheduled events once screen is on
        if (sScheduleDoublePress) {
          sScheduleDoublePress = false;
          HeadsetControlPlusService.handleGesture("double");
        }
        // handle scheduled events once screen is on
        if (sScheduleLongPress) {
          sScheduleLongPress = false;
          HeadsetControlPlusService.handleGesture("long");
        }
        Log.e(APP_TAG, "Screen On...turned off media clip");
      }
    }

    public boolean isScreenOn() {
      return mIsScreenOn;
    }
  }

}
