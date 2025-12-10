package com.example.sharedpreferencesapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

public class ListaAlumnosAdminFragment extends Fragment {

    private CardView cardListaAlumnos;
    private CardView cardNumerosAutorizados;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_opciones_alumnos, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        cardListaAlumnos = view.findViewById(R.id.card_lista_alumnos);
        cardNumerosAutorizados = view.findViewById(R.id.card_numeros_autorizados);

        // Configurar click listeners
        cardListaAlumnos.setOnClickListener(v -> abrirListaAlumnos());
        cardNumerosAutorizados.setOnClickListener(v -> abrirBloquesAutorizados());
    }

    private void abrirListaAlumnos() {
        ListaAlumnosSimpleFragment fragment = new ListaAlumnosSimpleFragment();
        FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.nav_host_fragment, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void abrirBloquesAutorizados() {
        BloquesAutorizadosFragment fragment = new BloquesAutorizadosFragment();
        FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.nav_host_fragment, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}

