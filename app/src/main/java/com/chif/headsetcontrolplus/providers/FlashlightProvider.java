/*
 * FlashlightProvider.java
 * handles interactions between Camera/Flashlight and the app
 */

package com.chif.headsetcontrolplus.providers;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.util.Log;
import androidx.annotation.NonNull;

public class FlashlightProvider {
  private static final String APP_TAG = FlashlightProvider.class.getSimpleName();
  private static boolean sIsFlashlightOn = false;
  private CameraManager camManager;
  private Context context;
  private static CameraManager.TorchCallback torchCallback = new CameraManager.TorchCallback() {
    @Override
    public void onTorchModeChanged(@NonNull String cameraId, boolean enabled) {
      super.onTorchModeChanged(cameraId, enabled);
      sIsFlashlightOn = enabled;
    }
  };

  /** Flashlight Provider.
   * handles interactions between Camera/Flashlight and the app
   */
  public FlashlightProvider(Context context) {
    this.context = context;
    registerCallback();
  }

  private void registerCallback() {
    camManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
    camManager.registerTorchCallback(torchCallback, null);// (callback, handler)
  }

  /*
  public void finalize() {
    camManager.unregisterTorchCallback(torchCallback);// (callback, handler)
  }
  */

  /** Toggles Flashlight.
   * Turns flashlight on or off
   */
  public void toggleFlashLight() {
    if (!sIsFlashlightOn) {
      turnFlashlightOn();
      Log.i(APP_TAG, "Flashlight On");
      return;
    }
    turnFlashlightOff();
    Log.i(APP_TAG, "Flashlight Off");
  }

  private void turnFlashlightOn() {
    try {
      camManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
      String cameraId = null;
      if (camManager != null) {
        cameraId = camManager.getCameraIdList()[0];
        camManager.setTorchMode(cameraId, true);
      }
    } catch (CameraAccessException e) {
      Log.e(APP_TAG, e.toString());
    }
  }

  private void turnFlashlightOff() {
    try {
      String cameraId;
      camManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
      if (camManager != null) {
        cameraId = camManager.getCameraIdList()[0]; // Usually front camera is at 0 position.
        camManager.setTorchMode(cameraId, false);
      }
    } catch (CameraAccessException e) {
      e.printStackTrace();
    }
  }
}