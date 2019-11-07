package com.guille.poddy.fragments;

import androidx.fragment.app.Fragment;
import com.guille.poddy.eventbus.*;
import org.greenrobot.eventbus.*;
import com.guille.poddy.eventbus.*;
import android.content.*;
import android.os.*;

public abstract class FragmentAbstract extends Fragment {
    @Override
    public void onStart() {
        super.onStart();
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        } else {
            EventBus.getDefault().unregister(this);
            EventBus.getDefault().register(this);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }
}
