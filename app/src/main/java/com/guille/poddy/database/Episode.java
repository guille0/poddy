package com.guille.poddy.database;

import android.os.Parcelable;
import android.os.Parcel;

public class Episode implements Parcelable {
    // Non-null
    public long id;
    public String title = "";
    public String description = "";
    public long podcastId;
    public Boolean downloaded;

    public String file = "";        // Only if downloaded is true
    public String imageUrl = "";
    public int episode = 0;
    public String date = "";

    public String guid = "";
    public String author = "";

    public String enclosureUrl = "";        // UNIQUE
    public String enclosureType = "";
    public Integer enclosureLength = 0;

    public long duration = 0;
    public long position = 0;

    // Podcast info

    public String podcastTitle;
    public String podcastImageUrl;

    public Episode(){}

    protected Episode(Parcel in) {
        id = in.readLong();
        title = in.readString();
        podcastId = in.readLong();
        byte downloadedVal = in.readByte();
        downloaded = downloadedVal == 0x02 ? null : downloadedVal != 0x00;
        file = in.readString();
        imageUrl = in.readString();
        description = in.readString();
        episode = in.readInt();
        date = in.readString();
        guid = in.readString();
        author = in.readString();
        enclosureUrl = in.readString();
        enclosureType = in.readString();
        enclosureLength = in.readByte() == 0x00 ? null : in.readInt();
        duration = in.readLong();
        position = in.readLong();
        podcastTitle = in.readString();
        podcastImageUrl = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(title);
        dest.writeLong(podcastId);
        if (downloaded == null) {
            dest.writeByte((byte) (0x02));
        } else {
            dest.writeByte((byte) (downloaded ? 0x01 : 0x00));
        }
        dest.writeString(file);
        dest.writeString(imageUrl);
        dest.writeString(description);
        dest.writeInt(episode);
        dest.writeString(date);
        dest.writeString(guid);
        dest.writeString(author);
        dest.writeString(enclosureUrl);
        dest.writeString(enclosureType);
        if (enclosureLength == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeInt(enclosureLength);
        }
        dest.writeLong(duration);
        dest.writeLong(position);
        dest.writeString(podcastTitle);
        dest.writeString(podcastImageUrl);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<Episode> CREATOR = new Parcelable.Creator<Episode>() {
        @Override
        public Episode createFromParcel(Parcel in) {
            return new Episode(in);
        }

        @Override
        public Episode[] newArray(int size) {
            return new Episode[size];
        }
    };
}