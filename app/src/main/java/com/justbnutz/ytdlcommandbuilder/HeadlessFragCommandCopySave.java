/*
 * Created by Brian Lau on 2018-05-29
 * Copyright (c) 2018. All rights reserved.
 *
 * Last modified: 2018-05-29
 */

package com.justbnutz.ytdlcommandbuilder;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.processors.PublishProcessor;

public class HeadlessFragCommandCopySave extends Fragment {

    public static final String TAG = ActivityMain.PACKAGE_NAME + ".HeadlessFragCommandCopySave";


    // List of RxDisposables for listening to Rx events (multiple pipeline style)
    private List<Disposable> mRxDisposables;

    // Rx Channels for Async operations & cross-Fragment communications
    private static final PublishProcessor<RxCopySavePackage> mRxCopyCommandRequest = PublishProcessor.create();
    private static final PublishProcessor<RxCopySavePackage> mRxSaveCommandRequest = PublishProcessor.create();
    private static final PublishProcessor<RxOpenAppPackage> mRxOpenAppRequest = PublishProcessor.create();


    public HeadlessFragCommandCopySave() {
        // Required public empty constructor
    }


    // region ================== STATIC METHODS ==================
    // ====== ================== ============== ==================


    /**
     * Use this factory method to create a new instance of this fragment using any provided parameters.
     * - newInstance vs Constructors: http://stackoverflow.com/a/14655001
     */
    public static HeadlessFragCommandCopySave newInstance() {
        return new HeadlessFragCommandCopySave();
    }


    /**
     * Send the given command to the system clipboard, also handles Snackbar prompts and responses
     */
    public static void requestCopyCommand(View startView, String commandText) {
        mRxCopyCommandRequest.onNext(
                new RxCopySavePackage(
                        startView,
                        commandText
                )
        );
    }


    /**
     * Send the given command to be saved to the Preset list, also handles Snackbar prompts and responses
     */
    public static void requestSaveCommand(View startView, String commandText) {
        mRxSaveCommandRequest.onNext(
                new RxCopySavePackage(
                        startView,
                        commandText
                )
        );
    }


    /**
     * Try and open an app with the given App ID, if it doesn't exist, open the search result link
     * from the Play Store
     */
    public static void requestOpenAppId(View startView, String packageName) {
        mRxOpenAppRequest.onNext(
                new RxOpenAppPackage(
                        startView,
                        packageName
                )
        );
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

        // Copy the given command
        mRxDisposables.add(
                mRxCopyCommandRequest
                        .onBackpressureDrop()
                        .subscribe(
                                new Consumer<RxCopySavePackage>() {
                                    @Override
                                    public void accept(RxCopySavePackage rxCopySavePackage) {
                                        copyCommand(
                                                rxCopySavePackage.startView,
                                                rxCopySavePackage.commandText
                                        );
                                    }
                                }
                        )
        );

        // Save the given command
        mRxDisposables.add(
                mRxSaveCommandRequest
                        .onBackpressureDrop()
                        .subscribe(
                                new Consumer<RxCopySavePackage>() {
                                    @Override
                                    public void accept(RxCopySavePackage rxCopySavePackage) {
                                        saveCommand(
                                                rxCopySavePackage.startView,
                                                rxCopySavePackage.commandText
                                        );
                                    }
                                }
                        )
        );

        // Open the given App Id
        mRxDisposables.add(
                mRxOpenAppRequest
                        .onBackpressureDrop()
                        .subscribe(
                                new Consumer<RxOpenAppPackage>() {
                                    @Override
                                    public void accept(RxOpenAppPackage rxOpenAppPackage) {
                                        openAppId(
                                                rxOpenAppPackage.startView,
                                                rxOpenAppPackage.packageName
                                        );
                                    }
                                }
                        )
        );
    }


    /**
     * Holder object for Copy / Save operations
     */
    private static class RxCopySavePackage {

        final View startView;
        final String commandText;

        RxCopySavePackage(View startView, String commandText) {
            this.startView = startView;
            this.commandText = commandText;
        }
    }


    /**
     * Holder object for Open App operations
     */
    private static class RxOpenAppPackage {

        final View startView;
        final String packageName;

        public RxOpenAppPackage(View startView, String packageName) {
            this.startView = startView;
            this.packageName = packageName;
        }
    }

    // endregion


    // region ================== COPY / SAVE OPS ==================
    // ====== ================== ============== ==================


    /**
     * Copy the given text to the system clipboard
     *
     * Reference: https://developer.android.com/guide/topics/text/copy-paste
     *
     * @param startView Start View for the Snackbar.make() operation
     */
    private void copyCommand(View startView, String commandText) {

        if (getContext() != null && !TextUtils.isEmpty(commandText)) {

            // Pack up the commmand to be copied
            ClipData clipData = ClipData.newPlainText(
                    getString(R.string.ytdl_command_hint),
                    commandText
            );

            // Get a handle to the system Clipboard Service
            ClipboardManager clipboardManager = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);

            if (clipboardManager != null) {
                // Set the data as the clipboard's primary clip.
                clipboardManager.setPrimaryClip(clipData);

                // Notify the user
                HeadlessFragSnackbar.requestPopSnackBar(
                        startView,
                        getString(R.string.command_copy_success),
                        Snackbar.LENGTH_SHORT,
                        getString(R.string.snackbar_open_cmd_app),
                        new SnackbarListenerOpenCmdLineApp(startView)
                );
            }
        }
    }


    /**
     * Save the given text to the Preset List
     *
     * @param startView Start View for the Snackbar.make() operation
     */
    private void saveCommand(View startView, String commandText) {

        if (!TextUtils.isEmpty(commandText)) {
            HeadlessFragPresetManager.requestSaveNewPreset(
                    commandText.trim()
            );

        } else {
            HeadlessFragSnackbar.requestPopSnackBar(
                    startView,
                    getString(R.string.command_save_preset_empty),
                    Snackbar.LENGTH_SHORT
            );
        }
    }


    /**
     * Snackbar Action handler to open Command Line Application
     */
    private class SnackbarListenerOpenCmdLineApp implements View.OnClickListener {

        final View mStartView;

        SnackbarListenerOpenCmdLineApp(View startView) {
            mStartView = startView;
        }


        @Override
        public void onClick(View view) {

            Context context = view.getContext();

            if (context != null) {
                // Fetch the package name of the Command Line App out of SharedPrefs
                String packageName = android.preference.PreferenceManager.getDefaultSharedPreferences(context)
                        .getString(
                                getString(R.string.prefkey_cmd_app),
                                getString(R.string.prefkey_cmd_app_default)
                        );

                openAppId(mStartView, packageName);
            }
        }
    }

    // endregion


    // region ================== EXTERNAL APP OPS ==================
    // ====== ================== ================ ==================


    private void openAppId(@NonNull View startView, String packageName) {

        Context context = startView.getContext();

        if (context != null && !TextUtils.isEmpty(packageName)) {

            // Try and fetch the Launch Intent for the given package name (https://stackoverflow.com/a/15465797)
            Intent cmdAppIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);

            // If the Intent is found and valid, open the app
            if (cmdAppIntent != null) {
                cmdAppIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(cmdAppIntent);

            } else {
                try {
                    // If the Intent is not found, then App not installed and try and open the Play Store link (https://developer.android.com/distribute/marketing-tools/linking-to-google-play#android-app)
                    Intent playStoreIntent = new Intent(Intent.ACTION_VIEW);
                    playStoreIntent.setData(Uri.parse("market://details?id=" + packageName));
                    context.startActivity(playStoreIntent);

                } catch (Exception e) {
                    e.printStackTrace();

                    HeadlessFragSnackbar.requestPopSnackBar(
                            startView,
                            getString(R.string.snackbar_cmd_app_not_found),
                            Snackbar.LENGTH_SHORT
                    );
                }
            }
        }
    }

    // endregion
}
