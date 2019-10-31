package com.guille.poddy.services;

import android.os.*;
import android.content.*;
import android.app.*;
import android.app.ActivityManager.RunningServiceInfo;

import com.guille.poddy.database.*;
import com.guille.poddy.Broadcast;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class MediaPlayerBridge {

    private static boolean isServiceRunning(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (MediaPlayerService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private static void startService(Context context, Episode ep, Podcast pod) {
        ServiceConnection serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                MediaPlayerService.LocalBinder binder = (MediaPlayerService.LocalBinder) service;
            }
            @Override
            public void onServiceDisconnected(ComponentName name) {
            }
        };

        Intent intent = new Intent(context, MediaPlayerService.class);
        intent.putExtra("episode", ep);
        intent.putExtra("podcast", pod);
        context.startService(intent);
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    public static void playAudio(Context context, Episode ep, Podcast pod) {
        if (!isServiceRunning(context)) {
            startService(context, ep, pod);
        } else {
            Intent intent = new Intent(Broadcast.PLAY_NEW_AUDIO);
            intent.putExtra("episode", ep);
            intent.putExtra("podcast", pod);

            final LocalBroadcastManager bm = LocalBroadcastManager.getInstance(context);
            bm.sendBroadcast(intent);
        }
    }

    public static void pauseOrResumeAudio(Context context) {
        if (isServiceRunning(context)) {
            Intent intent = new Intent(Broadcast.PAUSE_OR_RESUME);

            final LocalBroadcastManager bm = LocalBroadcastManager.getInstance(context);
            bm.sendBroadcast(intent);
        }
    }

    public static void rewindAudio(Context context) {
        if (isServiceRunning(context)) {
            Intent intent = new Intent(Broadcast.REWIND);

            final LocalBroadcastManager bm = LocalBroadcastManager.getInstance(context);
            bm.sendBroadcast(intent);
        }
    }

    public static void fastForwardAudio(Context context) {
        if (isServiceRunning(context)) {
            Intent intent = new Intent(Broadcast.FAST_FORWARD);

            final LocalBroadcastManager bm = LocalBroadcastManager.getInstance(context);
            bm.sendBroadcast(intent);
        }
    }

    public static void seekAudio(Context context, int seekTo) {
        if (isServiceRunning(context)) {
            Intent intent = new Intent(Broadcast.SEEK);
            intent.putExtra("seekTo", seekTo);

            final LocalBroadcastManager bm = LocalBroadcastManager.getInstance(context);
            bm.sendBroadcast(intent);
        }
    }

}
