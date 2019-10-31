package com.guille.poddy.activities;

import com.guille.poddy.database.*;
import com.guille.poddy.services.*;
import com.guille.poddy.*;

import android.text.Html;

import android.widget.*;

import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.text.method.ScrollingMovementMethod;

import android.content.*;
import android.text.util.Linkify;
import java.io.File;

import android.os.Bundle;
import android.os.Build;
import android.view.View;

import java.util.*;

import com.tonyodev.fetch2.*;

import android.graphics.*;

import com.guille.poddy.R;

public class ActivityEpisodeInfo extends ActivityAbstract {
    private Episode episode;
    private Podcast podcast;
    private ImageView imagePodcast;
    private TextView textEpisodeTitle, textPodcastTitle, textDescription, textDuration, textDate;
    private Button buttonDelete, buttonPlay;
    private ProgressBar progressBar;

    private List<Download> downloading = new ArrayList<>();

    // Broadcast listener for getting updates on downloads
    private final BroadcastReceiver downloadUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                List<Download> total = new ArrayList<>(downloading);
                downloading = (List<Download>) intent.getSerializableExtra("downloading");
                total.addAll(downloading);

                final Download download = Helpers.isBeingDownloaded(total, episode.id);
                if (download != null) {
                    if (Helpers.isBeingDownloaded(downloading, episode.id) == null) {
                        refreshEpisode(true);
                    } else {
                        refreshEpisode(false);
                    }
                }
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }
    };

    @Override
    public void onStart() {
        super.onStart();
        final LocalBroadcastManager bm = LocalBroadcastManager.getInstance(getApplicationContext());
        bm.registerReceiver(downloadUpdateReceiver, new IntentFilter(Broadcast.REFRESH_EPISODES));
        requestRefreshEpisode();
    }

    @Override
    public void onStop() {
        super.onStop();
        final LocalBroadcastManager bm = LocalBroadcastManager.getInstance(getApplicationContext());
        bm.unregisterReceiver(downloadUpdateReceiver);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_episode_info);

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // The podcast that was passed in
        episode = getIntent().getExtras().getParcelable("episode");
        podcast = getIntent().getExtras().getParcelable("podcast");

        imagePodcast = findViewById(R.id.imagePodcast);

        textEpisodeTitle = findViewById(R.id.textEpisodeTitle);
        textPodcastTitle = findViewById(R.id.textPodcastTitle);
        textDuration = findViewById(R.id.textDuration);
        textDate = findViewById(R.id.textDate);
        textDescription = findViewById(R.id.textDescription);

        progressBar = findViewById(R.id.progressBar);

        buttonPlay = findViewById(R.id.buttonPlay);
        buttonDelete = findViewById(R.id.buttonDelete);
        setUpButtons();


        textEpisodeTitle.setText(episode.title);
        textPodcastTitle.setText(podcast.title);
        textDate.setText(Helpers.dateToReadable(episode.date));
        setImage();

        if (!episode.duration.equals("")) {
            try {
                textDuration.setText(Helpers.milisecondsToString(Integer.parseInt(episode.duration)));
            } catch (Exception e){
                e.printStackTrace();
            }
        }
        // Description
        textDescription.setMovementMethod(new ScrollingMovementMethod());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            textDescription.setText(Html.fromHtml(episode.description, Html.FROM_HTML_MODE_COMPACT));
        } else {
            textDescription.setText(Html.fromHtml(episode.description));
        }
        Linkify.addLinks(textDescription, Linkify.WEB_URLS);
        // TODO change linkify, its annoying
    }

    private void setImage() {
        Bitmap bitmap = Helpers.getPodcastImage(this, episode, podcast);
        if (bitmap != null)
            imagePodcast.setImageBitmap(bitmap);
        else
            imagePodcast.setImageResource(R.drawable.ic_add_black_24dp);
    }

    private void requestRefreshEpisode() {
        Intent intent = new Intent(Broadcast.REQUEST_REFRESH_EPISODE);
        final LocalBroadcastManager bm = LocalBroadcastManager.getInstance(this);
        bm.sendBroadcast(intent);
    }

    private void setUpButtons() {
        refreshEpisode(false);
        buttonPlay.setOnClickListener(v -> {
            final DatabaseHelper dbh = DatabaseHelper.getInstance(getApplicationContext());
            episode = dbh.getEpisodeFromId(episode.id);
            Podcast pod = dbh.getPodcastFromEpisode(episode.id);

            if (episode.downloaded) {
                if (new File(episode.file).exists()) {
                    MediaPlayerBridge.playAudio(getApplicationContext(), episode, pod);
                } else {
                    // File doesn't exist, so set DOWNLOADED as false and refresh that episode
                    if (dbh.updateEpisodeDownloadedStatus(episode.id, false, "")) {
                        refreshEpisode(true);
                    }
                }
            } else {
                final Download download = Helpers.isBeingDownloaded(downloading, episode.id);
                if (download == null) {
                    DownloaderBridge.downloadEpisode(getApplicationContext(), episode, pod);
                } else {
                    DownloaderBridge.cancelDownload(getApplicationContext(), download);
                }
            }
        });

        buttonDelete.setOnClickListener(v -> {
            final DatabaseHelper dbh = DatabaseHelper.getInstance(getApplicationContext());
            episode = dbh.getEpisodeFromId(episode.id);
            if (episode.downloaded) {
                File checkFile = new File(episode.file);
                if(checkFile.exists()) {
                    checkFile.delete();
                }
                if (dbh.updateEpisodeDownloadedStatus(episode.id, false, "")) {
                    refreshEpisode(true);
                }
            }
        });
    }

    private void refreshEpisode(Boolean reloadDatabase){
        if (reloadDatabase) {
            final DatabaseHelper dbh = DatabaseHelper.getInstance(getApplicationContext());
            episode = dbh.getEpisodeFromId(episode.id);
            setImage();
        }
        if (episode.downloaded) {
            buttonPlay.setText("Play");
            buttonPlay.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_play_arrow_black_24dp, 0);
            progressBar.setVisibility(View.GONE);
        } else {
            final Download download = Helpers.isBeingDownloaded(this.downloading, episode.id);
            if (download != null) {
                buttonPlay.setText("Cancel");
                buttonPlay.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_cancel_black_24dp, 0);
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setProgress(download.getProgress());
            } else {
                buttonPlay.setText("Download");
                buttonPlay.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_file_download_black_24dp, 0);
                progressBar.setVisibility(View.GONE);
            }
        }
    }
}
