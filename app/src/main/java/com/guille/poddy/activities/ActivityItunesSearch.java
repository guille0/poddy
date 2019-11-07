package com.guille.poddy.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import android.app.ProgressDialog;

import androidx.appcompat.widget.Toolbar;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.widget.Toast;
import com.guille.poddy.*;
import com.guille.poddy.database.*;
import com.guille.poddy.services.*;

import java.util.ArrayList;
import java.util.List;

import org.json.*;
import com.guille.poddy.eventbus.*;
import org.greenrobot.eventbus.*;

public class ActivityItunesSearch extends ActivityAbstract implements RecyclerViewItunesSearch.ItemClickListener {
    private ProgressDialog pd;

    private RecyclerView recyclerView;

    private List<Podcast> podcasts;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_itunes_search);

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Set up search bar
        SearchView searchView = findViewById(R.id.searchView);
        searchView.setIconifiedByDefault(true);
        searchView.setFocusable(true);
        searchView.setIconified(false);
        searchView.requestFocusFromTouch();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // Set up the recycler view
                recyclerView.setHasFixedSize(true);
                recyclerView.setLayoutManager(new LinearLayoutManager(ActivityItunesSearch.this));

                // Show most popular podcasts on load
                final String itunesSearch = "https://itunes.apple.com/search?media=podcast&country=%s&term=%s%s";
                // Country, seach term, extra params
                final String url = String.format(itunesSearch, "US", query, "");

                search(url);
                if(!searchView.isIconified()) {
                    searchView.setIconified(true);
                }
                return false;
            }
            @Override
            public boolean onQueryTextChange(String s) {
                return false;
            }
        });


        // Set up the recycler view
        recyclerView = findViewById(R.id.podcastList);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void search(String url) {
        Helpers.ResponseToStringTask request = new Helpers.ResponseToStringTask() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                pd = new ProgressDialog(ActivityItunesSearch.this);
                pd.setMessage("Please wait");
                pd.setCancelable(false);
                pd.show();
            }

            @Override
            protected void onPostExecute(String result) {
                super.onPostExecute(result);
                if (pd.isShowing()){
                    pd.dismiss();
                }

                // Parse the JSON string and update the recycler view
                ActivityItunesSearch.this.updateRecyclerView(result);
            }
        };

        request.execute(url);

    }

    private void updateRecyclerView(String jsonString) {
        // Create a List<Podcast> out of the JSON file
        podcasts = parseJson(jsonString);

        RecyclerViewItunesSearch adapter = new RecyclerViewItunesSearch(this, podcasts);
        adapter.setClickListener(this);
        recyclerView.setAdapter(adapter);
    }

    private List<Podcast> parseJson(String jsonString) {
        // Create a List<Podcast> out of the JSON data
        List<Podcast> ps = new ArrayList<>();

        try {
            JSONTokener tokener = new JSONTokener(jsonString);
            JSONObject json = new JSONObject(tokener);

            JSONArray arr = json.getJSONArray("results");
            for (int i = 0; i < arr.length(); i++) {
                Podcast p = new Podcast();
                p.title = arr.getJSONObject(i).getString("trackName");
                p.url = arr.getJSONObject(i).getString("feedUrl");
//            p.artworkUrl60 = arr.getJSONObject(i).getString("artworkUrl60");
//            p.artworkUrl100 = arr.getJSONObject(i).getString("artworkUrl100");
                ps.add(p);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return ps;
    }

    @Override
    public void onItemClick(View view, int position) {
        // Add the podcast
        Toast.makeText(this,
                "Adding " + podcasts.get(position).title,
                Toast.LENGTH_SHORT).show();
        FeedUpdaterService.updateFeeds(getApplicationContext(), new String[] {podcasts.get(position).url});
    }
}
