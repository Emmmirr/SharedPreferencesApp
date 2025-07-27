package com.example.sharedpreferencesapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class CalendarioFragment extends Fragment {

    public CalendarioFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_calendario, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Configurar TabLayout y ViewPager
        TabLayout tabLayout = view.findViewById(R.id.tabLayoutCalendario);
        ViewPager2 viewPager = view.findViewById(R.id.viewPagerCalendario);

        // Crear y asignar adaptador para las pestañas
        CalendarioEstudianteAdapter adapter = new CalendarioEstudianteAdapter(this);
        viewPager.setAdapter(adapter);

        // Configurar las pestañas con TabLayoutMediator
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) {
                tab.setText("Fechas");
                tab.setIcon(android.R.drawable.ic_menu_my_calendar);
            } else {
                tab.setText("Documentos");
                tab.setIcon(android.R.drawable.ic_menu_upload);
            }
        }).attach();
    }
}