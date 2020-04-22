package com.chif.headsetcontrolplus;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.fragment.app.Fragment;

import com.github.paolorotolo.appintro.ISlideBackgroundColorHolder;
import com.github.paolorotolo.appintro.ISlidePolicy;

public class IntroSlideWelcome extends Fragment {
    private LinearLayout layoutContainer;

    public IntroSlideWelcome() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_intro_slide_welcome, container, false);

        layoutContainer = (LinearLayout) view.findViewById(R.id.intro_slide_layout_welcome);

        return view;
    }


}

