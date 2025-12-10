package com.example.sharedpreferencesapp;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class ViewPagerAdapter extends FragmentStateAdapter {
    public ViewPagerAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle) {
        super(fragmentManager, lifecycle);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new LoginTabFragment(); // Tab para maestros (admin)
            case 1:
                return new StudentTabFragment(); // Tab para estudiantes
            case 2:
                return new AdministratorTabFragment(); // Tab para administradores
            default:
                return new LoginTabFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 3; // Ahora tenemos 3 pestañas: Maestro, Estudiante y Administrador
    }
}