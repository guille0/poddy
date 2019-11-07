package com.guille.poddy;

import android.content.*;
import android.os.*;
import com.guille.poddy.fragments.FragmentFactory.ContentShown;
import com.guille.poddy.fragments.*;
import java.util.*;

public class Preferences {
    public static final String PREFS_NAME = "poddy.preferences";
    public static final String downloadDirectory = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PODCASTS).toString() + "/";
    public static final int downloadLimit = 3;
    public static final int downloadLimitMax = 10;

    public static final int PODCAST_ORDER_TITLE = 0;
    public static final int PODCAST_ORDER_RECENT = 1;
    public static final int podcastOrder = PODCAST_ORDER_RECENT;

    public static final int DATA_USAGE_ONLY_WIFI = 0;
    public static final int DATA_USAGE_DATA_AND_WIFI = 1;
    public static final int dataUsage = DATA_USAGE_ONLY_WIFI;


    // INTERFACE


    public static void setPodcastOrder(Context context, int value) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        try {
            editor.putInt("podcastOrder", value);
            editor.apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int getPodcastOrder(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt("podcastOrder", podcastOrder);
    }


    // NETWORK

    public static void setDataUsage(Context context, int value) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        try {
            editor.putInt("dataUsage", value);
            editor.apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int getDataUsage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt("dataUsage", dataUsage);
    }

    public static void setDownloadLimit(Context context, int value) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        if (value > 0 && value < downloadLimitMax) {
            try {
                editor.putInt("downloadLimit", value);
                editor.apply();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static int getDownloadLimit(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt("downloadLimit", downloadLimit);
    }

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
        return prefs.getString("downloadDirectory", downloadDirectory);
    }
}
