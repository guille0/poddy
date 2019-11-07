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

import com.guille.poddy.R;
import com.guille.poddy.database.DatabaseHelper;
import com.guille.poddy.database.Podcast;
import com.guille.poddy.services.*;
import com.guille.poddy.eventbus.*;
import org.greenrobot.eventbus.*;
import android.util.*;
import android.os.Bundle;

import java.util.List;

public abstract class ActivityAbstract extends AppCompatActivity {
    // Global toolbar buttons (override them in specific activities if needed)
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
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
                Toast.makeText(this,
                        "Updating all podcasts",
                        Toast.LENGTH_SHORT).show();

                FeedUpdaterService.updateFeeds(getApplicationContext(), feedUrls);
                return true;

            case R.id.newPodcast:
                Intent intent = new Intent(ActivityAbstract.this, ActivityNewPodcast.class);
                startActivity(intent);
                return true;

            case R.id.preferences:
                Intent intenty = new Intent(this, ActivityPreferences.class);
                startActivity(intenty);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
