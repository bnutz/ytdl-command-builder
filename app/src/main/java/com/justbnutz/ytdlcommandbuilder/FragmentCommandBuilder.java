/*
 * Created by Brian Lau on 2018-04-13
 * Copyright (c) 2018. All rights reserved.
 *
 * Last modified: 2018-04-13
 */

package com.justbnutz.ytdlcommandbuilder;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Animatable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnticipateInterpolator;
import android.view.animation.Interpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SearchView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.Schedulers;


public class FragmentCommandBuilder extends Fragment {

    public static final String TAG = ActivityMain.PACKAGE_NAME + ".FragmentCommandBuilder";


    // List of RxDisposables for listening to Rx events (multiple pipeline style)
    private List<Disposable> mRxDisposables;

    // Rx Channels for Async operations & cross-Fragment communications (https://gist.github.com/xmarcusv/36038b4ebe541d8243b984da906cc409)
    private static final PublishProcessor<Integer> mRxSwitchCommandBoxViewPager = PublishProcessor.create();
    private static final PublishProcessor<Boolean> mRxCommandBoxPageCheckRequest = PublishProcessor.create();
    private static final PublishProcessor<Boolean> mRxToggleFabVisibility = PublishProcessor.create();
    private static final PublishProcessor<Boolean> mRxUpdateTextSizeRequest = PublishProcessor.create();

    private static final PublishProcessor<RxYtdlHeaderPackage> mRxToggleHeaderItem = PublishProcessor.create();
    private static final PublishProcessor<String> mRxFilterOptionList = PublishProcessor.create();
    private static final PublishProcessor<String> mRxUpdateDragShadow = PublishProcessor.create();
    private static final PublishProcessor<String> mRxInsertNewOptionRequest = PublishProcessor.create();

    private static final PublishProcessor<String> mRxProcessSharedLinkRequest = PublishProcessor.create();

    // Drag Listener
    private ListenerYtdlOptionDrag mListenerYtdlOptionDrag;

    // List Adapters
    private AdapterYtdlOptions mAdapterYtdlOptions;
    private AdapterYtdlPresets mAdapterYtdlPresets;

    // Views
    private AppBarLayout mAppBarLayout;
    private EditText mTxtYtdlCommand;
    private ViewPager mViewPagerCommandBox;
    private FloatingActionButton mFabAction;
    private SearchView mSearchViewOptions;

    private RecyclerView mRecyclerPresetList;

    // Animation references
    private int mAnimateFabDuration;
    private Interpolator mAnimateFabInterpolator;


    public FragmentCommandBuilder() {}


    // region ================== STATIC METHODS ==================
    // ====== ================== ============== ==================


    /**
     * Use this factory method to create a new instance of this fragment using any provided parameters.
     * - newInstance vs Constructors: http://stackoverflow.com/a/14655001
     */
    public static FragmentCommandBuilder newInstance() {
        return new FragmentCommandBuilder();
    }


    /**
     * Set the selected Toolbox ViewPager page
     */
    public static void requestSelectToolboxOption(int tabTagId) {
        mRxSwitchCommandBoxViewPager.onNext(tabTagId);
    }


    /**
     * Trigger a YTDL to either expand/collapse its subitem list
     */
    public static void requestToggleHeaderItem(int headerId, ImageView headerIcon) {
        mRxToggleHeaderItem.onNext(
                new RxYtdlHeaderPackage(
                        headerId,
                        headerIcon
                )
        );
    }


    /**
     * Update the text label in the DragShadow
     */
    public static void requestDragShadowTextUpdate(String shadowText) {
        mRxUpdateDragShadow.onNext(shadowText);
    }


    /**
     * Toggle the visibility of the FAB button
     */
    public static void requestToggleFabVisibility(boolean isVisible) {
        mRxToggleFabVisibility.onNext(isVisible);
    }


    /**
     * Request to update the Text Size of the Edit Command TextBox
     */
    public static void requestUpdateTextSize() {
        mRxUpdateTextSizeRequest.onNext(true);
    }


    /**
     * Incoming request to update the command in the EditText with the given Option
     */
    public static void requestInsertNewOption(String newOption) {
        mRxInsertNewOptionRequest.onNext(newOption);
    }


    /**
     * Notify the ViewPager to ping out an update of its current Page status
     */
    public static void requestCommandToolboxPageUpdate() {
        mRxCommandBoxPageCheckRequest.onNext(true);
    }


    /**
     * Request to process an incoming Share To command
     */
    public static void requestPopShareToLinkDialog(String sharedLink) {
        mRxProcessSharedLinkRequest.onNext(sharedLink);
    }

    // endregion


    // region ================== PRIMARY FLOW ==================
    // ====== ================== ============ ==================


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getContext() != null) {
            mAdapterYtdlOptions = new AdapterYtdlOptions(getContext());
            mAdapterYtdlPresets = new AdapterYtdlPresets(getContext());

            mAnimateFabDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);
            mAnimateFabInterpolator = new AnticipateInterpolator();
        }
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_command_builder, container, false);
    }


    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initiate the Drag Zone Listener
        mListenerYtdlOptionDrag = new ListenerYtdlOptionDrag(
                (FrameLayout) view.findViewById(R.id.frame_dragshadow)
        );

        // Link the Fab button properties
        mFabAction = view.findViewById(R.id.fab_action_command);
        mFabAction.setOnClickListener(mFabClickListener);

        // Add a listener to the AppBarLayout to detect CollapsingToolbar events
        mAppBarLayout = view.findViewById(R.id.appbar_layout);
        mAppBarLayout.addOnOffsetChangedListener(mAppBarOffsetListener);

        // Link the Final Command TextView
        mTxtYtdlCommand = view.findViewById(R.id.txt_ytdl_command);
        mTxtYtdlCommand.setOnDragListener(mListenerYtdlOptionDrag);
        mTxtYtdlCommand.setOnTouchListener(mEditTextTouchListener);

        // Set EditText text size based off of SharedPrefs
        setCommandBoxTextSize();

        // Link the SearchView
        mSearchViewOptions = view.findViewById(R.id.searchview_option_list);
        mSearchViewOptions.setTranslationY(mSearchViewOptions.getHeight() * 2);
        mSearchViewOptions.setOnQueryTextListener(mSearchViewQueryTextListener);

        // Link the ViewPager for the Command Toolbox
        mViewPagerCommandBox = view.findViewById(R.id.viewpager_command_toolbox);
        mViewPagerCommandBox.setAdapter(new PagerAdapterCommandToolbox());
        mViewPagerCommandBox.addOnPageChangeListener(mViewPagerChangeListener);

        // Link the Options List RecyclerView
        RecyclerView recyclerViewYtdlOptions = view.findViewById(R.id.recycler_command_ytdl_options);
        recyclerViewYtdlOptions.setAdapter(mAdapterYtdlOptions);
        recyclerViewYtdlOptions.setOnDragListener(mListenerYtdlOptionDrag);

        // Pull focus to prevent the EditText above from immediately bringing up the virtual keyboard
        recyclerViewYtdlOptions.requestFocus();

        // Link the Preset List RecyclerView
        mRecyclerPresetList = view.findViewById(R.id.recycler_saved_presets);
        mRecyclerPresetList.setAdapter(mAdapterYtdlPresets);

        // Trigger the Ytdl List Load
        recyclerViewYtdlOptions.post(
                new Runnable() {
                    @Override
                    public void run() {
                        HeadlessFragRawHelpLoader.requestYtdlOptionList();
                    }
                }
        );

        // Trigger the Preset List Load
        mRecyclerPresetList.post(
                new Runnable() {
                    @Override
                    public void run() {
                        HeadlessFragPresetManager.requestCurrentPresetList();
                    }
                }
        );
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


    /**
     * Quick TouchListener to make the EditText draggable by disabling the CoordinatorLayout from
     * intercepting its touches.
     */
    private View.OnTouchListener mEditTextTouchListener = new View.OnTouchListener() {

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {

            switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_UP:
                    view.getParent().requestDisallowInterceptTouchEvent(false);
                    break;

                default:
                    view.getParent().requestDisallowInterceptTouchEvent(true);
                    break;
            }

            return false;
        }
    };


    /**
     * Quick OffSetChangedListener to detect when the CollapsingToolbarLayout is expanded / collapsed
     *
     * Reference: https://stackoverflow.com/a/31872915
     */
    AppBarLayout.OnOffsetChangedListener mAppBarOffsetListener = new AppBarLayout.OnOffsetChangedListener() {

        @Override
        public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
            if (verticalOffset == 0) {
                // Make sure the Fab button position is properly reset when expanded out again
                toggleFabVisibility(true);
            }
        }
    };

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

        // Toggle the ViewPager according to the selected Tab
        mRxDisposables.add(
                mRxSwitchCommandBoxViewPager
                        .onBackpressureDrop()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                new Consumer<Integer>() {
                                    @Override
                                    public void accept(Integer tabTagId) {

                                        // Switch the Toolbox ViewPager accordingly
                                        switch (tabTagId) {
                                            case R.id.tag_id_tab_search:
                                                toggleSearchViewVisibility(true);
                                                mViewPagerCommandBox.setCurrentItem(0, true);
                                                mFabAction.setTag(R.id.tag_id_fab_action_copy, true);

                                                // Collapse the AppBar to allow more room for results to show
                                                mAppBarLayout.setExpanded(false, true);

                                                // Set focus to SearchView
                                                mSearchViewOptions.requestFocus();

                                                break;

                                            case R.id.tag_id_tab_command_edit:
                                                toggleSearchViewVisibility(false);
                                                mViewPagerCommandBox.setCurrentItem(0, true);
                                                mFabAction.setTag(R.id.tag_id_fab_action_copy, true);

                                                // Expand the AppBar to show the Fab button
                                                mAppBarLayout.setExpanded(true, true);
                                                break;

                                            case R.id.tag_id_tab_command_preset:
                                                toggleSearchViewVisibility(false);
                                                mViewPagerCommandBox.setCurrentItem(1, true);
                                                mFabAction.setTag(R.id.tag_id_fab_action_copy, false);

                                                // Expand the AppBar to show the Fab button
                                                mAppBarLayout.setExpanded(true, true);
                                                break;

                                            default:
                                                break;
                                        }
                                    }
                                }
                        )
        );

        // Refresh the current ViewPager properties
        mRxDisposables.add(
                mRxCommandBoxPageCheckRequest
                        .onBackpressureBuffer()
                        .subscribe(
                                new Consumer<Boolean>() {
                                    @Override
                                    public void accept(Boolean aBoolean) {
                                        notifyTabPosition(
                                                mViewPagerCommandBox.getCurrentItem()
                                        );
                                    }
                                }
                        )
        );

        // Incoming YTDL Command Update
        mRxDisposables.add(
                HeadlessFragCommandEditor.rxYtdlCommandUpdate()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                new Consumer<HeadlessFragCommandEditor.RxCommandUpdatePackage>() {
                                    @Override
                                    public void accept(HeadlessFragCommandEditor.RxCommandUpdatePackage rxCommandUpdatePackage) {
                                        // Update the command in the Edit box
                                        mTxtYtdlCommand.setText(
                                                rxCommandUpdatePackage.newCommand
                                        );

                                        // Highlight the argument parameter (if needed)
                                        if (rxCommandUpdatePackage.selectionLength > 0) {
                                            mTxtYtdlCommand.setSelection(
                                                    rxCommandUpdatePackage.selectionStart,
                                                    rxCommandUpdatePackage.selectionStart + rxCommandUpdatePackage.selectionLength
                                            );

                                            // Request focus to show the highlight
                                            mTxtYtdlCommand.requestFocus();

                                            // Pop the keyboard to show the command needs to be edited (https://stackoverflow.com/a/8991563)
                                            InputMethodManager inputMethodManager = (InputMethodManager) mTxtYtdlCommand.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                                            if (inputMethodManager != null) {
                                                inputMethodManager.showSoftInput(mTxtYtdlCommand, InputMethodManager.SHOW_IMPLICIT);
                                            }
                                        }
                                    }
                                }
                        )
        );

        // Trigger a Option List load
        mRxDisposables.add(
                HeadlessFragRawHelpLoader.rxYtdlOptionList()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                new Consumer<List<ModelYtdlOptionSection>>() {
                                    @Override
                                    public void accept(List<ModelYtdlOptionSection> listYtdlOptions) {
                                        if (mAdapterYtdlOptions != null) {
                                            // Initialise the Adapter
                                            mAdapterYtdlOptions.setBaseOptionList(listYtdlOptions);
                                        }
                                    }
                                }
                        )
        );

        // Toggle Option Item expansion
        mRxDisposables.add(
                mRxToggleHeaderItem
                        .onBackpressureBuffer()
                        // Updating ImageView first, so do initial work  on main thread
                        .observeOn(AndroidSchedulers.mainThread())
                        .map(
                                new Function<RxYtdlHeaderPackage, List<ModelYtdlOptionSection>>() {
                                    @Override
                                    public List<ModelYtdlOptionSection> apply(RxYtdlHeaderPackage rxYtdlHeaderPackage) {
                                        List<ModelYtdlOptionSection> baseList = mAdapterYtdlOptions.getBaseList();

                                        // Fetch the Base List out of the adapter and toggle the target Header item
                                        if (rxYtdlHeaderPackage.headerId < baseList.size()) {

                                            ModelYtdlOptionSection targetHeader = baseList.get(rxYtdlHeaderPackage.headerId);
                                            targetHeader.setExpanded(!targetHeader.isExpanded());

                                            // Set the ImageView icon accordingly
                                            if (targetHeader.isExpanded()) {
                                                rxYtdlHeaderPackage.headerIcon.setImageResource(R.drawable.avd_anim_collapse);
                                            } else {
                                                rxYtdlHeaderPackage.headerIcon.setImageResource(R.drawable.avd_anim_expand);
                                            }

                                            Animatable iconHeader = (Animatable) rxYtdlHeaderPackage.headerIcon.getDrawable();
                                            iconHeader.start();
                                        }

                                        return baseList;
                                    }
                                }
                        )
                        // List sorting can be done on background thread
                        .observeOn(Schedulers.computation())
                        .map(
                                new Function<List<ModelYtdlOptionSection>, RxYtdlOptionsUpdatePackage>() {
                                    @Override
                                    public RxYtdlOptionsUpdatePackage apply(List<ModelYtdlOptionSection> newBaseList) {

                                        // Base list in the Adapter is now updated, so should be able to generate the new flattened Show List
                                        List<ModelYtdlBaseItem> newShowList = mAdapterYtdlOptions.generateNewShowList();

                                        // Calculate the List Movements to send back to main thread
                                        return new RxYtdlOptionsUpdatePackage(
                                                newShowList,
                                                DiffUtil.calculateDiff(
                                                        new AdapterYtdlOptions.OptionListDiffUtilCallback(
                                                                mAdapterYtdlOptions.getShowList(),
                                                                newShowList
                                                        ),
                                                        true
                                                )
                                        );
                                    }
                                }
                        )
                        // Switch to main thread to update UI
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                new Consumer<RxYtdlOptionsUpdatePackage>() {
                                    @Override
                                    public void accept(RxYtdlOptionsUpdatePackage rxYtdlOptionsUpdatePackage) {
                                        mAdapterYtdlOptions.updateOptionList(
                                                rxYtdlOptionsUpdatePackage.newShowList,
                                                rxYtdlOptionsUpdatePackage.listMovements
                                        );
                                    }
                                }
                        )
        );

        // Run Option Item filtering
        mRxDisposables.add(
                mRxFilterOptionList
                        // Flood control - only need to take latest filter
                        .onBackpressureDrop()
                        // Run filtering on background thread
                        .observeOn(Schedulers.computation())
                        .map(
                                new Function<String, RxYtdlOptionsUpdatePackage>() {
                                    @Override
                                    public RxYtdlOptionsUpdatePackage apply(String filterString) {

                                        // Make sure the String is not empty, as it doesn't play nice with DiffUtil
                                        if (!TextUtils.isEmpty(filterString)) {

                                            // Generate the new List based on the filter string
                                            List<ModelYtdlBaseItem> newFilterList = mAdapterYtdlOptions.generateFilterShowList(filterString);

                                            // Calculate the List Movements to send back to main thread
                                            return new RxYtdlOptionsUpdatePackage(
                                                    newFilterList,
                                                    DiffUtil.calculateDiff(
                                                            new AdapterYtdlOptions.OptionListDiffUtilCallback(
                                                                    mAdapterYtdlOptions.getShowList(),
                                                                    newFilterList
                                                            ),
                                                            true
                                                    )
                                            );

                                        } else {
                                            // Should never reach here, but construct a null package just in case (since Rx map can no longer return a plain null)
                                            return new RxYtdlOptionsUpdatePackage(
                                                    null,
                                                    null
                                            );
                                        }
                                    }
                                }
                        )
                        // Switch to main thread to update UI
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                new Consumer<RxYtdlOptionsUpdatePackage>() {
                                    @Override
                                    public void accept(RxYtdlOptionsUpdatePackage rxYtdlOptionsUpdatePackage) {
                                        if (rxYtdlOptionsUpdatePackage.listMovements != null) {
                                            mAdapterYtdlOptions.updateOptionList(
                                                    rxYtdlOptionsUpdatePackage.newShowList,
                                                    rxYtdlOptionsUpdatePackage.listMovements
                                            );
                                        }
                                    }
                                }
                        )
        );

        // Update DragShadow label in the Drag Listener
        mRxDisposables.add(
                mRxUpdateDragShadow
                        .onBackpressureBuffer()
                        .subscribe(
                                new Consumer<String>() {
                                    @Override
                                    public void accept(String shadowText) {
                                        if (mListenerYtdlOptionDrag != null) {
                                            mListenerYtdlOptionDrag.setShadowText(shadowText);
                                        }
                                    }
                                }
                        )
        );

        // Toggle the FAB visibility
        mRxDisposables.add(
                mRxToggleFabVisibility
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                new Consumer<Boolean>() {
                                    @Override
                                    public void accept(Boolean isVisible) {
                                        toggleFabVisibility(isVisible);
                                    }
                                }
                        )
        );

        // Update the TextSize in the EditText
        mRxDisposables.add(
                mRxUpdateTextSizeRequest
                        .onBackpressureDrop()
                        .subscribe(
                                new Consumer<Boolean>() {
                                    @Override
                                    public void accept(Boolean aBoolean) {
                                        setCommandBoxTextSize();
                                    }
                                }
                        )
        );

        // Send the new Option and current YTDL command to be processed
        mRxDisposables.add(
                mRxInsertNewOptionRequest
                        .observeOn(Schedulers.computation())
                        .subscribe(
                                new Consumer<String>() {
                                    @Override
                                    public void accept(String newOption) {
                                        HeadlessFragCommandEditor.requestInsertYtdlCommandOption(
                                                mTxtYtdlCommand.getText().toString(),
                                                newOption
                                        );
                                    }
                                }
                        )
        );

        // Incoming Preset List update
        mRxDisposables.add(
                HeadlessFragPresetManager.rxPresetList()
                        .onBackpressureDrop()
                        // Calculate list movements on background thread
                        .observeOn(Schedulers.computation())
                        .map(
                                new Function<List<String>, RxPresetListUpdatePackage>() {
                                    @Override
                                    public RxPresetListUpdatePackage apply(List<String> newPresetList) {
                                        return new RxPresetListUpdatePackage(
                                                newPresetList,
                                                DiffUtil.calculateDiff(
                                                        new AdapterYtdlPresets.PresetListDiffUtilCallback(
                                                                mAdapterYtdlPresets.getPresetList(),
                                                                newPresetList
                                                        )
                                                )
                                        );
                                    }
                                }
                        )
                        // Switch to main thread and update UI
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                new Consumer<RxPresetListUpdatePackage>() {
                                    @Override
                                    public void accept(RxPresetListUpdatePackage rxPresetListUpdatePackage) {
                                        mAdapterYtdlPresets.updatePresetList(
                                                rxPresetListUpdatePackage.newPresetList,
                                                rxPresetListUpdatePackage.listMovements
                                        );

                                        if (mAdapterYtdlPresets.getItemCount() > 0) {
                                            mRecyclerPresetList.smoothScrollToPosition(0);
                                        }
                                    }
                                }
                        )
        );

        // Scroll to the given position in the Preset List
        mRxDisposables.add(
                HeadlessFragPresetManager.rxPresetPosition()
                        .onBackpressureDrop()
                        .subscribe(
                                new Consumer<Integer>() {
                                    @Override
                                    public void accept(Integer position) {
                                        if (position < mAdapterYtdlPresets.getItemCount()) {
                                            mRecyclerPresetList.smoothScrollToPosition(position);
                                        }
                                    }
                                }
                        )
        );

        // Process an incoming Shared Link
        mRxDisposables.add(
                mRxProcessSharedLinkRequest
                        .onBackpressureDrop()
                        .subscribe(
                                new Consumer<String>() {
                                    @Override
                                    public void accept(String sharedLink) {
                                        // Pop the dialog
                                        popPresetListDialog(sharedLink);
                                    }
                                }
                        )
        );
    }


    /**
     * Holder object for handling the YTDL Option Header taps
     */
    private static class RxYtdlHeaderPackage {

        final int headerId;
        final ImageView headerIcon;

        RxYtdlHeaderPackage(int headerId, ImageView headerIcon) {
            this.headerId = headerId;
            this.headerIcon = headerIcon;
        }
    }


    /**
     * Holder object for the YTDL Option List update
     */
    private static class RxYtdlOptionsUpdatePackage {

        final List<ModelYtdlBaseItem> newShowList;
        final DiffUtil.DiffResult listMovements;

        RxYtdlOptionsUpdatePackage(List<ModelYtdlBaseItem> newShowList, DiffUtil.DiffResult listMovements) {
            this.newShowList = newShowList;
            this.listMovements = listMovements;
        }
    }


    /**
     * Holder object for Preset List updates
     */
    private static class RxPresetListUpdatePackage {

        final List<String> newPresetList;
        final DiffUtil.DiffResult listMovements;

        RxPresetListUpdatePackage(List<String> newPresetList, DiffUtil.DiffResult listMovements) {
            this.newPresetList = newPresetList;
            this.listMovements = listMovements;
        }
    }

    // endregion


    // region ================== TEXTSIZE OPS ==================
    // ====== ================== ============ ==================


    /**
     * Read the latest text size from SharedPrefs and update the text size of the EditText box
     * containing the YTDL command.
     */
    private void setCommandBoxTextSize() {
        if (getContext() != null) {
            // Fetch the base list of text sizes
            String[] textSizes = getResources().getStringArray(R.array.pref_text_size_list);

            // Fetch the current text size preference
            String selectedTextSize = PreferenceManager.getDefaultSharedPreferences(getContext()).getString(
                    getString(R.string.prefkey_text_size),
                    ""
            );

            // See where it falls within the String array
            int textSizeIndex = Arrays.asList(textSizes).indexOf(selectedTextSize);

            // If a valid index was found, set the text size accordingly
            switch (textSizeIndex) {
                case 0: // Sub Atomic
                    mTxtYtdlCommand.setTextSize(TypedValue.COMPLEX_UNIT_SP, 2);
                    break;
                case 1: // Extra Small
                    mTxtYtdlCommand.setTextSize(TypedValue.COMPLEX_UNIT_SP, 8);
                    break;
                case 2: // Small
                    mTxtYtdlCommand.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                    break;
                case 3: // Medium
                    mTxtYtdlCommand.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
                    break;
                case 4: // Large
                    mTxtYtdlCommand.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
                    break;
                case 5: // Extra Large
                    mTxtYtdlCommand.setTextSize(TypedValue.COMPLEX_UNIT_SP, 32);
                    break;
                case 6: // WTF
                    mTxtYtdlCommand.setTextSize(TypedValue.COMPLEX_UNIT_SP, 72);
                    break;
                default:
                    break;
            }
        }
    }

    // endregion


    // region ================== VIEWPAGER OPS ==================
    // ====== ================== ============= ==================


    private ViewPager.SimpleOnPageChangeListener mViewPagerChangeListener = new ViewPager.SimpleOnPageChangeListener() {
        @Override
        public void onPageSelected(int position) {

            // Update the actual ViewPager panel properties
            updateFabProperties(position);

            // Notify the TabLayout to update the selected Tab
            notifyTabPosition(position);
        }
    };


    /**
     * Check the current ViewPager and SearchView positions and send a ping out to update the TabLayout
     * accordingly
     */
    private void notifyTabPosition(int viewPagerPosition) {

        switch (viewPagerPosition) {
            case 0:
                // Detect whether we're showing SearchView or not
                Boolean showSearch = (Boolean) mViewPagerCommandBox.getTag(R.id.tag_id_searchview_visible);

                if (showSearch != null && showSearch) {
                    // Set the tab to the Search option
                    toggleSearchViewVisibility(true);
                    ActivityMain.requestSetSelectedTab(R.id.tag_id_tab_search);

                } else {
                    // Set the tab to the Edit option
                    toggleSearchViewVisibility(false);
                    ActivityMain.requestSetSelectedTab(R.id.tag_id_tab_command_edit);

                }
                break;

            case 1:
                toggleSearchViewVisibility(false);
                ActivityMain.requestSetSelectedTab(R.id.tag_id_tab_command_preset);
                break;

            default:
                break;
        }
    }


    /**
     * Update the FAB icon and action accordingly based on the given ViewPager position
     * Also send an update the main Activity to update the TabLayout as well
     */
    private void updateFabProperties(int position) {

        switch (position) {
            case 0: // Search / Build Command
                mFabAction.setTag(R.id.tag_id_fab_action_copy, true);
                mFabAction.setImageResource(R.drawable.avd_anim_save_to_copy);
                ((Animatable) mFabAction.getDrawable()).start();

                break;

            case 1: // Save Command
                mFabAction.setTag(R.id.tag_id_fab_action_copy, false);
                mFabAction.setImageResource(R.drawable.avd_anim_copy_to_save);
                ((Animatable) mFabAction.getDrawable()).start();
                break;

            default:
                break;
        }
    }


    /**
     * Simple PagerAdapter to handle the views inside the non-Fragment ViewPager
     * Reference: https://stackoverflow.com/a/18710611
     */
    private class PagerAdapterCommandToolbox extends PagerAdapter {

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {

            switch (position) {
                case 0:
                    return container.findViewById(R.id.recycler_command_ytdl_options);

                case 1:
                    return container.findViewById(R.id.recycler_saved_presets);

                default:
                    return super.instantiateItem(container, position);
            }
        }


        @Override
        public int getCount() {
            return 2;
        }


        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }
    }

    // endregion


    // region ================== FAB BUTTON OPS ==================
    // ====== ================== ============== ==================


    /**
     * Since Fab.hide() / Fab.show() doesn't seem to work properly, need to do our own fancy animation
     * to toggle the Fab visibility.
     */
    private void toggleFabVisibility(boolean isVisible) {

        float moveDistance = (isVisible)
                ? 0
                : mFabAction.getWidth() * 2;

        mFabAction
                .animate()
                .setDuration(mAnimateFabDuration)
                .setInterpolator(mAnimateFabInterpolator)
                .translationX(moveDistance);
    }


    /**
     * Perform the relevant click action depending on the currently selected Tab / ViewPage
     */
    private View.OnClickListener mFabClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

            Boolean fabActionIsCopy = (Boolean) view.getTag(R.id.tag_id_fab_action_copy);

            if (fabActionIsCopy != null) {

                if (fabActionIsCopy) {
                    // Send the command to the clipboard
                    HeadlessFragCommandCopySave.requestCopyCommand(
                            mFabAction,
                            mTxtYtdlCommand.getText().toString()
                    );

                } else {
                    // Send the current text in the EditText to the Preset List
                    HeadlessFragCommandCopySave.requestSaveCommand(
                            mFabAction,
                            mTxtYtdlCommand.getText().toString()
                    );
                }
            }
        }
    };

    // endregion


    // region ================== SEARCHVIEW OPS ==================
    // ====== ================== ============== ==================


    /**
     * Animate the SearchView in/out depending on whether we're switched in or not
     */
    private void toggleSearchViewVisibility(boolean isVisible) {

        Boolean currentlyVisible = (Boolean) mViewPagerCommandBox.getTag(R.id.tag_id_searchview_visible);

        // Only make changes if the visibility is different
        if (currentlyVisible == null || currentlyVisible != isVisible) {

            // Store the visibility in the Tag so it can be used in the PageChangeListener
            mViewPagerCommandBox.setTag(R.id.tag_id_searchview_visible, isVisible);

            float moveDistance = (isVisible)
                    ? 0
                    : mSearchViewOptions.getHeight() * 2;

            // Animate the SearchView accordingly
            mSearchViewOptions
                    .animate()
                    .setDuration(mAnimateFabDuration)
                    .translationY(moveDistance);

            // Reset the Option List if hiding
            if (!isVisible && !TextUtils.isEmpty(mSearchViewOptions.getQuery())) {
                mSearchViewOptions.setQuery("", false);
                mSearchViewOptions.clearFocus();
            }
        }
    }


    /**
     * Quick QueryTextListener to handle YTDL Option filtering from the SearchView
     */
    SearchView.OnQueryTextListener mSearchViewQueryTextListener = new SearchView.OnQueryTextListener() {

        @Override
        public boolean onQueryTextSubmit(String filterText) {
            filterOptions(filterText);
            return false;
        }

        @Override
        public boolean onQueryTextChange(String filterText) {
            filterOptions(filterText);
            return true;
        }


        private void filterOptions(String filterText) {
            if (!TextUtils.isEmpty(filterText)) {
                mRxFilterOptionList.onNext(filterText);
            } else {
                HeadlessFragRawHelpLoader.requestYtdlOptionList();
            }
        }
    };

    // endregion


    // region ================== SHARED LINK OPS ==================
    // ====== ================== =============== ==================


    /**
     * Pop the dialog for selecting from a list of installed apps
     */
    private void popPresetListDialog(String sharedLink) {

        // Check if the DialogFragment already exists, make sure to remove it first
        DialogFragment presetListDialogue = (DialogFragment) getChildFragmentManager().findFragmentByTag(DialogPresetPicker.TAG);
        if (presetListDialogue != null) {
            getChildFragmentManager()
                    .beginTransaction()
                    .remove(presetListDialogue)
                    .commitNow();
        }

        // Pop the new Preset Command List dialogue
        presetListDialogue = DialogPresetPicker.newInstance(sharedLink);
        presetListDialogue.show(
                getChildFragmentManager(),
                DialogPresetPicker.TAG
        );
    }

    // endregion
}
