package com.example.sharedpreferencesapp;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class CalendarioEstudianteAdapter extends FragmentStateAdapter {

    public CalendarioEstudianteAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0) {
            return new FechasEstudianteFragment();
        } else {
            return new DocumentosEstudianteFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 2; // Tenemos dos pestañas: Fechas y Documentos
    }
}