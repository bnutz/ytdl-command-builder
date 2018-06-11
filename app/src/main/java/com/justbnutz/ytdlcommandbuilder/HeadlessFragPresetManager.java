/*
 * Created by Brian Lau on 2018-05-14
 * Copyright (c) 2018. All rights reserved.
 *
 * Last modified: 2018-05-14
 */

package com.justbnutz.ytdlcommandbuilder;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.PreferenceManager;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.Schedulers;

/**
 * Utility Fragment for managing the save and load operations of the Preset List
 */
public class HeadlessFragPresetManager extends Fragment {

    public static final String TAG = ActivityMain.PACKAGE_NAME + ".HeadlessFragPresetManager";


    // List of RxDisposables for listening to Rx events (multiple pipeline style)
    private List<Disposable> mRxDisposables;

    // Rx Channels for Async operations & cross-Fragment communications
    private static final PublishProcessor<Boolean> mRxCurrentPresetListRequest = PublishProcessor.create();
    private static final PublishProcessor<String> mRxSaveNewPresetRequest = PublishProcessor.create();
    private static final PublishProcessor<String> mRxDirectSavePreset = PublishProcessor.create();
    private static final PublishProcessor<Integer> mRxRemovePresetItemRequest = PublishProcessor.create();

    private static final PublishProcessor<List<String>> mRxNewPresetListResponse = PublishProcessor.create();
    private static final PublishProcessor<Integer> mRxPresetListScrollPosition = PublishProcessor.create();


    // Preferences
    private SharedPreferences mSharedPrefs;


    public HeadlessFragPresetManager() {
        // Required public empty constructor
    }


    // region ================== STATIC METHODS ==================
    // ====== ================== ============== ==================


    /**
     * Use this factory method to create a new instance of this fragment using any provided parameters.
     * - newInstance vs Constructors: http://stackoverflow.com/a/14655001
     */
    public static HeadlessFragPresetManager newInstance() {
        return new HeadlessFragPresetManager();
    }


    /**
     * Request to ping out the current Preset List along the rxPresetList() channel
     */
    public static void requestCurrentPresetList() {
        mRxCurrentPresetListRequest.onNext(true);
    }


    /**
     * Add a new preset command to the existing list of saved Presets
     */
    public static void requestSaveNewPreset(String presetCommand) {
        mRxSaveNewPresetRequest.onNext(presetCommand);
    }


    /**
     * Remove a Preset Command from the given position
     */
    public static void requestRemovePresetItem(int presetPosition) {
        mRxRemovePresetItemRequest.onNext(presetPosition);
    }


    /**
     * Rx Observable (Flowable) to emit the latest list of saved Presets
     */
    public static Flowable<List<String>> rxPresetList() {
        return mRxNewPresetListResponse;
    }


    /**
     * Rx Observable (Flowable) to emit index position of existing Preset commands
     */
    public static Flowable<Integer> rxPresetPosition() {
        return mRxPresetListScrollPosition;
    }

    // endregion


    // region ================== PRIMARY FLOW ==================
    // ====== ================== ============ ==================


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getContext() != null) {
            mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
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

    // endregion


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

        // Trigger a Preset List emission
        mRxDisposables.add(
                mRxCurrentPresetListRequest
                        .observeOn(Schedulers.computation())
                        .subscribe(
                                new Consumer<Boolean>() {
                                    @Override
                                    public void accept(Boolean aBoolean) {
                                        mRxNewPresetListResponse.onNext(
                                                loadPresetList()
                                        );
                                    }
                                }
                        )
        );

        // Add new Preset Command directly (only accessible within this class)
        mRxDisposables.add(
                mRxDirectSavePreset
                        .onBackpressureBuffer()
                        .subscribe(
                                new Consumer<String>() {
                                    @Override
                                    public void accept(String presetCommand) {
                                        savePresetCommand(presetCommand);
                                    }
                                }
                        )
        );

        // Add new Preset Command with validation
        mRxDisposables.add(
                mRxSaveNewPresetRequest
                        .onBackpressureBuffer()
                        .observeOn(Schedulers.computation())
                        .map(
                                new Function<String, RxSaveCommandPackage>() {
                                    @Override
                                    public RxSaveCommandPackage apply(String presetCommand) {
                                        // Do a validation of whether the command contains a URL or not
                                        return new RxSaveCommandPackage(
                                                presetCommand,
                                                checkUrlStringExists(presetCommand)
                                        );
                                    }
                                }
                        )
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                new Consumer<RxSaveCommandPackage>() {
                                    @Override
                                    public void accept(RxSaveCommandPackage rxSaveCommandPackage) {

                                        // If the command contains a target URL as well, then need to check with user to keep or not
                                        if (getContext() != null && rxSaveCommandPackage.urlPosition > 0) {

                                            // Instantiate an AlertDialog.Builder and set its properties
                                            AlertDialog.Builder dBuilder = new AlertDialog.Builder(getContext());
                                            dBuilder.setTitle(R.string.save_preset_url_found_title)
                                                    .setMessage(R.string.save_preset_url_found_message)
                                                    .setPositiveButton(R.string.dialog_confirm_yes, rxSaveCommandPackage)
                                                    .setNegativeButton(R.string.dialog_confirm_no, rxSaveCommandPackage);

                                            // Pop the dialogue
                                            dBuilder.create()
                                                    .show();

                                        } else {
                                            // If no URL found, then just immediately save the command
                                            savePresetCommand(rxSaveCommandPackage.presetCommand);
                                        }
                                    }
                                }
                        )
        );

        // Remove a Preset Command from the list
        mRxDisposables.add(
                mRxRemovePresetItemRequest
                        .onBackpressureBuffer()
                        .observeOn(Schedulers.computation())
                        .subscribe(
                                new Consumer<Integer>() {
                                    @Override
                                    public void accept(Integer removePosition) {
                                        removePresetCommand(removePosition);
                                    }
                                }
                        )
        );
    }


    /**
     * Quick POJO to hold the Save Command processing properties.
     * Also acts as a ClickListener handler to manage when a URL is detected in the command
     */
    private static class RxSaveCommandPackage implements DialogInterface.OnClickListener {

        final String presetCommand;
        final int urlPosition;

        RxSaveCommandPackage(String presetCommand, int urlPosition) {
            this.presetCommand = presetCommand;
            this.urlPosition = urlPosition;
        }

        @Override
        public void onClick(DialogInterface dialogInterface, int clickId) {

            switch (clickId) {
                case DialogInterface.BUTTON_POSITIVE:
                    // Keep the URL, send straight to save
                    mRxDirectSavePreset.onNext(presetCommand);
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    // Remove the URL, trim out that section of the command
                    mRxDirectSavePreset.onNext(presetCommand.substring(0, urlPosition));
                    break;

                default:
                    break;
            }
        }
    }

    // endregion


    // region ================== URL VALIDATION ==================
    // ====== ================== ============== ==================


    /**
     * Take a given command and see if a URL exists at the end of it
     *
     * @return Index position from start of URL (if found), if no URL found, then just return -1
     */
    private int checkUrlStringExists(String checkCommand) {

        Pattern patternEndUrl = Pattern.compile(
                ".+?\\s(['\"]?https?://.+)$"
        );

        Matcher matchEndUrl = patternEndUrl.matcher(checkCommand);

        if (matchEndUrl.find() && matchEndUrl.groupCount() > 0) {
            // If URL(s) found, return index position of the (first) URL
            return checkCommand.length() - matchEndUrl.group(1).length();

        } else {
            // If no URL found, return no position
            return -1;
        }
    }

    // endregion


    // region ================== PRESET SAVE & LOAD OPS ==================
    // ====== ================== ====================== ==================


    /**
     * Load a list of Preset Commands from SharedPrefs, if this is the first time - then will load
     * the default list.
     *
     * Reference: https://github.com/google/gson/blob/master/UserGuide.md#TOC-Array-Examples
     */
    private List<String> loadPresetList() {

        // Initialise the GSON handler
        Gson gsonThing = new Gson();

        // List array is stored in preferences as a GSON-encoded string array; allows us to store arrays and preserve order (unlike getStringSet)
        String presetListRaw = mSharedPrefs.getString(
                getString(R.string.prefkey_preset_list),
                gsonThing.toJson(
                        getResources().getStringArray(R.array.default_preset_list)
                )
        );

        // Parse out the Preference String into an array
        String[] presetList = gsonThing.fromJson(presetListRaw, String[].class);

        // Convert the resultant Set back to an ArrayList and return it out
        return new ArrayList<>(
                Arrays.asList(presetList)
        );
    }


    /**
     * Save the given List of Preset Commands to SharedPrefs
     */
    private void savePresetList(List<String> presetList) {

        // Initialise the GSON handler
        Gson gsonThing = new Gson();

        // Save the new list to SharedPrefs
        mSharedPrefs.edit().putString(
                getString(R.string.prefkey_preset_list),
                gsonThing.toJson(presetList)
        ).apply();
    }


    /**
     * Save a given String to the main list of Preset commands
     */
    private void savePresetCommand(String presetCommand) {

        // Retrieve the current list of Preset commands
        List<String> presetList = loadPresetList();

        // Check if the command already exists in the list
        int checkExists = presetList.indexOf(presetCommand);

        if (checkExists < 0) {
            // If the command doesn't already exist, add it to the top
            presetList.add(0, presetCommand);

            // Save the list
            savePresetList(presetList);

            // Send the resultant list back out
            mRxNewPresetListResponse.onNext(presetList);

        } else {
            // If the command is already in the list, then send its position out so we can scroll to it
            mRxPresetListScrollPosition.onNext(checkExists);
        }
    }


    /**
     * Remove the item from the given position
     */
    private void removePresetCommand(int presetPosition) {

        // Retrieve the current list of Preset commands
        List<String> presetList = loadPresetList();

        // Check the position is valid
        if (presetPosition < presetList.size()) {

            // Remove the item
            presetList.remove(presetPosition);

            // Save the list
            savePresetList(presetList);

            // Send the resultant list back out
            mRxNewPresetListResponse.onNext(presetList);
        }
    }

    // endregion

}
