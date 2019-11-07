package com.guille.poddy.fragments.recyclerviews;

import androidx.recyclerview.widget.RecyclerView;
import android.view.*;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;

import android.content.Context;
import android.graphics.*;

import com.guille.poddy.R;
import com.guille.poddy.database.*;
import com.guille.poddy.*;
import java.lang.Math;

import org.jetbrains.annotations.NotNull;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.*;

public class RecyclerViewPodcastsImage extends RecyclerViewPodcastsAbstract {
    private int imagesPerRow = 3;
    public RecyclerViewPodcastsImage(Context context, int perRow) {
        inflater = LayoutInflater.from(context);
        imagesPerRow = perRow;
    }

    @NotNull
    @Override
    public RecyclerViewPodcastsAbstract.ViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.podcast_row_images, parent, false);

        final int smallest = Math.min(parent.getMeasuredWidth(), parent.getMeasuredHeight());
        final int width = smallest / imagesPerRow;
        view.setLayoutParams(new LayoutParams(width, width));

        return new RecyclerViewPodcastsAbstract.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerViewPodcastsImage.ViewHolder holder, int position) {
        Podcast pod = podcasts.get(position);

        Picasso.get()
                .load(pod.imageUrl)
                .fit()
                .centerCrop()
                .into(holder.imageView);
    }
}
