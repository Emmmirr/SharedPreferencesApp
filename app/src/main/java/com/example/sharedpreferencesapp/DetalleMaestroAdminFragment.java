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

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class DetalleMaestroAdminFragment extends Fragment {

    private static final String ARG_MAESTRO_ID = "maestroId";
    private static final String ARG_MAESTRO_NAME = "maestroName";

    private String maestroId;
    private String maestroName;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private DetalleMaestroTabsAdapter adapter;
    private TextView tvNombreMaestro;
    private ImageView btnBack;

    public static DetalleMaestroAdminFragment newInstance(String maestroId, String maestroName) {
        DetalleMaestroAdminFragment fragment = new DetalleMaestroAdminFragment();
        Bundle args = new Bundle();
        args.putString(ARG_MAESTRO_ID, maestroId);
        args.putString(ARG_MAESTRO_NAME, maestroName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            maestroId = getArguments().getString(ARG_MAESTRO_ID);
            maestroName = getArguments().getString(ARG_MAESTRO_NAME);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_detalle_maestro_admin, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvNombreMaestro = view.findViewById(R.id.tv_nombre_maestro);
        btnBack = view.findViewById(R.id.btn_back);
        tabLayout = view.findViewById(R.id.tabLayout);
        viewPager = view.findViewById(R.id.viewPager);

        // Establecer nombre del maestro
        if (maestroName != null) {
            tvNombreMaestro.setText(maestroName);
        }

        // Botón de regreso
        btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        // Configurar adapter
        adapter = new DetalleMaestroTabsAdapter(this, maestroId);
        viewPager.setAdapter(adapter);

        // Conectar TabLayout con ViewPager2
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("Información");
                    break;
                case 1:
                    tab.setText("Alumnos Asignados");
                    break;
            }
        }).attach();
    }
}

