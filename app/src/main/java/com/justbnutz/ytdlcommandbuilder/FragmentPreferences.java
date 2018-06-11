/*
 * Created by Brian Lau on 2018-05-09
 * Copyright (c) 2018. All rights reserved.
 *
 * Last modified: 2018-05-09
 */

package com.justbnutz.ytdlcommandbuilder;


import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceManager;

import com.google.android.gms.oss.licenses.OssLicensesMenuActivity;

import java.util.ArrayList;
import java.util.List;


public class FragmentPreferences extends PreferenceFragmentCompat implements Preference.OnPreferenceClickListener, SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String TAG = ActivityMain.PACKAGE_NAME + ".FragmentPreferences";


    public FragmentPreferences() {
        // Required empty public constructor
    }


    /**
     * Use this factory method to create a new instance of this fragment using any provided parameters.
     * - newInstance vs Constructors: http://stackoverflow.com/a/14655001
     */
    public static FragmentPreferences newInstance() {
        return new FragmentPreferences();
    }


    // region ================== PRIMARY FLOW ==================
    // ====== ================== ============ ==================


    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

        // Load the preferences XML
        addPreferencesFromResource(R.xml.preferences);

        // Populate the initial values of the Start Tab ListPreference
        setupStartTabList();

        // Link and update the App Picker preference label
        updateCurrentCmdApp();

        // Set the About section labels and click listeners
        setupVersionLabel();
        findPreference(getString(R.string.prefkey_about_contact)).setOnPreferenceClickListener(this);
        findPreference(getString(R.string.prefkey_about_playstore)).setOnPreferenceClickListener(this);
        findPreference(getString(R.string.prefkey_about_privacy)).setOnPreferenceClickListener(this);
        findPreference(getString(R.string.prefkey_about_licences)).setOnPreferenceClickListener(this);
    }


    @Override
    public void onStart() {
        super.onStart();
        getPreferenceScreen()
                .getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }


    @Override
    public void onStop() {
        getPreferenceScreen()
                .getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
        super.onStop();
    }


    @Override
    public boolean onPreferenceClick(Preference preference) {

        String clickedKey = preference.getKey();

        if (clickedKey.equals(getString(R.string.prefkey_cmd_app))) {
            popAppPickerDialog();

        } else if (clickedKey.equals(getString(R.string.prefkey_about_version))) {
            launchAboutLink(
                    getString(R.string.about_link_version)
            );

        } else if (clickedKey.equals(getString(R.string.prefkey_about_contact))) {
            launchAboutLink(
                    getString(R.string.about_link_contact)
            );

        } else if (clickedKey.equals(getString(R.string.prefkey_about_playstore))) {
            launchAboutLink(
                    getString(R.string.about_link_playstore)
            );

        } else if (clickedKey.equals(getString(R.string.prefkey_about_privacy))) {
            launchAboutLink(
                    getString(R.string.about_link_privacy)
            );

        } else if (clickedKey.equals(getString(R.string.prefkey_about_licences))) {
            launchLicenceActivity();
        }

        return true;
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String prefKey) {

        if (prefKey.equals(getString(R.string.prefkey_cmd_app))) {
            updateCurrentCmdApp();

        } else if (prefKey.equals(getString(R.string.prefkey_text_size))) {
            FragmentCommandBuilder.requestUpdateTextSize();

        }
    }

    // endregion


    // region ================== PREF LABEL UPDATES ==================
    // ====== ================== ================== ==================


    /**
     * Set up the list of available Tab options to use as the starting tab
     */
    private void setupStartTabList() {
        ListPreference startTabPreference = (ListPreference) findPreference(getString(R.string.prefkey_start_tab));

        // Loop through the Tab Property arrays and load up the String values
        List<String> tabEntries = new ArrayList<>();
        List<String> tabEntryValues = new ArrayList<>();

        for (int tabIndex = 0; tabIndex < UtilTabProperties.getTabLabelIds().length; tabIndex++) {
            tabEntries.add(
                    getString(UtilTabProperties.getTabLabelIds()[tabIndex])
            );

            // ListPreference needs the Values to be stored as Strings, so need to convert the int to String
            tabEntryValues.add(
                    String.valueOf(UtilTabProperties.getTabTags()[tabIndex])
            );
        }

        // ArrayList to String[] - https://stackoverflow.com/a/4042464
        startTabPreference.setEntries(
                tabEntries.toArray(new String[0])
        );
        startTabPreference.setEntryValues(
                tabEntryValues.toArray(new String[0])
        );
    }


    /**
     * Set up the Preference that will provide the user with a list of apps to pick from to place in the
     * Command Line shortcut
     */
    private void updateCurrentCmdApp() {

        if (getContext() != null) {
            // Fetch the Preference item
            Preference cmdAppPreference = findPreference(getString(R.string.prefkey_cmd_app));

            // Fetch the current Cmd App ID
            String currentAppId = PreferenceManager.getDefaultSharedPreferences(getContext())
                    .getString(
                            getString(R.string.prefkey_cmd_app),
                            getString(R.string.prefkey_cmd_app_default)
                    );

            // Setup the Preference properties
            cmdAppPreference.setOnPreferenceClickListener(this);
            cmdAppPreference.setSummary(
                    String.format(
                            getString(R.string.pref_cmd_app_summary),
                            currentAppId
                    )
            );
        }
    }


    /**
     * Try and populate the Version label into the About Version preference label
     */
    private void setupVersionLabel() {

        String versionString = "";

        if (getContext() != null) {

            // Try and get the version string
            try {
                PackageInfo packageInfo = getContext()
                        .getPackageManager()
                        .getPackageInfo(
                                getContext().getPackageName(),
                                0
                        );

                if (packageInfo != null) {
                    versionString = packageInfo.versionName;
                }

            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }

        // Enable the click listener
        Preference prefVersion = findPreference(getString(R.string.prefkey_about_version));
        prefVersion.setSummary(versionString);
        prefVersion.setOnPreferenceClickListener(this);
    }

    // endregion


    // region ================== EXTERNAL ACTIONS ==================
    // ====== ================== ================ ==================


    /**
     * Pop the dialog for selecting from a list of installed apps
     */
    private void popAppPickerDialog() {

        // Check if the DialogFragment already exists, make sure to remove it first
        DialogFragment editDialogue = (DialogFragment) getChildFragmentManager().findFragmentByTag(DialogAppPicker.TAG);
        if (editDialogue != null) {
            getChildFragmentManager()
                    .beginTransaction()
                    .remove(editDialogue)
                    .commitNow();
        }

        // Pop the new Edit Pic Source dialogue
        editDialogue = new DialogAppPicker();
        editDialogue.show(
                getChildFragmentManager(),
                DialogAppPicker.TAG
        );
    }


    /**
     * Launch an internet weblink
     */
    private void launchAboutLink(String webLink) {

        Intent browserIntent = new Intent(
                Intent.ACTION_VIEW,
                Uri.parse(webLink)
        );

        startActivity(browserIntent);
    }


    /**
     * Open the Open Source Licences panel
     *
     * References:
     * - https://developers.google.com/android/guides/opensource
     * - https://medium.com/@jokatavr/how-to-use-android-open-source-notices-299a0635b5c2
     */
    private void launchLicenceActivity() {
        if (getActivity() != null) {

            Intent licenceIntent = new Intent(
                    getActivity(),
                    OssLicensesMenuActivity.class
            );

            startActivity(licenceIntent);
        }
    }

    // endregion
}
