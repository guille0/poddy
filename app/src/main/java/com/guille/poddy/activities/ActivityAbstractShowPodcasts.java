package com.guille.poddy.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.guille.poddy.Broadcast;

public abstract class ActivityAbstractShowPodcasts extends ActivityAbstract {
    // Activity that shows podcasts or episodes
    // Has methods for listening for updates and refreshing itself

    protected abstract String getPodcastShown();
    protected abstract void refresh();

    // Broadcast listener for refreshing activity after downloading podcast/rss
    private final BroadcastReceiver refreshPodcasts = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                String podcastUrl = intent.getExtras().getString("podcastUrl");

                // Each activity has a podcastShown variable, for the podcast it's displaying
                // If we are showing that podcast in the current activity, refresh it
                if (getPodcastShown().equals(podcastUrl) || getPodcastShown().equals("all")) {
                    Toast.makeText(ActivityAbstractShowPodcasts.this, "Refreshing screen", Toast.LENGTH_SHORT).show();
                    refresh();
                }
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }
    };

    @Override
    public void onStart() {
        super.onStart();
        final LocalBroadcastManager bm = LocalBroadcastManager.getInstance(getApplicationContext());
        bm.registerReceiver(refreshPodcasts, new IntentFilter(Broadcast.REFRESH_PODCASTS));
    }

    @Override
    public void onStop() {
        super.onStop();
        final LocalBroadcastManager bm = LocalBroadcastManager.getInstance(getApplicationContext());
        bm.unregisterReceiver(refreshPodcasts);
    }


}
