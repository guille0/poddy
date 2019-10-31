package com.guille.poddy.fragments;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.io.File;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.guille.poddy.R;
import com.guille.poddy.database.*;
import com.guille.poddy.activities.*;
import com.guille.poddy.services.*;
import com.guille.poddy.*;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.os.*;
import android.content.*;

import com.tonyodev.fetch2.*;

import org.jetbrains.annotations.NotNull;

import java.util.*;


public class FragmentEpisodes extends Fragment implements RecyclerViewEpisodes.ItemClickListener{
    private RecyclerView recyclerView;
    private RecyclerViewEpisodes adapter;

    private String kind;
    private Podcast podcast;
    private List<Episode> episodes;

    private List<Download> downloading = new ArrayList<>();

    public FragmentEpisodes() {

    }

    // Broadcast listener for getting updates on downloads
    private final BroadcastReceiver downloadUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                List<Download> total = new ArrayList<>(downloading);
                downloading = (List<Download>) intent.getSerializableExtra("downloading");
                total.addAll(downloading);

                adapter.setDownloadingList(downloading);
                for (int i=0; i<episodes.size(); i++) {
                    final Download download = Helpers.isBeingDownloaded(total, episodes.get(i).id);
                    if (download != null) {
                        if (Helpers.isBeingDownloaded(downloading, episodes.get(i).id) == null) {
                            refreshEpisode(i, true);
                        } else {
                            refreshEpisode(i, false);
                        }
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
        // set broadcast receivers
        final LocalBroadcastManager bm = LocalBroadcastManager.getInstance(getActivity().getApplicationContext());
        bm.registerReceiver(downloadUpdateReceiver, new IntentFilter(Broadcast.REFRESH_EPISODES));

        requestRefreshEpisode();
    }

    private void refreshEpisode(int position, Boolean reloadDatabase) {
        if (reloadDatabase) {
            final DatabaseHelper dbh = DatabaseHelper.getInstance(getActivity().getApplicationContext());
            Episode reloadedEpisode = dbh.getEpisodeFromId(episodes.get(position).id);

            episodes.set(position, reloadedEpisode);
            adapter.refreshEpisode(position, reloadedEpisode);
        }
        adapter.notifyItemChanged(position);
    }

    private void requestRefreshEpisode() {
        Intent intent = new Intent(Broadcast.REQUEST_REFRESH_EPISODE);
        final LocalBroadcastManager bm = LocalBroadcastManager.getInstance(getActivity().getApplicationContext());
        bm.sendBroadcast(intent);
    }

    @Override
    public void onStop() {
        super.onStop();
        // remove broadcast receivers
        final LocalBroadcastManager bm = LocalBroadcastManager.getInstance(getActivity().getApplicationContext());
        bm.unregisterReceiver(downloadUpdateReceiver);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_episodes, container, false);
    }

    @Override
    public void onViewCreated(@NotNull View view, @Nullable Bundle savedInstanceState) {
        DatabaseHelper databaseHelper = DatabaseHelper.getInstance(getActivity().getApplicationContext());

        Bundle bundle = getArguments();
        // Get the settings for what kind of content we are going to display
        kind = bundle.getString("kind");
        if (kind.equals("podcastEpisodes")) {
            podcast = bundle.getParcelable("podcast");
        }

        // Depending on the arguments we get all episodes of a podcast or just this week's
        // TODO do an ENUM for this
        switch (kind) {
            case "podcastEpisodes":
                episodes = databaseHelper.getEpisodesFromPodcast(podcast.id);
                break;
            case "thisWeek":
                episodes = databaseHelper.getEpisodesFromThisWeek();
                break;
            case "downloaded":
                episodes = databaseHelper.getDownloadedEpisodes();
                break;
            case "all":
                episodes = databaseHelper.getAllEpisodes();
                break;
        }

        // set up the RecyclerView
        recyclerView = getView().findViewById(R.id.episodeList);

        recyclerView.setHasFixedSize(false);

        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        adapter = new RecyclerViewEpisodes(getActivity(), episodes);

        adapter.setClickListener(this);

        recyclerView.setAdapter(adapter);

    }

    @Override
    public void onImageClick(View view, int position) {
        final DatabaseHelper dbh = DatabaseHelper.getInstance(getActivity().getApplicationContext());
        Episode ep = dbh.getEpisodeFromId(episodes.get(position).id);
        Podcast pod = dbh.getPodcastFromEpisode(ep.id);

        if (ep.downloaded) {
            if (new File(ep.file).exists()) {
                MediaPlayerBridge.playAudio(getActivity().getApplicationContext(), ep, pod);
            } else {
                // File doesn't exist, so set DOWNLOADED as false and refresh that episode
                if (dbh.updateEpisodeDownloadedStatus(ep.id, false, "")) {
                    adapter.changeEpisodeDownloaded(position, false);
                    adapter.notifyItemChanged(position);
                }
            }
        } else {
            final Download download = Helpers.isBeingDownloaded(this.downloading, ep.id);
            if (download == null) {
                DownloaderBridge.downloadEpisode(getActivity().getApplicationContext(), ep, pod);
            } else {
                DownloaderBridge.cancelDownload(getActivity().getApplicationContext(), download);
            }
        }
    }

    @Override
    public void onItemClick(View view, int position) {
        final DatabaseHelper dbh = DatabaseHelper.getInstance(getActivity().getApplicationContext());
        final Episode ep = episodes.get(position);
        final Podcast pod = dbh.getPodcastFromEpisode(ep.id);

        Intent intent = new Intent(getActivity(), ActivityEpisodeInfo.class);
        intent.putExtra("episode", ep);
        intent.putExtra("podcast", pod);
        startActivity(intent);
    }
}