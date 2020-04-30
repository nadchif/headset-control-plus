Troubleshoot - Headset Control Plus
====

* [The app does not detect my headset](https://github.com/nadchif/headset-control-plus/blob/master/docs/TROUBLESHOOT.md#the-app-does-not-detect-my-headset)
* [The app does not work when the screen is off](https://github.com/nadchif/headset-control-plus/blob/master/docs/TROUBLESHOOT.md#the-app-does-not-work-when-the-screen-is-off)
* [I'm experiencing another issue](https://github.com/nadchif/headset-control-plus/issues/new?assignees=&labels=bug&template=bug_report.md&title=)

## The app does not detect my headset

There are several reason's why the headset may work as expected. Headset Control Plus works by running as an [Accessibility Service](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService). As a service, it listens for [keyEvents](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService#onKeyEvent(android.view.KeyEvent)) from the user. If the keyEvent is from [the headset key](https://developer.android.com/reference/android/view/KeyEvent#KEYCODE_HEADSETHOOK), the service will react and run the action the user has set. ([See Code](https://github.com/nadchif/headset-control-plus/blob/master/app/src/main/java/com/chif/headsetcontrolplus/HeadsetControlPlusService.java)). 

Check the following:

1. The headset button actually works

2. Headset Control Plus is enabled as an [Accessibility Service](https://www.techbone.net/android/user-manual/accessibility-menu#android_9). 
      
      See: [How to get to Accessibility Settings](https://www.techbone.net/android/user-manual/accessibility-menu#android_9)

If this is still not working [click here](https://github.com/nadchif/headset-control-plus/issues/new?assignees=&labels=bug&template=bug_report.md&title=) to bring the issue to our attention

## The app does not work when the screen is off
~~This is a known issue, the Accessibility Service can only receive [keyEvents](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService#onKeyEvent(android.view.KeyEvent)) when the screen is on. We are still figuring a way to make it continue to work while the screen is off.~~
In the recent versions of Headset Control Plus, gestures can now work while the screen is off. However, the following limitations still apply:
1. Some "aggressive" music player can "steal" [MediaSession](https://developer.android.com/reference/android/support/v4/media/session/MediaSessionCompat) focus, HCP may no longer receive Headset Events, until the screen goes on and off again.
2. On some devices, `long press` may still not work when screen is off, because of apps like BixBy that possess the gesture action.


## I press play/pause and nothing happens
This happens could happen if 1) you have not recently played music from a music player or your music player does not support commands from *com.android.music.musicservicecommand*.
For controlling music, Headset Control Plus, broadcasts intents to *com.android.music.musicservicecommand*

Play/Pause:
action: `com.android.music.musicservicecommand`
extra: `"command":"togglepause"`

Next Track:
action: `com.android.music.musicservicecommand`
extra: `"command":"next"`

Previous Track:
action: `com.android.music.musicservicecommand`
extra: `"command":"previous"`

Whether these commands are acted upon after the broadcast, depends on your music app.

## Strava actions not wroking
The Strava actions work by running the `http://strava.com/nfc/record/toggle` intent. How this is then handled depends on your operating system configuration and Strava. For example, if your operating system is set to launch the URL in a browser, **IT WILL NOT WORK**. Needless to say, Strava must be already installed and you must be logged in already.

Some users have also reported issues like [this](https://groups.google.com/d/msg/strava-api/Uywi_830YWE/aqWmzyr8CwAJ)


