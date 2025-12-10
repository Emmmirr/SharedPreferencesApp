package com.example.sharedpreferencesapp;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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

public class ListaAlumnosSimpleFragment extends Fragment {

    private static final String TAG = "ListaAlumnosSimpleFragment";
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvNoAlumnos;
    private TextInputEditText etSearch;
    private ImageView btnBack;
    private FirebaseFirestore db;
    private List<UserProfile> alumnosList = new ArrayList<>();
    private List<UserProfile> filteredList = new ArrayList<>();
    private AlumnoAdminAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_lista_alumnos_simple, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recycler_alumnos);
        progressBar = view.findViewById(R.id.progress_bar);
        tvNoAlumnos = view.findViewById(R.id.tv_no_alumnos);
        etSearch = view.findViewById(R.id.etSearch);
        btnBack = view.findViewById(R.id.btn_back);

        // Configurar RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AlumnoAdminAdapter(getContext(), filteredList);

        // Configurar listeners
        adapter.setOnAlumnoClickListener(alumno -> {
            // Navegar a detalle del alumno
            String nombre = "";
            if (alumno.getFullName() != null && !alumno.getFullName().isEmpty()) {
                nombre = alumno.getFullName();
            } else if (alumno.getDisplayName() != null && !alumno.getDisplayName().isEmpty()) {
                nombre = alumno.getDisplayName();
            } else if (alumno.getEmail() != null && !alumno.getEmail().isEmpty()) {
                nombre = alumno.getEmail();
            } else {
                nombre = "Sin nombre";
            }

            DetalleAlumnoAdminFragment fragment = DetalleAlumnoAdminFragment.newInstance(
                    alumno.getUserId(), nombre
            );

            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.nav_host_fragment, fragment)
                    .addToBackStack(null)
                    .commit();
        });

        adapter.setOnEstadoClickListener(studentId -> {
            // Abrir BottomSheet para editar estado
            BottomSheetEditarEstado bottomSheet = BottomSheetEditarEstado.newInstance(studentId);
            bottomSheet.setOnEstadoGuardadoListener(() -> {
                // Recargar la lista para actualizar badges
                loadAlumnos();
            });
            bottomSheet.show(getParentFragmentManager(), "BottomSheetEditarEstado");
        });

        recyclerView.setAdapter(adapter);

        // Inicializar Firebase
        db = FirebaseFirestore.getInstance();

        // Configurar búsqueda
        setupSearch();

        // Botón de regreso
        btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        // Cargar alumnos
        loadAlumnos();
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterAlumnos(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterAlumnos(String query) {
        filteredList.clear();
        if (query.isEmpty()) {
            filteredList.addAll(alumnosList);
        } else {
            String lowerQuery = query.toLowerCase();
            for (UserProfile alumno : alumnosList) {
                String nombre = alumno.getFullName() != null ? alumno.getFullName().toLowerCase() : "";
                String email = alumno.getEmail() != null ? alumno.getEmail().toLowerCase() : "";
                String control = alumno.getControlNumber() != null ? alumno.getControlNumber().toLowerCase() : "";
                if (nombre.contains(lowerQuery) || email.contains(lowerQuery) || control.contains(lowerQuery)) {
                    filteredList.add(alumno);
                }
            }
        }
        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void loadAlumnos() {
        progressBar.setVisibility(View.VISIBLE);
        tvNoAlumnos.setVisibility(View.GONE);

        db.collection("user_profiles")
                .whereEqualTo("userType", "student")
                .get()
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        alumnosList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                UserProfile alumno = UserProfile.fromMap(document.getData());
                                alumno.setUserId(document.getId());
                                alumnosList.add(alumno);
                            } catch (Exception e) {
                                Log.e(TAG, "Error parseando alumno: " + document.getId(), e);
                            }
                        }
                        filteredList.clear();
                        filteredList.addAll(alumnosList);
                        adapter.notifyDataSetChanged();
                        updateEmptyState();
                    } else {
                        Log.e(TAG, "Error cargando alumnos", task.getException());
                        Toast.makeText(getContext(), "Error al cargar alumnos", Toast.LENGTH_SHORT).show();
                        updateEmptyState();
                    }
                });
    }

    private void updateEmptyState() {
        if (filteredList.isEmpty()) {
            tvNoAlumnos.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvNoAlumnos.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }
}

