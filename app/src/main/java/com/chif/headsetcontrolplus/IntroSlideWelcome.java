/*
 * IntroSlideSetup.java
 * Shows the welcoming remarks
 */
package com.chif.headsetcontrolplus;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.fragment.app.Fragment;

public class IntroSlideWelcome extends Fragment {
    private LinearLayout layoutContainer;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_intro_slide_welcome, container, false);
        layoutContainer = (LinearLayout) view.findViewById(R.id.intro_slide_layout_welcome);

        return view;
    }
}

