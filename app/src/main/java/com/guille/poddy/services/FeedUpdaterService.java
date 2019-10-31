package com.guille.poddy.services;

import android.util.Log;

import com.guille.poddy.*;
import com.guille.poddy.database.*;
import com.guille.poddy.Helpers.ResponseToStringTask;

import java.nio.charset.StandardCharsets;

import android.app.*;
import android.os.*;
import android.content.*;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.*;
import java.util.*;
import android.graphics.Bitmap;

public class FeedUpdaterService extends Service {
    // Updates feeds one by one
    // Allows queueing

    private Queue<String> queue = new ArrayDeque<>();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        String[] feedUrls = intent.getExtras().getStringArray("feedUrls");
        enqueueFeeds(feedUrls);

        return super.onStartCommand(intent, flags, startId);
    }

    private final BroadcastReceiver feedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String[] feedUrls = intent.getExtras().getStringArray("feedUrls");
            enqueueFeeds(feedUrls);
        }
    };


    @Override
    public void onCreate() {
        super.onCreate();
        final LocalBroadcastManager bm = LocalBroadcastManager.getInstance(getApplicationContext());
        bm.registerReceiver(feedReceiver, new IntentFilter(Broadcast.UPDATE_FEEDS));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        final LocalBroadcastManager bm = LocalBroadcastManager.getInstance(getApplicationContext());
        bm.unregisterReceiver(feedReceiver);
    }

    private void enqueueFeeds(String[] feedUrls) {
        // Put urls in queue (if they aren't there already)
        for (String url : feedUrls)
            if (!queue.contains(url)) queue.add(url);
        startUpdate();
    }

    private void startUpdate() {
        if (!queue.isEmpty()) {
            // Pops feed url from queue and updates it
            String feed = queue.remove();
            updatePodcastFromUrl(feed);
        } else stopSelf();
    }


    private void updatePodcastFromUrl(String url) {
        ResponseToStringTask request = new ResponseToStringTask() {
            @Override
            protected void onPostExecute(String result) {
                super.onPostExecute(result);

                InputStream is = new ByteArrayInputStream(result.getBytes(StandardCharsets.UTF_8));
                updatePodcast(is, url);
            }
        };
        request.execute(url);
    }


    private void updatePodcast(InputStream is, String url) {
        DatabaseHelper dbh = DatabaseHelper.getInstance(getApplicationContext());
        // Parse the rss file and check if it's valid
        ParsedRss parsed = RssParser.parseXMLFromStream(is, url);
        if  (parsed == null || parsed.podcast.title == null) {
            Log.e("PodcastUpdater", "Could not parse RSS feed");
            return;
        }
        // Set the feed's url and directory
        // The directory is only set if it's a new podcast
        parsed.podcast.url = url;
        parsed.podcast.directory = Preferences.getDownloadDirectory(getApplicationContext()) + parsed.podcast.title + "/";

        final String imageFile = parsed.podcast.directory + "podcastArt.png";
        parsed.podcast.imageFile = imageFile;

        // Download the image
        Helpers.ResponseToImageTask request = new Helpers.ResponseToImageTask() {
            @Override
            protected void onPostExecute(Bitmap result) {
                super.onPostExecute(result);
                // Save bitmap to file
                File checkFile = new File(imageFile);
                if(checkFile.exists()) {
                    checkFile.delete();
                }
                try {
                    new File (parsed.podcast.directory).mkdirs();
                    result.compress(Bitmap.CompressFormat.PNG, 100, new FileOutputStream(imageFile));
                } catch (Exception e) { e.printStackTrace(); }
            }
        };
        request.execute(parsed.podcast.imageUrl);


        // Check if that podcast already exists in database or create it
        final long podcastId = dbh.getOrCreatePodcast(parsed.podcast).id;

        // Now that we have the podcastId add episodes to database
        dbh.addEpisodes(parsed.episodes, podcastId);

        // Refresh activities that show podcasts after updating podcast
        try {
            Intent intent = new Intent(Broadcast.REFRESH_PODCASTS);
            intent.putExtra("podcastUrl", url);

            final LocalBroadcastManager bm = LocalBroadcastManager.getInstance(getApplicationContext());
            bm.sendBroadcast(intent);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Get the next feed from the queue, if there is one
            startUpdate();
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
}
