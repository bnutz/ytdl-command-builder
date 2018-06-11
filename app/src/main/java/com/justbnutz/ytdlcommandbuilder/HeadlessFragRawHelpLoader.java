/*
 * Created by Brian Lau on 2018-04-25
 * Copyright (c) 2018. All rights reserved.
 *
 * Last modified: 2018-04-25
 */

package com.justbnutz.ytdlcommandbuilder;

import android.support.v4.app.Fragment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
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
 * Utility Fragment for parsing out the YTDL help text into a list array on a background thread.
 * Results to be sent out via Rx
 */
public class HeadlessFragRawHelpLoader extends Fragment {

    public static final String TAG = ActivityMain.PACKAGE_NAME + ".HeadlessFragRawHelpLoader";


    // List of RxDisposables for listening to Rx events (multiple pipeline style)
    private List<Disposable> mRxDisposables;

    // Rx Channels for Async operations & cross-Fragment communications
    private static final PublishProcessor<Boolean> mRxYtdlOptionListRequest = PublishProcessor.create();
    private static final PublishProcessor<List<ModelYtdlOptionSection>> mRxYtdlOptionListResponse = PublishProcessor.create();


    public HeadlessFragRawHelpLoader() {
        // Required public empty constructor
    }


    // region ================== STATIC METHODS ==================
    // ====== ================== ============== ==================


    /**
     * Use this factory method to create a new instance of this fragment using any provided parameters.
     * - newInstance vs Constructors: http://stackoverflow.com/a/14655001
     */
    public static HeadlessFragRawHelpLoader newInstance() {
        return new HeadlessFragRawHelpLoader();
    }


    /**
     * Trigger an option list load flow.
     * Results can be retrieved by subscribing to rxYtdlOptionList()
     */
    public static void requestYtdlOptionList() {
        mRxYtdlOptionListRequest.onNext(true);
    }


    /**
     * Rx Observable (Flowable) to emit the latest parsed YTDL Options
     */
    public static Flowable<List<ModelYtdlOptionSection>> rxYtdlOptionList() {
        return mRxYtdlOptionListResponse;
    }

    // endregion


    // region ================== PRIMARY FLOW ==================
    // ====== ================== ============ ==================


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

        // Trigger a Option List load
        mRxDisposables.add(
                mRxYtdlOptionListRequest
                        .onBackpressureDrop()
                        // Run ops on computation thread
                        .observeOn(Schedulers.computation())
                        .map(
                                new Function<Boolean, List<ModelYtdlOptionSection>>() {
                                    @Override
                                    public List<ModelYtdlOptionSection> apply(Boolean aVoid) {
                                        return loadYtdlOptionFile();
                                    }
                                }
                        )
                        // Switch to main thread to update UI
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                new Consumer<List<ModelYtdlOptionSection>>() {
                                    @Override
                                    public void accept(List<ModelYtdlOptionSection> ytdlBaseItems) {
                                        mRxYtdlOptionListResponse.onNext(ytdlBaseItems);
                                    }
                                }
                        )
        );
    }

    // endregion


    // region ================== OPTION FILE READER ==================
    // ====== ================== ================== ==================


    /**
     * Parses out the YTDL options from the raw output generated by the "youtube-dl --help" command.
     * (The output is stored as a plain text file in the res/raw/ytdl_options.txt file)
     */
    private List<ModelYtdlOptionSection>  loadYtdlOptionFile() {

        // Init the Option List object
        List<ModelYtdlOptionSection> ytdlOptionsList = new ArrayList<>();

        if (getContext() != null) {

            // Open the InputStream to the raw text file
            InputStream inputStream = getContext().getResources().openRawResource(R.raw.ytdl_options);

            // Set up the Reader object to read the stream
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(inputStream)
            );

            // If all whitespace only up to third character; then it's a header
            Pattern patternSectionHeader = Pattern.compile(
                    "^\\s{2}(\\w.+)$"
            );
            // If all whitespace only up to fifth character; then it's the next option
            Pattern patternOptionFlag = Pattern.compile(
                    "^\\s{4}(-.{32})(\\w.+)$"
            );
            // Extract the actual command switch out of the potential list of possible options
            Pattern patternCmdSwitch = Pattern.compile(
                    "^.*?,? ?(-[-\\w :]+)$"
            );
            // If all whitespace only up to thirty-eighth character; then it's a continuation of the description
            Pattern patternOptionDescCont = Pattern.compile(
                    "^\\s{37}(\\w.+)$"
            );

            // Header Id is used so we know which heading each option should appear under
            int itemId = 0;
            int headerId = -1;

            // Keep a reference to the current Option Item object, as the Description field might need to be added to
            ModelYtdlOptionItem currentOptionItem = null;

            String currentLine;
            String currentFlagLabel;
            String currentCmdSwitch = "";

            // Initialise the Regex tools
            Matcher matchOptionFlag;
            Matcher matchCmdSwitch;
            Matcher matchOptionDescCont;
            Matcher matchSectionHeader;

            try {

                // Read the next line into the buffer, and if its not EOF; parse out its line-type and process accordingly.
                while ((currentLine = bufferedReader.readLine()) != null) {

                    // Running the most-likely Regex first, to try and reduce time taken to process
                    matchOptionFlag = patternOptionFlag.matcher(currentLine);

                    // Check if next Option Item
                    if (matchOptionFlag.find() && matchOptionFlag.groupCount() > 1) {

                        // If there was a previous Option Item in holding, add it to the list first
                        if (currentOptionItem != null && ytdlOptionsList.size() > currentOptionItem.getSectionId()) {
                            ytdlOptionsList
                                    .get(currentOptionItem.getSectionId())
                                    .getYtdlOptionItems()
                                    .add(currentOptionItem);
                        }

                        currentFlagLabel = matchOptionFlag.group(1).trim();

                        // Extract the actual command switch out
                        matchCmdSwitch = patternCmdSwitch.matcher(currentFlagLabel);
                        if (matchCmdSwitch.find() && matchCmdSwitch.groupCount() > 0) {
                            currentCmdSwitch = matchCmdSwitch.group(1);
                        }

                        // Create a new Option Item based on the latest line
                        currentOptionItem = new ModelYtdlOptionItem(
                                itemId++,
                                headerId,
                                currentCmdSwitch,
                                currentFlagLabel,
                                matchOptionFlag.group(2).trim()
                        );

                    } else {
                        // If not a Flag line, check if it's a Continued Description line
                        matchOptionDescCont = patternOptionDescCont.matcher(currentLine);

                        if (matchOptionDescCont.find() && matchOptionDescCont.groupCount() > 0 && currentOptionItem != null) {
                            // If it's a continued description, append to the existing item object
                            currentOptionItem.appendDescription(
                                    matchOptionDescCont.group(1).trim()
                            );

                        } else {
                            // If not Flag or Description, check if it's a header
                            matchSectionHeader = patternSectionHeader.matcher(currentLine);

                            if (matchSectionHeader.find() && matchSectionHeader.groupCount() > 0) {
                                // Increment the HeaderID
                                headerId += 1;

                                // Add it to the list
                                ytdlOptionsList.add(
                                        new ModelYtdlOptionSection(
                                                itemId++,
                                                headerId,
                                                matchSectionHeader.group(1).trim()
                                        )
                                );
                            }
                        }
                    }
                }

                // The while loop will have exited before the last created Option Item was added to the list, so need to add it in
                if (currentOptionItem != null && ytdlOptionsList.size() > currentOptionItem.getSectionId()) {
                    ytdlOptionsList
                            .get(currentOptionItem.getSectionId())
                            .getYtdlOptionItems()
                            .add(currentOptionItem);
                }

                bufferedReader.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return ytdlOptionsList;
    }

    // endregion

}
