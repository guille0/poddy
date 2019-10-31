package com.guille.poddy.services;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.guille.poddy.Broadcast;

public class FeedUpdaterBridge {

    private static boolean isServiceRunning(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (FeedUpdaterService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private static void updateFeeds(Context context, String[] feedUrls) {
        if (!isServiceRunning(context)) {
            Intent intent = new Intent(context, FeedUpdaterService.class);
            intent.putExtra("feedUrls", feedUrls);
            context.startService(intent);
        } else {
            Intent intent = new Intent(Broadcast.UPDATE_FEEDS);
            intent.putExtra("feedUrls", feedUrls);
            final LocalBroadcastManager bm = LocalBroadcastManager.getInstance(context);
            bm.sendBroadcast(intent);
        }
    }

    public static void updateFeed(Context context, String feedUrl) {
        updateFeeds(context, new String[] {feedUrl});
    }
}
