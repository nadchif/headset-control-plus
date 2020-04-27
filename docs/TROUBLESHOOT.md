Troubleshoot - Headset Control Plus
====

* [The app does not detect my headset](#)
* [The app does not work when the screen is off](#)
* [I'm experiencing another issue](https://github.com/nadchif/headset-control-plus/issues/new?assignees=&labels=bug&template=bug_report.md&title=)

## The app does not detect my headset

There are several reason's why the headset may work as expected. Headset Control Plus works by running as an [Accessibility Service](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService). As a service, it listens for [keyEvents](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService#onKeyEvent(android.view.KeyEvent)) from the user. If the keyEvent is from [the headset key](https://developer.android.com/reference/android/view/KeyEvent#KEYCODE_HEADSETHOOK), the service will react and run the action the user has set. ([See Code](https://github.com/nadchif/headset-control-plus/blob/master/app/src/main/java/com/chif/headsetcontrolplus/HeadsetControlPlusService.java)). 

Check the following:

1. The headset button actually works

2. Headset Control Plus is enabled as an [Accessibility Service](https://www.verizonwireless.com/support/knowledge-base-215346/). 

If this is still not working [click here](https://github.com/nadchif/headset-control-plus/issues/new?assignees=&labels=bug&template=bug_report.md&title=) to bring the issue to our attention

## The app does not work when the screen is off
This is a known issue, the Accessibility Service can only receive [keyEvents](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService#onKeyEvent(android.view.KeyEvent)) when the screen is on. We are still figuring a way to make it continue to work while the screen is off.

