package com.guille.poddy.services;

import com.tonyodev.fetch2.FetchListener;
import com.tonyodev.fetch2.AbstractFetchListener;
import com.tonyodev.fetch2.Fetch;

import com.tonyodev.fetch2.Download;
import com.tonyodev.fetch2.Status;
import android.widget.Toast;
import com.tonyodev.fetch2.Request;

import com.tonyodev.fetch2.Priority;
import com.tonyodev.fetch2.NetworkType;
import com.tonyodev.fetch2.FetchConfiguration;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationCompat.Action;
import android.app.*;

import com.guille.poddy.database.*;
import com.guille.poddy.*;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.os.*;
import android.content.*;

import org.jetbrains.annotations.NotNull;

import java.io.File;

import android.media.MediaMetadataRetriever;
import android.util.Log;

import java.util.*;

import android.webkit.MimeTypeMap;
import com.guille.poddy.eventbus.*;
import com.guille.poddy.*;
import org.greenrobot.eventbus.*;

public class DownloaderService extends Service {
    // HashMap that points a download id to an object that contains episode info
//    private HashMap<Long, DownloadingEpisode> downloading = new HashMap<Long, DownloadingEpisode>();
    private Fetch fetch;

    private void downloadEpisode(Episode ep, Podcast pod) {
        // Do not queue it if already downloading
        fetch.getDownloadsWithStatus(Arrays.asList(
                Status.DOWNLOADING, Status.QUEUED, Status.NONE, Status.PAUSED, Status.ADDED), downloads -> {
            for (Download download : downloads) {
                if (ep.id == Long.parseLong(download.getTag()))
                    return;
            }
        });

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

        final int dataUsage = Preferences.getDataUsage(this);

        Request request = new Request(ep.enclosureUrl, file);
        request.addHeader("clientKey", "SD78DF93_3947&MVNGHE1WONG");
        request.setTag(Long.toString(ep.id));

        request.setPriority(Priority.HIGH);
        switch (dataUsage) {
            case Preferences.DATA_USAGE_ONLY_WIFI:
                request.setNetworkType(NetworkType.WIFI_ONLY);
            case Preferences.DATA_USAGE_DATA_AND_WIFI:
                request.setNetworkType(NetworkType.ALL);
            default:
                request.setNetworkType(NetworkType.ALL);
        }

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
        final int downloadLimit = Preferences.getDownloadLimit(this);

        FetchConfiguration fetchConfiguration = new FetchConfiguration.Builder(this)
                .setDownloadConcurrentLimit(downloadLimit)
                .build();
        fetch = Fetch.Impl.getInstance(fetchConfiguration);

        fetch.addListener(fetchListener);
        fetch.cancelAll();

        return super.onStartCommand(intent, flags, startId);
    }

    private void sendRefreshEpisodeBroadcast(Boolean tryToClose) {
        if (fetch.isClosed()) return;
        // Send download update broadcast
        fetch.getDownloadsWithStatus(Arrays.asList(
                Status.DOWNLOADING, Status.QUEUED, Status.NONE, Status.PAUSED, Status.ADDED), downloads -> {

            EventBus.getDefault().postSticky(new MessageEvent.EpisodesBeingDownloaded(downloads));

            if (downloads.isEmpty()) {
                if (tryToClose) {
                    Log.i("DownloaderService", "Stopping");
                    DownloaderService.this.stopSelf();
                }
            } else {
                // Update notification
                int totalProgress = 0;
                for (Download download : downloads) {
                    totalProgress += download.getProgress();
                }
                notificationBuilder.setProgress(100, totalProgress/downloads.size(), false);
                if (downloads.size() == 1)
                    notificationBuilder.setContentTitle("Downloading " + downloads.size() + " file...");
                else
                    notificationBuilder.setContentTitle("Downloading " + downloads.size() + " files...");
                updateNotification();
            }


        });
    }
    private void sendRefreshEpisodeBroadcast() {
        sendRefreshEpisodeBroadcast(false);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onReceiveDownloadEpisode(MessageEvent.DownloadEpisode event) {
        downloadEpisode(event.episode, event.podcast);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onReceiveCancelDownload(MessageEvent.CancelDownload event) {
        fetch.delete(event.download.getId());
    }

    @Override
    public void onCreate() {
        super.onCreate();
        EventBus.getDefault().register(this);

        createNotificationChannel();
        buildBaseNotification();

        updateNotification();
    }

    @Override
    public void onDestroy() {
        cancelNotification();

        EventBus.getDefault().unregister(this);
        if (!fetch.isClosed()) {
            fetch.cancelAll();
            fetch.close();
        }
        super.onDestroy();
    }

    // NOTIFICATION

    public static final String CHANNEL_ID = "com.guille.poddy.services.DownloaderService.Notif";
    public static final int NOTIFICATION_ID = 109;
    private NotificationCompat.Builder notificationBuilder;

    private void buildBaseNotification() {
        notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Downloads")
                .setSmallIcon(R.drawable.ic_poddy)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    private void updateNotification() {
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.notify(NOTIFICATION_ID, notificationBuilder.build());
    }

    private void cancelNotification() {
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.cancel(NOTIFICATION_ID);
    }


    private final FetchListener fetchListener = new AbstractFetchListener() {
        @Override
        public void onQueued(@NotNull Download download, boolean waitingOnNetwork) {
            Log.d("DownloaderService", "Download queued");
            sendRefreshEpisodeBroadcast();
        }
        @Override
        public void onError(@NotNull Download download, @org.jetbrains.annotations.NotNull com.tonyodev.fetch2.Error error, @org.jetbrains.annotations.Nullable Throwable throwable) {
            super.onError(download, error, throwable);
            Log.i("DownloaderService", "Error in download");
            Toast.makeText(DownloaderService.this,
                    "Error downloading episode",
                    Toast.LENGTH_SHORT).show();
            sendRefreshEpisodeBroadcast(true);
        }
        @Override
        public void onPaused(@NotNull Download download) {
            Log.d("DownloaderService", "Download paused");
            sendRefreshEpisodeBroadcast();
        }
        @Override
        public void onResumed(@NotNull Download download) {
            Log.d("DownloaderService", "Download resumed");
            sendRefreshEpisodeBroadcast();
        }
        @Override
        public void onAdded(@org.jetbrains.annotations.NotNull Download download) {
            Log.d("DownloaderService", "Download added");
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
            dbh.updateEpisodeDuration(episodeId, Long.parseLong(duration));

            // Refresh UI to show downloaded episode
            EventBus.getDefault().post(new MessageEvent.RefreshEpisode(episodeId));
            // Since we already refreshed the episode, send a RefreshPodcast with empty string
            EventBus.getDefault().post(new MessageEvent.RefreshPodcast(""));

            Toast.makeText(DownloaderService.this,
                    "Download completed",
                    Toast.LENGTH_SHORT).show();
            Log.i("DownloaderService", "Download completed");
            sendRefreshEpisodeBroadcast(true);
        }
        @Override
        public void onProgress(@org.jetbrains.annotations.NotNull Download download, long etaInMilliseconds, long downloadedBytesPerSecond) {
            Log.d("DownloaderService", "Download progress");
            sendRefreshEpisodeBroadcast();
        }
        @Override
        public void onCancelled(@NotNull Download download) {
            Log.d("DownloaderService", "Download cancelled");
            sendRefreshEpisodeBroadcast(true);
        }
        @Override
        public void onRemoved(@NotNull Download download) {
            Log.d("DownloaderService", "Download removed");
            sendRefreshEpisodeBroadcast(true);
        }
        @Override
        public void onDeleted(@NotNull Download download) {
            Log.d("DownloaderService", "Download deleted");
            Toast.makeText(DownloaderService.this,
                    "Download cancelled",
                    Toast.LENGTH_SHORT).show();
            sendRefreshEpisodeBroadcast(true);
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

    public static boolean isServiceRunning(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (DownloaderService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public static void download(Context context, Episode ep, Podcast pod) {
        if (!isServiceRunning(context)) {
            // Start the service and play the audio
            ServiceConnection serviceConnection = new ServiceConnection() {
                @Override
                public void onServiceDisconnected(ComponentName name) {}
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    DownloaderService.LocalBinder binder = (DownloaderService.LocalBinder) service;
                    binder.getService().downloadEpisode(ep, pod);
                    context.unbindService(this);
                }
            };

            Intent intent = new Intent(context, DownloaderService.class);
            context.startService(intent);
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

        } else {
            // Send a Bus with the ep and pod
            EventBus.getDefault().post(new MessageEvent.DownloadEpisode(ep, pod));
        }
    }
}
