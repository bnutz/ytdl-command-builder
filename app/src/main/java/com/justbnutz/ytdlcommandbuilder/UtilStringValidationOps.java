/*
 * Created by Brian Lau on 2018-06-06
 * Copyright (c) 2018. All rights reserved.
 *
 * Last modified: 2018-06-06
 */

package com.justbnutz.ytdlcommandbuilder;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UtilStringValidationOps {


    /**
     * Private constructor prevents the default parameter-less constructor from being used elsewhere in your code.
     * Additionally, making the class final prevents it from being extended in subclasses, which is a best practice for utility classes.
     * Though since we're declaring a private constructor, other classes wouldn't be able to extend it anyway,
     * but it is still a best practice to mark the class as final.
     * - http://stackoverflow.com/questions/14398747/hide-utility-class-constructor-utility-classes-should-not-have-a-public-or-def
     */
    private UtilStringValidationOps() {}


    /**
     * Take a given command and see if a URL exists at the end of it
     *
     * @return Index position from start of URL (if found), if no URL found, then just return -1
     */
    static int checkUrlStringExists(String checkCommand) {

        Pattern patternEndUrl = Pattern.compile(
                ".+?\\s(['\"]?https?://.+)$"
        );

        Matcher matchEndUrl = patternEndUrl.matcher(checkCommand);

        if (matchEndUrl.find() && matchEndUrl.groupCount() > 0) {
            // If URL(s) found, return index position of the (first) URL
            return checkCommand.length() - matchEndUrl.group(1).length();

        } else {
            // If no URL found, return no position
            return -1;
        }
    }


    /**
     * Take a given Option String and see it ends in a bunch of capital letters, indicating that
     * it's an option that requires parameter arguments
     *
     * @return An int[] array where:
     *         int[0] is the start index of the argument
     *         int[1] is the length of the argument
     */
    static int[] checkParameterArgs(String checkOption) {

        // Check if the end of the String ends in spaces, capital letters or numbers
        Pattern patternArgExists = Pattern.compile(
                "^-.+?\\s([\\sA-Z0-9]+)$"
        );

        Matcher matchArgExists = patternArgExists.matcher(checkOption);

        if (matchArgExists.find() && matchArgExists.groupCount() > 0) {
            // If argument string found, return its index and length
            return new int[] {
                    checkOption.length() - matchArgExists.group(1).length(),
                    matchArgExists.group(1).length()
            };

        } else {
            // If no URL found, return an empty array
            return new int[] {};
        }
    }

}
