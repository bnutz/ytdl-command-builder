/*
 * Created by Brian Lau on 2018-05-16
 * Copyright (c) 2018. All rights reserved.
 *
 * Last modified: 2018-05-16
 */

package com.justbnutz.ytdlcommandbuilder;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.processors.PublishProcessor;


/**
 * A Headless Fragment for handling Snackbar events.
 * Doing it this way so we can queue up multiple Snackbar messages at a time
 */
public class HeadlessFragSnackbar extends Fragment{

    public static final String TAG = ActivityMain.PACKAGE_NAME + ".HeadlessFragSnackbar";

    // List of RxSubscriptions for listening to Rx events (multiple pipeline style)
    private List<Disposable> mRxDisposables = new ArrayList<>();

    // Reactive path for incoming SnackBar requests
    private static final PublishProcessor<PopSnackBar> mRxPopSnackBarRequest = PublishProcessor.create();

    private List<PopSnackBar> mSnackQueue;

    private Snackbar mCurrentSnackBar;


    public HeadlessFragSnackbar() {}


    // region ================== STATIC METHODS ==================
    // ====== ================== ============== ==================


    /**
     * Use this factory method to create a new instance of this fragment using any provided parameters.
     * - newInstance vs Constructors: http://stackoverflow.com/a/14655001
     */
    public static HeadlessFragSnackbar newInstance() {
        return new HeadlessFragSnackbar();
    }


    /**
     * Queue up a Snackbar with a given duration and pop it off when ready. If there are multiple
     * Snackbars to be sent, then each will be added to the end of the line and popped when the previous
     * is done.
     */
    public static void requestPopSnackBar(View startView,
                                          String snackMsg,
                                          int snackDuration) {
        mRxPopSnackBarRequest.onNext(
                new PopSnackBar(
                        startView,
                        snackMsg,
                        snackDuration
                )
        );
    }


    /**
     * Queue up a Snackbar with a given duration and pop it off when ready.
     * Provides an additional option to create a Snackbar with a action button.
     */
    public static void requestPopSnackBar(View startView,
                                          String snackMsg,
                                          int snackDuration,
                                          String actionMsg,
                                          View.OnClickListener actionListener) {
        mRxPopSnackBarRequest.onNext(
                new PopSnackBar(
                        startView,
                        snackMsg,
                        snackDuration,
                        actionMsg,
                        actionListener
                )
        );
    }

    // endregion


    // region ================== PRIMARY FLOW ==================
    // ====== ================== ============ ==================


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mSnackQueue = new ArrayList<>();
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
        for (Disposable rxSub: mRxDisposables) {
            if (!rxSub.isDisposed()) {
                rxSub.dispose();
            }
        }
        mRxDisposables.clear();
    }


    /**
     * Subscribe to the relevant Rx Observables for this fragment and place them in the RxSub list
     */
    private void initRxDisposables() {
        mRxDisposables = new ArrayList<>();

        // RxBus Snackbar Event - Add the Snackbar to the queue (if it qualifies) and pop them off sequentially
        mRxDisposables.add(
                mRxPopSnackBarRequest.subscribe(
                        new Consumer<PopSnackBar>() {
                            @Override
                            public void accept(PopSnackBar popSnackBar) {

                                // Only add if the Snackbar does not already exist in the queue
                                if (!mSnackQueue.contains(popSnackBar)) {
                                    mSnackQueue.add(popSnackBar);

                                    // Send it off to be popped
                                    popNextSnackbar();
                                }
                            }
                        }
                )
        );
    }


    /**
     * Holder object for storing the properties we want to send to the Snackbar
     */
    private static class PopSnackBar {

        final View startView;
        final String snackbarText;
        final int snackbarDuration;
        final String actionText;
        final View.OnClickListener actionClickListener;

        PopSnackBar(View initialView, String snackMsg, int snackLength) {
            startView = initialView;
            snackbarText = snackMsg;
            snackbarDuration = snackLength;
            actionText = null;
            actionClickListener = null;
        }


        PopSnackBar(View initialView, String snackMsg, int snackLength, String actionMsg, View.OnClickListener actionListener) {
            startView = initialView;
            snackbarText = snackMsg;
            snackbarDuration = snackLength;
            actionText = actionMsg;
            actionClickListener = actionListener;
        }


        @Override
        public int hashCode() {
            // Since we're overriding equals, should also provide a hashcode based on the Snackbar properties as well
            if ((actionText == null) || (actionClickListener == null)) {
                return startView.hashCode() + snackbarText.hashCode() + snackbarDuration;

            } else {
                return startView.hashCode() + snackbarText.hashCode() + snackbarDuration + actionText.hashCode() + actionClickListener.hashCode();

            }
        }


        @Override
        public boolean equals(Object checkThing) {
            // Compare each element individually to see if they're all the same
            if (checkThing instanceof PopSnackBar) {

                if (actionText == null
                        && actionClickListener == null
                        && ((PopSnackBar) checkThing).actionText == null
                        && ((PopSnackBar) checkThing).actionClickListener == null) {

                    return (snackbarText.equals(((PopSnackBar) checkThing).snackbarText)
                            && (snackbarDuration == ((PopSnackBar) checkThing).snackbarDuration));


                } else if (actionText != null
                        && actionClickListener != null
                        && ((PopSnackBar) checkThing).actionText != null
                        && ((PopSnackBar) checkThing).actionClickListener != null) {

                    return (snackbarText.equals(((PopSnackBar) checkThing).snackbarText)
                            && (snackbarDuration == ((PopSnackBar) checkThing).snackbarDuration)
                            && actionText.equals(((PopSnackBar) checkThing).actionText));

                    // Not comparing actionClickListener as it looks like we can't compare the click properties of those objects
                }
            }

            return false;
        }
    }

    // endregion


    // region ================== SNACKBAR OPERATIONS ==================
    // ====== ================== =================== ==================


    /**
     * Check the Snackbar Queue, and if there is one waiting and we're not already showing one - pop it off
     */
    private void popNextSnackbar() {

        // Only initiate the Snackbar if there isn't one already showing
        if (mCurrentSnackBar == null
                && (mSnackQueue.size() > 0)) {

            mCurrentSnackBar = makeNextSnackBar(
                    mSnackQueue.get(0).startView,
                    mSnackQueue.get(0).snackbarText,
                    mSnackQueue.get(0).snackbarDuration,
                    mSnackQueue.get(0).actionText,
                    mSnackQueue.get(0).actionClickListener
            );

            if (mCurrentSnackBar != null) {
                mCurrentSnackBar.show();
            }
        }
    }


    /**
     * Create a new Snackbar object based on the given properties
     *
     * @param startView View from which the Snackbar can build from (Snackbar.make() will automatically
     *                  trace back along its parent Views until a suitable View can be found)
     */
    private Snackbar makeNextSnackBar(View startView,
                                      String snackbarText, int snackbarDuration,
                                      String actionText, @Nullable View.OnClickListener actionClickListener) {

        try {
            Snackbar nextSnackBar = Snackbar.make(
                    startView,
                    snackbarText,
                    snackbarDuration
            );

            // Add the Callback to detect when the Snackbar has finished
            nextSnackBar.addCallback(mSnackCallback);

            // Add the Snackbar action (if one is needed)
            if (actionClickListener != null) {
                nextSnackBar.setAction(
                        actionText,
                        actionClickListener
                );
            }
            return nextSnackBar;

        } catch (Exception e) {

            if (mSnackQueue.size() > 0) {
                mSnackQueue.remove(0);
                popNextSnackbar();
            }
            return null;
        }
    }


    /**
     * Standard Snackbar callback object for reporting Snackbar events
     */
    Snackbar.Callback mSnackCallback = new Snackbar.Callback() {

        @Override
        public void onDismissed(Snackbar departingSnackbar, int event) {
            super.onDismissed(departingSnackbar, event);

            // Reset the statuses and initiate the next Snack (if one is there)
            mCurrentSnackBar = null;
            if (mSnackQueue.size() > 0) {
                mSnackQueue.remove(0);
                popNextSnackbar();
            }
        }
    };

    // endregion
}

