package com.guille.poddy.fragments;

import com.guille.poddy.R;

import android.os.Bundle;
import androidx.preference.*;
import java.util.*;

import org.greenrobot.eventbus.*;

import com.guille.poddy.*;
import com.guille.poddy.eventbus.*;
import com.guille.poddy.fragments.FragmentFactory.ContentShown;

public class FragmentPreferences extends PreferenceFragmentCompat {
    public FragmentPreferences(){}

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String h) {
        addPreferencesFromResource(R.xml.fragment_preference);
        setValues();
        setUpListeners();
    }

    private void setValues() {
        ((ListPreference) findPreference("podcast_order")).setValueIndex(Preferences.getPodcastOrder(getActivity()));
        ((EditTextPreference) findPreference("download_limit")).setText(Integer.toString(Preferences.getDownloadLimit(getActivity())));
        ((ListPreference) findPreference("data_usage")).setValueIndex(Preferences.getDataUsage(getActivity()));
    }

    private void setUpListeners() {
        findPreference("podcast_order").setOnPreferenceChangeListener(
                (preference, value) -> {
                    if (value instanceof String) {
                        Preferences.setPodcastOrder(getActivity(), Integer.parseInt((String) value));
                        EventBus.getDefault().post(new MessageEvent.RefreshPodcast(""));
                        return true;
                    }
                    return false;
                });

        findPreference("data_usage").setOnPreferenceChangeListener(
                (preference, value) -> {
                    if (value instanceof String) {
                        Preferences.setDataUsage(getActivity(), Integer.parseInt((String) value));
                        return true;
                    }
                    return false;
                });

        findPreference("download_limit").setOnPreferenceChangeListener(
                (preference, value) -> {
                    if (value instanceof String) {
                        try {
                            final int n = Integer.parseInt((String) value);
                            if (n > 0 && n < Preferences.downloadLimitMax) {
                                Preferences.setDownloadLimit(getActivity(), n);
                                return true;
                            }
                            return false;
                        } catch (Exception e) {
                            return false;
                        }
                    }
                    return false;
                });
    }
}
