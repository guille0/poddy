package com.guille.poddy.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;
import android.widget.Toast;
import androidx.fragment.app.FragmentTransaction;

import com.guille.poddy.R;
import com.guille.poddy.database.*;
import com.guille.poddy.fragments.*;
import com.guille.poddy.fragments.recyclerviews.FragmentEpisodes;
import com.guille.poddy.services.*;
import com.guille.poddy.eventbus.*;
import org.greenrobot.eventbus.*;

public class ActivityPodcastEpisodes extends ActivityAbstract {
    private Podcast podcast;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_podcast_episodes);

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Load up fragment to show episodes
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        // The podcast that was passed in
        podcast = getIntent().getExtras().getParcelable("podcast");

        // Creating fragment
        Bundle bundle = new Bundle();
        bundle.putInt("kind", FragmentFactory.ContentShown.PODCAST_EPISODES);
        bundle.putParcelable("podcast", podcast);
        FragmentEpisodes fragInfo = new FragmentEpisodes();
        fragInfo.setArguments(bundle);

        fragmentTransaction.replace(R.id.eps, fragInfo);
        fragmentTransaction.commit();
    }

    // Options menu

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Set up dropdown menu
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_podcast_episodes, menu);
        return super.onCreateOptionsMenu(menu);
    }

    // On this screen we override some of the option buttons
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.update:
            case R.id.refresh_podcast:
                // On update we only update THIS podcast
                Toast.makeText(this,
                        "Updating "+podcast.title,
                        Toast.LENGTH_SHORT).show();
                FeedUpdaterService.updateFeeds(getApplicationContext(), new String[] {podcast.url});
                return true;
            case R.id.newPodcast:
                Intent intent = new Intent(ActivityPodcastEpisodes.this, ActivityNewPodcast.class);
                startActivity(intent);
                return true;
            case R.id.delete_podcast:
                DatabaseHelper dbh = DatabaseHelper.getInstance(getApplicationContext());
                dbh.deletePodcast(podcast.id);
                this.finish();
                EventBus.getDefault().post(new MessageEvent.RefreshPodcast(podcast.url));
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
