/*
 * Created by Brian Lau on 2018-05-02
 * Copyright (c) 2018. All rights reserved.
 *
 * Last modified: 2018-05-02
 */

package com.justbnutz.ytdlcommandbuilder;

import android.content.ClipData;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.DragEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;


/**
 * Custom DragListener that tracks the current Drag position to make sure the corresponding custom DragShadow
 * matches its y-position.
 *
 * References:
 * - https://stackoverflow.com/a/27824866
 * - https://stackoverflow.com/a/22978205
 */
public class ListenerYtdlOptionDrag implements View.OnDragListener {

    private final FrameLayout mDragShadowContainer;
    private final TextView mDragShadowView;

    private final ViewGroup.MarginLayoutParams mMarginParams;


    ListenerYtdlOptionDrag(@NonNull FrameLayout dragShadow) {

        mDragShadowContainer = dragShadow;
        mDragShadowView = dragShadow.findViewById(R.id.lbl_dragshadow);

        mMarginParams = (ViewGroup.MarginLayoutParams) mDragShadowContainer.getLayoutParams();
    }


    /**
     * Update the Text label in the DragShadow
     */
    void setShadowText(String shadowText) {
        if (mDragShadowView != null && !TextUtils.isEmpty(shadowText)) {
            mDragShadowView.setText(shadowText);
        }
    }


    /**
     * Set the Y-position of the custom DragShadow and update the shadow status icons
     *
     * @param dragZone Containing View that the user is currently dragging within.
     * @param currentDragY Current y-position relative to the containing drag zone
     */
    private void setShadowPosition(View dragZone, int currentDragY) {

        // Drag Shadow draws from top edge by default, bring it up a so it looks directly below the finger
        currentDragY -= Math.round(mDragShadowView.getHeight() * 0.75);

        switch (dragZone.getId()) {
            case R.id.txt_ytdl_command:
                // Set the custom DragShadow y-position
                mMarginParams.topMargin = currentDragY - dragZone.getHeight();

                // Set the add command Drop Hint icons
                mDragShadowView.setCompoundDrawablesWithIntrinsicBounds(
                        dragZone.getContext().getResources().getDrawable(R.drawable.ic_add_box_white),
                        null,
                        dragZone.getContext().getResources().getDrawable(R.drawable.ic_add_box_white),
                        null
                );

                break;

            case R.id.recycler_command_ytdl_options:
                // FrameLayout matches RecyclerView bounds, so can use direct drag position
                mMarginParams.topMargin = currentDragY;

            default:
                // Set the default Drop Hint icons
                mDragShadowView.setCompoundDrawablesWithIntrinsicBounds(
                        dragZone.getContext().getResources().getDrawable(R.drawable.ic_up_white),
                        null,
                        dragZone.getContext().getResources().getDrawable(R.drawable.ic_up_white),
                        null
                );
                break;
        }

        // Move the custom DragShadow accordingly
        mDragShadowContainer.setLayoutParams(mMarginParams);
    }


    @Override
    public boolean onDrag(View view, DragEvent dragEvent) {

        switch (dragEvent.getAction()) {

            case DragEvent.ACTION_DRAG_STARTED:
            case DragEvent.ACTION_DRAG_ENTERED:
                // Hide FAB button when dragging
                FragmentCommandBuilder.requestToggleFabVisibility(false);
                mDragShadowContainer.setVisibility(View.VISIBLE);
                setShadowPosition(view, (int) dragEvent.getY());
                break;

            case DragEvent.ACTION_DRAG_LOCATION:
                setShadowPosition(view, (int) dragEvent.getY());
                break;

            case DragEvent.ACTION_DROP:
                // Show FAB button when drag done
                FragmentCommandBuilder.requestToggleFabVisibility(true);
                mDragShadowContainer.setVisibility(View.GONE);

                // Only allow drop action within the command drop zone
                if (view.getId() == R.id.txt_ytdl_command) {

                    // Retrieve the clipData from the dragged object
                    ClipData.Item item = dragEvent.getClipData().getItemAt(0);

                    // Send it to the parent Fragment to be processed into the current YTDL command
                    FragmentCommandBuilder.requestInsertNewOption(
                            String.valueOf(item.getText())
                    );
                }
                break;

            case DragEvent.ACTION_DRAG_EXITED:
                // Tidy up when exiting current drag zone
                FragmentCommandBuilder.requestToggleFabVisibility(true);
                mDragShadowContainer.setVisibility(View.GONE);

                break;

            default:
                break;
        }

        return true;
    }
}
