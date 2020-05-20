/** Shared Code Base.
 * Hosts common and shared service functions
 */

package com.chif.headsetcontrolplus.shared;

import android.content.ComponentName;
import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.KeyEvent;

public class ServiceBase {
  /**
   * Checks if the Accessibility Service is enabled. Returns true if it is
   * @param context - The Context
   * @param accessibilityService - the Class of the accessibility service.
   *                            Like "HeadsetControlPlusService.class"
   * @return true if service is enabled
   */
  public static boolean isAccessibilityServiceEnabled(final Context context,
                                                       final Class<?> accessibilityService) {
    ComponentName expectedComponentName = new ComponentName(context, accessibilityService);

    String enabledServicesSetting = Settings.Secure.getString(context.getContentResolver(),
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
    if (enabledServicesSetting == null) {
      return false;
    }

    TextUtils.SimpleStringSplitter colonSplitter = new TextUtils.SimpleStringSplitter(':');
    colonSplitter.setString(enabledServicesSetting);

    while (colonSplitter.hasNext()) {
      String componentNameString = colonSplitter.next();
      ComponentName enabledService = ComponentName.unflattenFromString(componentNameString);

      if (enabledService != null && enabledService.equals(expectedComponentName)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Checks if the provided keyCode is to be acknowledged as a headset key.
   * @param keyCode - The keyCode
   * @return true if the keyCode is headset
   */
  public static boolean isSupportedKey(final int keyCode) {
    return (keyCode != KeyEvent.KEYCODE_HEADSETHOOK
            && keyCode != KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
            && keyCode != KeyEvent.KEYCODE_MEDIA_PLAY);
  }
}
