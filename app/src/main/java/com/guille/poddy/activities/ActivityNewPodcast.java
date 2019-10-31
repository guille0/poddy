package com.guille.poddy.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.widget.Toolbar;

import com.guille.poddy.R;
import com.guille.poddy.services.FeedUpdaterBridge;

public class ActivityNewPodcast extends ActivityAbstract {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_podcast);

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Buttons
        // ITUNES BUTTON
        Button buttonItunes = findViewById(R.id.buttonItunes);
        buttonItunes.setOnClickListener(view -> {
            Intent intent = new Intent(ActivityNewPodcast.this, ActivityItunesSearch.class);
            startActivity(intent);
        });

        // Test button (DOUGHBOYS)
        Button buttonTest = findViewById(R.id.buttonTest);
        buttonTest.setOnClickListener(view -> FeedUpdaterBridge.updateFeed(getApplicationContext(), "https://rss.art19.com/doughboys"));

        // Test button (HDTGP)
        Button buttonTest2 = findViewById(R.id.buttonTest2);
        buttonTest2.setOnClickListener(view -> FeedUpdaterBridge.updateFeed(getApplicationContext(), "https://rss.art19.com/how-did-this-get-played"));

        // Test button (HANDBOOK)
        Button buttonTest3 = findViewById(R.id.buttonTest3);
        buttonTest3.setOnClickListener(view -> FeedUpdaterBridge.updateFeed(getApplicationContext(), "https://rss.art19.com/hollywood-handbook"));

        // Raw feed button
        Button buttonRawUrl = findViewById(R.id.buttonRawUrl);
        buttonRawUrl.setOnClickListener(view -> {
            EditText text = findViewById(R.id.textRawUrlEdit);
            String url = text.getText().toString();
            FeedUpdaterBridge.updateFeed(getApplicationContext(), url);
        });
    }
}
