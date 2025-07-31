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
        // --- INICIO DE CÓDIGO MODIFICADO ---
        if (position == 0) {
            // Pestaña 0: Muestra las fechas asignadas en modo solo lectura.
            return new FechasAsignadasFragment();
        } else {
            // Pestaña 1: Muestra la interfaz para subir los documentos del calendario.
            return new MiCalendarioDocumentosFragment();
        }
        // --- FIN DE CÓDIGO MODIFICADO ---
    }

    @Override
    public int getItemCount() {
        return 2; // Tenemos dos pestañas: Fechas y Documentos
    }
}