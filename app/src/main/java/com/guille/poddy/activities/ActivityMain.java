package com.guille.poddy.activities;

import android.os.Bundle;
import android.content.*;

import com.google.android.material.tabs.TabLayout;

import androidx.viewpager.widget.ViewPager;

import androidx.annotation.NonNull;

import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.content.pm.PackageManager;

import androidx.preference.*;
import com.guille.poddy.R;

import androidx.appcompat.widget.Toolbar;
import com.guille.poddy.eventbus.*;
import org.greenrobot.eventbus.*;


public class ActivityMain extends ActivityAbstract {
    private ViewPager viewPager;

    // Permissions stuff
    private final int REQUEST_CODE_PERMISSIONS = 101;
    private final String[] REQUIRED_PERMISSIONS = new String[]{
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.READ_EXTERNAL_STORAGE",
//            "android.permission.MEDIA_CONTENT_CONTROL"
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Request permissions if needed
        if (allPermissionsGranted()) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        // database test stuff
//        deleteDatabase("podcastDatabase");

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Set up the tabs
        SectionsPagerAdapterMain sectionsPagerAdapter = new SectionsPagerAdapterMain(this, getSupportFragmentManager());
        TabLayout tabs = findViewById(R.id.tabs);

        // set up viewpager
        viewPager = findViewById(R.id.viewPager);
        viewPager.setAdapter(sectionsPagerAdapter);
        viewPager.setOffscreenPageLimit(3);
        tabs.setupWithViewPager(viewPager);
    }

    // PERMISSIONS

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return true;
            }
        }
        return false;
    }
}