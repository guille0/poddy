package com.guille.poddy.fragments;

import java.util.*;

import android.os.Bundle;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.guille.poddy.R;

import com.guille.poddy.activities.ActivityPodcastEpisodes;
import com.guille.poddy.database.*;

import org.jetbrains.annotations.NotNull;


public class FragmentPodcasts extends Fragment implements RecyclerViewPodcasts.ItemClickListener {
    private RecyclerView recyclerView;

    private List<Podcast> podcasts;

    public FragmentPodcasts() {
        Bundle bundle = new Bundle();
        this.setArguments(bundle);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_podcasts, container, false);
    }

    @Override
    public void onViewCreated(@NotNull View view, @Nullable Bundle savedInstanceState) {
        // Get all podcasts from database
        DatabaseHelper databaseHelper = DatabaseHelper.getInstance(getActivity().getApplicationContext());
        podcasts = databaseHelper.getAllPodcasts();

        // set up the RecyclerView
        recyclerView = getView().findViewById(R.id.podcastList);

        recyclerView.setHasFixedSize(true);

        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        RecyclerViewPodcasts adapter = new RecyclerViewPodcasts(getActivity(), podcasts);

        adapter.setClickListener(this);

        recyclerView.setAdapter(adapter);
    }


    @Override
    public void onItemClick(View view, int position) {
        // Starts a new activity for this podcast's episodes
        Intent intent = new Intent(getActivity(), ActivityPodcastEpisodes.class);
        intent.putExtra("podcast", podcasts.get(position));
        startActivity(intent);
    }

}