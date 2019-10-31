package com.guille.poddy.activities;


import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.TextView;

import android.content.Context;

import com.guille.poddy.R;
import com.guille.poddy.database.*;

import org.jetbrains.annotations.NotNull;

import java.util.*;

public class RecyclerViewItunesSearch extends RecyclerView.Adapter<com.guille.poddy.activities.RecyclerViewItunesSearch.ViewHolder> {

    private List<Podcast> mData;
    private LayoutInflater mInflater;
    private com.guille.poddy.activities.RecyclerViewItunesSearch.ItemClickListener mClickListener;

    // data is passed into the constructor
    RecyclerViewItunesSearch(Context context, List<Podcast> data) {
        this.mInflater = LayoutInflater.from(context);
        this.mData = data;
    }

    // inflates the row layout from xml when needed
    @NotNull
    @Override
    public com.guille.poddy.activities.RecyclerViewItunesSearch.ViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {

        // TODO change podcast_row if we want to show searches differently
        View view = mInflater.inflate(R.layout.podcast_row, parent, false);
        return new com.guille.poddy.activities.RecyclerViewItunesSearch.ViewHolder(view);
    }

    // binds the data to the TextView in each row
    @Override
    public void onBindViewHolder(com.guille.poddy.activities.RecyclerViewItunesSearch.ViewHolder holder, int position) {
        Podcast pcast = mData.get(position);
        holder.myTextView.setText(pcast.title);
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return mData.size();
    }


    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView myTextView;

        ViewHolder(View itemView) {
            super(itemView);
            myTextView = itemView.findViewById(R.id.textPodcastTitle);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onItemClick(view, getAdapterPosition());
        }
    }

    // allows clicks events to be caught
    void setClickListener(com.guille.poddy.activities.RecyclerViewItunesSearch.ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }
}

