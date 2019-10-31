package com.guille.poddy.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.guille.poddy.R;
import com.guille.poddy.database.DatabaseHelper;
import com.guille.poddy.database.Podcast;
import com.guille.poddy.fragments.FragmentEpisodes;
import com.guille.poddy.services.FeedUpdaterBridge;

public class ActivityPodcastEpisodes extends ActivityAbstractShowPodcasts {
    private String podcastShown = "all";
    private Podcast podcast;

    @Override
    public String getPodcastShown() {
        return podcastShown;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
        // Variable that indicates which Podcast we are displaying in this Activity
        podcastShown = podcast.url;

        // Creating fragment
        Bundle bundle = new Bundle();
        bundle.putString("kind", "podcastEpisodes");
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

    // SET UP MEDIABAR and fragment height

//    private void setUpMediaBar() {
//        // Set up mediabar
//        LinearLayout mediabar = findViewById(R.id.layoutMediabar);
//        // Creating fragment in mediabar
//        FragmentManager fragmentManager = getSupportFragmentManager();
//        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
//        Bundle bundle = new Bundle();
//        FragmentMediaPlayerBar fragInfo = new FragmentMediaPlayerBar();
//        fragInfo.setArguments(bundle);
//
//        fragmentTransaction.replace(R.id.layoutMediabar, fragInfo);
//        fragmentTransaction.commit();
//
//        // Set the height of the viewpager
//        mediabar.post(new Runnable() {
//            @Override
//            public void run() {
//                int height = mediabar.getHeight();
//                LinearLayout layout = (LinearLayout) findViewById(R.id.layout_content);
//                MarginLayoutParams params = (MarginLayoutParams) layout.getLayoutParams();
//                params.bottomMargin = height;
//            }
//        });
//    }


    // On this screen we override some of the option buttons
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.update:
                // On update we only update THIS podcast
                FeedUpdaterBridge.updateFeed(getApplicationContext(), podcast.url);
                return true;

            case R.id.newPodcast:
                Intent intent = new Intent(ActivityPodcastEpisodes.this, ActivityNewPodcast.class);
                startActivity(intent);
                return true;

            case R.id.refresh_podcast:
                FeedUpdaterBridge.updateFeed(getApplicationContext(), podcast.url);
                return true;

            case R.id.delete_podcast:
                DatabaseHelper dbh = DatabaseHelper.getInstance(getApplicationContext());
                dbh.deletePodcast(podcast.id);
                this.finish();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // REFRESH

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    protected void refresh() {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.eps);

        getSupportFragmentManager()
                .beginTransaction()
                .detach(currentFragment)
                .attach(currentFragment)
                .commit();
        Log.i("ActivityPodcastEpisodes", "Refreshed Episodes Fragment");
    }
}
