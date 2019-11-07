package com.guille.poddy.fragments.recyclerviews;

import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.TextView;
import android.widget.ImageView;

import android.content.Context;

import com.guille.poddy.R;
import com.guille.poddy.database.*;
import com.guille.poddy.fragments.*;
import com.guille.poddy.*;

import org.jetbrains.annotations.NotNull;

import java.util.*;

public abstract class RecyclerViewPodcastsAbstract extends RecyclerView.Adapter<RecyclerViewPodcastsAbstract.ViewHolder> {

    protected List<Podcast> podcasts;
    protected LayoutInflater inflater;
    protected RecyclerViewPodcastsImage.ItemClickListener clickListener;

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ImageView imageView;
        TextView textView;

        ViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.textPodcastTitle);
            imageView = itemView.findViewById(R.id.imagePodcast);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (clickListener != null) clickListener.onItemClick(view, getAdapterPosition());
        }
    }


    @Override
    public int getItemCount() {
        return podcasts.size();
    }

    public List<Podcast> getItems() {
        return podcasts;
    }

    public void setItems(List<Podcast> pods) {
        podcasts = pods;
    }

    void setClickListener(RecyclerViewPodcastsImage.ItemClickListener itemClickListener) {
        clickListener = itemClickListener;
    }

    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }


}
