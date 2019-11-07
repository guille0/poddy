package com.guille.poddy.activities;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.guille.poddy.fragments.*;
import com.guille.poddy.*;

import java.util.*;


public class SectionsPagerAdapterMain extends FragmentPagerAdapter {
    private List<String> titles;

    private List<Integer> pagesContent = Arrays.asList(
            FragmentFactory.ContentShown.THIS_WEEK,
            FragmentFactory.ContentShown.ALL_PODCASTS_IMAGE,
            FragmentFactory.ContentShown.DOWNLOADED
    );

    private List<Fragment> pages = new ArrayList<>();

    public SectionsPagerAdapterMain(Context context, FragmentManager fm) {
        super(fm);
        titles = FragmentFactory.getFragmentTitles(context, pagesContent);

        // Create all the fragments here
        for (int kind : pagesContent) {
            pages.add(FragmentFactory.createBasicFragment(kind));
        }
    }

    @Override
    public Fragment getItem(int position) {
        return pages.get(position);
//        return FragmentFactory.createBasicFragment(pagesContent.get(position));
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return titles.get(position);
    }

    @Override
    public int getCount() {
        return pagesContent.size();
    }

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }


}