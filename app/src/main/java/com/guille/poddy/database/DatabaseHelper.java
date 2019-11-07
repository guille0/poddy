package com.guille.poddy.database;

import android.database.sqlite.*;
import android.database.Cursor;
import android.content.Context;
import android.util.Log;
import android.content.ContentValues;

import java.util.*;

public class DatabaseHelper extends SQLiteOpenHelper{
    private static DatabaseHelper sInstance;

    public static synchronized DatabaseHelper getInstance(Context context) {
        // Use the application context, which will ensure that you
        // don't accidentally leak an Activity's context.
        // See this article for more information: http://bit.ly/6LRzfx
        if (sInstance == null) {
            sInstance = new DatabaseHelper(context.getApplicationContext());
        }
        return sInstance;
    }

    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Database Info
    private static final String DATABASE_NAME = "podcastDatabase";
    private static final int DATABASE_VERSION = 1;


    // Called when the database connection is being configured.
    // Configure database settings for things like foreign key support, write-ahead logging, etc.
    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    // Called when the database is created for the FIRST time.
    // If a database already exists on disk with the same DATABASE_NAME, this method will NOT be called.
    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_PODCASTS_TABLE = "" +
                "CREATE TABLE podcasts (" +
                "id INTEGER PRIMARY KEY," +
                "title TEXT NOT NULL," +
                "description TEXT," +
                "url TEXT NOT NULL UNIQUE," +

                "directory TEXT," +
                "imageUrl TEXT" +
                ")";

        String CREATE_EPISODES_TABLE = "" +
                "CREATE TABLE episodes (" +
                "id INTEGER PRIMARY KEY," +
                "podcastId INTEGER REFERENCES podcasts ON DELETE CASCADE," +
                "title TEXT," +
                "description TEXT," +
                "downloaded BOOLEAN," +

                "file TEXT," +
                "imageUrl TEXT," +
                "episode INTEGER," +
                "date TEXT," +

                "guid TEXT," +
                "author TEXT," +

                "enclosureUrl TEXT UNIQUE," +
                "enclosureType TEXT," +
                "enclosureLength INTEGER," +

                "position INTEGER DEFAULT 0," +
                "duration INTEGER DEFAULT 0" +
                ")";

        db.execSQL(CREATE_PODCASTS_TABLE);
        db.execSQL(CREATE_EPISODES_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion != newVersion) {
            // Simplest implementation is to drop all old tables and recreate them
            db.execSQL("DROP TABLE IF EXISTS podcasts");
            db.execSQL("DROP TABLE IF EXISTS episodes");
            onCreate(db);
        }
    }

    // ADD

    private long addPodcast(Podcast podcast) {
        long podcastId = -1;
        // If the podcast title doesn't exist, add the podcast
        SQLiteDatabase db = getWritableDatabase();
        // It's a good idea to wrap our insert in a transaction. This helps with performance and ensures
        // consistency of the database.
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put("title", podcast.title);
            values.put("description", podcast.description);
            values.put("url", podcast.url);
            values.put("directory", podcast.directory);
            values.put("imageUrl", podcast.imageUrl);

            podcastId = db.insertWithOnConflict("podcasts", null, values, SQLiteDatabase.CONFLICT_IGNORE);
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.d("db", "Error while trying to add post to database." +
                        "Maybe that title already exists?");
        } finally {
            db.endTransaction();
        }
        return podcastId;
    }

    public void addEpisodes(List<Episode> eps, long podcastId) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values;

        db.beginTransaction();
        try {
            for (Episode ep: eps) {
                values = new ContentValues();
                values.put("podcastId", podcastId);
                values.put("title", ep.title);
                values.put("description", ep.description);
                values.put("downloaded", ep.downloaded);

                values.put("file", ep.file);
                values.put("imageUrl", ep.imageUrl);
                values.put("episode", ep.episode);
                values.put("date", ep.date);

                values.put("guid", ep.guid);
                values.put("author", ep.author);

                values.put("enclosureUrl", ep.enclosureUrl);
                values.put("enclosureType", ep.enclosureType);
                values.put("enclosureLength", ep.enclosureLength);

                values.put("duration", ep.duration);
                values.put("position", ep.position);

                db.insertWithOnConflict("episodes", null, values, SQLiteDatabase.CONFLICT_IGNORE);
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.d("db", "Error while trying to add episode to database.");
        } finally {
            db.endTransaction();
        }
    }

    private void updatePodcast(Podcast podcast, long podcastId) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put("url", podcast.url);
            values.put("description", podcast.description);
            values.put("imageUrl", podcast.imageUrl);

            db.update("podcasts", values, "id=?", new String[] {Long.toString(podcastId)});
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.d("db", "Error while trying to update database.");
        } finally {
            db.endTransaction();
        }
    }

    public Boolean updateEpisodeDownloadedStatus(long episodeId, Boolean downloaded, String file) {
        boolean success = true;
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put("file", file);
            values.put("downloaded", downloaded);

            db.update("episodes", values, "id=?", new String[] {Long.toString(episodeId)});
            db.setTransactionSuccessful();
            Log.i("db", "EPISODE CHANGED DOWNLOADED, with filename" + file);
        } catch (Exception e) {
            Log.d("db", "Error while trying to update database.");
            success = false;
        } finally {
            db.endTransaction();
        }
        return success;
    }

    public void updateEpisodePosition(long episodeId, long position) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put("position", position);

            db.update("episodes", values, "id=?", new String[] {Long.toString(episodeId)});
            db.setTransactionSuccessful();
            Log.i("db", "EPISODE CHANGED POSITION");
        } catch (Exception e) {
            Log.d("db", "Error while trying to update database.");
        } finally {
            db.endTransaction();
        }
    }

    public void updateEpisodeDuration(long episodeId, long duration) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put("duration", duration);

            db.update("episodes", values, "id=?", new String[] {Long.toString(episodeId)});
            db.setTransactionSuccessful();
            Log.i("db", "EPISODE CHANGED POSITION");
        } catch (Exception e) {
            Log.d("db", "Error while trying to update database.");
        } finally {
            db.endTransaction();
        }
    }

    // DELETE

    public void deletePodcast(long podcastId) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            String query = "id=?";
            db.delete("podcasts", query, new String[] {Long.toString(podcastId)});
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.d("db", "Error while trying to delete podcast.");
            e.printStackTrace();
        } finally {
            db.endTransaction();
        }
    }

    // GET

    // GET PODCASTS

    public Podcast getOrCreatePodcast(Podcast pod) {
        // Checks if it exists based on the feed url
        String query = "SELECT * FROM podcasts WHERE url= ?";

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(query, new String[] {pod.url});
        long podcastId = -1;

        try {
            if (cursor.getCount() > 0) {
                // It exists, so update it with current info
                cursor.moveToFirst();
                podcastId = cursor.getInt(cursor.getColumnIndex("id"));
                updatePodcast(pod, podcastId);
            } else {
                // It's new, so create it
                podcastId = addPodcast(pod);
            }
        } catch (Exception e) {
            Log.e("db.getOrCreatePodcast", "Error checking for podcast in database");
        } finally {
            if (cursor != null && !cursor.isClosed()) cursor.close();
        }
        // Return it along with the ID, so we can add episodes to it
        pod.id = podcastId;
        return pod;
    }

    public List<Podcast> getAllPodcasts() {
        String query = "SELECT * FROM podcasts ORDER BY title ASC";

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        return getPodcastsFromCursor(cursor);
    }

     public List<Podcast> getAllPodcastsOrderByDate() {
        String query = "SELECT p.*, MAX(e.date) FROM podcasts p " +
                 "INNER JOIN episodes e ON e.podcastId = p.id " +
                 "GROUP BY p.id " +
                 "ORDER BY DATE(e.date) DESC ";

         SQLiteDatabase db = getReadableDatabase();
         Cursor cursor = db.rawQuery(query, null);

         return getPodcastsFromCursor(cursor);
     }

    public Podcast getPodcastFromId(long id) {
        String query = "SELECT * FROM podcasts " +
                "WHERE id = ?";

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(query, new String[] {Long.toString(id)});

        return getPodcastsFromCursor(cursor).get(0);
    }

    public Podcast getPodcastFromEpisode(long episodeId) {
        String query = "SELECT p.* FROM podcasts p " +
                "INNER JOIN episodes e ON e.podcastId= p.id "  +
                "WHERE e.id=? ";

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(query, new String[] {Long.toString(episodeId)});

        return getPodcastsFromCursor(cursor).get(0);
    }

    private List<Podcast> getPodcastsFromCursor(Cursor cursor) {
        List<Podcast> podcasts = new ArrayList<>();

        try {
            if (cursor.moveToFirst()) {
                do {
                    Podcast pcast = new Podcast();
                    pcast.id = cursor.getInt(cursor.getColumnIndex("id"));
                    pcast.title = cursor.getString(cursor.getColumnIndex("title"));
                    pcast.description = cursor.getString(cursor.getColumnIndex("description"));
                    pcast.url = cursor.getString(cursor.getColumnIndex("url"));
                    pcast.directory = cursor.getString(cursor.getColumnIndex("directory"));
                    pcast.imageUrl = cursor.getString(cursor.getColumnIndex("imageUrl"));

                    podcasts.add(pcast);
                } while(cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.d("db", "Error while trying to get podcasts from database");
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
        return podcasts;
    }

    // GET EPISODES

    public Episode getEpisodeFromId(long episodeId) {
//        String query = "SELECT * FROM episodes WHERE id =?";
        String query = "SELECT e.*, p.title as podcastTitle, p.imageUrl as podcastImageUrl FROM episodes e " +
                "INNER JOIN podcasts p ON e.podcastId= p.id "  +
                "WHERE e.id=? ";

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(query, new String[] {Long.toString(episodeId)});

        return this.getEpisodesFromCursor(cursor).get(0);
    }


    public List<Episode> getAllEpisodes() {
//        String query = "SELECT * FROM episodes ORDER BY DATE(date) DESC";
        String query = "SELECT e.*, p.title as podcastTitle, p.imageUrl as podcastImageUrl FROM episodes e " +
                "INNER JOIN podcasts p ON e.podcastId= p.id "  +
                "ORDER BY DATE(e.date) DESC ";

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        return this.getEpisodesFromCursor(cursor);
    }


    public List<Episode> getEpisodesFromPodcast(long podcastId) {
//        String query = "SELECT * FROM episodes WHERE podcastId=? ORDER BY DATE(date) DESC";
        String query = "SELECT e.*, p.title as podcastTitle, p.imageUrl as podcastImageUrl FROM episodes e " +
                "INNER JOIN podcasts p ON e.podcastId= p.id " +
                "WHERE p.id = ? "  +
                "ORDER BY DATE(e.date) DESC ";

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(query, new String[] {Long.toString(podcastId)});

        return this.getEpisodesFromCursor(cursor);
    }


    public List<Episode> getDownloadedEpisodes() {
//        String query = "SELECT * FROM episodes " +
//                "WHERE downloaded = 1 " +
//                "ORDER BY DATE(date) DESC";
        String query = "SELECT e.*, p.title as podcastTitle, p.imageUrl as podcastImageUrl FROM episodes e " +
                "INNER JOIN podcasts p ON e.podcastId= p.id " +
                "WHERE e.downloaded = 1 "  +
                "ORDER BY DATE(e.date) DESC ";

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        return this.getEpisodesFromCursor(cursor);
    }


    public List<Episode> getEpisodesFromThisWeek() {
//        String query = "SELECT * FROM episodes " +
//                "WHERE DATE(date) >= DATE('now', 'weekday 0', '-7 days')" +
//                "ORDER BY date(date) DESC";
        String query = "SELECT e.*, p.title as podcastTitle, p.imageUrl as podcastImageUrl FROM episodes e " +
                "INNER JOIN podcasts p ON e.podcastId= p.id " +
                "WHERE DATE(e.date) >= DATE('now', 'weekday 0', '-7 days') "  +
                "ORDER BY DATE(e.date) DESC ";

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        return this.getEpisodesFromCursor(cursor);
    }


    private List<Episode> getEpisodesFromCursor(Cursor cursor) {
        List<Episode> episodes = new ArrayList<>();

        try {
            if (cursor.moveToFirst()) {
                do {
                    Episode ep = new Episode();
                    ep.id = cursor.getInt(cursor.getColumnIndex("id"));
                    ep.title = cursor.getString(cursor.getColumnIndex("title"));
                    ep.downloaded = cursor.getInt(cursor.getColumnIndex("downloaded")) > 0;
                    ep.podcastId = cursor.getInt(cursor.getColumnIndex("podcastId"));
                    ep.description = cursor.getString(cursor.getColumnIndex("description"));

                    ep.file = cursor.getString(cursor.getColumnIndex("file"));
                    ep.date = cursor.getString(cursor.getColumnIndex("date"));
                    ep.imageUrl = cursor.getString(cursor.getColumnIndex("imageUrl"));
                    ep.episode = cursor.getInt(cursor.getColumnIndex("episode"));

                    ep.guid = cursor.getString(cursor.getColumnIndex("guid"));
                    ep.author = cursor.getString(cursor.getColumnIndex("author"));

                    ep.enclosureUrl = cursor.getString(cursor.getColumnIndex("enclosureUrl"));
                    ep.enclosureType = cursor.getString(cursor.getColumnIndex("enclosureType"));
                    ep.enclosureLength = cursor.getInt(cursor.getColumnIndex("enclosureLength"));

                    ep.duration = cursor.getInt(cursor.getColumnIndex("duration"));
                    ep.position = cursor.getInt(cursor.getColumnIndex("position"));

                    ep.podcastTitle = cursor.getString(cursor.getColumnIndex("podcastTitle"));
                    ep.podcastImageUrl = cursor.getString(cursor.getColumnIndex("podcastImageUrl"));

                    episodes.add(ep);

                } while(cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.d("db", "Error while trying to get episodes from database");
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
        return episodes;
    }
}
