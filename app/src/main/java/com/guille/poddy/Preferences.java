package com.guille.poddy;

import android.content.*;
import android.os.*;

public class Preferences {
    public static final String PREFS_NAME = "poddy.preferences";
    public static final String defDownloadDirectory = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PODCASTS).toString() + "/";

    public static void setDownloadDirectory(Context context, String value) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        try {
            editor.putString("downloadDirectory", value);
            editor.apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getDownloadDirectory(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString("downloadDirectory", defDownloadDirectory);
    }
}
