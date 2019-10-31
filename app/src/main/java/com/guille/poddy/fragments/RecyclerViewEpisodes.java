package com.guille.poddy.fragments;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ProgressBar;

import androidx.recyclerview.widget.RecyclerView;

import com.guille.poddy.R;
import com.guille.poddy.*;
import com.guille.poddy.database.*;

import com.tonyodev.fetch2.*;


import org.jetbrains.annotations.NotNull;

import java.util.*;

public class RecyclerViewEpisodes extends RecyclerView.Adapter<RecyclerViewEpisodes.ViewHolder> {

    public static final int CLICK_IMAGE = 0;
    public static final int CLICK_TEXT = 1;

    private List<Episode> mData;
    private LayoutInflater mInflater;
    private ItemClickListener mClickListener;

    private List<Download> downloading = new ArrayList<>();

    public void refreshEpisode(int position, Episode ep) {
        mData.set(position, ep);
    }

    // data is passed into the constructor
    RecyclerViewEpisodes(Context context, List<Episode> data) {
        this.mInflater = LayoutInflater.from(context);
        this.mData = data;
    }

    public void setDownloadingList(List<Download> list) {
        downloading = list;
    }

    public void changeEpisodeDownloaded(int position, Boolean downloaded) {
        mData.get(position).downloaded = downloaded;
    }

    // inflates the row layout from xml when needed
    @NotNull
    @Override
    public ViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.episode_row, parent, false);
        return new ViewHolder(view);
    }

    // binds the data to the TextView in each row
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Episode ep = mData.get(position);
        holder.textView.setText(ep.title);
        if (ep.downloaded) {
            holder.imageView.setImageResource(R.drawable.ic_play_arrow_black_24dp);
            holder.progressBar.setVisibility(View.GONE);
        } else {
            final Download download = Helpers.isBeingDownloaded(this.downloading, ep.id);
            if (download != null) {
                holder.imageView.setImageResource(R.drawable.ic_cancel_black_24dp);
                holder.progressBar.setVisibility(View.VISIBLE);
                holder.progressBar.setProgress(download.getProgress());
            } else {
                holder.progressBar.setVisibility(View.GONE);
                holder.imageView.setImageResource(R.drawable.ic_file_download_black_24dp);
            }
        }
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return mData.size();
    }


    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private TextView textView;
        private ImageView imageView;
        private ProgressBar progressBar;

        ViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.textEpisodeTitle);
            imageView = itemView.findViewById(R.id.imageDownload);
            progressBar = itemView.findViewById(R.id.progressBar);

            textView.setOnClickListener(this);
            imageView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) {
                if (view.getId() == imageView.getId()) {
                    mClickListener.onImageClick(view, getAdapterPosition());
                } else {
                    mClickListener.onItemClick(view, getAdapterPosition());
                }

            }
        }
    }

    // allows clicks events to be caught
    void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(View view, int position);
        void onImageClick(View view, int position);
    }
}