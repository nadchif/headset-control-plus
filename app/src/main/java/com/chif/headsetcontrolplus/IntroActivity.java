/** Intro Activity.
 * Starts the introduction slider that will inform the user about the settings
 * and test the headset button
 */

package com.chif.headsetcontrolplus;

import android.Manifest;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.chif.headsetcontrolplus.slides.IntroSlidePermissions;
import com.chif.headsetcontrolplus.slides.IntroSlideSetup;
import com.chif.headsetcontrolplus.slides.IntroSlideWelcome;
import com.github.paolorotolo.appintro.AppIntro2;

public class IntroActivity extends AppIntro2 {
  @Override
  protected void onCreate(final @Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Add welcome slide
    addSlide(new IntroSlideWelcome());

    // Add accessibility permissions slide
    addSlide(new IntroSlidePermissions());

    // Add headset setup slide
    addSlide(new IntroSlideSetup());

    // Hide Skip/Done button
    showSkipButton(false);

    // Ask for permissions
    askForPermissions(new String[]{
        Manifest.permission.CAMERA,
        Manifest.permission.FOREGROUND_SERVICE,
        Manifest.permission.WAKE_LOCK}, 1);

  }

  @Override
  public void onDonePressed(final Fragment currentFragment) {
    super.onDonePressed(currentFragment);
    finish();
  }

}
