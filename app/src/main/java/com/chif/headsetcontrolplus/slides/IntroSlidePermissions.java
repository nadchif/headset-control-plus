/*
 * IntroSlidePermissions.java
 * Explains to the user the permissions the app will require. In this case its just accessibility
 */

package com.chif.headsetcontrolplus.slides;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import com.chif.headsetcontrolplus.HeadsetControlPlusService;
import com.chif.headsetcontrolplus.R;
import com.github.paolorotolo.appintro.ISlidePolicy;


public class IntroSlidePermissions extends Fragment implements ISlidePolicy {
  private Button mLaunchSettingsBtn;
  private TextView mSuccessMessage;

  private ContentObserver mObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
    @Override
    public void onChange(final boolean selfChange) {
      super.onChange(selfChange);
      boolean accessibilityServiceEnabled = isAccessibilityServiceEnabled(getActivity(),
              HeadsetControlPlusService.class);
      if (accessibilityServiceEnabled) {
        Toast.makeText(getContext(), getString(R.string.success_require_access),
                Toast.LENGTH_SHORT).show();
        mLaunchSettingsBtn.setVisibility(View.GONE);
        mSuccessMessage.setVisibility(View.VISIBLE);
      } else {
        Toast.makeText(getContext(), getString(R.string.err_require_access),
                Toast.LENGTH_SHORT).show();
        mSuccessMessage.setVisibility(View.GONE);
        mLaunchSettingsBtn.setVisibility(View.VISIBLE);
      }
    }
  };

  @Override
  public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                           final Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_intro_slide_permissions, container, false);
    mLaunchSettingsBtn = view.findViewById(R.id.btn_settings);
    mSuccessMessage = view.findViewById(R.id.txt_success);

    boolean serviceEnabled = isAccessibilityServiceEnabled(getActivity(),
            HeadsetControlPlusService.class);
    if (serviceEnabled) {
      Toast.makeText(getContext(), getString(R.string.success_require_access),
              Toast.LENGTH_SHORT).show();
      mLaunchSettingsBtn.setVisibility(View.GONE);
      mSuccessMessage.setVisibility(View.VISIBLE);
    } else {
      Toast.makeText(getContext(), getString(R.string.err_require_access),
              Toast.LENGTH_SHORT).show();
      mSuccessMessage.setVisibility(View.GONE);
      mLaunchSettingsBtn.setVisibility(View.VISIBLE);
    }

    mLaunchSettingsBtn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(final View view) {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivity(intent);
        getActivity().finish();
      }
    });

    Uri uri = Settings.Secure.getUriFor(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
    getActivity().getContentResolver().registerContentObserver(uri, false, mObserver);

    return view;
  }

  @Override
  public void onDestroyView() {
    getActivity().getContentResolver().unregisterContentObserver(mObserver);
    super.onDestroyView();
  }

  @Override
  public boolean isPolicyRespected() {
    // check if accessibity is enabled . return false if it is not
    boolean enabled = isAccessibilityServiceEnabled(getActivity(), HeadsetControlPlusService.class);
    return enabled;
  }

  @Override
  public void onUserIllegallyRequestedNextPage() {
    Toast.makeText(getContext(), getString(R.string.err_require_access), Toast.LENGTH_SHORT).show();
  }

  private static boolean isAccessibilityServiceEnabled(Context context,
                                                       Class<?> accessibilityService) {
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

}

