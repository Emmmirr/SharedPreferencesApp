package com.example.sharedpreferencesapp;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ListaMaestrosFragment extends Fragment {

    private static final String TAG = "ListaMaestrosFragment";
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvNoMaestros;
    private TextInputEditText etSearch;
    private FirebaseFirestore db;
    private List<UserProfile> maestrosList = new ArrayList<>();
    private List<UserProfile> filteredList = new ArrayList<>();
    private MaestroAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_lista_maestros, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recycler_maestros);
        progressBar = view.findViewById(R.id.progress_bar);
        tvNoMaestros = view.findViewById(R.id.tv_no_maestros);
        etSearch = view.findViewById(R.id.etSearch);

        // Configurar RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new MaestroAdapter(getContext(), filteredList);
        recyclerView.setAdapter(adapter);

        // Inicializar Firebase
        db = FirebaseFirestore.getInstance();

        // Configurar búsqueda
        setupSearch();

        // Cargar maestros
        loadMaestros();
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterMaestros(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterMaestros(String query) {
        filteredList.clear();
        if (query.isEmpty()) {
            filteredList.addAll(maestrosList);
        } else {
            String lowerQuery = query.toLowerCase();
            for (UserProfile maestro : maestrosList) {
                String nombre = maestro.getFullName() != null ? maestro.getFullName().toLowerCase() : "";
                String email = maestro.getEmail() != null ? maestro.getEmail().toLowerCase() : "";
                if (nombre.contains(lowerQuery) || email.contains(lowerQuery)) {
                    filteredList.add(maestro);
                }
            }
        }
        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void loadMaestros() {
        progressBar.setVisibility(View.VISIBLE);
        tvNoMaestros.setVisibility(View.GONE);

        db.collection("user_profiles")
                .whereEqualTo("userType", "admin")
                .get()
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        maestrosList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                // Log para debug
                                Log.d(TAG, "Documento maestro ID: " + document.getId());
                                Log.d(TAG, "Datos del documento: " + document.getData());

                                UserProfile maestro = UserProfile.fromMap(document.getData());
                                maestro.setUserId(document.getId());

                                // Log para verificar datos parseados
                                Log.d(TAG, "Maestro parseado - Nombre: " + maestro.getFullName() +
                                        ", DisplayName: " + maestro.getDisplayName() +
                                        ", Email: " + maestro.getEmail());

                                maestrosList.add(maestro);
                            } catch (Exception e) {
                                Log.e(TAG, "Error parseando maestro: " + document.getId(), e);
                            }
                        }
                        filteredList.clear();
                        filteredList.addAll(maestrosList);
                        adapter.notifyDataSetChanged();
                        updateEmptyState();
                    } else {
                        Log.e(TAG, "Error cargando maestros", task.getException());
                        Toast.makeText(getContext(), "Error al cargar maestros", Toast.LENGTH_SHORT).show();
                        updateEmptyState();
                    }
                });
    }

    private void updateEmptyState() {
        if (filteredList.isEmpty()) {
            tvNoMaestros.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvNoMaestros.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }
}

