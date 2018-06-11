/*
 * Created by Brian Lau on 2018-04-18
 * Copyright (c) 2018. All rights reserved.
 *
 * Last modified: 2018-04-18
 */

package com.justbnutz.ytdlcommandbuilder;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class AdapterYtdlOptions extends RecyclerView.Adapter<AdapterYtdlOptions.ViewHolderYtdlBase> {


    private final LayoutInflater mInflater;

    // The main source list containing only the Headers at the parent level, and the nested YTDL Option items under each header - this will not change
    private List<ModelYtdlOptionSection> mYtdlOptionBaseList;

    // The list that will be used as the item display - Effectively a semi-flattened version of mYtdlOptionBaseList (Option items copied to parent level to show in RecyclerView)
    private List<ModelYtdlBaseItem> mYtdlOptionShowList;



    AdapterYtdlOptions(Context context) {
        mInflater = LayoutInflater.from(context);
    }


    /**
     * Populate the adapter with the base list of full YTDL options
     */
    void setBaseOptionList(@NonNull List<ModelYtdlOptionSection> ytdlBaseOptionList) {

        mYtdlOptionBaseList = ytdlBaseOptionList;

        // Everything is collapsed when first populated, so can just update in main thread.
        notifyItemRangeRemoved( 0, getItemCount());
        mYtdlOptionShowList = generateNewShowList();
        notifyItemRangeInserted(0, getItemCount());
    }


    // region ================== DEFAULT ADAPTER OVERRIDES ==================
    // ====== ================== ========================= ==================


    @Override
    public int getItemViewType(int position) {
        return mYtdlOptionShowList.get(position).getViewType();
    }


    @NonNull
    @Override
    public ViewHolderYtdlBase onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        // Return the corresponding ViewHolder depending on what type of row item is in the list
        switch (viewType) {
            case ModelYtdlBaseItem.OPTION_HEADER:
                return new ViewHolderYtdlYtdlOptionHeader(
                        mInflater.inflate(R.layout.itemrow_ytdl_option_header, parent, false)
                );

            case ModelYtdlBaseItem.OPTION_ITEM:
                return new ViewHolderYtdlOptionItem(
                        mInflater.inflate(R.layout.itemrow_ytdl_option_item, parent, false)
                );

            default:
                // (Blank ViewHolder since we can't just return null anymore, technically this will never be called)
                return new ViewHolderNull(
                        mInflater.inflate(R.layout.itemrow_ytdl_option_header, parent, false)
                );
        }
    }


    @Override
    public void onBindViewHolder(@NonNull ViewHolderYtdlBase viewHolder, int position) {
        // Load the item and run the corresponding bind action for that Item Type
        viewHolder.bindAction(
                mYtdlOptionShowList.get(position)
        );
    }


    @Override
    public int getItemCount() {
        return (mYtdlOptionShowList != null)
                ? mYtdlOptionShowList.size()
                : 0;
    }


    @Override
    public long getItemId(int position) {
        return (mYtdlOptionShowList != null)
                ? mYtdlOptionShowList.get(position).getSuperId()
                : -1;
    }


    // endregion


    // region ================== LIST TOGGLE OPS ==================
    // ====== ================== =============== ==================


    /**
     * Run through the Base Header List and:
     * 1. Add all Header items to Show List
     * 2. If a Header is expanded, add its sub-items to the Show List as well
     */
    List<ModelYtdlBaseItem> generateNewShowList() {

        // Initialise the new Show List
        List<ModelYtdlBaseItem> newShowList = new ArrayList<>();

        // Loop the Base List of Headers
        for (ModelYtdlOptionSection headerCheckItem : mYtdlOptionBaseList) {

            // Add the Headers first
            newShowList.add(headerCheckItem);

            // If the Header is expanded, add its sub-items too
            if (headerCheckItem.isExpanded()) {
                newShowList.addAll(headerCheckItem.getYtdlOptionItems());
            }
        }

        return newShowList;
    }


    /**
     * Run through the Base Header List and:
     * 1. Check all Option items to see if the Option text matches the given Filter String
     * 2. If an item matches, add it to the Filtered Show List
     * (Headers not used in this view)
     */
    List<ModelYtdlBaseItem> generateFilterShowList(String filterString) {

        // Normalise the case
        filterString = filterString.toLowerCase();

        // Initialise the new Show List
        List<ModelYtdlBaseItem> newFilterList = new ArrayList<>();

        // Loop the Base List of Headers
        for (ModelYtdlOptionSection headerCheckItem : mYtdlOptionBaseList) {

            // Loop through the actual Option Item under each header
            for (ModelYtdlOptionItem optionItem : headerCheckItem.getYtdlOptionItems()) {

                // If the Option text contains the filterString, add it to the thing
                if (optionItem.getFullDescription().toLowerCase().contains(filterString)) {
                    newFilterList.add(optionItem);
                }
            }
        }

        return newFilterList;
    }


    List<ModelYtdlBaseItem> getShowList() {
        return mYtdlOptionShowList;
    }


    /**
     * Refresh the Show List
     */
    void updateOptionList(List<ModelYtdlBaseItem> newShowList, DiffUtil.DiffResult listMovements) {
        mYtdlOptionShowList.clear();
        mYtdlOptionShowList.addAll(newShowList);
        listMovements.dispatchUpdatesTo(this);
    }


    /**
     * Using DiffUtil in place of notifyDatasetChanged()
     *
     * Reference:
     * - https://medium.com/@iammert/using-diffutil-in-android-recyclerview-bdca8e4fbb00
     */
    static class OptionListDiffUtilCallback extends DiffUtil.Callback {

        List<ModelYtdlBaseItem> mOldList;
        List<ModelYtdlBaseItem> mNewList;

        OptionListDiffUtilCallback(@NonNull List<ModelYtdlBaseItem> oldList, @NonNull List<ModelYtdlBaseItem> newList) {
            mOldList = oldList;
            mNewList = newList;
        }

        @Override
        public int getOldListSize() {
            return (mOldList != null) ? mOldList.size() : 0;
        }

        @Override
        public int getNewListSize() {
            return (mNewList != null) ? mNewList.size() : 0;
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return (mOldList.get(oldItemPosition).getSuperId() == mNewList.get(newItemPosition).getSuperId());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return (mOldList.get(oldItemPosition).getSuperId() == mNewList.get(newItemPosition).getSuperId());
        }
    }


    List<ModelYtdlOptionSection> getBaseList() {
        return mYtdlOptionBaseList;
    }

    // endregion


    // region ================== VIEWHOLDERS TYPES ==================
    // ====== ================== ================= ==================

    
    /**
     * Base parent ViewHolder template, onBindViewHolder() actions will be run via the
     * abstract (and therefore overridden) bindAction() method.
     */
    static abstract class ViewHolderYtdlBase extends RecyclerView.ViewHolder {

        ViewHolderYtdlBase(View itemView) {
            super(itemView);
        }

        public abstract void bindAction(ModelYtdlBaseItem listItem);
    }
    

    /**
     * ViewHolder for the Options Header Row (will be clicked to expand child items)
     */
    static class ViewHolderYtdlYtdlOptionHeader extends ViewHolderYtdlBase implements View.OnClickListener {

        final TextView lblYtdlOptionHeader;
        final ImageView imgYtdlExpansionIndicator;
        int headerId;

        ViewHolderYtdlYtdlOptionHeader(View itemView) {
            super(itemView);

            lblYtdlOptionHeader = itemView.findViewById(R.id.txt_ytdl_option_header);
            imgYtdlExpansionIndicator = itemView.findViewById(R.id.img_expansion_indicator);

            itemView.setOnClickListener(this);
        }


        @Override
        public void bindAction(ModelYtdlBaseItem listItem) {
            // Set the Header ID
            headerId = listItem.getSectionId();

            // Set the toggle icon direction
            if (((ModelYtdlOptionSection) listItem).isExpanded()) {
                imgYtdlExpansionIndicator.setImageResource(R.drawable.avd_anim_expand);
            } {
                imgYtdlExpansionIndicator.setImageResource(R.drawable.avd_anim_collapse);
            }

            // Map the Header Model to the item and set the Header Title text
            lblYtdlOptionHeader.setText(
                    ((ModelYtdlOptionSection) listItem).getOptionHeader()
            );
        }


        @Override
        public void onClick(View view) {
            // Notify the Adapter to update its expanded lists
            FragmentCommandBuilder.requestToggleHeaderItem(headerId, imgYtdlExpansionIndicator);
        }
    }


    /**
     * ViewHolder for the actual Option Items
     */
    static class ViewHolderYtdlOptionItem extends ViewHolderYtdlBase {

        final TextView lblYtdlOptionFlag;
        final TextView lblYtdlOptionDescription;
        final ImageView imgYtdlOptionDragHandle;

        final View bckYtdlOptionFlag;

        final ListenerYtdlOptionTouch mTouchListener;

        @SuppressLint("ClickableViewAccessibility")
        ViewHolderYtdlOptionItem(View itemView) {
            super(itemView);

            lblYtdlOptionFlag = itemView.findViewById(R.id.txt_ytdl_option_flag);
            lblYtdlOptionDescription = itemView.findViewById(R.id.txt_ytdl_option_description);
            imgYtdlOptionDragHandle = itemView.findViewById(R.id.img_option_drag_handle);
            bckYtdlOptionFlag = itemView.findViewById(R.id.back_ytdl_option_flag);

            mTouchListener = new ListenerYtdlOptionTouch();

            lblYtdlOptionFlag.setOnTouchListener(mTouchListener);
            imgYtdlOptionDragHandle.setOnTouchListener(mTouchListener);
            bckYtdlOptionFlag.setOnTouchListener(mTouchListener);
        }


        @Override
        public void bindAction(ModelYtdlBaseItem listItem) {

            // Map the Item Model to the item and set the TextView properties
            lblYtdlOptionFlag.setText(
                    ((ModelYtdlOptionItem) listItem).getFlagLabel().replace(" ", "\n")
            );

            lblYtdlOptionDescription.setText(
                    ((ModelYtdlOptionItem) listItem).getDescription()
            );

            // Place the command syntax in the draggable Views
            String cmdSwitch = ((ModelYtdlOptionItem) listItem).getCmdSwitch();
            lblYtdlOptionFlag.setTag(R.id.tag_id_cmd_switch, cmdSwitch);
            imgYtdlOptionDragHandle.setTag(R.id.tag_id_cmd_switch, cmdSwitch);
            bckYtdlOptionFlag.setTag(R.id.tag_id_cmd_switch, cmdSwitch);

        }
    }


    /**
     * Blank ViewHolder since onCreateViewHolder() no longer returns nulls, Adapter will not show
     * anything here.
     */
    static class ViewHolderNull extends ViewHolderYtdlBase {

        ViewHolderNull(View itemView) {
            super(itemView);
        }

        @Override
        public void bindAction(ModelYtdlBaseItem listItem) {}
    }

    // endregion

}
