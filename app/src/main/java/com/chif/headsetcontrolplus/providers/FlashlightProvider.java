/*
 * FlashlightProvider.java
 * Handles interactions between Camera/Flashlight and the app.
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
  private CameraManager mCamManager;
  private Context mContext;
  private static CameraManager.TorchCallback torchCallback = new CameraManager.TorchCallback() {
    @Override
    public void onTorchModeChanged(@NonNull String cameraId, boolean enabled) {
      super.onTorchModeChanged(cameraId, enabled);
      sIsFlashlightOn = enabled;
    }
  };

  /** Flashlight Provider.
   * Handles interactions between Camera/Flashlight and the app.
   */
  public FlashlightProvider(Context context) {
    this.mContext = context;
    registerCallback();
  }

  private void registerCallback() {
    mCamManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
    mCamManager.registerTorchCallback(torchCallback, null);// (callback, handler)
  }

  /*
  public void finalize() {
    mCamManager.unregisterTorchCallback(torchCallback);// (callback, handler)
  }
  */

  /** Toggles Flashlight.
   * Turns Flashlight on or off.
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
      mCamManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
      String cameraId = null;
      if (mCamManager != null) {
        cameraId = mCamManager.getCameraIdList()[0];
        mCamManager.setTorchMode(cameraId, true);
      }
    } catch (CameraAccessException e) {
      Log.e(APP_TAG, e.toString());
    }
  }

  private void turnFlashlightOff() {
    try {
      String cameraId;
      mCamManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
      if (mCamManager != null) {
        cameraId = mCamManager.getCameraIdList()[0]; // Usually front camera is at 0 position.
        mCamManager.setTorchMode(cameraId, false);
      }
    } catch (CameraAccessException e) {
      e.printStackTrace();
    }
  }
}
