package com.guille.poddy;

import android.os.AsyncTask;

import java.io.*;
import java.net.*;
import java.util.*;
import java.text.*;

import com.guille.poddy.database.*;

import com.tonyodev.fetch2.*;

import android.net.*;
import android.graphics.*;
import android.media.*;
import android.content.*;

public class Helpers {

    public static Bitmap getPodcastImage(Context context, Episode episode, Podcast podcast) {
        try {
            if (!episode.file.equals("") && new File(episode.file).exists()) {
                MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                Uri uri = Uri.parse(episode.file);
                mmr.setDataSource(context, uri);

                byte[] data = mmr.getEmbeddedPicture();
                if (data != null) {
                    return BitmapFactory.decodeByteArray(data, 0, data.length);
                } else {
                    return null;
                }
            } else if (!podcast.imageFile.equals("") && new File(podcast.imageFile).exists()) {
                return BitmapFactory.decodeFile(podcast.imageFile);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Download isBeingDownloaded(List<Download> downloads, long episodeId) {
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

    public static class ResponseToImageTask extends AsyncTask<String, String, Bitmap> {

        protected Bitmap doInBackground(String... params) {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(params[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();
                InputStream in = connection.getInputStream();
                return BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            super.onPostExecute(result);
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

                return buffer.toString();

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

    public static String milisecondsToString(int ms) {
        int second = (ms / 1000) % 60;
        int minute = (ms / (1000 * 60)) % 60;
        int hour = (ms / (1000 * 60 * 60)) % 24;

        return String.format("%02d:%02d:%02d", hour, minute, second);
    }

    public static String dateToReadable(String date) {
        try {
            SimpleDateFormat originalDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            SimpleDateFormat convertDate = new SimpleDateFormat("dd MMM yyyy");

            final Date firstDate = originalDate.parse(date);

            StringBuffer stringBuffer = new StringBuffer();
            convertDate.format(firstDate, stringBuffer, new FieldPosition(0));

            return stringBuffer.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
