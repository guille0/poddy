package com.guille.poddy.activities;

import com.guille.poddy.R;
import com.guille.poddy.fragments.FragmentPreferences;
import android.os.*;
import android.content.*;

public class ActivityPreferences extends ActivityAbstract {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);

        getSupportFragmentManager().beginTransaction().replace(R.id.layoutFragment,
                new FragmentPreferences()).commit();
    }

}