package com.guille.poddy.activities;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.guille.poddy.R;
import com.guille.poddy.fragments.FragmentEpisodes;
import com.guille.poddy.fragments.FragmentPodcasts;


public class SectionsPagerAdapterMain extends FragmentPagerAdapter {

    @StringRes
    private static final int[] TAB_TITLES = new int[]{R.string.tab_this_week, R.string.tab_all_podcasts, R.string.tab_downloaded};
    // TODO weakreference to context
    private final Context mContext;

    public SectionsPagerAdapterMain(Context context, FragmentManager fm) {
        super(fm);
        mContext = context;
    }

    @Override
    public Fragment getItem(int position) {
        Bundle bundle = new Bundle();
        FragmentEpisodes fragInfo = new FragmentEpisodes();
        switch (position) {
            case 0:
                bundle.putString("kind", "thisWeek");
                fragInfo.setArguments(bundle);
                return fragInfo;
            case 1:
                return new FragmentPodcasts();
            case 2:
                bundle.putString("kind", "downloaded");
                fragInfo.setArguments(bundle);
                return fragInfo;
        }
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return mContext.getResources().getString(TAB_TITLES[position]);
    }

    @Override
    public int getCount() {
        return 3;
    }

    @Override
    public int getItemPosition(@NonNull Object object) {
        return POSITION_NONE;
    }
}