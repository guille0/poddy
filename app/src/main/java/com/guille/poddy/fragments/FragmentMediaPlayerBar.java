package com.guille.poddy.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.util.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.guille.poddy.R;
import com.guille.poddy.activities.ActivityMediaPlayer;
import com.guille.poddy.database.Episode;
import com.guille.poddy.database.Podcast;
import com.guille.poddy.services.*;
import com.guille.poddy.eventbus.*;
import org.greenrobot.eventbus.*;

import org.jetbrains.annotations.NotNull;


public class FragmentMediaPlayerBar extends FragmentAbstract {
    private ImageButton buttonPlay, buttonPause;
    private TextView textEpisodeTitle;
    private ProgressBar progressBar;

    private Episode ep;
    private Podcast pod;

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onReceiveAudioBeingPlayedEpisode(MessageEvent.AudioBeingPlayedEpisode event) {
        ep = event.episode;
        textEpisodeTitle.setText(ep.title);
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onReceiveAudioBeingPlayedStatus(MessageEvent.AudioBeingPlayedStatus event) {
        final int status = event.status;

        if (status == MediaPlayerService.STOPPED) {
            hide();
        } else {
            show();
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
        final int progress = Math.round(((float) event.currentPosition / (float) event.duration) * 100);
        progressBar.setProgress(progress);
    }

    private void hide() {
        FragmentManager fm = getActivity().getSupportFragmentManager();
        fm.beginTransaction()
                .hide(FragmentMediaPlayerBar.this)
                .commitAllowingStateLoss();
    }

    private void show() {
        FragmentManager fm = getActivity().getSupportFragmentManager();
        fm.beginTransaction()
                .show(FragmentMediaPlayerBar.this)
                .commitAllowingStateLoss();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mediaplayerbar, container, false);
    }

    @Override
    public void onViewCreated(@NotNull View view, @Nullable Bundle savedInstanceState) {
        if (!MediaPlayerService.isServiceRunning(getActivity().getApplicationContext())) {
            hide();
        }

        buttonPlay = getView().findViewById(R.id.buttonResume);
        buttonPause = getView().findViewById(R.id.buttonPause);
        textEpisodeTitle = getView().findViewById(R.id.textEpisodeTitle);
        progressBar = getView(). findViewById(R.id.progressBar);

        textEpisodeTitle.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ActivityMediaPlayer.class);
            startActivity(intent);
        });

        buttonPlay.setOnClickListener(v ->
                EventBus.getDefault().post(new MessageEvent.PauseOrResume()));

        buttonPause.setOnClickListener(v ->
                EventBus.getDefault().post(new MessageEvent.PauseOrResume()));
    }
}