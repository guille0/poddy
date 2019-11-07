package com.guille.poddy.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.widget.Toolbar;

import com.guille.poddy.R;
import com.guille.poddy.services.*;
import com.guille.poddy.eventbus.*;
import org.greenrobot.eventbus.*;

public class ActivityNewPodcast extends ActivityAbstract {
    @Override
    public void onCreate(Bundle savedInstanceState) {
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

        // Raw feed button
        Button buttonRawUrl = findViewById(R.id.buttonRawUrl);
        buttonRawUrl.setOnClickListener(view -> {
            EditText text = findViewById(R.id.textRawUrlEdit);
            String url = text.getText().toString();

            FeedUpdaterService.updateFeeds(getApplicationContext(), new String[] {url});
        });
    }
}
