package org.commcare.fragments;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.commcare.dalvik.R;
import org.javarosa.core.services.locale.Localization;

/**
 * Fragment for inputting app installation URL, "returned" through the URLInstaller interface.
 *
 * @author Daniel Luna (dcluna@dimagi.com)
 */
public class SetupEnterURLFragment extends Fragment {
    private static final String interfaceName = URLInstaller.class.getName();

    private URLInstaller listener;

    private EditText profileLocation;

    public interface URLInstaller {
        /**
         * Called when user fills in an URL and presses 'Start Install'.
         * The parent activity is responsible for implementing this interface and doing something with the URL.
         *
         * @param url URL typed by the user
         */
        void onURLChosen(String url);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_setup_enter_url, container, false);
        Button installButton = view.findViewById(R.id.start_install);
        installButton.setText(Localization.get("install.button.start"));
        profileLocation = view.findViewById(R.id.edit_profile_location);
        TextView appProfile = view.findViewById(R.id.app_profile_txt_view);
        appProfile.setText(Localization.get("install.appprofile"));

        installButton.setOnClickListener(v -> {
            getFragmentManager().popBackStack(); // equivalent to pressing the "back" button
            // no need for a null check because onAttach is called before onCreateView
            listener.onURLChosen(getURL()); // returns the chosen URL to the parent Activity
        });

        return view;
    }

    /**
     * Returns the chosen URL in the UI, prefixing it with http:// if not set.
     *
     * @return The current URL
     */
    private String getURL() {
        String url = profileLocation.getText().toString();
        if (url == null || url.length() == 0) {
            return url;
        }
        // if it's not the last (which should be "Raw") choice, we'll use the prefix
        if (!url.contains("://")) { // if there is no (http|jr):// prefix, we'll assume it's a http://bit.ly/ URL
            url = "http://bit.ly/" + url;
        }
        return url;
    }

    @Override
    public void onPause() {
        super.onPause();
        Activity activity = this.getActivity();

        if (activity != null) {
            if (activity.getCurrentFocus() != null) {
                InputMethodManager imm = (InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Activity activity = this.getActivity();

        if (activity != null) {
            View editBox = activity.findViewById(R.id.edit_profile_location);
            editBox.requestFocus();

            InputMethodManager inputMethodManager = (InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.showSoftInput(editBox, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof Activity) {
            ((Activity)context).getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }

        if (!(context instanceof URLInstaller)) {
            throw new ClassCastException(context + " must implemement " + interfaceName);
        } else {
            listener = (URLInstaller)context;
        }
    }
}
