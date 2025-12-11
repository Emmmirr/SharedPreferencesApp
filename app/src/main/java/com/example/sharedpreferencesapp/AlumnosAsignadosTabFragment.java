package com.example.sharedpreferencesapp;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AlumnosAsignadosTabFragment extends Fragment {

    private static final String TAG = "AlumnosAsignadosTab";
    private static final String ARG_MAESTRO_ID = "maestroId";

    private String maestroId;
    private FirebaseFirestore db;
    private FirebaseManager firebaseManager;

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvNoAlumnos;
    private AlumnoSimpleAdapter adapter;
    private List<UserProfile> alumnosList = new ArrayList<>();

    public static AlumnosAsignadosTabFragment newInstance(String maestroId) {
        AlumnosAsignadosTabFragment fragment = new AlumnosAsignadosTabFragment();
        Bundle args = new Bundle();
        args.putString(ARG_MAESTRO_ID, maestroId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            maestroId = getArguments().getString(ARG_MAESTRO_ID);
        }
        db = FirebaseFirestore.getInstance();
        firebaseManager = new FirebaseManager();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_alumnos_asignados_tab, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recycler_alumnos);
        progressBar = view.findViewById(R.id.progress_bar);
        tvNoAlumnos = view.findViewById(R.id.tv_no_alumnos);

        // Configurar RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AlumnoSimpleAdapter(getContext(), alumnosList);

        // Configurar listener para click en alumno (navegar a detalle)
        adapter.setOnAlumnoClickListener(alumno -> {
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

        recyclerView.setAdapter(adapter);

        cargarAlumnosAsignados();
    }

    private void cargarAlumnosAsignados() {
        if (maestroId == null) return;

        progressBar.setVisibility(View.VISIBLE);
        tvNoAlumnos.setVisibility(View.GONE);

        Log.d(TAG, "Cargando alumnos asignados para maestro ID: " + maestroId);

        // Cargar alumnos asignados usando FirebaseManager
        firebaseManager.cargarEstudiantesAsignados(maestroId, task -> {
            progressBar.setVisibility(View.GONE);

            if (task.isSuccessful() && task.getResult() != null) {
                alumnosList.clear();
                Log.d(TAG, "Alumnos encontrados: " + task.getResult().size());

                for (QueryDocumentSnapshot document : task.getResult()) {
                    try {
                        UserProfile alumno = UserProfile.fromMap(document.getData());
                        alumno.setUserId(document.getId());
                        alumnosList.add(alumno);
                    } catch (Exception e) {
                        Log.e(TAG, "Error parseando alumno: " + document.getId(), e);
                    }
                }

                adapter.notifyDataSetChanged();
                updateEmptyState();
            } else {
                Log.e(TAG, "Error cargando alumnos asignados", task.getException());
                updateEmptyState();
            }
        });
    }

    private void updateEmptyState() {
        if (alumnosList.isEmpty()) {
            tvNoAlumnos.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvNoAlumnos.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }
}

