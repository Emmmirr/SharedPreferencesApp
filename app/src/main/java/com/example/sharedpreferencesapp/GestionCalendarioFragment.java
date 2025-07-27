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

public class GestionCalendarioFragment extends Fragment {

    // Ya no necesitamos el método newInstance ni la constante ARG_TAB_TO_OPEN aquí.

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_gestion_calendario, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TabLayout tabLayout = view.findViewById(R.id.tabLayoutCalendario);
        ViewPager2 viewPager = view.findViewById(R.id.viewPagerCalendario);

        CalendarioTabsAdapter adapter = new CalendarioTabsAdapter(this);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) {
                tab.setText("Fechas");
                tab.setIcon(android.R.drawable.ic_menu_my_calendar);
            } else {
                tab.setText("Documentos");
                tab.setIcon(android.R.drawable.ic_menu_upload);
            }
        }).attach();

        // --- INICIO DE CÓDIGO MODIFICADO ---
        // Leemos los argumentos que nos pasó el NavController desde la MainActivity
        if (getArguments() != null) {
            // El nombre del argumento ("TAB_TO_OPEN") debe coincidir con el usado en MainActivity
            int tabToOpen = getArguments().getInt("TAB_TO_OPEN", -1);
            if (tabToOpen != -1) {
                viewPager.post(() -> viewPager.setCurrentItem(tabToOpen, false));
            }
        }
        // --- FIN DE CÓDIGO MODIFICADO ---
    }
}