/*
 * IntroActivity.java
 * Starts the introduction slider that will inform the user about the settings
 * and test the headset button
 */

package com.chif.headsetcontrolplus;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.github.paolorotolo.appintro.AppIntro2;

public class IntroActivity extends AppIntro2 {
  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Add welcome slide
    addSlide(new IntroSlideWelcome());

    // Add accessibility permissions slide
    addSlide(new IntroSlidePermissions());

    // Add headset setup slide
    addSlide(new IntroSlideSetup());

    // Hide Skip/Done button
    showSkipButton(false);
  }

  @Override
  public void onDonePressed(Fragment currentFragment) {
    super.onDonePressed(currentFragment);
    finish();
  }

}