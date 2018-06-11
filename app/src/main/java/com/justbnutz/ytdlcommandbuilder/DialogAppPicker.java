/*
 * Created by Brian Lau on 2018-05-21
 * Copyright (c) 2018. All rights reserved.
 *
 * Last modified: 2018-05-21
 */

package com.justbnutz.ytdlcommandbuilder;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.Schedulers;

/**
 * Custom Dialog to read the list of currently installed apps and show them in a DropDown List
 * Clicking one will select the ID of that app and show it in the TextBox below the list
 */
public class DialogAppPicker extends DialogFragment {

    public static final String TAG = ActivityMain.PACKAGE_NAME + ".DialogAppPicker";

    // Rx tools
    private List<Disposable> mRxDisposables;
    private static final PublishProcessor<Boolean> mRxLoadAppList = PublishProcessor.create();

    // Preferences
    private SharedPreferences mSharedPrefs;

    // Views
    private Spinner mSpinnerInstalledApps;
    private EditText mEditTextAppId;


    @NonNull
    @Override
    @SuppressLint("InflateParams")
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        if (getActivity() != null && getContext() != null) {

            // Load in the SharedPrefs
            mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());

            // Use the Builder class for convenient dialog construction
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());

            // Get the layout inflater and pump up the view (Pass null as the parent view because its going in the dialog layout)
            View viewDialog = getActivity()
                    .getLayoutInflater()
                    .inflate(R.layout.dialog_app_picker, null);

            // Link the ArrayAdapter to the dropdown list Spinner
            mSpinnerInstalledApps = viewDialog.findViewById(R.id.spinner_app_list);
            mSpinnerInstalledApps.setOnItemSelectedListener(mAdapterItemSelectedListener);
            mSpinnerInstalledApps.setAdapter(
                    new AdapterAppList(
                            getActivity().getLayoutInflater(),
                            getContext(),
                            R.layout.spinneritem_basic_dropdown_picker,
                            R.id.lbl_alertdialog_dropdown_item
                    )
            );

            // Trigger the App List load after the Spinner View object is drawn
            mSpinnerInstalledApps.post(
                    new Runnable() {
                        @Override
                        public void run() {
                            if (mSpinnerInstalledApps.getAdapter() != null) {
                                mRxLoadAppList.onNext(true);
                            }
                        }
                    }
            );

            // Link the EditText view
            mEditTextAppId = viewDialog.findViewById(R.id.edit_txt_app_picker_id);
            mEditTextAppId.setText(
                    mSharedPrefs.getString(
                            getString(R.string.prefkey_cmd_app),
                            getString(R.string.prefkey_cmd_app_default)
                    )
            );

            // Set up the core Dialog properties
            dialogBuilder.setView(viewDialog)
                    .setPositiveButton(R.string.dialog_confirm_ok, mDialogClickListener)
                    .setNegativeButton(R.string.dialog_confirm_cancel, mDialogClickListener)
                    .setNeutralButton(R.string.dialog_confirm_default, mDialogClickListener);

            // Create the AlertDialog object and return it
            return dialogBuilder.create();

        } else {
            return super.onCreateDialog(savedInstanceState);
        }
    }


    @Override
    public void onStart() {
        super.onStart();

        clearRxDisposables();
        initRxDisposables();
    }


    @Override
    public void onStop() {

        clearRxDisposables();

        super.onStop();
    }


    // region ================== RXBUS OPERATIONS ==================
    // ====== ================== ================ ==================


    /**
     * Clears out any existing RxDisposables
     */
    private void clearRxDisposables() {

        if (mRxDisposables != null) {
            for (Disposable rxDisp : mRxDisposables) {
                if (!rxDisp.isDisposed()) {
                    rxDisp.dispose();
                }
            }

            mRxDisposables.clear();
        }
    }


    /**
     * Subscribe to the relevant Rx Observables for this fragment and place them in the RxSub list
     */
    private void initRxDisposables() {
        mRxDisposables = new ArrayList<>();

        // Trigger a Option List load
        mRxDisposables.add(
                mRxLoadAppList
                        .onBackpressureDrop()
                        .observeOn(Schedulers.computation())
                        .map(
                                new Function<Boolean, List<SpinnerItemAppInfo>>() {
                                    @Override
                                    public List<SpinnerItemAppInfo> apply(Boolean aVoid) {

                                        List<SpinnerItemAppInfo> appList = new ArrayList<>();

                                        if (getContext() != null) {
                                            // Get an instance of the PackageManager
                                            PackageManager pkgManager = getContext().getPackageManager();
                                            CharSequence appName;
                                            Drawable appIcon;

                                            // Create an Intent to filter for all installed apps that we can actually launch (https://stackoverflow.com/a/30446616)
                                            Intent appCheckIntent = new Intent(Intent.ACTION_MAIN, null);
                                            appCheckIntent.addCategory(Intent.CATEGORY_LAUNCHER);

                                            // Loop through each app returned from the filtered list
                                            for (ResolveInfo eachApp : pkgManager.queryIntentActivities(appCheckIntent, 0)) {

                                                appName = eachApp.loadLabel(pkgManager);
                                                appIcon = eachApp.loadIcon(pkgManager);

                                                // Only add those with valid PackageNames
                                                if (eachApp.activityInfo != null && !TextUtils.isEmpty(eachApp.activityInfo.packageName)) {
                                                    appList.add(
                                                            new SpinnerItemAppInfo(
                                                                    appName,
                                                                    appIcon,
                                                                    eachApp.activityInfo.packageName
                                                            )
                                                    );                                                }
                                            }

                                            // Sort the App List alphabetically by App Name
                                            Collections.sort(appList, new Comparator<SpinnerItemAppInfo>() {
                                                @Override
                                                public int compare(SpinnerItemAppInfo itemAppInfo1, SpinnerItemAppInfo itemAppInfo2) {
                                                    return String.valueOf(itemAppInfo1.appName)
                                                            .compareToIgnoreCase(String.valueOf(itemAppInfo2.appName));
                                                }
                                            });
                                        }

                                        // Insert a dummy item for the initial hint text
                                        SpinnerItemAppInfo hintItem = new SpinnerItemAppInfo(
                                                getString(R.string.app_picker_select),
                                                null,
                                                null
                                        );
                                        appList.add(0, hintItem);

                                        return appList;
                                    }
                                }
                        )
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                new Consumer<List<SpinnerItemAppInfo>>() {
                                    @Override
                                    public void accept(List<SpinnerItemAppInfo> appList) {
                                        // Swap the visibilities between the Loading panel and the SpinnerView
                                        ((View) mSpinnerInstalledApps.getParent()).findViewById(R.id.lbl_app_list_loading).setVisibility(View.GONE);
                                        mSpinnerInstalledApps.setVisibility(View.VISIBLE);

                                        // Update the Spinner with the loaded App List
                                        AdapterAppList spinnerAdapter = (AdapterAppList) mSpinnerInstalledApps.getAdapter();

                                        if (spinnerAdapter != null) {
                                            spinnerAdapter.clear();
                                            spinnerAdapter.addAll(appList);
                                        }
                                    }
                                }
                        )
        );
    }

    // endregion


    // region ================== EVENT LISTENERS ==================
    // ====== ================== =========================== ==================


    private AdapterView.OnItemSelectedListener mAdapterItemSelectedListener = new AdapterView.OnItemSelectedListener() {

        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int position, long itemId) {

            AdapterAppList spinnerAdapter = (AdapterAppList) mSpinnerInstalledApps.getAdapter();

            if (spinnerAdapter != null) {
                SpinnerItemAppInfo selectedItem = spinnerAdapter.getItem(position);

                if (selectedItem != null && !TextUtils.isEmpty(selectedItem.packageName)) {
                    mEditTextAppId.setText(selectedItem.packageName);
                }
            }
        }


        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {}

    };


    private DialogInterface.OnClickListener mDialogClickListener = new DialogInterface.OnClickListener() {

        @Override
        public void onClick(DialogInterface dialogInterface, int buttonId) {

            switch (buttonId) {
                case DialogInterface.BUTTON_POSITIVE:

                    // Save to EditText contents to SharedPrefs
                    String packageName = mEditTextAppId.getText().toString().trim();
                    if (!TextUtils.isEmpty(packageName)) {
                        mSharedPrefs.edit()
                                .putString(
                                        getString(R.string.prefkey_cmd_app),
                                        packageName
                                ).apply();
                    }
                    break;

                case DialogInterface.BUTTON_NEUTRAL:
                    // Restore Default package name value to SharedPrefs
                    mSharedPrefs.edit()
                            .putString(
                                    getString(R.string.prefkey_cmd_app),
                                    getString(R.string.prefkey_cmd_app_default)
                            ).apply();
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                default:
                    // Do nothing, just exit
                    break;
            }
        }
    };

    // endregion


    // region ================== APP LIST ARRAY ADAPTER ==================
    // ====== ================== ====================== ==================


    /**
     * Custom ArrayAdapter to handle the Dropdown List (Spinner) items and their corresponding icons.
     */
    private class AdapterAppList extends ArrayAdapter<SpinnerItemAppInfo> {

        private final LayoutInflater mLayoutInflater;


        AdapterAppList(@NonNull LayoutInflater layoutInflator, @NonNull Context context, int resource, int textViewResourceId) {
            super(context, resource, textViewResourceId);
            mLayoutInflater = layoutInflator;
        }


        /**
         * ItemRows of the DropDownList
         */
        @Override
        public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            return getCustomView(position, parent,
                    18f,
                    Typeface.NORMAL,
                    true
            );
        }


        /**
         * Main cover view of the DropDownList
         */
        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            return getCustomView(position, parent,
                    20f,
                    Typeface.BOLD,
                    false
            );
        }


        private View getCustomView(int position, ViewGroup parent, float textSize, int textStyle, boolean addMargins) {

            // Inflate the row item for the dropdown list
            View spinnerItemView = mLayoutInflater
                    .inflate(R.layout.spinneritem_basic_dropdown_picker, parent, false);

            // Read the properties out of the current ApplicationInfo
            SpinnerItemAppInfo appInfo = getItem(position);

            if (appInfo != null) {

                // Set up the TextView attributes
                TextView itemText = spinnerItemView.findViewById(R.id.lbl_alertdialog_dropdown_item);
                itemText.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
                itemText.setTypeface(itemText.getTypeface(), textStyle);
                itemText.setText(appInfo.appName);

                // Calculate icon sizes and padding relative to text size
                int iconSize = Math.round(itemText.getTextSize());
                int iconPadding = Math.round(iconSize / 2);

                // Set up the App Icon as a compound drawable (if present)
                if (appInfo.appIcon != null) {

                    // Prepare the icon size
                    appInfo.appIcon.setBounds(0, 0, iconSize, iconSize);

                    itemText.setCompoundDrawablePadding(iconPadding);
                    itemText.setCompoundDrawables(
                            appInfo.appIcon,
                            null, null, null
                    );
                }

                // Add margins accordingly
                if (addMargins) {
                    itemText.setPadding(
                            iconSize, 0,
                            iconSize, 0
                    );
                }
            }
            return spinnerItemView;
        }
    }


    /**
     * Simple POJO to hold the relevant apps to show in the Spinner list
     */
    private static class SpinnerItemAppInfo {

        final CharSequence appName;
        final Drawable appIcon;
        final String packageName;

        SpinnerItemAppInfo(CharSequence appName, Drawable appIcon, String packageName) {
            this.appName = appName;
            this.appIcon = appIcon;
            this.packageName = packageName;
        }
    }

    // endregion
}
