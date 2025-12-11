package com.example.sharedpreferencesapp;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class DetalleMaestroTabsAdapter extends FragmentStateAdapter {

    private final String maestroId;

    public DetalleMaestroTabsAdapter(@NonNull Fragment fragment, String maestroId) {
        super(fragment);
        this.maestroId = maestroId;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return InformacionMaestroTabFragment.newInstance(maestroId);
            case 1:
                return AlumnosAsignadosTabFragment.newInstance(maestroId);
            default:
                return InformacionMaestroTabFragment.newInstance(maestroId);
        }
    }

    @Override
    public int getItemCount() {
        return 2; // Información, Alumnos Asignados
    }
}

