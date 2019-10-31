package com.guille.poddy.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.ProgressBar;

import android.content.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.guille.poddy.R;
import com.guille.poddy.activities.*;
import com.guille.poddy.services.*;
import com.guille.poddy.database.*;
import com.guille.poddy.Broadcast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.jetbrains.annotations.NotNull;


public class FragmentMediaPlayerBar extends Fragment {
    private ImageButton imagePlayButton;
    private TextView textEpisodeTitle;
    private ProgressBar progressBar;

    private Episode ep;
    private Podcast pod;
    private int status, currentPosition, duration;

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

            final int progress = Math.round(((float) currentPosition / (float) duration) * 100);

//            if (textEpisodeTitle.getText().toString() != ep.title)
            textEpisodeTitle.setText(ep.title);
            progressBar.setProgress(progress);

            if (status == MediaPlayerService.PAUSED) {
                imagePlayButton.setImageResource(R.drawable.ic_play_arrow_black_24dp);
            } else {
                imagePlayButton.setImageResource(R.drawable.ic_pause_black_24dp);
            }

        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    public FragmentMediaPlayerBar() {
        Bundle bundle = new Bundle();
        this.setArguments(bundle);
    }

    private void requestRefreshMediaPlayer() {
        Intent intent = new Intent(Broadcast.REQUEST_REFRESH_MEDIAPLAYER);
        final LocalBroadcastManager bm = LocalBroadcastManager.getInstance(getActivity().getApplicationContext());
        bm.sendBroadcast(intent);
    }

    @Override
    public void onStart() {
        super.onStart();
        requestRefreshMediaPlayer();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set receiver for updating the bar
        IntentFilter filter = new IntentFilter(Broadcast.REFRESH_MEDIAPLAYER);

        final LocalBroadcastManager bm = LocalBroadcastManager.getInstance(getActivity().getApplicationContext());
        bm.registerReceiver(getMusicPlayerInfo, filter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        final LocalBroadcastManager bm = LocalBroadcastManager.getInstance(getActivity().getApplicationContext());
        bm.unregisterReceiver(getMusicPlayerInfo);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mediaplayerbar, container, false);
    }

    @Override
    public void onViewCreated(@NotNull View view, @Nullable Bundle savedInstanceState) {
        imagePlayButton = getView().findViewById(R.id.imagePlayButton);
        textEpisodeTitle = getView().findViewById(R.id.textEpisodeTitle);
        progressBar = getView(). findViewById(R.id.progressBar);

        textEpisodeTitle.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ActivityMediaPlayer.class);
            startActivity(intent);
        });

        imagePlayButton.setOnClickListener(v -> MediaPlayerBridge.pauseOrResumeAudio(FragmentMediaPlayerBar.this.getActivity().getApplication()));

    }


}