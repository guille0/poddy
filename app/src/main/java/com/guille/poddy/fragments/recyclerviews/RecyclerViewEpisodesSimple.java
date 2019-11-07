package com.guille.poddy.fragments.recyclerviews;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.guille.poddy.*;

import com.guille.poddy.R;
import com.guille.poddy.database.*;

import org.jetbrains.annotations.NotNull;

public class RecyclerViewEpisodesSimple extends RecyclerViewEpisodesAbstract {

    public RecyclerViewEpisodesSimple(Context context) {
        inflater = LayoutInflater.from(context);
    }

    // inflates the row layout from xml when needed
    @NotNull
    @Override
    public ViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.episode_row, parent, false);
        return new ViewHolder(view);
    }

    // binds the data to the TextView in each row
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Episode ep = episodes.get(position);
        // First time creation
        holder.textEpisodeTitle.setText(ep.title);
        holder.textDate.setText(Helpers.dateToReadable(ep.date));
        updateStatus(holder, ep);
    }
}