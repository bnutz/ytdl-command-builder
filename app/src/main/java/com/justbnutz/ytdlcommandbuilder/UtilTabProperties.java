/*
 * Created by Brian Lau on 2018-05-18
 * Copyright (c) 2018. All rights reserved.
 *
 * Last modified: 2018-05-18
 */


/**
 * Utility class to hold the properties and order of the app Tabs, separated out so it can be loaded
 * from ActivityMain for the TabLayout, and from FragmentPreferences for the ListPreference
 */
package com.justbnutz.ytdlcommandbuilder;

public final class UtilTabProperties {


    // NOTE: Each array item needs to correspond with the other properties so all the icons, labels and tags line up.
    private static final int[] ICON_IDS = {
            R.drawable.ic_help_outline_black,
            R.drawable.ic_search_black,
            R.drawable.ic_keyboard_black,
            R.drawable.ic_bookmark_border_black,
            R.drawable.ic_settings_black
    };

    private static final int[] LABEL_IDS = {
            R.string.tab_description_howto,
            R.string.tab_description_search,
            R.string.tab_description_command_edit,
            R.string.tab_description_command_preset,
            R.string.tab_description_settings
    };

    private static final int[] TAG_IDS = {
            R.id.tag_id_tab_howto,
            R.id.tag_id_tab_search,
            R.id.tag_id_tab_command_edit,
            R.id.tag_id_tab_command_preset,
            R.id.tag_id_tab_settings
    };


    /**
     * Private constructor prevents the default parameter-less constructor from being used elsewhere in your code.
     * Additionally, making the class final prevents it from being extended in subclasses, which is a best practice for utility classes.
     * Though since we're declaring a private constructor, other classes wouldn't be able to extend it anyway,
     * but it is still a best practice to mark the class as final.
     * - http://stackoverflow.com/questions/14398747/hide-utility-class-constructor-utility-classes-should-not-have-a-public-or-def
     */
    private UtilTabProperties() {}


    static int[] getTabIconIds() {
        return ICON_IDS;
    }


    static int[] getTabLabelIds() {
        return LABEL_IDS;
    }


    static int[] getTabTags() {
        return TAG_IDS;
    }


    /**
     * Default Start Tab as String value for use in SharedPrefs and to be compatible with
     * ListPreferences String requirements
     */
    static String getDefaultStartTabId() {
        return String.valueOf(TAG_IDS[0]);
    }
}
