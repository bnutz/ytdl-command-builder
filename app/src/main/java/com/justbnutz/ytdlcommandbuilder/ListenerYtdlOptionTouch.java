/*
 * Created by Brian Lau on 2018-05-02
 * Copyright (c) 2018. All rights reserved.
 *
 * Last modified: 2018-05-02
 */

package com.justbnutz.ytdlcommandbuilder;

import android.content.ClipData;
import android.os.Build;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;

public class ListenerYtdlOptionTouch implements View.OnTouchListener {

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {

        if (view.getTag(R.id.tag_id_cmd_switch) != null) {

            // Parse the MotionEvent
            switch (motionEvent.getAction()) {

                case MotionEvent.ACTION_DOWN:

                    // Extract the switch option that will be used
                    String cmdSwitch = String.valueOf(view.getTag(R.id.tag_id_cmd_switch));

                    if (!TextUtils.isEmpty(cmdSwitch)) {
                        // Pack up the commmand switch that will be inserted
                        ClipData clipData = ClipData.newPlainText(
                                view.getContext().getString(R.string.ytdl_command_hint),
                                cmdSwitch
                        );

                        // Create an empty drag shadow (since we'll be using a custom one)
                        View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder();

                        // Update the custom DragShadow text
                        FragmentCommandBuilder.requestDragShadowTextUpdate(cmdSwitch);

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

                            view.startDragAndDrop(
                                    clipData,
                                    shadowBuilder,
                                    view,
                                    0
                            );

                        } else {
                            view.startDrag(
                                    clipData,
                                    shadowBuilder,
                                    view,
                                    0
                            );
                        }
                    }
                    return true;

                case MotionEvent.ACTION_UP:
                    // Suppress "OnTouchListener should also do performClick() for accessibility" warning
                    view.performClick();
                    return true;

                default:
                    return false;
            }

        } else {
            return false;

        }
    }
}
