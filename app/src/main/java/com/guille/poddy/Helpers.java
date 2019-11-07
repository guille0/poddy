package com.guille.poddy;

import android.annotation.SuppressLint;
import android.os.AsyncTask;

import java.io.*;
import java.net.*;
import java.util.*;
import java.text.*;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.guille.poddy.database.*;

import com.squareup.picasso.Callback;
import com.tonyodev.fetch2.*;

import android.net.*;
import android.graphics.*;
import android.widget.*;
import android.view.*;
import android.media.*;
import android.content.*;

public class Helpers {

//    public static Bitmap getPodcastImage(Context context, Episode episode, Podcast podcast) {
//        boolean usePodcastImage = false;
//
//        // If we fail to get the image for the episode, use the Podcast image file
//        if (episode != null && !episode.file.equals("") && new File(episode.file).exists()) {
//            try {
//                MediaMetadataRetriever mmr = new MediaMetadataRetriever();
//                Uri uri = Uri.parse(episode.file);
//                mmr.setDataSource(context, uri);
//
//                byte[] data = mmr.getEmbeddedPicture();
//                if (data != null) {
//                    return BitmapFactory.decodeByteArray(data, 0, data.length);
//                } else {
//                    usePodcastImage = true;
//                }
//            } catch (Exception e) {
//                usePodcastImage = true;
//            }
//        } else if (!podcast.imageFile.equals("") && new File(podcast.imageFile).exists()) {
//            usePodcastImage = true;
//        }
//
//        if (usePodcastImage)
//            return getBitmapFromFile(podcast.imageFile);
//        return null;
//    }

//    public static Bitmap getBitmapFromFile(String file) {
//        try {
//            return BitmapFactory.decodeFile(file);
//        } catch (Exception e) {
//            return null;
//        }
//    }

    public static void setEpisodeImage(ImageView imagePodcast, Episode episode) {
        if (!episode.imageUrl.equals("")) {
            Picasso.get()
                    .load(episode.imageUrl)
                    .fit()
                    .centerCrop()
                    .into(imagePodcast);
        } else if (!episode.podcastImageUrl.equals("")){
            Picasso.get()
                    .load(episode.podcastImageUrl)
                    .fit()
                    .centerCrop()
                    .into(imagePodcast);
        }
    }

    public static Download isBeingDownloaded(List<Download> downloads, long episodeId) {
        if (downloads.isEmpty()) return null;

        for (Download download : downloads) {
            try {
                if (episodeId == Long.parseLong(download.getTag()))
                    return download;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static void downloadImage(String strUrl, String directory, String imageFile) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(strUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();
            InputStream in = connection.getInputStream();

            Bitmap result = BitmapFactory.decodeStream(in);
            // Save bitmap to file
            File checkFile = new File(imageFile);
            if(checkFile.exists()) {
                checkFile.delete();
            }
            try {
                new File (directory).mkdirs();
                result.compress(Bitmap.CompressFormat.PNG, 100, new FileOutputStream(imageFile));
            } catch (Exception e) { e.printStackTrace(); }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public static class ResponseToStringTask extends AsyncTask<String, String, String> {

        protected String doInBackground(String... params) {
            HttpURLConnection connection = null;
            BufferedReader reader = null;

            try {
                URL url = new URL(params[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                InputStream stream = connection.getInputStream();

                reader = new BufferedReader(new InputStreamReader(stream));

                StringBuilder builder = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    builder.append(line).append("\n");
                }

                return builder.toString();

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
        }
    }

    @SuppressLint("DefaultLocale")
    public static String milisecondsToString(long ms) {
        final long second = (ms / 1000) % 60;
        final long minute = (ms / (1000 * 60)) % 60;
        final long hour = (ms / (1000 * 60 * 60)) % 24;

        return String.format("%02d:%02d:%02d", hour, minute, second);
    }

    @SuppressLint("DefaultLocale")
    public static String secondsToString(long sec) {
        final long second = sec % 60;
        final long minute = (sec * 60) % 60;
        final long hour = (sec * 60 * 60) % 24;

        return String.format("%02d:%02d:%02d", hour, minute, second);
    }

    public static long hhmmssToMs(String hhmmss) {
        DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            Date date = dateFormat.parse(hhmmss);
            return date.getTime();
        } catch (Exception e) {
            return 0;
        }
    }

    public static String dateToReadable(String date) {
        try {
            SimpleDateFormat originalDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
            SimpleDateFormat convertDate = new SimpleDateFormat("dd MMM yyyy", Locale.US);

            final Date firstDate = originalDate.parse(date);

            StringBuffer stringBuffer = new StringBuffer();
            convertDate.format(firstDate, stringBuffer, new FieldPosition(0));

            return stringBuffer.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
