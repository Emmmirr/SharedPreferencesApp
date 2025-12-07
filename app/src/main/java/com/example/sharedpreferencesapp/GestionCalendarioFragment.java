package com.example.sharedpreferencesapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

public class GestionCalendarioFragment extends Fragment {

    private ViewPager2 viewPager;
    private TextView tabFechas, tabDocumentos;
    private CalendarioTabsAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_gestion_calendario, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Inicializar vistas
        viewPager = view.findViewById(R.id.viewPagerCalendario);
        tabFechas = view.findViewById(R.id.tabFechas);
        tabDocumentos = view.findViewById(R.id.tabDocumentos);

        ImageView btnBack = view.findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                if (getActivity() != null) {
                    getActivity().onBackPressed();
                }
            });
        }

        // Configurar ViewPager
        adapter = new CalendarioTabsAdapter(this);
        viewPager.setAdapter(adapter);

        // Configurar tabs personalizados
        setupTabs();

        // Leer argumentos para abrir una pestaña específica
        if (getArguments() != null) {
            int tabToOpen = getArguments().getInt("TAB_TO_OPEN", -1);
            if (tabToOpen != -1) {
                viewPager.post(() -> viewPager.setCurrentItem(tabToOpen, false));
                updateTabSelection(tabToOpen);
            }
        }
    }

    private void setupTabs() {
        tabFechas.setOnClickListener(v -> {
            viewPager.setCurrentItem(0, true);
            updateTabSelection(0);
        });

        tabDocumentos.setOnClickListener(v -> {
            viewPager.setCurrentItem(1, true);
            updateTabSelection(1);
        });

        // Listener para sincronizar tabs con ViewPager
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateTabSelection(position);
            }
        });

        // Seleccionar primera pestaña por defecto
        updateTabSelection(0);
    }

    private void updateTabSelection(int position) {
        if (position == 0) {
            // Pestaña Fechas seleccionada
            tabFechas.setTextColor(getResources().getColor(R.color.primary));
            tabFechas.setTextSize(16);
            tabFechas.setTypeface(null, android.graphics.Typeface.BOLD);
            tabDocumentos.setTextColor(getResources().getColor(R.color.text_secondary));
            tabDocumentos.setTextSize(16);
            tabDocumentos.setTypeface(null, android.graphics.Typeface.NORMAL);
        } else {
            // Pestaña Documentos seleccionada
            tabDocumentos.setTextColor(getResources().getColor(R.color.primary));
            tabDocumentos.setTextSize(16);
            tabDocumentos.setTypeface(null, android.graphics.Typeface.BOLD);
            tabFechas.setTextColor(getResources().getColor(R.color.text_secondary));
            tabFechas.setTextSize(16);
            tabFechas.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
    }
}