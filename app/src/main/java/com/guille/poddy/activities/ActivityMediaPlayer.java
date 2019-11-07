package com.guille.poddy.activities;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.view.View;

import androidx.appcompat.widget.Toolbar;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.guille.poddy.Helpers;
import com.guille.poddy.R;
import com.guille.poddy.database.*;
import com.guille.poddy.services.*;

import com.guille.poddy.eventbus.*;
import org.greenrobot.eventbus.*;

public class ActivityMediaPlayer extends ActivityAbstract {
    private Episode episode;
    private Podcast podcast;

    private TextView textEpisodeTitle, textPodcastTitle, textCurrentPosition, textDuration;
    private ImageView imagePodcast;
    private ImageButton buttonPlay, buttonPause, buttonRewind, buttonFastForward;
    private SeekBar seekBar;


    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onReceiveAudioBeingPlayedEpisode(MessageEvent.AudioBeingPlayedEpisode event) {
        episode = event.episode;
        textEpisodeTitle.setText(episode.title);

        // Reload the podcast object
        if (podcast == null || podcast.id != episode.podcastId) {
            final DatabaseHelper dbh = DatabaseHelper.getInstance(getApplicationContext());
            podcast = dbh.getPodcastFromEpisode(episode.id);
        }

        textPodcastTitle.setText(episode.podcastTitle);

        Helpers.setEpisodeImage(imagePodcast, episode);
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onReceiveAudioBeingPlayedStatus(MessageEvent.AudioBeingPlayedStatus event) {
        final int status = event.status;

        if (status != MediaPlayerService.STOPPED) {
            if (status == MediaPlayerService.PAUSED) {
                buttonPlay.setVisibility(View.VISIBLE);
                buttonPause.setVisibility(View.GONE);
            } else {
                buttonPlay.setVisibility(View.GONE);
                buttonPause.setVisibility(View.VISIBLE);
            }
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onReceiveAudioBeingPlayedPosition(MessageEvent.AudioBeingPlayedPosition event) {
        // Seekbar %
        final int progress = Math.round(((float) event.currentPosition / (float) event.duration) * 100);
        seekBar.setProgress(progress);

        // Setting duration as text
        textCurrentPosition.setText(Helpers.milisecondsToString(event.currentPosition));
        textDuration.setText(Helpers.milisecondsToString(event.duration));
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        } else {
            EventBus.getDefault().unregister(this);
            EventBus.getDefault().register(this);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_player);

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        imagePodcast = findViewById(R.id.imagePodcast);

        textEpisodeTitle = findViewById(R.id.textEpisodeTitle);
        textPodcastTitle = findViewById(R.id.textPodcastTitle);

        textCurrentPosition = findViewById(R.id.textCurrentPosition);
        textDuration = findViewById(R.id.textDuration);

        buttonPlay = findViewById(R.id.buttonResume);
        buttonPause = findViewById(R.id.buttonPause);
        buttonRewind = findViewById(R.id.buttonRewind);
        buttonFastForward = findViewById(R.id.buttonFastForward);
        seekBar = findViewById(R.id.seekBar);

        // Set buttons for controlling media

        buttonPlay.setOnClickListener(v ->
                EventBus.getDefault().post(new MessageEvent.PauseOrResume()));
        buttonPause.setOnClickListener(v ->
                EventBus.getDefault().post(new MessageEvent.PauseOrResume()));
        buttonRewind.setOnClickListener(v ->
                EventBus.getDefault().post(new MessageEvent.Rewind()));
        buttonFastForward.setOnClickListener(v ->
                EventBus.getDefault().post(new MessageEvent.FastForward()));

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStartTrackingTouch(SeekBar seekBar){}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar){}
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser)
                    EventBus.getDefault().post(new MessageEvent.SeekTo(progress));
            }
        });
    }
}
