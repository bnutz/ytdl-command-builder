/*
 * Created by Brian Lau on 2018-05-07
 * Copyright (c) 2018. All rights reserved.
 *
 * Last modified: 2018-05-07
 */

package com.justbnutz.ytdlcommandbuilder;

import android.support.v4.app.Fragment;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.Schedulers;

/**
 * Utility Fragment to handle the processing of the final command output.
 * Results are sent out via Rx.
 */
public class HeadlessFragCommandEditor extends Fragment{

    public static final String TAG = ActivityMain.PACKAGE_NAME + ".HeadlessFragCommandEditor";


    // List of RxDisposables for listening to Rx events (multiple pipeline style)
    private List<Disposable> mRxDisposables;

    // Rx Channels for Async operations & cross-Fragment communications
    private static final PublishProcessor<String[]> mRxInsertYtdlCommandOption = PublishProcessor.create();
    private static final PublishProcessor<RxCommandUpdatePackage> mRxYtdlCommandUpdateResponse = PublishProcessor.create();


    public HeadlessFragCommandEditor() {
        // Required public empty constructor
    }


    // region ================== STATIC METHODS ==================
    // ====== ================== ============== ==================


    /**
     * Use this factory method to create a new instance of this fragment using any provided parameters.
     * - newInstance vs Constructors: http://stackoverflow.com/a/14655001
     */
    public static HeadlessFragCommandEditor newInstance() {
        return new HeadlessFragCommandEditor();
    }


    /**
     * Receive and process and full YTDL command update
     */
    public static void requestReplaceYtdlCommand(String newCommand) {
        mRxYtdlCommandUpdateResponse.onNext(
                new RxCommandUpdatePackage(
                        newCommand
                )
        );
    }


    /**
     * Receive and process an incoming addition to the YTDL command
     */
    public static void requestInsertYtdlCommandOption(String currentCommand, String newOption) {
        mRxInsertYtdlCommandOption.onNext(
                new String[] {
                        currentCommand,
                        newOption
                }
        );
    }


    /**
     * Rx Observable (Flowable) to emit the latest command update
     */
    public static Flowable<RxCommandUpdatePackage> rxYtdlCommandUpdate() {
        return mRxYtdlCommandUpdateResponse;
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

        // Process incoming YTDL option addition
        mRxDisposables.add(
                mRxInsertYtdlCommandOption
                        .onBackpressureBuffer()
                        .observeOn(Schedulers.computation())
                        .subscribe(
                                new Consumer<String[]>() {
                                    @Override
                                    public void accept(String[] ytdlCommandBits) {
                                        insertYtdlCommandOption(
                                                ytdlCommandBits[0],
                                                ytdlCommandBits[1]
                                        );
                                    }
                                }
                        )
        );
    }


    /**
     * Holder object for Edit Command Updates
     */
    static class RxCommandUpdatePackage {

        final String newCommand;
        final int selectionStart;
        final int selectionLength;

        RxCommandUpdatePackage(String newCommand, int selectionStart, int selectionLength) {
            this.newCommand = newCommand;
            this.selectionStart = selectionStart;
            this.selectionLength = selectionLength;
        }


        RxCommandUpdatePackage(String newCommand) {
            this.newCommand = newCommand;
            selectionStart = 0;
            selectionLength = 0;
        }
    }

    // endregion


    // region ================== YTDL COMMAND OPS ==================
    // ====== ================== ================ ==================


    /**
     * Take an incoming YTDL option and try and insert it into the command gracefully
     */
    private void insertYtdlCommandOption(String currentCommand, String newOption) {

        if (!TextUtils.isEmpty(newOption)) {

            String baseCommand = getString(R.string.ytdl_command_init);
            StringBuilder commandBuilder = new StringBuilder(currentCommand);

            int selectionStart = 0;
            int selectionLength = 0;

            // Check if the option has argument parameters to be highlighted
            int[] highlightBits = UtilStringValidationOps.checkParameterArgs(newOption);
            if (highlightBits.length > 0) {
                selectionStart = highlightBits[0];
                selectionLength = highlightBits[1];
            }

            // Make sure the command starts properly
            if (!currentCommand.startsWith(baseCommand)) {

                // If the given command does not already start with the base command, and is not empty, and first character is not a space, then add a space
                if (!TextUtils.isEmpty(currentCommand) && !currentCommand.startsWith(" ")) {
                    commandBuilder.insert(0, " ");
                }

                // Prefix the base command to the given current string
                commandBuilder.insert(0, baseCommand);
            }

            // Check if there is a URL at the end of the command
            int urlPosition = UtilStringValidationOps.checkUrlStringExists(commandBuilder.toString());

            // If no URL found, can just append the new option to the end of the String
            if (urlPosition < 0) {
                commandBuilder
                        .append(" ")
                        .append(newOption);

                selectionStart = commandBuilder.length() - selectionLength;

            } else {
                // If URL found, need to insert before the position
                commandBuilder
                        .insert(urlPosition, " ")
                        .insert(urlPosition, newOption);

                selectionStart += urlPosition;
            }

            // Update the EditText in the main Fragment
            mRxYtdlCommandUpdateResponse.onNext(
                    new RxCommandUpdatePackage(
                            commandBuilder.toString(),
                            selectionStart,
                            selectionLength
                    )
            );
        }
    }

    // endregion

}
