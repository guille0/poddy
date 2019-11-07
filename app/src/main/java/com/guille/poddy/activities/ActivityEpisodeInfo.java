package com.guille.poddy.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.text.method.ScrollingMovementMethod;
import android.text.util.Linkify;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;

import com.guille.poddy.Helpers;
import com.guille.poddy.R;
import com.guille.poddy.database.DatabaseHelper;
import com.guille.poddy.database.Episode;
import com.guille.poddy.database.Podcast;
import com.guille.poddy.services.*;
import com.guille.poddy.*;
import com.tonyodev.fetch2.Download;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import com.guille.poddy.eventbus.*;
import org.greenrobot.eventbus.*;

public class ActivityEpisodeInfo extends ActivityAbstract {
    private Episode episode;
    private Podcast podcast;
    private ImageView imagePodcast;
    private TextView textEpisodeTitle, textPodcastTitle, textDescription, textDuration, textDate;
    private Button buttonResume, buttonStartPlay, buttonPause, buttonDownload, buttonCancel;
    private Button buttonDelete;
    private ProgressBar progressBar;

    private boolean listeningToThis;
    private int listeningStatus;

    private List<Download> downloads = new ArrayList<>();

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onReceiveRefreshEpisode(MessageEvent.RefreshEpisode event) {
        if (episode.id == event.episodeId)
            refreshEpisode(true);
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onReceiveAudioBeingPlayedStatus(MessageEvent.AudioBeingPlayedStatus event) {
        if (event.episodeId == episode.id) {
            listeningToThis = true;
            listeningStatus = event.status;
        } else {
            listeningToThis = false;
        }
        refreshEpisode(false);
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onReceiveEpisodesBeingDownloaded(MessageEvent.EpisodesBeingDownloaded event) {
        List<Download> total = new ArrayList<>(downloads);
        downloads = event.downloads;
        total.addAll(downloads);

        final Download download = Helpers.isBeingDownloaded(total, episode.id);
        if (download != null) {
            if (Helpers.isBeingDownloaded(downloads, episode.id) == null) {
                refreshEpisode(true);
            } else {
                refreshEpisode(false);
            }
        }
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
        setContentView(R.layout.activity_episode_info);

        // The podcast that was passed in
        episode = getIntent().getExtras().getParcelable("episode");
        podcast = getIntent().getExtras().getParcelable("podcast");

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(episode.title);

        imagePodcast = findViewById(R.id.imagePodcast);

        textEpisodeTitle = findViewById(R.id.textEpisodeTitle);
        textPodcastTitle = findViewById(R.id.textPodcastTitle);
        textDuration = findViewById(R.id.textDuration);
        textDate = findViewById(R.id.textDate);
        textDescription = findViewById(R.id.textDescription);

        progressBar = findViewById(R.id.progressBar);

        buttonDelete = findViewById(R.id.buttonDelete);

        // All variants of the same button
        buttonResume = findViewById(R.id.buttonResume);
        buttonStartPlay = findViewById(R.id.buttonStartPlay);
        buttonPause = findViewById(R.id.buttonPause);
        buttonDownload = findViewById(R.id.buttonDownload);
        buttonCancel = findViewById(R.id.buttonCancel);
        setUpButtons();

        textEpisodeTitle.setText(episode.title);
        textPodcastTitle.setText(episode.podcastTitle);
        textDate.setText(Helpers.dateToReadable(episode.date));

        Helpers.setEpisodeImage(imagePodcast, episode);

        textDuration.setText(Helpers.milisecondsToString(episode.duration));
        // Description
        textDescription.setMovementMethod(LinkMovementMethod.getInstance());
        textDescription.setAutoLinkMask(Linkify.WEB_URLS);
        textDescription.setMovementMethod(new ScrollingMovementMethod());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            textDescription.setText(Html.fromHtml(episode.description, 0));
        } else {
            textDescription.setText(Html.fromHtml(episode.description));
        }
    }

    private void setUpButtons() {
        refreshEpisode(false);

        buttonPause.setOnClickListener(v ->
                EventBus.getDefault().post(new MessageEvent.PauseOrResume()));

        buttonResume.setOnClickListener(v ->
                EventBus.getDefault().post(new MessageEvent.PauseOrResume()));

        buttonStartPlay.setOnClickListener(v -> {
            if (episode.downloaded) {
                if (new File(episode.file).exists()) {
                    MediaPlayerService.coldPlayAudio(this, episode.id);
                } else {
                    // File doesn't exist, so set DOWNLOADED as false and refresh that episode
                    final DatabaseHelper dbh = DatabaseHelper.getInstance(getApplicationContext());
                    if (dbh.updateEpisodeDownloadedStatus(episode.id, false, "")) {
                        refreshEpisode(true);
                    }
                }
            }
        });

        buttonDownload.setOnClickListener(v -> {
            final Download download = Helpers.isBeingDownloaded(downloads, episode.id);
            if (download == null) {
                DownloaderService.download(getApplicationContext(), episode, podcast);
            }
        });

        buttonCancel.setOnClickListener(v -> {
            final Download download = Helpers.isBeingDownloaded(downloads, episode.id);
            if (download != null) {
                EventBus.getDefault().post(new MessageEvent.CancelDownload(download));
            }
        });

        buttonDelete.setOnClickListener(v -> {
            final DatabaseHelper dbh = DatabaseHelper.getInstance(getApplicationContext());
//            episode = dbh.getEpisodeFromId(episode.id);
            if (episode.downloaded) {
                File checkFile = new File(episode.file);
                if(checkFile.exists()) {
                    checkFile.delete();
                }
                if (dbh.updateEpisodeDownloadedStatus(episode.id, false, "")) {
                    refreshEpisode(true);
                    EventBus.getDefault().post(new MessageEvent.RefreshPodcast(""));
                }
            }
        });
    }

    private void refreshEpisode(Boolean reloadDatabase){
        if (reloadDatabase) {
            final DatabaseHelper dbh = DatabaseHelper.getInstance(getApplicationContext());
            episode = dbh.getEpisodeFromId(episode.id);
            Helpers.setEpisodeImage(imagePodcast, episode);
            textDuration.setText(Helpers.milisecondsToString(episode.duration));
        }

        final int STARTPLAY = 1;
        final int RESUME = 2;
        final int PAUSE = 3;
        final int DOWNLOAD = 4;
        final int CANCEL = 5;
        int activeButton;

        if (episode.downloaded) {
            if (listeningToThis && listeningStatus != MediaPlayerService.STOPPED) {
                if (listeningStatus == MediaPlayerService.PAUSED)
                    activeButton = RESUME;
                else
                    activeButton = PAUSE;
            } else
                activeButton = STARTPLAY;
            progressBar.setVisibility(View.GONE);
        } else {
            final Download download = Helpers.isBeingDownloaded(this.downloads, episode.id);
            if (download != null) {
                activeButton = CANCEL;
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setProgress(download.getProgress());
            } else {
                activeButton = DOWNLOAD;
                progressBar.setVisibility(View.GONE);
            }
        }

        switch (activeButton) {
            case STARTPLAY:
                buttonResume.setVisibility(View.GONE);
                buttonPause.setVisibility(View.GONE);
                buttonDownload.setVisibility(View.GONE);
                buttonCancel.setVisibility(View.GONE);
                buttonStartPlay.setVisibility(View.VISIBLE);
                buttonDelete.setVisibility(View.VISIBLE);
                break;
            case RESUME:
                buttonStartPlay.setVisibility(View.GONE);
                buttonPause.setVisibility(View.GONE);
                buttonDownload.setVisibility(View.GONE);
                buttonCancel.setVisibility(View.GONE);
                buttonResume.setVisibility(View.VISIBLE);
                buttonDelete.setVisibility(View.VISIBLE);
                break;
            case PAUSE:
                buttonStartPlay.setVisibility(View.GONE);
                buttonDownload.setVisibility(View.GONE);
                buttonCancel.setVisibility(View.GONE);
                buttonResume.setVisibility(View.GONE);
                buttonPause.setVisibility(View.VISIBLE);
                buttonDelete.setVisibility(View.VISIBLE);
                break;
            case DOWNLOAD:
                buttonStartPlay.setVisibility(View.GONE);
                buttonCancel.setVisibility(View.GONE);
                buttonResume.setVisibility(View.GONE);
                buttonPause.setVisibility(View.GONE);
                buttonDelete.setVisibility(View.GONE);
                buttonDownload.setVisibility(View.VISIBLE);
                break;
            case CANCEL:
                buttonStartPlay.setVisibility(View.GONE);
                buttonResume.setVisibility(View.GONE);
                buttonPause.setVisibility(View.GONE);
                buttonDownload.setVisibility(View.GONE);
                buttonDelete.setVisibility(View.GONE);
                buttonCancel.setVisibility(View.VISIBLE);
                break;
        }
    }
}
