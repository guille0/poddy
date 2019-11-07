package com.guille.poddy.fragments.recyclerviews;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.guille.poddy.R;
import com.guille.poddy.*;
import com.guille.poddy.database.*;

import org.jetbrains.annotations.NotNull;

public class RecyclerViewEpisodesComplex extends RecyclerViewEpisodesAbstract {

    public RecyclerViewEpisodesComplex(Context context) {
        inflater = LayoutInflater.from(context);
    }

    @NotNull
    @Override
    public ViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.episode_row_complex, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Episode ep = episodes.get(position);
        // First time creation
        holder.textEpisodeTitle.setText(ep.title);
        // Extra views for Complex
        holder.textPodcastTitle.setText(ep.podcastTitle);
        holder.textDate.setText(Helpers.dateToReadable(ep.date));
        holder.textDuration.setText(Helpers.milisecondsToString(ep.duration));
        updateStatus(holder, ep);
    }
}