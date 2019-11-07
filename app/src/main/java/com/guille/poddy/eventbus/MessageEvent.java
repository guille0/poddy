package com.guille.poddy.eventbus;

import com.guille.poddy.database.*;
import com.tonyodev.fetch2.*;
import java.util.*;

public class MessageEvent {

    // Sent to FeedUpdaterService (not sticky)
    public static class UpdateFeeds {
        public String [] urls;
        public UpdateFeeds(String [] feeds) {
            urls = feeds;
        }
    }

    // Sent by FeedUpdaterService (not sticky)
    public static class RefreshPodcast {
        public String url;
        public RefreshPodcast(String u) {
            url = u;
        }
    }

    // Sent to DownloaderService (not sticky)
    public static class DownloadEpisode {
        public Episode episode;
        public Podcast podcast;

        public DownloadEpisode(Episode ep, Podcast pod) {
            episode = ep;
            podcast = pod;
        }
    }
    public static class CancelDownload {
        public Download download;

        public CancelDownload(Download dl) {
            download = dl;
        }
    }

    // Sent by DownloaderService (sticky)
    public static class EpisodesBeingDownloaded {
        public List<Download> downloads;

        public EpisodesBeingDownloaded(List<Download> dls){
            downloads = dls;
        }
    }

    // (non-sticky)
    public static class RefreshEpisode {
        public long episodeId;

        public RefreshEpisode(long id) {
            episodeId = id;
        }
    }

    // Sent to MediaPlayerService (not sticky)
//    EventBus.getDefault().post(new MessageEvent.SeekTo(position));
//    EventBus.getDefault().post(new MessageEvent.PauseOrResume());
//    EventBus.getDefault().post(new MessageEvent.FastForward());
//    EventBus.getDefault().post(new MessageEvent.Rewind());
//    EventBus.getDefault().post(new MessageEvent.Stop());

    public static class PlayNewAudio {
        public long episodeId;
        public PlayNewAudio(long id) {
            episodeId = id;
        }
    }
    public static class SeekTo {
        public int position;
        public SeekTo(int pos) {
            position = pos;
        }
    }

    public static class PauseOrResume {}
    public static class FastForward {}
    public static class Rewind {}
    public static class Stop {}

    // Sent by MediaPlayerService (sticky)
    public static class AudioBeingPlayedEpisode {
        // Sent as sticky on play new episode
        public Episode episode;

        public AudioBeingPlayedEpisode(Episode ep) {
            episode = ep;
        }
    }

    public static class AudioBeingPlayedStatus {
        // Sent as sticky on play/pause/stop
        public long episodeId;
        public int status;

        public AudioBeingPlayedStatus(long id, int st) {
            episodeId = id;
            status = st;
        }
    }

    public static class AudioBeingPlayedPosition {
        // Sent as sticky on tick (e.g. 250 ms)
        public long episodeId;
        public long currentPosition;
        public long duration;

        public AudioBeingPlayedPosition(long id, long pos, long dur) {
            episodeId = id;
            currentPosition = pos;
            duration = dur;
        }
    }
}
