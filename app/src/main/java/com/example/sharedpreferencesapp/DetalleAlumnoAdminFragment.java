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

public class DetalleAlumnoAdminFragment extends Fragment {

    private static final String ARG_STUDENT_ID = "studentId";
    private static final String ARG_STUDENT_NAME = "studentName";

    private String studentId;
    private String studentName;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private DetalleAlumnoTabsAdapter adapter;
    private TextView tvNombreAlumno;
    private ImageView btnBack;

    public static DetalleAlumnoAdminFragment newInstance(String studentId, String studentName) {
        DetalleAlumnoAdminFragment fragment = new DetalleAlumnoAdminFragment();
        Bundle args = new Bundle();
        args.putString(ARG_STUDENT_ID, studentId);
        args.putString(ARG_STUDENT_NAME, studentName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            studentId = getArguments().getString(ARG_STUDENT_ID);
            studentName = getArguments().getString(ARG_STUDENT_NAME);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_detalle_alumno_admin, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvNombreAlumno = view.findViewById(R.id.tv_nombre_alumno);
        btnBack = view.findViewById(R.id.btn_back);
        tabLayout = view.findViewById(R.id.tabLayout);
        viewPager = view.findViewById(R.id.viewPager);

        // Establecer nombre del alumno
        if (studentName != null) {
            tvNombreAlumno.setText(studentName);
        }

        // Botón de regreso
        btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        // Configurar adapter
        adapter = new DetalleAlumnoTabsAdapter(this, studentId);
        viewPager.setAdapter(adapter);

        // Conectar TabLayout con ViewPager2
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("Información");
                    break;
                case 1:
                    tab.setText("Estado de Residencia");
                    break;
                case 2:
                    tab.setText("Documentos");
                    break;
            }
        }).attach();
    }
}

