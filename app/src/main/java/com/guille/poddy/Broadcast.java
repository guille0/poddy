package com.guille.poddy;

import android.content.*;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class Broadcast {
    public static void sendToast(Context context, String text) {
        Intent intent = new Intent(Broadcast.SEND_TOAST);
        intent.putExtra("text", text);
        final LocalBroadcastManager bm = LocalBroadcastManager.getInstance(context);
        bm.sendBroadcast(intent);
    }

    public static void refreshPodcast(Context context, String url) {
        Intent intent = new Intent(Broadcast.REFRESH_PODCASTS);
        intent.putExtra("podcastUrl", url);
        final LocalBroadcastManager bm = LocalBroadcastManager.getInstance(context);
        bm.sendBroadcast(intent);
    }

    // ActivityAbstract (GLOBAL UI ELEMENTS)
        // Sends a toast with text "text"
        public static final String SEND_TOAST = "Global.SendToast";
        // Refreshes the download progress of episodes
        public static final String REFRESH_EPISODES = "Global.RefreshEpisodes";
        // Refreshes activity/fragment if intent's "podcastUrl" == Activity.getShownPodcast();
        public static final String REFRESH_PODCASTS = "Global.RefreshPodcasts";


    // DownloaderService
        public static final String DOWNLOAD = "Downloader.Download";
        public static final String CANCEL_DOWNLOAD = "Downloader.CancelDownload";
        public static final String REQUEST_REFRESH_EPISODE = "Downloader.RequestRefreshEpisode";

    // FeedUpdaterService
        public static final String UPDATE_FEEDS = "FeedUpdater.UpdateFeeds";

    // MediaPlayerService
        // Controls sent to the service
        public static final String PLAY_NEW_AUDIO = "MediaPlayer.PlayNewAudio";
        public static final String PAUSE_OR_RESUME = "MediaPlayer.PauseOrResume";
        public static final String FAST_FORWARD = "MediaPlayer.FastForward";
        public static final String REWIND = "MediaPlayer.Rewind";
        public static final String STOP = "MediaPlayer.Stop";
        public static final String SEEK = "MediaPlayer.Seek";

        // Service sends broadcast to the media player fragment and refreshes its position, status
        public static final String REFRESH_MEDIAPLAYER = "MediaPlayer.RefreshMediaPlayer";
        public static final String REQUEST_REFRESH_MEDIAPLAYER = "MediaPlayer.RequestRefreshMediaPlayer";

}
