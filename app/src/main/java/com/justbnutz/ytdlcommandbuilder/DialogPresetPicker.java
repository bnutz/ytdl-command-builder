/*
 * Created by Brian Lau on 2018-06-05
 * Copyright (c) 2018. All rights reserved.
 *
 * Last modified: 2018-06-05
 */

package com.justbnutz.ytdlcommandbuilder;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
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
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

/**
 * Custom Dialog to list the current set of Preset Saved commands and allow the user to pick one to be
 * used with a given URL
 */
public class DialogPresetPicker extends DialogFragment {

    public static final String TAG = ActivityMain.PACKAGE_NAME + ".DialogPresetPicker";

    private static final String BUNDLEKEY_SHARE_LINK = TAG + ".BUNDLEKEY_SHARE_LINK";

    // Rx tools
    private List<Disposable> mRxDisposables;

    // Views
    private Spinner mSpinnerPresetList;
    private EditText mEditTextSharedCommand;

    private String mSharedLink;


    public DialogPresetPicker() {}


    public static DialogPresetPicker newInstance(String sharedLink) {

        // Init the objects
        DialogPresetPicker dialogPresetPicker = new DialogPresetPicker();
        Bundle dialogArgs = new Bundle();

        // Wrap the given link in a Bundle and set it as a Fragment argument
        dialogArgs.putString(BUNDLEKEY_SHARE_LINK, sharedLink);
        dialogPresetPicker.setArguments(dialogArgs);

        return dialogPresetPicker;
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        if (getActivity() != null && getContext() != null) {

            // Retrieve the Shared Link
            mSharedLink = (getArguments() != null)
                    ? getArguments().getString(BUNDLEKEY_SHARE_LINK, "")
                    : "";

            // Use the Builder class for convenient dialog construction
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());

            // Get the layout inflater and pump up the view (Pass null as the parent view because its going in the dialog layout)
            @SuppressLint("InflateParams")
            View viewDialog = getActivity()
                    .getLayoutInflater()
                    .inflate(R.layout.dialog_preset_picker, null);

            // Link the ArrayAdapter to the dropdown list Spinner
            mSpinnerPresetList = viewDialog.findViewById(R.id.spinner_preset_list);
            mSpinnerPresetList.setOnItemSelectedListener(mAdapterItemSelectedListener);
            mSpinnerPresetList.setAdapter(
                    new AdapterPresetList(
                            getActivity().getLayoutInflater(),
                            getContext(),
                            R.layout.spinneritem_monospaced_dropdown_picker,
                            R.id.lbl_monospace_dropdown_item
                    )
            );

            // Trigger the Preset List load after the Spinner View object is drawn
            mSpinnerPresetList.post(
                    new Runnable() {
                        @Override
                        public void run() {
                            if (mSpinnerPresetList.getAdapter() != null) {
                                HeadlessFragPresetManager.requestCurrentPresetList();
                            }
                        }
                    }
            );

            // Link the EditText view
            mEditTextSharedCommand = viewDialog.findViewById(R.id.edit_txt_preset_command);

            // Set up the core Dialog properties
            dialogBuilder.setView(viewDialog)
                    .setTitle(R.string.share_to_title)
                    .setPositiveButton(R.string.dialog_confirm_ok, mDialogClickListener)
                    .setNegativeButton(R.string.dialog_confirm_cancel, mDialogClickListener);

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

        // Incoming Preset List update
        mRxDisposables.add(
                HeadlessFragPresetManager.rxPresetList()
                        .onBackpressureDrop()
                        .observeOn(Schedulers.computation())
                        .map(
                                new Function<List<String>, List<String>>() {

                                    @Override
                                    public List<String> apply(List<String> checkList) {

                                        List<String> presetList = new ArrayList<>();

                                        // Insert the base barebones command to the front of the list
                                        presetList.add(getString(R.string.ytdl_command_init));

                                        // Run through each item in the list and trim out any trailing URLs
                                        for (String eachPreset : checkList) {
                                            int urlIndex = UtilStringValidationOps.checkUrlStringExists(eachPreset);

                                            if (urlIndex >= 0) {
                                                presetList.add(eachPreset.substring(0, urlIndex));

                                            } else {
                                                presetList.add(eachPreset);
                                            }
                                        }

                                        return presetList;
                                    }
                                }
                        )
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                new Consumer<List<String>>() {
                                    @Override
                                    public void accept(List<String> presetList) {

                                        // Swap the visibilities between the Loading panel and the SpinnerView
                                        ((View) mSpinnerPresetList.getParent()).findViewById(R.id.lbl_preset_list_loading).setVisibility(View.GONE);
                                        mSpinnerPresetList.setVisibility(View.VISIBLE);

                                        // Update the Spinner with the loaded App List
                                        AdapterPresetList spinnerAdapter = (AdapterPresetList) mSpinnerPresetList.getAdapter();

                                        if (spinnerAdapter != null) {
                                            spinnerAdapter.clear();
                                            spinnerAdapter.addAll(presetList);
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

            AdapterPresetList spinnerAdapter = (AdapterPresetList) mSpinnerPresetList.getAdapter();

            if (spinnerAdapter != null) {
                String selectedItem = spinnerAdapter.getItem(position);

                if (!TextUtils.isEmpty(selectedItem)) {

                    StringBuilder shareCommand = new StringBuilder();
                    shareCommand
                            .append(selectedItem.trim())
                            .append(" ")
                            .append(mSharedLink);

                    // Append mSharedLink to the selected option
                    mEditTextSharedCommand.setText(shareCommand);
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

                    // Send the final command to the edit box
                    HeadlessFragCommandEditor.requestReplaceYtdlCommand(
                            mEditTextSharedCommand.getText().toString().trim()
                    );
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                default:
                    // Do nothing, just exit
                    break;
            }
        }
    };

    // endregion


    // region ================== PRESET LIST ARRAY ADAPTER ==================
    // ====== ================== ========================= ==================


    /**
     * Custom ArrayAdapter to handle the Dropdown List (Spinner) items and their corresponding icons.
     */
    private class AdapterPresetList extends ArrayAdapter<String> {

        private final LayoutInflater mLayoutInflater;


        AdapterPresetList(@NonNull LayoutInflater layoutInflator, @NonNull Context context, int resource, int textViewResourceId) {
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


        private View getCustomView(int position, ViewGroup parent, float textSize, int textStyle, boolean isCoverView) {

            // Inflate the row item for the dropdown list
            View spinnerItemView = mLayoutInflater
                    .inflate(R.layout.spinneritem_monospaced_dropdown_picker, parent, false);

            // Read the command out of the list
            String presetCommand = getItem(position);

            if (!TextUtils.isEmpty(presetCommand)) {

                // Set up the TextView attributes
                TextView itemText = spinnerItemView.findViewById(R.id.lbl_monospace_dropdown_item);
                itemText.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
                itemText.setTypeface(itemText.getTypeface(), textStyle);
                itemText.setText(presetCommand);

                // Add margins accordingly
                if (isCoverView) {
                    // Calculate padding relative to text size
                    int paddingSize = Math.round(itemText.getTextSize());

                    itemText.setPadding(
                            paddingSize, 0,
                            paddingSize, 0
                    );

                } else {
                    // If just the main cover view, restrict the maxlines
                    itemText.setMaxLines(1);

                }
            }

            return spinnerItemView;
        }
    }

    // endregion
}
