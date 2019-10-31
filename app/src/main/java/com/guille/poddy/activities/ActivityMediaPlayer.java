package com.guille.poddy.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.guille.poddy.Broadcast;
import com.guille.poddy.Helpers;
import com.guille.poddy.R;
import com.guille.poddy.database.Episode;
import com.guille.poddy.database.Podcast;
import com.guille.poddy.services.MediaPlayerBridge;
import com.guille.poddy.services.MediaPlayerService;

public class ActivityMediaPlayer extends ActivityAbstract {

    private int status, currentPosition, duration;
    private Episode ep;
    private Podcast pod;

    private TextView textEpisodeTitle, textPodcastTitle, textCurrentPosition, textDuration;
    private ImageView imagePodcast;
    private ImageButton imagePlayButton, imageRewindButton, imageFastForwardButton;
    private SeekBar seekBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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

        imagePlayButton = findViewById(R.id.imagePlayButton);
        imageRewindButton = findViewById(R.id.imageRewindButton);
        imageFastForwardButton = findViewById(R.id.imageFastForwardButton);
        seekBar = findViewById(R.id.seekBar);

        // Set receiver for updating content
        IntentFilter filter = new IntentFilter(Broadcast.REFRESH_MEDIAPLAYER);

        final LocalBroadcastManager bm = LocalBroadcastManager.getInstance(getApplicationContext());
        bm.registerReceiver(getMusicPlayerInfo, filter);

        // Set broadcasters for controlling media

        imagePlayButton.setOnClickListener(v -> MediaPlayerBridge.pauseOrResumeAudio(getApplication()));

        imageRewindButton.setOnClickListener(v -> MediaPlayerBridge.rewindAudio(getApplication()));

        imageFastForwardButton.setOnClickListener(v -> MediaPlayerBridge.fastForwardAudio(getApplication()));

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStartTrackingTouch(SeekBar seekBar){}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar){}
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser)
                    MediaPlayerBridge.seekAudio(getApplication(), progress);
            }
        });
    }

    private void requestRefreshMediaPlayer() {
        Intent intent = new Intent(Broadcast.REQUEST_REFRESH_MEDIAPLAYER);
        final LocalBroadcastManager bm = LocalBroadcastManager.getInstance(getApplicationContext());
        bm.sendBroadcast(intent);
    }

    @Override
    public void onStart() {
        super.onStart();
        requestRefreshMediaPlayer();
    }

    private final BroadcastReceiver getMusicPlayerInfo = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshBar(intent);
        }
    };

    private void refreshBar(Intent intent) {
        try {
            status = intent.getExtras().getInt("status");
            currentPosition = intent.getExtras().getInt("currentPosition");
            duration = intent.getExtras().getInt("duration");

            ep = intent.getExtras().getParcelable("episode");
            pod = intent.getExtras().getParcelable("podcast");

            final String stringCurrentPosition = Helpers.milisecondsToString(currentPosition);
            final String stringDuration = Helpers.milisecondsToString(duration);

            final int progress = Math.round(((float) currentPosition / (float) duration) * 100);

            seekBar.setProgress(progress);
            textCurrentPosition.setText(stringCurrentPosition);
            textDuration.setText(stringDuration);

            if (!textEpisodeTitle.getText().toString().equals(ep.title)) {
                textEpisodeTitle.setText(ep.title);
                textPodcastTitle.setText(pod.title);
                setImage();
            }

            if (status == MediaPlayerService.PAUSED) {
                imagePlayButton.setImageResource(R.drawable.ic_play_arrow_black_24dp);
            } else {
                imagePlayButton.setImageResource(R.drawable.ic_pause_black_24dp);
            }

        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    private void setImage() {
        Bitmap bitmap = Helpers.getPodcastImage(this, ep, pod);
        if (bitmap != null)
            imagePodcast.setImageBitmap(bitmap);
        else
            imagePodcast.setImageResource(R.drawable.ic_add_black_24dp);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        final LocalBroadcastManager bm = LocalBroadcastManager.getInstance(getApplicationContext());
        bm.unregisterReceiver(getMusicPlayerInfo);
    }
}
