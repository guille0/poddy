package com.guille.poddy.services;

import android.os.Parcelable;
import android.os.Parcel;
import com.guille.poddy.database.*;

public class DownloadingEpisode implements Parcelable {
    public Episode episode;
    public Podcast podcast;
    public int progress;
    public int requestId;

    public DownloadingEpisode(Episode ep, Podcast pod, int prog, int id) {
        episode = ep;
        podcast = pod;
        progress = prog;
        requestId = id;
    }

    protected DownloadingEpisode(Parcel in) {
        episode = (Episode) in.readValue(Episode.class.getClassLoader());
        podcast = (Podcast) in.readValue(Podcast.class.getClassLoader());
        progress = in.readInt();
        requestId = in.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(episode);
        dest.writeValue(podcast);
        dest.writeInt(progress);
        dest.writeInt(requestId);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<DownloadingEpisode> CREATOR = new Parcelable.Creator<DownloadingEpisode>() {
        @Override
        public DownloadingEpisode createFromParcel(Parcel in) {
            return new DownloadingEpisode(in);
        }

        @Override
        public DownloadingEpisode[] newArray(int size) {
            return new DownloadingEpisode[size];
        }
    };
}