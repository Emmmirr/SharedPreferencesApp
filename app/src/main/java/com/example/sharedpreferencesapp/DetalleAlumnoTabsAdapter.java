package com.example.sharedpreferencesapp;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class DetalleAlumnoTabsAdapter extends FragmentStateAdapter {

    private final String studentId;

    public DetalleAlumnoTabsAdapter(@NonNull Fragment fragment, String studentId) {
        super(fragment);
        this.studentId = studentId;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return InformacionAlumnoTabFragment.newInstance(studentId);
            case 1:
                return EstadoResidenciaAdminTabFragment.newInstance(studentId);
            case 2:
                return DocumentosAlumnoTabFragment.newInstance(studentId);
            default:
                return InformacionAlumnoTabFragment.newInstance(studentId);
        }
    }

    @Override
    public int getItemCount() {
        return 3; // Información, Estado de Residencia, Documentos
    }
}

