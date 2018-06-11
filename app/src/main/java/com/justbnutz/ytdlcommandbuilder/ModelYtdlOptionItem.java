/*
 * Created by Brian Lau on 2018-04-18
 * Copyright (c) 2018. All rights reserved.
 *
 * Last modified: 2018-04-18
 */

package com.justbnutz.ytdlcommandbuilder;

/**
 * Model class to hold the Option Item properties
 *
 * Reference: https://medium.com/@ruut_j/a-recyclerview-with-multiple-item-types-bce7fbd1d30e
 */
public class ModelYtdlOptionItem implements ModelYtdlBaseItem {

    private final int superId;
    private final int parentId;

    // The extracted switch option to be inserted into the command (without dupes)
    private final String txtCmdSwitch;

    // The switch as listed in the help file
    private final String lblOptionFlag;

    // The switch description as listed in the help file
    private final StringBuilder lblOptionDescription;


    ModelYtdlOptionItem(int itemId, int headerId, String cmdSwitch, String optionFlag, String optionDescription) {
        superId = itemId;
        parentId = headerId;
        txtCmdSwitch = cmdSwitch;
        lblOptionFlag = optionFlag;
        lblOptionDescription = new StringBuilder(optionDescription);
    }


    public String getCmdSwitch() {
        return txtCmdSwitch;
    }


    public String getFlagLabel() {
        return lblOptionFlag;
    }


    public void appendDescription(String extraDescription) {
        lblOptionDescription
                .append(" ")
                .append(extraDescription);
    }


    public String getDescription() {
        return lblOptionDescription.toString();
    }


    public String getFullDescription() {
        return lblOptionFlag + " " + lblOptionDescription.toString();
    }


    @Override
    public int getViewType() {
        return ModelYtdlBaseItem.OPTION_ITEM;
    }


    @Override
    public int getSuperId() {
        return superId;
    }


    @Override
    public int getSectionId() {
        return parentId;
    }

}
