package com.guille.poddy.fragments.recyclerviews;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.io.File;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.guille.poddy.R;
import com.guille.poddy.database.*;
import com.guille.poddy.activities.*;
import com.guille.poddy.fragments.FragmentAbstract;
import com.guille.poddy.fragments.FragmentFactory;
import com.guille.poddy.services.*;
import com.guille.poddy.*;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.DiffUtil.*;

import android.os.*;
import android.content.*;

import com.tonyodev.fetch2.*;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import com.guille.poddy.eventbus.*;
import org.greenrobot.eventbus.*;
import android.os.AsyncTask;


@SuppressWarnings("ALL")
public class FragmentEpisodes extends FragmentAbstract implements RecyclerViewEpisodesAbstract.ItemClickListener {
    private RecyclerView recyclerView;
    private RecyclerViewEpisodesAbstract adapter;

    private int kind;
    private boolean showingOnePodcast;
    private Podcast podcast;
    private List<Episode> episodes;

    private long listeningEpisodeId;
    private int listeningStatus;

    public static final String UPDATE_ALL = "ALL";
    public static final String UPDATE_STATUS = "LISTENING_STATUS";
    public static final String UPDATE_PROGRESS = "DOWNLOAD_PROGRESS";

    private List<Download> downloads = new ArrayList<>();

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onReceiveRefreshPodcast(MessageEvent.RefreshPodcast event) {
        // Tries to reload IF we are showing a view of multiple podcasts
        // or if we are only showing 1 podcast and we happen to refresh that one
        if (!showingOnePodcast || (showingOnePodcast && event.url.equals(podcast.url))) {
            // Threading this because it might be expensive/be executed several times in a row
            AsyncTask load = new AsyncTask<Void, Void, Boolean>() {
                @Override
                protected Boolean doInBackground(Void... voids) {
                    // Load episodes from the database
                    loadEpisodes();
                    return true;
                }
                @Override
                protected void onPostExecute(Boolean result) {
                    // Check differences between new eps/shown eps and refresh RecyclerView
                    updateEpisodes();
                }
            }.execute();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onReceiveRefreshEpisode(MessageEvent.RefreshEpisode event) {
        for (int i=0; i<episodes.size(); i++) {
            if (episodes.get(i).id == event.episodeId) {
                refreshEpisode(i, UPDATE_STATUS, true);
                break;
            }
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onReceiveEpisodesBeingDownloaded(MessageEvent.EpisodesBeingDownloaded event) {
        List<Download> previous = new ArrayList<> (downloads);
        downloads = event.downloads;
        adapter.setDownloadingList(downloads);

        for (int i=0; i<episodes.size(); i++) {
            final boolean now = (Helpers.isBeingDownloaded(downloads, episodes.get(i).id) != null);
            final boolean before = (Helpers.isBeingDownloaded(previous, episodes.get(i).id) != null);
            if (now || before) {
                refreshEpisode(i, UPDATE_PROGRESS, false);
                // If it was being downloaded before but not now
                if (now && !before) {
                    refreshEpisode(i, UPDATE_STATUS, false);
                }
                if (!now && before) {
                    refreshEpisode(i, UPDATE_STATUS, true);
                }
            }
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onReceiveAudioBeingPlayedStatus(MessageEvent.AudioBeingPlayedStatus event) {
        for (int i=0; i<episodes.size(); i++) {
            if (event.episodeId == episodes.get(i).id) {
                refreshEpisode(i, UPDATE_STATUS, false);
                break;
            }
        }
        adapter.setListeningStatus(event.episodeId, event.status);
    }

    private void refreshEpisode(int position, String payload, Boolean reloadDatabase) {
        if (reloadDatabase) {
            final DatabaseHelper dbh = DatabaseHelper.getInstance(getActivity().getApplicationContext());
            Episode reloadedEpisode = dbh.getEpisodeFromId(episodes.get(position).id);

            episodes.set(position, reloadedEpisode);
            adapter.setEpisode(position, reloadedEpisode);
        }
        adapter.notifyItemChanged(position, payload);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_episodes, container, false);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        // Get the settings for what kind of content we are going to display
        kind = bundle.getInt("kind");
        if (kind == FragmentFactory.ContentShown.PODCAST_EPISODES) {
            podcast = bundle.getParcelable("podcast");
            showingOnePodcast = true;
        }
        loadEpisodes();
    }

    @Override
    public void onViewCreated(@NotNull View view, @Nullable Bundle savedInstanceState) {
        // Simple or complex? (simple only shown in PODCAST_EPISODES)
        switch (kind) {
            case FragmentFactory.ContentShown.PODCAST_EPISODES:
                adapter = new RecyclerViewEpisodesSimple(getActivity());
                break;
            default:
                adapter = new RecyclerViewEpisodesComplex(getActivity());
        }
        adapter.setItems(episodes);
        adapter.setClickListener(this);

        // Set up the RecyclerView
        recyclerView = getView().findViewById(R.id.episodeList);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(20);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(adapter);
    }

    private void loadEpisodes() {
        final DatabaseHelper dbh = DatabaseHelper.getInstance(getActivity().getApplicationContext());
        // Depending on the arguments we get all episodes of a podcast or just this week's
        switch (kind) {
            case FragmentFactory.ContentShown.PODCAST_EPISODES:
                episodes = dbh.getEpisodesFromPodcast(podcast.id);
                break;
            case FragmentFactory.ContentShown.THIS_WEEK:
                episodes = dbh.getEpisodesFromThisWeek();
                break;
            case FragmentFactory.ContentShown.DOWNLOADED:
                episodes = dbh.getDownloadedEpisodes();
                break;
            case FragmentFactory.ContentShown.ALL_EPISODES:
                episodes = dbh.getAllEpisodes();
                break;
        }
    }

    private void updateEpisodes() {
        List<Episode> oldEpisodes = adapter.getItems();
        DiffResult result = DiffUtil.calculateDiff(new EpisodeComparator(oldEpisodes, episodes));
        adapter.setItems(episodes);
        result.dispatchUpdatesTo(adapter);
    }

    public static class EpisodeComparator extends Callback {
        List<Episode> oldEpisodes;
        List<Episode> newEpisodes;

        public EpisodeComparator(List<Episode> o, List<Episode> n) {
            oldEpisodes = o;
            newEpisodes = n;
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            final Episode oldEp = oldEpisodes.get(oldItemPosition);
            final Episode newEp = newEpisodes.get(newItemPosition);

            if (oldEp.id != newEp.id) return false;
            if (oldEp.podcastId != newEp.podcastId) return false;
            if (oldEp.downloaded != newEp.downloaded) return false;
            if (oldEp.position != newEp.position) return false;

            if (!oldEp.title.equals(newEp.title)) return false;
            if (!oldEp.file.equals(newEp.file)) return false;
            if (!oldEp.description.equals(newEp.description)) return false;
            if (!oldEp.date.equals(newEp.date)) return false;
            if (!oldEp.enclosureUrl.equals(newEp.enclosureUrl)) return false;
            if (oldEp.duration != newEp.duration) return false;
            return oldEp.podcastTitle.equals(newEp.podcastTitle);
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            final Episode oldEp = oldEpisodes.get(oldItemPosition);
            final Episode newEp = newEpisodes.get(newItemPosition);
            return oldEp.id == newEp.id;
        }

        @Override
        public int getNewListSize() {
            return newEpisodes.size();
        }

        @Override
        public int getOldListSize() {
            return oldEpisodes.size();
        }
    }

    // Click listeners

    @Override
    public void onDownloadClick(View view, int position) {
        Episode episode = episodes.get(position);
        final DatabaseHelper dbh = DatabaseHelper.getInstance(getActivity().getApplicationContext());
        podcast = dbh.getPodcastFromId(episode.podcastId);

        final Download download = Helpers.isBeingDownloaded(downloads, episode.id);
        if (download == null) {
            DownloaderService.download(getActivity().getApplicationContext(), episode, podcast);
        }
    }

    @Override
    public void onCancelClick(View view, int position) {
        Episode episode = episodes.get(position);
        final DatabaseHelper dbh = DatabaseHelper.getInstance(getActivity().getApplicationContext());
        podcast = dbh.getPodcastFromId(episode.podcastId);

        final Download download = Helpers.isBeingDownloaded(downloads, episode.id);
        if (download != null) {
            EventBus.getDefault().post(new MessageEvent.CancelDownload(download));
        }
    }

    @Override
    public void onPlayClick(View view, int position) {
        EventBus.getDefault().post(new MessageEvent.PauseOrResume());
    }

    @Override
    public void onPauseClick(View view, int position) {
        EventBus.getDefault().post(new MessageEvent.PauseOrResume());
    }

    @Override
    public void onStartPlayClick(View view, int position) {
        Episode episode = episodes.get(position);
        final DatabaseHelper dbh = DatabaseHelper.getInstance(getActivity().getApplicationContext());
        podcast = dbh.getPodcastFromId(episode.podcastId);

        if (episode.downloaded) {
            if (new File(episode.file).exists()) {
                MediaPlayerService.coldPlayAudio(getActivity(), episode.id);
            } else {
                // File doesn't exist, so set DOWNLOADED as false and refresh that episode
                if (dbh.updateEpisodeDownloadedStatus(episode.id, false, "")) {
                    refreshEpisode(position, UPDATE_STATUS, true);
                }
            }
        }
    }

    @Override
    public void onItemClick(View view, int position) {
        final DatabaseHelper dbh = DatabaseHelper.getInstance(getActivity().getApplicationContext());
        final Episode ep = episodes.get(position);
        final Podcast pod = dbh.getPodcastFromId(ep.podcastId);

        Intent intent = new Intent(getActivity(), ActivityEpisodeInfo.class);
        intent.putExtra("episode", ep);
        intent.putExtra("podcast", pod);
        startActivity(intent);
    }
}