/*
 * Created by Brian Lau on 2018-05-17
 * Copyright (c) 2018. All rights reserved.
 *
 * Last modified: 2018-05-17
 */

package com.justbnutz.ytdlcommandbuilder;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


/**
 * Fragment for holding the instructions and commands on how to set up YTDL in Termux
 */
public class FragmentHowTo extends Fragment implements View.OnClickListener {

    public static final String TAG = ActivityMain.PACKAGE_NAME + ".FragmentHowTo";


    public FragmentHowTo() {}


    // region ================== STATIC METHODS ==================
    // ====== ================== ============== ==================


    /**
     * Use this factory method to create a new instance of this fragment using any provided parameters.
     * - newInstance vs Constructors: http://stackoverflow.com/a/14655001
     */
    public static FragmentHowTo newInstance() {
        return new FragmentHowTo();
    }

    // endregion


    // region ================== PRIMARY FLOW ==================
    // ====== ================== ============ ==================


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_how_to, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Set up the click listeners for the command boxes
        view.findViewById(R.id.lbl_howto_step1_command).setOnClickListener(this);
        view.findViewById(R.id.lbl_howto_step2_command).setOnClickListener(this);
        view.findViewById(R.id.lbl_howto_step3_command).setOnClickListener(this);
        view.findViewById(R.id.lbl_howto_step4_command).setOnClickListener(this);
        view.findViewById(R.id.lbl_howto_step5_command).setOnClickListener(this);
        view.findViewById(R.id.lbl_howto_step6_edit_intro).setOnClickListener(this);
        view.findViewById(R.id.lbl_howto_step6_preset_intro).setOnClickListener(this);

        // Link and make sure the href links in the Tips box are clickable (https://stackoverflow.com/a/2746708)
        TextView txtTipsBox = view.findViewById(R.id.lbl_howto_extra_tips);
        txtTipsBox.setMovementMethod(LinkMovementMethod.getInstance());

    }


    @Override
    public void onClick(View view) {

        switch (view.getId()) {
            case R.id.lbl_howto_step1_command:
                // Get the text out of the TextBox
                String packageName = getString(R.string.prefkey_cmd_app_default);

                if (!TextUtils.isEmpty(packageName)) {
                    // Send the App ID to be opened
                    HeadlessFragCommandCopySave.requestOpenAppId(
                            view,
                            packageName
                    );
                }
                break;

            case R.id.lbl_howto_step2_command:
            case R.id.lbl_howto_step3_command:
            case R.id.lbl_howto_step4_command:
            case R.id.lbl_howto_step5_command:
                // Get the text out of the TextBox
                String commandText = ((TextView) view).getText().toString().trim();

                if (!TextUtils.isEmpty(commandText)) {
                    // Send clicked command to clipboard
                    HeadlessFragCommandCopySave.requestCopyCommand(
                            view,
                            commandText
                    );
                }
                break;

            case R.id.lbl_howto_step6_edit_intro:
                // Click the Command Builder tab
                ActivityMain.requestClickTab(R.id.tag_id_tab_command_edit);

                // Send a Snackbar hint on how to use
                HeadlessFragSnackbar.requestPopSnackBar(
                        view,
                        getString(R.string.snackbar_edit_hint),
                        Snackbar.LENGTH_SHORT
                );
                break;

            case R.id.lbl_howto_step6_preset_intro:
                // Click the Command Preset tab
                ActivityMain.requestClickTab(R.id.tag_id_tab_command_preset);
                break;

            default:
                break;
        }
    }

    // endregion

}
