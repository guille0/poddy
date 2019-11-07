package com.guille.poddy.services;

import android.util.*;

import com.guille.poddy.*;
import com.guille.poddy.database.*;
import com.guille.poddy.Helpers.ResponseToStringTask;

import java.nio.charset.StandardCharsets;
import android.os.AsyncTask;

import java.io.*;
import java.net.*;
import java.util.*;
import java.text.*;
import android.net.*;
import android.graphics.*;
import android.media.*;
import android.app.*;
import android.os.*;
import android.content.*;
import com.guille.poddy.eventbus.*;
import org.greenrobot.eventbus.*;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.*;
import java.util.*;
import android.graphics.Bitmap;

public class FeedUpdaterService extends Service {
    // Updates feeds one by one
    // Allows queueing

    private Queue<String> queue = new ArrayDeque<>();

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onReceiveUpdateFeeds(MessageEvent.UpdateFeeds event) {
        enqueueFeeds(event.urls);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    private void enqueueFeeds(String[] feedUrls) {
        // Put urls in queue (if they aren't there already)
        for (String url : feedUrls)
            if (!queue.contains(url)) queue.add(url);
    }

    private void startUpdate() {
        if (!queue.isEmpty()) {
            // Gets feed url from queue and updates it (it gets removed when we finish updating)
            String feed = queue.peek();
            updatePodcastFromUrl(feed);
            Log.d("FeedUpdaterService", "Downloading rss...");
        } else {
            Log.d("FeedUpdaterService", "Closing service");
            stopSelf();
        }
    }

    private void updatePodcastFromUrl(String feedUrl) {
        ResponseToStringTask request = new ResponseToStringTask() {
            @Override
            protected void onPostExecute(String result) {
                InputStream is = new ByteArrayInputStream(result.getBytes(StandardCharsets.UTF_8));
                Log.d("FeedUpdaterService", "Downloaded rss, parsing now...");
                new ParseRss(is, feedUrl).execute();
            }
        };
        request.execute(feedUrl);
    }

    public class ParseRss extends AsyncTask<Void, ParsedRss, ParsedRss> {
        private InputStream is;
        private String url;

        public ParseRss(InputStream inputStream, String stringUrl) {
            is = inputStream;
            url = stringUrl;
        }

        @Override
        protected ParsedRss doInBackground(Void... params) {

            ParsedRss parsed = RssParser.parseXMLFromStream(is, url);
            if  (parsed == null || parsed.podcast.title == null) {
                Log.e("PodcastUpdater", "Could not parse RSS feed");
                return null;
            }
            Log.d("FeedUpdaterService", "Done parsing...");
            // Set the feed's url and directory
            // The directory is only set if it's a new podcast
            parsed.podcast.url = url;
            final String podcastFolder = parsed.podcast.title.replaceAll("[\\\\/:*?\"<>+|]", "");
            parsed.podcast.directory = Preferences.getDownloadDirectory(getApplicationContext()) + podcastFolder + "/";

            // Add episodes to database in new thread
            Log.d("FeedUpdaterService", "Updating db now...");
            final DatabaseHelper dbh = DatabaseHelper.getInstance(getApplicationContext());
            final long podcastId = dbh.getOrCreatePodcast(parsed.podcast).id;
            // Main operation, add all the episodes to the database (or ignore if exists)
            dbh.addEpisodes(parsed.episodes, podcastId);
            Log.d("FeedUpdaterService", "DB updated");

            return parsed;
        }

        @Override
        protected void onPostExecute(ParsedRss parsed) {
            // Refresh activities that show podcasts after updating podcast
            EventBus.getDefault().post(new MessageEvent.RefreshPodcast(parsed.podcast.url));
            queue.remove();
            startUpdate();
            super.onPostExecute(parsed);
        }
    }


    // Binder stuff
    private final IBinder iBinder = new FeedUpdaterService.LocalBinder();
    @Override
    public IBinder onBind(Intent intent) {
        return iBinder;
    }
    public class LocalBinder extends Binder {
        public FeedUpdaterService getService() {
            return FeedUpdaterService.this;
        }
    }

    public static boolean isServiceRunning(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (FeedUpdaterService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public static void updateFeeds(Context context, String[] feeds) {
            if (!isServiceRunning(context)) {
                // Start the service and play the audio
                ServiceConnection serviceConnection = new ServiceConnection() {
                    @Override
                    public void onServiceDisconnected(ComponentName name) {}
                    @Override
                    public void onServiceConnected(ComponentName name, IBinder service) {
                        FeedUpdaterService.LocalBinder binder = (FeedUpdaterService.LocalBinder) service;
                        binder.getService().enqueueFeeds(feeds);
                        binder.getService().startUpdate();
                        context.unbindService(this);
                    }
                };

                Intent intent = new Intent(context, FeedUpdaterService.class);
                context.startService(intent);
                context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

            } else {
                // Send a Bus with the feeds
                EventBus.getDefault().post(new MessageEvent.UpdateFeeds(feeds));
            }
        }
}
