package com.guille.poddy.fragments;

import android.os.*;
import android.content.*;
import androidx.fragment.app.Fragment;

import com.guille.poddy.R;
import com.guille.poddy.fragments.recyclerviews.FragmentEpisodes;
import com.guille.poddy.fragments.recyclerviews.FragmentPodcasts;

import java.util.*;

public class FragmentFactory {


    public static class ContentShown {
        // FragmentEpisodes podcast episodes
        public static final int PODCAST_EPISODES = -1;

        // FragmentPodcasts
        public static final int ALL_PODCASTS_TEXT = 0;
        public static final int ALL_PODCASTS_IMAGE = 1;


        // FragmentEpisodes
        public static final int THIS_WEEK = 2;
        public static final int DOWNLOADED = 3;
        public static final int ALL_EPISODES = 4;

        public static final int[] POSSIBLE_TABS = new int[] {
                ALL_PODCASTS_TEXT,
                ALL_PODCASTS_IMAGE,
                THIS_WEEK,
                DOWNLOADED,
                ALL_EPISODES
        };
    }
    public static final int MAX_TABS = ContentShown.POSSIBLE_TABS.length;

    public static List<String> getFragmentTitles(Context context, List<Integer> pages) {
        List<String> titles = new ArrayList<>();
        for (int fragment : pages) {
            switch (fragment) {
                case ContentShown.ALL_PODCASTS_TEXT:
                case ContentShown.ALL_PODCASTS_IMAGE:
                    titles.add(context.getResources().getString(R.string.tab_all_podcasts));
                    break;
                case ContentShown.THIS_WEEK:
                    titles.add(context.getResources().getString(R.string.tab_this_week));
                    break;
                case ContentShown.DOWNLOADED:
                    titles.add(context.getResources().getString(R.string.tab_downloaded));
                    break;
                case ContentShown.ALL_EPISODES:
                    titles.add(context.getResources().getString(R.string.tab_all_episodes));
                    break;
            }
        }
        return titles;
    }

    public static String getFragmentDescription(Context context, int fragment) {
        switch (fragment) {
            case ContentShown.ALL_PODCASTS_TEXT:
                return context.getResources().getString(R.string.tab_desc_all_podcasts_text);
            case ContentShown.ALL_PODCASTS_IMAGE:
                return context.getResources().getString(R.string.tab_desc_all_podcasts_image);
            case ContentShown.THIS_WEEK:
                return context.getResources().getString(R.string.tab_desc_this_week);
            case ContentShown.DOWNLOADED:
                return context.getResources().getString(R.string.tab_desc_downloaded);
            case ContentShown.ALL_EPISODES:
                return context.getResources().getString(R.string.tab_desc_all_episodes);
            default:
                return "hey";
        }
    }



    public static Fragment createBasicFragment(int fragmentId) {
        Bundle bundle = new Bundle();
        FragmentAbstract fragment;

        switch (fragmentId) {
            // Podcasts
            case ContentShown.ALL_PODCASTS_TEXT:
                bundle.putInt("kind", ContentShown.ALL_PODCASTS_TEXT);
                fragment = new FragmentPodcasts();
                fragment.setArguments(bundle);
                return fragment;

            case ContentShown.ALL_PODCASTS_IMAGE:
                bundle.putInt("kind", ContentShown.ALL_PODCASTS_IMAGE);
                fragment = new FragmentPodcasts();
                fragment.setArguments(bundle);
                return fragment;

            // Episodes
            case ContentShown.THIS_WEEK:
                bundle.putInt("kind", ContentShown.THIS_WEEK);
                fragment = new FragmentEpisodes();
                fragment.setArguments(bundle);
                return fragment;


            case ContentShown.DOWNLOADED:
                bundle.putInt("kind", ContentShown.DOWNLOADED);
                fragment = new FragmentEpisodes();
                fragment.setArguments(bundle);
                return fragment;

            case ContentShown.ALL_EPISODES:
                bundle.putInt("kind", ContentShown.ALL_EPISODES);
                fragment = new FragmentEpisodes();
                fragment.setArguments(bundle);
                return fragment;

            default:
                bundle.putInt("kind", ContentShown.ALL_EPISODES);
                fragment = new FragmentEpisodes();
                fragment.setArguments(bundle);
                return fragment;
        }
    }

}
