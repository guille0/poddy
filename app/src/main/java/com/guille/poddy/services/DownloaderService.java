package com.guille.poddy.services;

import com.tonyodev.fetch2.*;

import android.app.*;

import com.guille.poddy.database.*;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.os.*;
import android.content.*;

import org.jetbrains.annotations.NotNull;

import java.io.File;

import android.media.MediaMetadataRetriever;
import android.util.Log;

import java.util.*;

import com.guille.poddy.Broadcast;

import android.webkit.MimeTypeMap;

public class DownloaderService extends Service {
    // HashMap that points a download id to an object that contains episode info
//    private HashMap<Long, DownloadingEpisode> downloading = new HashMap<Long, DownloadingEpisode>();
    private Fetch fetch;
    private final int downloadLimit = 3;

    private void downloadEpisode(Episode ep, Podcast pod) {
        // Do not queue it if already downloading
//        if (downloading.containsKey(ep.id)) return;

        final String directory = pod.directory;
        final String fileExtension = MimeTypeMap.getFileExtensionFromUrl(ep.enclosureUrl);

        // Replace problematic characters from filename
        String title = ep.title.replaceAll("[\\\\/:*?\"<>+|]", "");
        if (title.equals("")) title = "episode";

        // Check if a file with that name exists. If so, add a number to it
        String file;
        int count = 0;
        do {
            if (count==0)
                file = directory + title + "." + fileExtension;
            else
                file = directory + title + "-" + count + "." + fileExtension;
            count++;
        } while (new File(file).exists());

        File checkFile = new File(file);
        if(checkFile.exists()) {
            checkFile.delete();
        }

        Request request = new Request(ep.enclosureUrl, file);
        request.setPriority(Priority.HIGH);
        request.setNetworkType(NetworkType.ALL);
        request.addHeader("clientKey", "SD78DF93_3947&MVNGHE1WONG");
        request.setTag(Long.toString(ep.id));

//        downloadListPut(ep.id, new DownloadingEpisode(ep, pod, 0, request.getId()));

        fetch.enqueue(request, updatedRequest -> {
        }, error -> {
            // Could not start download
            Log.i("fetch", "cant download");
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("DownloaderService", "Started DownloaderService");
        // Configuration
        FetchConfiguration fetchConfiguration = new FetchConfiguration.Builder(this)
                .setDownloadConcurrentLimit(downloadLimit)
                .build();
        fetch = Fetch.Impl.getInstance(fetchConfiguration);

        fetch.addListener(fetchListener);

        // Download the ep
        Episode episode = intent.getExtras().getParcelable("episode");
        Podcast podcast = intent.getExtras().getParcelable("podcast");
        downloadEpisode(episode, podcast);

        return super.onStartCommand(intent, flags, startId);
    }


    private void sendRefreshEpisodeBroadcast() {
        try {
        // Send download update broadcast
        fetch.getDownloadsWithStatus(Arrays.asList(Status.DOWNLOADING, Status.QUEUED, Status.NONE, Status.PAUSED), downloads -> {
            Intent intent = new Intent(Broadcast.REFRESH_EPISODES);
            intent.putExtra("downloading", (ArrayList<Download>) downloads);
            final LocalBroadcastManager bm = LocalBroadcastManager.getInstance(getApplicationContext());
            bm.sendBroadcast(intent);
        });
        } catch (Exception e) {
            Log.e("DownloaderService", "Could not send Broadcast.REFRESH_EPISODE");
        }
    }

    private final BroadcastReceiver requestRefreshEpisodeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            sendRefreshEpisodeBroadcast();
        }
    };

    private final BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Episode episode = intent.getExtras().getParcelable("episode");
            Podcast podcast = intent.getExtras().getParcelable("podcast");
            // This checks if it's already downloading so it won't do it twice
            downloadEpisode(episode, podcast);
        }
    };

    private final BroadcastReceiver cancelReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final Download download = intent.getExtras().getParcelable("download");
            fetch.delete(download.getId());
        }
    };


    @Override
    public void onCreate() {
        super.onCreate();
        final LocalBroadcastManager bm = LocalBroadcastManager.getInstance(getApplicationContext());
        bm.registerReceiver(downloadReceiver, new IntentFilter(Broadcast.DOWNLOAD));
        bm.registerReceiver(cancelReceiver, new IntentFilter(Broadcast.CANCEL_DOWNLOAD));
        bm.registerReceiver(requestRefreshEpisodeReceiver, new IntentFilter(Broadcast.REQUEST_REFRESH_EPISODE));
    }

    @Override
    public void onDestroy() {
        Log.i("asdasd", "destroyed");
        final LocalBroadcastManager bm = LocalBroadcastManager.getInstance(getApplicationContext());
        bm.unregisterReceiver(downloadReceiver);
        bm.unregisterReceiver(cancelReceiver);
        bm.unregisterReceiver(requestRefreshEpisodeReceiver);
        fetch.close();
        super.onDestroy();
    }

    private final FetchListener fetchListener = new AbstractFetchListener() {
        @Override
        public void onQueued(@NotNull Download download, boolean waitingOnNetwork) {
            sendRefreshEpisodeBroadcast();
        }
        @Override
        public void onError(@NotNull Download download, @org.jetbrains.annotations.NotNull com.tonyodev.fetch2.Error error, @org.jetbrains.annotations.Nullable Throwable throwable) {
            super.onError(download, error, throwable);
            sendRefreshEpisodeBroadcast();
            Broadcast.sendToast(DownloaderService.this, "Error downloading episode");
        }
        @Override
        public void onPaused(@NotNull Download download) {
            sendRefreshEpisodeBroadcast();
        }
        @Override
        public void onResumed(@NotNull Download download) {
            sendRefreshEpisodeBroadcast();
        }
        @Override
        public void onAdded(@org.jetbrains.annotations.NotNull Download download) {
            sendRefreshEpisodeBroadcast();
        }
        @Override
        public void onCompleted(@org.jetbrains.annotations.NotNull Download download) {
            // Save the episode back to the database with the filename and downloaded = true
            final DatabaseHelper dbh = DatabaseHelper.getInstance(getApplicationContext());
            final long episodeId = Long.parseLong(download.getTag());
            dbh.updateEpisodeDownloadedStatus(episodeId, true, download.getFile());

            // Get the Duration of the episode and save it
            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            mmr.setDataSource(download.getFile());
            final String duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            mmr.release();
            dbh.updateEpisodeDuration(episodeId, duration);

            // Refresh UI to show downloaded episode
            final Podcast podcast = dbh.getPodcastFromEpisode(episodeId);
            Broadcast.refreshPodcast(DownloaderService.this, podcast.url);

            Broadcast.sendToast(DownloaderService.this, "Episode downloaded");
        }
        @Override
        public void onProgress(@org.jetbrains.annotations.NotNull Download download, long etaInMilliseconds, long downloadedBytesPerSecond) {
            sendRefreshEpisodeBroadcast();
        }
        @Override
        public void onCancelled(@NotNull Download download) {
            sendRefreshEpisodeBroadcast();
        }
        @Override
        public void onRemoved(@NotNull Download download) {
            sendRefreshEpisodeBroadcast();
        }
        @Override
        public void onDeleted(@NotNull Download download) {
            Broadcast.sendToast(DownloaderService.this, "Download cancelled");
            sendRefreshEpisodeBroadcast();
        }
    };

    // Binder given to clients
    private final IBinder iBinder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return iBinder;
    }

    class LocalBinder extends Binder {
        public DownloaderService getService() {
            return DownloaderService.this;
        }
    }

}
