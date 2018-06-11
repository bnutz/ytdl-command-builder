/*
 * Created by Brian Lau on 2018-06-06
 * Copyright (c) 2018. All rights reserved.
 *
 * Last modified: 2018-06-06
 */

package com.justbnutz.ytdlcommandbuilder;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.widget.FrameLayout;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.processors.PublishProcessor;

public class HeadlessFragAdLoader extends Fragment {

    public static final String TAG = ActivityMain.PACKAGE_NAME + ".HeadlessFragAdLoader";


    // AdMob App IDs
    private static final String ADMOB_APP_ID_TEST = "ca-app-pub-3940256099942544~3347511713";
    private static final String ADMOB_APP_ID_LIVE = "ca-app-pub-5266307679569901~6474244988";

    // AdMob Ad Unit IDs
    private static final String ADMOB_AD_UNIT_TEST = "ca-app-pub-3940256099942544/6300978111";
    private static final String ADMOB_AD_UNIT_MAIN = "ca-app-pub-5266307679569901/6378467095";
    private static final String ADMOB_AD_UNIT_PAGE = "ca-app-pub-5266307679569901/7167970437";


    // List of RxDisposables for listening to Rx events (multiple pipeline style)
    private List<Disposable> mRxDisposables;

    // Rx Channels for Async operations & cross-Fragment communications
    private static final PublishProcessor<RxAdLoadPackage> mRxInitAdView = PublishProcessor.create();


    public HeadlessFragAdLoader() {}


    // region ================== STATIC METHODS ==================
    // ====== ================== ============== ==================


    /**
     * Use this factory method to create a new instance of this fragment using any provided parameters.
     * - newInstance vs Constructors: http://stackoverflow.com/a/14655001
     */
    public static HeadlessFragAdLoader newInstance() {
        return new HeadlessFragAdLoader();
    }


    /**
     * Load the main AdView panel
     */
    public static void requestLoadMainAd(FrameLayout adContainer) {
        mRxInitAdView.onNext(
                new RxAdLoadPackage(
                        ADMOB_AD_UNIT_MAIN,
                        adContainer
                )
        );
    }


    /**
     * Load the AdView panel for the main ViewPager ends
     */
    public static void requestLoadPageAd(FrameLayout adContainer) {
        mRxInitAdView.onNext(
                new RxAdLoadPackage(
                        ADMOB_AD_UNIT_PAGE,
                        adContainer
                )
        );
    }

    // endregion


    // region ================== PRIMARY FLOW ==================
    // ====== ================== ============ ==================


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getContext() != null) {
            // Initialise Admob components (https://developers.google.com/admob/android/quick-start)
            MobileAds.initialize(getContext(), ADMOB_APP_ID_LIVE);
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

        // Init an Ad load
        mRxDisposables.add(
                mRxInitAdView
                        .onBackpressureBuffer()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                new Consumer<RxAdLoadPackage>() {
                                    @Override
                                    public void accept(RxAdLoadPackage adLoadPackage) {
                                        loadAdView(
                                                adLoadPackage.adUnitId,
                                                adLoadPackage.adContainerView
                                        );
                                    }
                                }
                        )
        );
    }

    // endregion


    // region ================== ADVIEW OPERATIONS ==================
    // ====== ================== ================= ==================


    /**
     * Initiate the designated AdMob panel into the given container View
     */
    private void loadAdView(String adUnitId, FrameLayout adContainerView) {

        if (getContext() != null
                && !TextUtils.isEmpty(adUnitId)
                && adContainerView != null
                && adContainerView.getChildCount() < 1) {

            // If the AdView container does not yet contain an AdView object, create it
            AdView adView = new AdView(getContext());

            // Set the banner size according to which page its on
            switch (adUnitId) {
                case ADMOB_AD_UNIT_MAIN:
                    adView.setAdSize(AdSize.BANNER);
                    break;

                case ADMOB_AD_UNIT_PAGE:
                    adView.setAdSize(AdSize.MEDIUM_RECTANGLE);
                    break;

                default:
                    return;
            }

            // Set the Ad Unit ID
            if (!BuildConfig.DEBUG) {
                adView.setAdUnitId(adUnitId);

            } else {
                adView.setAdUnitId(ADMOB_AD_UNIT_TEST);

            }

            // Add the AdView to the container
            adContainerView.addView(adView);

            // If the AdView was successfully created, init the Ad Load
            AdRequest adRequest = new AdRequest.Builder().build();
            adView.loadAd(adRequest);
        }
    }


    private static class RxAdLoadPackage {

        final String adUnitId;
        final FrameLayout adContainerView;

        RxAdLoadPackage(String adUnitId, FrameLayout adContainerView) {
            this.adUnitId = adUnitId;
            this.adContainerView = adContainerView;
        }
    }

    // endregion
}
