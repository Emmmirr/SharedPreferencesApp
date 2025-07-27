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
            case 1:
                return new SignupTabFragment(); // Registro admin (existente)
            case 2:
                return new StudentTabFragment(); // Nuevo tab para estudiantes
            default:
                return new LoginTabFragment(); // Login admin (existente)
        }
    }

    @Override
    public int getItemCount() {
        return 3; // Ahora tenemos 3 pestañas
    }
}