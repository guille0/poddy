package com.guille.poddy.fragments.recyclerviews;

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

public class RecyclerViewPodcastsText extends RecyclerViewPodcastsAbstract {
    public RecyclerViewPodcastsText(Context context) {
        inflater = LayoutInflater.from(context);
    }

    @NotNull
    @Override
    public RecyclerViewPodcastsAbstract.ViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.podcast_row, parent, false);
        return new RecyclerViewPodcastsAbstract.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerViewPodcastsText.ViewHolder holder, int position) {
        Podcast pod = podcasts.get(position);
        holder.textView.setText(pod.title);
    }
}
