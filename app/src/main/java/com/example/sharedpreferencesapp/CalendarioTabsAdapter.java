package com.example.sharedpreferencesapp;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class CalendarioTabsAdapter extends FragmentStateAdapter {

    public CalendarioTabsAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0) {
            return new ListaCalendariosFragment();
        }
        return new DocumentosTabFragment();
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}