/*
 * Created by Brian Lau on 2018-04-23
 * Copyright (c) 2018. All rights reserved.
 *
 * Last modified: 2018-04-23
 */

package com.justbnutz.ytdlcommandbuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Model class to hold the Option Header title element
 *
 * Reference: https://medium.com/@ruut_j/a-recyclerview-with-multiple-item-types-bce7fbd1d30e
 */
public class ModelYtdlOptionSection implements ModelYtdlBaseItem {

    private final int superId;
    private final int sectionId;
    private final String lblOptionHeader;
    private final List<ModelYtdlOptionItem> ytdlOptionItems;
    private boolean itemExpanded;

    ModelYtdlOptionSection(int itemId, int parentId, String optionHeaderLabel) {
        superId = itemId;
        sectionId = parentId;
        itemExpanded = false;
        lblOptionHeader = optionHeaderLabel;

        ytdlOptionItems = new ArrayList<>();
    }


    public String getOptionHeader() {
        return lblOptionHeader;
    }


    @Override
    public int getViewType() {
        return ModelYtdlBaseItem.OPTION_HEADER;
    }


    @Override
    public int getSuperId() {
        return superId;
    }


    @Override
    public int getSectionId() {
        return sectionId;
    }


    public void setExpanded(boolean isExpanded) {
        itemExpanded = isExpanded;
    }


    public boolean isExpanded() {
        return itemExpanded;
    }


    public List<ModelYtdlOptionItem> getYtdlOptionItems() {
        return ytdlOptionItems;
    }
}
