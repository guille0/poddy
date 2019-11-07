package com.guille.poddy.fragments.recyclerviews;

import java.util.*;

import android.os.Bundle;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;

import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.GridLayoutManager;

import com.guille.poddy.R;

import com.guille.poddy.activities.ActivityPodcastEpisodes;
import com.guille.poddy.database.*;
import com.guille.poddy.*;

import org.jetbrains.annotations.NotNull;
import com.guille.poddy.eventbus.*;
import com.guille.poddy.fragments.FragmentAbstract;
import com.guille.poddy.fragments.FragmentFactory;
import android.util.*;

import org.greenrobot.eventbus.*;

import androidx.recyclerview.widget.DiffUtil.*;

public class FragmentPodcasts extends FragmentAbstract implements RecyclerViewPodcastsText.ItemClickListener {
    private RecyclerView recyclerView;
    private RecyclerViewPodcastsAbstract adapter;
    private List<Podcast> podcasts;

    private int kind;
    private int imagesPerRow;
    private int podcastOrder;

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onReceiveRefreshPodcast(MessageEvent.RefreshPodcast event) {
        // Load them from the database
        loadPodcasts();
        // Check differences between new eps/shown eps and refresh RecyclerView
        updatePodcasts();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_podcasts, container, false);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        // Get the settings for what kind of content we are going to display
        kind = bundle.getInt("kind");
        // Get all podcasts from database
        loadPodcasts();
    }

    @Override
    public void onViewCreated(@NotNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        imagesPerRow = 3;

        // set up the RecyclerView
        recyclerView = view.findViewById(R.id.podcastList);
        recyclerView.setHasFixedSize(true);

        switch (kind) {
            case FragmentFactory.ContentShown.ALL_PODCASTS_TEXT:
                adapter = new RecyclerViewPodcastsText(getActivity());
                recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
                break;
            case FragmentFactory.ContentShown.ALL_PODCASTS_IMAGE:
                adapter = new RecyclerViewPodcastsImage(getActivity(), imagesPerRow);
                recyclerView.setItemViewCacheSize(20);
                recyclerView.setDrawingCacheEnabled(true);
                recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);

                RecyclerView.LayoutManager layoutManager =
                        new GridLayoutManager(getActivity(), imagesPerRow);
                recyclerView.setLayoutManager(layoutManager);

                break;
            default:
                Log.e("FragmentPodcasts", "Fragment kind not found! Displaying default");
                adapter = new RecyclerViewPodcastsText(getActivity());
                recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        }

        adapter.setItems(podcasts);
        adapter.setClickListener(this);

        recyclerView.setAdapter(adapter);
    }

    private void loadPodcasts() {
        podcastOrder = Preferences.getPodcastOrder(getActivity());

        final DatabaseHelper databaseHelper = DatabaseHelper.getInstance(getActivity().getApplicationContext());
        switch (podcastOrder) {
            case 0:
                podcasts = databaseHelper.getAllPodcasts();
                break;
            default:
                podcasts = databaseHelper.getAllPodcastsOrderByDate();
        }
    }

    private void updatePodcasts() {
        List<Podcast> oldPodcasts = adapter.getItems();
        DiffResult result = DiffUtil.calculateDiff(new PodcastComparator(oldPodcasts, podcasts));
        adapter.setItems(podcasts);
        result.dispatchUpdatesTo(adapter);
    }

    static class PodcastComparator extends DiffUtil.Callback {
        List<Podcast> oldPodcasts;
        List<Podcast> newPodcasts;

        PodcastComparator(List<Podcast> o, List<Podcast> n) {
            oldPodcasts = o;
            newPodcasts = n;
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            final Podcast oldPod = oldPodcasts.get(oldItemPosition);
            final Podcast newPod = newPodcasts.get(newItemPosition);

            if (oldPod.id != newPod.id) return false;
            if (!oldPod.url.equals(newPod.url)) return false;

            if (!oldPod.title.equals(newPod.title)) return false;
            if (!oldPod.description.equals(newPod.description)) return false;
//            if (!oldPod.imageFile.equals(newPod.imageFile)) return false;
//            if (!oldPod.imageUrl.equals(newPod.imageUrl)) return false;
            return oldPod.directory.equals(newPod.directory);
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            final Podcast oldEp = oldPodcasts.get(oldItemPosition);
            final Podcast newEp = newPodcasts.get(newItemPosition);
            return oldEp.id == newEp.id;
        }

        @Override
        public int getNewListSize() {
            return newPodcasts.size();
        }

        @Override
        public int getOldListSize() {
            return oldPodcasts.size();
        }
    }

    // Click listeners

    @Override
    public void onItemClick(View view, int position) {
        // Starts a new activity for this podcast's episodes
        Intent intent = new Intent(getActivity(), ActivityPodcastEpisodes.class);
        intent.putExtra("podcast", podcasts.get(position));
        startActivity(intent);
    }

}