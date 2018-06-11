/*
 * Created by Brian Lau on 2018-04-13
 * Copyright (c) 2018. All rights reserved.
 *
 * Last modified: 2018-04-13
 */

package com.justbnutz.ytdlcommandbuilder;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.text.TextUtils;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.Schedulers;

public class ActivityMain extends AppCompatActivity implements TabLayout.OnTabSelectedListener {

    public static final String PACKAGE_NAME = "com.justbnutz.ytdlcommandbuilder";

    public static final String TAG = PACKAGE_NAME + ".ActivityMain";

    public static final String INTENTEXTRA_SHARE_TEXT = TAG + ".INTENTEXTRA_SHARE_TEXT";


    // List of RxDisposables for listening to Rx events (multiple pipeline style)
    private List<Disposable> mRxDisposables;

    // Rx Channels for Async operations & cross-Fragment communications
    private static final PublishProcessor<Integer> mRxTabUpdate = PublishProcessor.create();
    private static final PublishProcessor<Integer> mRxTabClick = PublishProcessor.create();

    // Preferences
    private SharedPreferences mSharedPrefs;

    // FragmentManager
    private FragmentManager mFragmentManager;

    // Views
    private ViewPager mFragmentPager;
    private TabLayout mTabLayout;


    // region ================== STATIC METHODS ==================
    // ====== ================== ============== ==================


    /**
     * Update the TabLayout selected Tab to the item with the given Tag ID
     *
     * Note that this is a UI update only, ViewPage selection is not performed
     */
    public static void requestSetSelectedTab(int tabTagId) {
        mRxTabUpdate.onNext(tabTagId);
    }


    /**
     * Programmatically click the Tab with the given ID.
     *
     * This simulates a user actually clicking the Tab item
     */
    public static void requestClickTab(int tabTagId) {
        mRxTabClick.onNext(tabTagId);
    }

    // endregion


    // region ================== PRIMARY FLOW ==================
    // ====== ================== ============ ==================


    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load in the SharedPrefs
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        loadDefaultPreferences();

        // Set the layout
        setContentView(R.layout.activity_main);

        // Hold a reference to the Fragment Manager
        mFragmentManager = getSupportFragmentManager();

        // Load in the Headless Fragments
        initHeadlessFragment(HeadlessFragSnackbar.TAG);
        initHeadlessFragment(HeadlessFragRawHelpLoader.TAG);
        initHeadlessFragment(HeadlessFragCommandEditor.TAG);
        initHeadlessFragment(HeadlessFragPresetManager.TAG);
        initHeadlessFragment(HeadlessFragCommandCopySave.TAG);
        initHeadlessFragment(HeadlessFragAdLoader.TAG);

        // Link the ViewPager and its Adapter
        mFragmentPager = findViewById(R.id.viewpager_fragment_holder);
        mFragmentPager.setAdapter(new AdapterMainFragmentPager(mFragmentManager));
        mFragmentPager.addOnPageChangeListener(mFragmentPagerChangeListener);

        // Set up the TabLayout
        mTabLayout = findViewById(R.id.tabs_fragment_holder);
        mTabLayout.addOnTabSelectedListener(this);
        populateDefaultTabs();

        // Initiate the starting Tab (programmatically click)
        mTabLayout.post(
                new Runnable() {
                    @Override
                    public void run() {

                        // Check to see if app was opened via Share To action, if so then process that flow
                        String sharedLink = getIntent().getStringExtra(INTENTEXTRA_SHARE_TEXT);

                        if (TextUtils.isEmpty(sharedLink)) {
                            // If no Shared Link found, then retrieve the Start Tab out of SharedPrefs (need to convert it back from String first)
                            int startTabTagId = Integer.parseInt(
                                    mSharedPrefs.getString(
                                            getString(R.string.prefkey_start_tab),
                                            UtilTabProperties.getDefaultStartTabId()
                                    )
                            );

                            // Find the Tab in the TabLayout and click it
                            findTab(startTabTagId).select();

                        } else {
                            // If the app was opened via Shared Link, then process that text in the Edit Command tab
                            findTab(R.id.tag_id_tab_command_edit).select();

                            // Pop dialog to check whether to create a barebones command with this link, or to append it to one of the presets
                            FragmentCommandBuilder.requestPopShareToLinkDialog(sharedLink);
                        }

                        // Initiate the main Ad banner load
                        HeadlessFragAdLoader.requestLoadMainAd(
                                (FrameLayout) findViewById(R.id.adview_container_main)
                        );
                    }
                }
        );
    }


    @Override
    protected void onStart() {
        super.onStart();

        clearRxDisposables();
        initRxDisposables();
    }


    @Override
    protected void onStop() {

        clearRxDisposables();

        super.onStop();
    }

    // endregion


    // region ================== FIRST-TIME LOAD OPS ==================
    // ====== ================== =================== ==================


    /**
     * Check if SharedPrefs have been initialised before, if not - then load in default values so PreferenceFragment
     * labels can be properly updated when it is loaded.
     */
    private void loadDefaultPreferences() {

        // Check the Start Tab
        runDefaultStringPref(
                getString(R.string.prefkey_start_tab),
                UtilTabProperties.getDefaultStartTabId()
        );

        // Check Text Size (use the Medium value as default)
        String[] textSizeList = getResources().getStringArray(R.array.pref_text_size_list);
        if (textSizeList.length > 2) {
            runDefaultStringPref(
                    getString(R.string.prefkey_text_size),
                    textSizeList[3]
            );
        }

        // Check Command Line App
        runDefaultStringPref(
                getString(R.string.prefkey_cmd_app),
                getString(R.string.prefkey_cmd_app_default)
        );
    }


    /**
     * Check the given prefKey if anything is stored under that key. If a blank String value is found, then save
     * in the given default value.
     *
     * Doing it this was as PreferenceManager.setDefaultValues doesn't seem to work the way I need it to work.
     */
    private void runDefaultStringPref(String prefKey, String defaultValue) {

        // Fetch the SharedPref value under the given key
        String checkPref = mSharedPrefs.getString(prefKey, "");

        // If an empty String was loaded, save the given defaultValue
        if (TextUtils.isEmpty(checkPref)) {
            mSharedPrefs.edit()
                    .putString(prefKey, defaultValue)
                    .apply();
        }
    }

    // endregion


    // region ================== TABLAYOUT OPS ==================
    // ====== ================== ============= ==================


    /**
     * Populate the main TabLayout with the app tabs
     */
    private void populateDefaultTabs() {

        // Loop through the Tab Property arrays and add
        for (int tabIndex = 0; tabIndex < UtilTabProperties.getTabLabelIds().length; tabIndex++) {
            mTabLayout.addTab(
                    mTabLayout.newTab()
                            .setIcon(UtilTabProperties.getTabIconIds()[tabIndex])
                            .setTag(UtilTabProperties.getTabTags()[tabIndex])
                            .setContentDescription(UtilTabProperties.getTabLabelIds()[tabIndex])
            );
        }
    }


    /**
     * Run through the available tabs in mTabLayout and return the one that matches the given Tab TagId
     */
    private TabLayout.Tab findTab(int tabTagId) {

        TabLayout.Tab checkTab;

        for (int tabIndex = 0; tabIndex < mTabLayout.getTabCount(); tabIndex++) {
            checkTab = mTabLayout.getTabAt(tabIndex);

            // Make sure there a Tag in the Tab to check with
            if (checkTab != null
                    && checkTab.getTag() != null
                    && checkTab.getTag().equals(tabTagId)) {

                // If the Tag in the Tab matches the give TagString, then we've found our selected Tag
                return checkTab;
            }
        }

        // If we get here, then return a Dummy tab as Rx Functions can no longer return a null
        return mTabLayout.newTab();
    }


    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        selectTab(tab);
    }


    @Override
    public void onTabUnselected(TabLayout.Tab tab) {}


    @Override
    public void onTabReselected(TabLayout.Tab tab) {
        selectTab(tab);
    }


    /**
     * Toggle the FragmentPager according to the selected tab
     */
    private void selectTab(TabLayout.Tab tab) {

        Boolean updateUiOnly = (Boolean) mTabLayout.getTag(R.id.tag_id_tab_update_indicator_only);

        if (updateUiOnly == null || !updateUiOnly) {
            // Only do ViewPager actions on direct button presses (ViewPageChange updates will toggle this off)

            if (mFragmentPager.getAdapter() != null) {

                switch (tab.getPosition()) {
                    case 0: // How To
                        switchFragmentPager(FragmentHowTo.TAG);
                        break;

                    case 1: // Search
                        switchFragmentPager(FragmentCommandBuilder.TAG);
                        FragmentCommandBuilder.requestSelectToolboxOption(R.id.tag_id_tab_search);
                        break;

                    case 2: // Command Builder
                        switchFragmentPager(FragmentCommandBuilder.TAG);
                        FragmentCommandBuilder.requestSelectToolboxOption(R.id.tag_id_tab_command_edit);
                        break;

                    case 3: // Presets
                        switchFragmentPager(FragmentCommandBuilder.TAG);
                        FragmentCommandBuilder.requestSelectToolboxOption(R.id.tag_id_tab_command_preset);
                        break;

                    case 4: // Settings
                        switchFragmentPager(FragmentPreferences.TAG);
                        break;

                    default:
                        break;
                }
            }
        }

        // Reset the updateUiOnly flag when done
        mTabLayout.setTag(R.id.tag_id_tab_update_indicator_only, false);
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

        // Set the TabLayout according to the current open page
        mRxDisposables.add(
                mRxTabUpdate
                        .onBackpressureDrop()
                        .observeOn(Schedulers.computation())
                        .map(
                                new Function<Integer, TabLayout.Tab>() {
                                    @Override
                                    public TabLayout.Tab apply(Integer tagId) {
                                        return findTab(tagId);
                                    }
                                }
                        )
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                new Consumer<TabLayout.Tab>() {
                                    @Override
                                    public void accept(TabLayout.Tab selectedTab) {
                                        if (!selectedTab.isSelected()) {
                                            // TabLayout.Tab don't have key-based tags, so need to store this in the parent TabLayout instead
                                            mTabLayout.setTag(R.id.tag_id_tab_update_indicator_only, true);

                                            // Refresh just the indicator of the TabLayout
                                            selectedTab.select();
                                        }
                                    }
                                }
                        )
        );

        // Simulate a user clicking the given Tab
        mRxDisposables.add(
                mRxTabClick
                        .onBackpressureDrop()
                        .observeOn(Schedulers.computation())
                        .map(
                                new Function<Integer, TabLayout.Tab>() {
                                    @Override
                                    public TabLayout.Tab apply(Integer tagId) {
                                        return findTab(tagId);
                                    }
                                }
                        )
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                new Consumer<TabLayout.Tab>() {
                                    @Override
                                    public void accept(TabLayout.Tab selectedTab) {
                                        if (!selectedTab.isSelected()) {
                                            // TabLayout.Tab don't have key-based tags, so need to store this in the parent TabLayout instead
                                            mTabLayout.setTag(R.id.tag_id_tab_update_indicator_only, false);

                                            // Refresh just the indicator of the TabLayout
                                            selectedTab.select();
                                        }
                                    }
                                }
                        )
        );
    }

    // endregion


    // region ================== FRAGMENT OPS ==================
    // ====== ================== ============ ==================


    /**
     * Initialise or Restore the given Headless Fragment (no container View used)
     */
    private void initHeadlessFragment(String fragmentTag) {

        Fragment headlessFragment = mFragmentManager.findFragmentByTag(fragmentTag);

        if (headlessFragment == null) {
            // If the Headless Fragment does not yet exist, create it and add it to the stack

            switch (fragmentTag) {

                case HeadlessFragSnackbar.TAG:
                    headlessFragment = HeadlessFragSnackbar.newInstance();
                    break;

                case HeadlessFragRawHelpLoader.TAG:
                    headlessFragment = HeadlessFragRawHelpLoader.newInstance();
                    break;

                case HeadlessFragCommandEditor.TAG:
                    headlessFragment = HeadlessFragCommandEditor.newInstance();
                    break;

                case HeadlessFragPresetManager.TAG:
                    headlessFragment = HeadlessFragPresetManager.newInstance();
                    break;

                case HeadlessFragCommandCopySave.TAG:
                    headlessFragment = HeadlessFragCommandCopySave.newInstance();
                    break;

                case HeadlessFragAdLoader.TAG:
                    headlessFragment = HeadlessFragAdLoader.newInstance();
                    break;

                default:
                    break;
            }

            if (headlessFragment != null) {
                // Headless fragment is now valid, load it in
                mFragmentManager
                        .beginTransaction()
                        .add(
                                headlessFragment,
                                fragmentTag
                        )
                        .commit();
            }
        }
    }


    /**
     * Switch the FragmentViewPager to the Fragment with the given Tag
     *
     * (Assumes the ViewPager definitely contains a Fragment with the given Tag)
     */
    private void switchFragmentPager(String fragmentTag) {

        PagerAdapter fragmentPagerAdapter = mFragmentPager.getAdapter();

        if (!TextUtils.isEmpty(fragmentTag)
                && fragmentPagerAdapter != null) {

            // Temporarily remove the PageChangeListener as this can affect Tab updates
            mFragmentPager.removeOnPageChangeListener(mFragmentPagerChangeListener);

            // Switch the FragmentPager
            mFragmentPager.setCurrentItem(
                    fragmentPagerAdapter.getItemPosition(fragmentTag),
                    true
            );

            // Put the PageChangeListener back
            mFragmentPager.addOnPageChangeListener(mFragmentPagerChangeListener);
        }
    }


    /**
     * ViewPager Adapter for handling the UI Fragments
     */
    private static class AdapterMainFragmentPager extends FragmentPagerAdapter {

        private final List<Fragment> mYtdlFragments;
        private final List<String> mYtdlFragmentTags;

        AdapterMainFragmentPager(FragmentManager fm) {
            super(fm);

            // Initialise and set up the Fragments and corresponding Tag arrays
            mYtdlFragments = new ArrayList<>();
            mYtdlFragmentTags = new ArrayList<>();

            addFragment(FragmentAdMobPanel.newInstance(), FragmentAdMobPanel.TAG);
            addFragment(FragmentHowTo.newInstance(), FragmentHowTo.TAG);
            addFragment(FragmentCommandBuilder.newInstance(), FragmentCommandBuilder.TAG);
            addFragment(FragmentPreferences.newInstance(), FragmentPreferences.TAG);
            addFragment(FragmentAdMobPanel.newInstance(), FragmentAdMobPanel.TAG);
        }


        @Override
        public Fragment getItem(int position) {

            if (position < mYtdlFragments.size()) {
                return mYtdlFragments.get(position);

            } else {
                return null;

            }
        }


        @Override
        public int getCount() {
            return mYtdlFragments.size();
        }


        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            return null;
        }


        @Override
        public int getItemPosition(@NonNull Object fragmentTag) {
            if (fragmentTag instanceof String) {
                return mYtdlFragmentTags.indexOf(fragmentTag);
            }
            return super.getItemPosition(fragmentTag);
        }


        /**
         * Add a new Fragment and its corresponding Tag to the ViewPager
         */
        private void addFragment(Fragment newFragment, String fragmentTag) {
            mYtdlFragments.add(newFragment);
            mYtdlFragmentTags.add(fragmentTag);
        }
    }


    /**
     * Update the TabLayout when FragmentPager changes
     */
    private ViewPager.SimpleOnPageChangeListener mFragmentPagerChangeListener = new ViewPager.SimpleOnPageChangeListener() {
        @Override
        public void onPageSelected(int position) {

            switch (position) {
                case 1: // How To Fragment
                    mRxTabUpdate.onNext(R.id.tag_id_tab_howto);
                    break;

                case 2: // Send to FragmentCommandBuilder to be handled there
                    FragmentCommandBuilder.requestCommandToolboxPageUpdate();
                    break;

                case 3: // Preference Fragment
                    mRxTabUpdate.onNext(R.id.tag_id_tab_settings);
                    break;

                default:
                    super.onPageSelected(position);
                    break;
            }
        }
    };

    // endregion
}
