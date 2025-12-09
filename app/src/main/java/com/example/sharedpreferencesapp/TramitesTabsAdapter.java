package com.example.sharedpreferencesapp;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class TramitesTabsAdapter extends FragmentStateAdapter {

    public TramitesTabsAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // position 0: Antes de iniciar
        // position 1: Durante
        // position 2: Al finalizar
        return TramitesTabFragment.newInstance(position);
    }

    @Override
    public int getItemCount() {
        return 3; // Antes, Durante, Finalizar
    }
}

