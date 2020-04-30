/*
 * IntroSlideSetup.java
 * Shows the welcoming remarks
 */

package com.chif.headsetcontrolplus.slides;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import com.chif.headsetcontrolplus.R;

public class IntroSlideWelcome extends Fragment {

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {

    View view = inflater.inflate(R.layout.fragment_intro_slide_welcome,
            container, false);

    return view;
  }
}

