/*
 * Created by Brian Lau on 2018-04-13
 * Copyright (c) 2018. All rights reserved.
 *
 * Last modified: 2018-04-13
 */

package com.justbnutz.ytdlcommandbuilder;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;


/**
 * Activity for showing a splash screen while we're waiting for the app to load.
 *
 * Reference: https://android.jlelse.eu/right-way-to-create-splash-screen-on-android-e7f1709ba154
 * - 2018/04/13
 */
public class ActivitySplashScreen extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set up the actual app Activity
        Intent appIntent = new Intent(ActivitySplashScreen.this, ActivityMain.class);

        // If app was opened via Share To panel, extract out any text that was passed through and send that to the main Activity
        appIntent.putExtra(ActivityMain.INTENTEXTRA_SHARE_TEXT, checkIntentImportText(getIntent()));

        // Start the main Activity
        startActivity(appIntent);

        // Close Splash Screen Activity
        finish();
    }


    /**
     * Check a given Intent to see if it contains "android.intent.action.SEND" actions as specified
     * in the app manifest.
     *
     * If so, try and extract the text out of the Intent to send to the main Activity
     */
    private String checkIntentImportText(Intent appIntent) {

        String[] textExtraLabels = {
                Intent.EXTRA_TEXT,
                Intent.EXTRA_HTML_TEXT
        };

        String importText = "";
        String intentType = appIntent.getType();

        if (!TextUtils.isEmpty(intentType)) {

            // See if we can extract some text content with the known labels
            for (String extraLabel : textExtraLabels) {

                importText = appIntent.getStringExtra(extraLabel);

                if (!TextUtils.isEmpty(importText)) {
                    // If we were able to read some text out, break out of the loop
                    break;
                }
            }
        }

        return importText;
    }

}
