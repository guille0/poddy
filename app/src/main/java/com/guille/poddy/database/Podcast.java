package com.guille.poddy.database;

import android.os.Parcelable;
import android.os.Parcel;

public class Podcast implements Parcelable {
    public long id;

    // Information gotten from the rss feed
    public String title;
    public String description;
    public String imageFile;
    public String imageUrl;
    public String directory;

    // Link to the rss feed (unique)
    public String url;

    public Podcast() {}

    protected Podcast(Parcel in) {
        id = in.readLong();
        title = in.readString();
        description = in.readString();
        imageFile = in.readString();
        imageUrl = in.readString();
        directory = in.readString();
        url = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(title);
        dest.writeString(description);
        dest.writeString(imageFile);
        dest.writeString(imageUrl);
        dest.writeString(directory);
        dest.writeString(url);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<Podcast> CREATOR = new Parcelable.Creator<Podcast>() {
        @Override
        public Podcast createFromParcel(Parcel in) {
            return new Podcast(in);
        }

        @Override
        public Podcast[] newArray(int size) {
            return new Podcast[size];
        }
    };
}