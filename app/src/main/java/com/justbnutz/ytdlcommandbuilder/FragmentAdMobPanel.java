/*
 * Created by Brian Lau on 2018-06-07
 * Copyright (c) 2018. All rights reserved.
 *
 * Last modified: 2018-06-07
 */

package com.justbnutz.ytdlcommandbuilder;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ScrollView;

/**
 * Very simple Fragment for holding an AdMob AdView to be used in a FragmentPagerAdapter
 */
public class FragmentAdMobPanel extends Fragment {

    public static final String TAG = ActivityMain.PACKAGE_NAME + ".FragmentAdMobPanel";

    private FrameLayout mAdContainer;


    public FragmentAdMobPanel() {}


    // region ================== STATIC METHODS ==================
    // ====== ================== ============== ==================


    /**
     * Use this factory method to create a new instance of this fragment using any provided parameters.
     * - newInstance vs Constructors: http://stackoverflow.com/a/14655001
     */
    public static FragmentAdMobPanel newInstance() {
        return new FragmentAdMobPanel();
    }

    // endregion


    // region ================== PRIMARY FLOW ==================
    // ====== ================== ============ ==================


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        // Put the Ad Container view in a ScrollView in case the user rotates their device (FrameLayout by itself does not scroll)
        ScrollView scrollableContainer = new ScrollView(getContext());
        scrollableContainer.setPadding(10, 10, 10, 10);
        scrollableContainer.setClipToPadding(false);
        scrollableContainer.setClipChildren(false);

        // Can just use the Big Pic item layout as it already contains everything we need
        return inflater.inflate(R.layout.fragment_admob_panel, scrollableContainer, true);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Link the Ad Container View
        mAdContainer = view.findViewById(R.id.adview_container_page);
    }


    @Override
    public void onResume() {
        super.onResume();

        if (mAdContainer != null) {
            // Initiate the Ad Load
            HeadlessFragAdLoader.requestLoadPageAd(mAdContainer);
        }
    }

    // endregion
}
