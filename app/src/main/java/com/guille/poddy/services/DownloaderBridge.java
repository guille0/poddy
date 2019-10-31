package com.guille.poddy.services;

import android.app.ActivityManager;
import android.os.*;
import android.content.*;

import com.tonyodev.fetch2.*;

import com.guille.poddy.database.*;
import com.guille.poddy.Broadcast;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class DownloaderBridge {

    private static boolean isServiceRunning(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (DownloaderService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public static void downloadEpisode(Context context, Episode ep, Podcast pod) {
        if (!isServiceRunning(context)) {
            Intent intent = new Intent(context, DownloaderService.class);
            intent.putExtra("episode", ep);
            intent.putExtra("podcast", pod);
            context.startService(intent);
        } else {
            Intent intent = new Intent(Broadcast.DOWNLOAD);
            intent.putExtra("episode", ep);
            intent.putExtra("podcast", pod);
            final LocalBroadcastManager bm = LocalBroadcastManager.getInstance(context);
            bm.sendBroadcast(intent);
        }
    }

    public static void cancelDownload(Context context, Download download) {
        Intent intent = new Intent(Broadcast.CANCEL_DOWNLOAD);
        intent.putExtra("download", (Parcelable) download);
        final LocalBroadcastManager bm = LocalBroadcastManager.getInstance(context);
        bm.sendBroadcast(intent);
    }
}
