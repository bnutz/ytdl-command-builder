/*
 * Created by Brian Lau on 2018-04-23
 * Copyright (c) 2018. All rights reserved.
 *
 * Last modified: 2018-04-23
 */

package com.justbnutz.ytdlcommandbuilder;

/**
 * An interface to hold and return a ViewType identifier.
 * Also serves as a base class that can be used in a RecyclerView Adapter
 *
 * Reference: https://medium.com/@ruut_j/a-recyclerview-with-multiple-item-types-bce7fbd1d30e
 */
public interface ModelYtdlBaseItem {

    int OPTION_HEADER = 0;
    int OPTION_ITEM = 1;

    // Type of View Item this is
    int getViewType();

    // Unique Id of the item in the overall list
    int getSuperId();

    // Id of either the Header itself, or which Header it falls under
    int getSectionId();
}
