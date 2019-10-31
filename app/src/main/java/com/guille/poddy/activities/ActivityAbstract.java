package com.guille.poddy.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.guille.poddy.Broadcast;
import com.guille.poddy.R;
import com.guille.poddy.database.DatabaseHelper;
import com.guille.poddy.database.Podcast;
import com.guille.poddy.services.FeedUpdaterBridge;

import java.util.List;

public abstract class ActivityAbstract extends AppCompatActivity{
    // Toast that gets displayed no matter what activity we're on
    private final BroadcastReceiver sendToast = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                String text = intent.getExtras().getString("text");
                Toast.makeText(ActivityAbstract.this, text, Toast.LENGTH_SHORT).show();
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }
    };

    @Override
    public void onStart() {
        super.onStart();
        // set broadcast receivers
        final LocalBroadcastManager bm = LocalBroadcastManager.getInstance(getApplicationContext());
        bm.registerReceiver(sendToast, new IntentFilter(Broadcast.SEND_TOAST));
    }

    @Override
    public void onStop() {
        super.onStop();
        // remove broadcast receivers
        final LocalBroadcastManager bm = LocalBroadcastManager.getInstance(getApplicationContext());
        bm.unregisterReceiver(sendToast);
    }

    // Global toolbar buttons (override them in specific activities if needed)
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.update:
                // Update all podcasts
                final DatabaseHelper dbh = DatabaseHelper.getInstance(getApplicationContext());
                List <Podcast> podcasts = dbh.getAllPodcasts();

                // String [] with all the urls
                String[] feedUrls = new String[podcasts.size()];
                for (int i=0; i<podcasts.size(); i++) {
                    feedUrls[i] = podcasts.get(i).url;
                }

                FeedUpdaterBridge.updateFeeds(getApplicationContext(), feedUrls);
                return true;

            case R.id.newPodcast:
                Intent intent = new Intent(ActivityAbstract.this, ActivityNewPodcast.class);
                startActivity(intent);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
