package com.example.sharedpreferencesapp;

import android.app.AlertDialog;
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

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ListaAlumnosAdminFragment extends Fragment {

    private static final String TAG = "ListaAlumnosAdminFragment";
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvNoAlumnos;
    private TextInputEditText etSearch;
    private FirebaseFirestore db;
    private List<UserProfile> alumnosList = new ArrayList<>();
    private List<UserProfile> filteredList = new ArrayList<>();
    private AlumnoAdminAdapter adapter;

    // Números de control autorizados
    private RecyclerView recyclerAutorizados;
    private TextView tvNoAutorizados;
    private MaterialButton btnAgregarAutorizado;
    private List<String> numerosAutorizadosList = new ArrayList<>();
    private NumeroAutorizadoAdapter numeroAutorizadoAdapter;
    private FirebaseManager firebaseManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_lista_alumnos_admin, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recycler_alumnos);
        progressBar = view.findViewById(R.id.progress_bar);
        tvNoAlumnos = view.findViewById(R.id.tv_no_alumnos);
        etSearch = view.findViewById(R.id.etSearch);

        // Números de control autorizados
        recyclerAutorizados = view.findViewById(R.id.recycler_autorizados);
        tvNoAutorizados = view.findViewById(R.id.tv_no_autorizados);
        btnAgregarAutorizado = view.findViewById(R.id.btn_agregar_autorizado);

        // Configurar RecyclerView de alumnos
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AlumnoAdminAdapter(getContext(), filteredList);
        recyclerView.setAdapter(adapter);

        // Configurar RecyclerView de números autorizados
        recyclerAutorizados.setLayoutManager(new LinearLayoutManager(getContext()));
        numeroAutorizadoAdapter = new NumeroAutorizadoAdapter(getContext(), numerosAutorizadosList, this::eliminarNumeroAutorizado);
        recyclerAutorizados.setAdapter(numeroAutorizadoAdapter);

        // Inicializar Firebase
        db = FirebaseFirestore.getInstance();
        firebaseManager = new FirebaseManager();

        // Configurar búsqueda
        setupSearch();

        // Configurar botón agregar número autorizado
        btnAgregarAutorizado.setOnClickListener(v -> mostrarDialogoAgregarAutorizado());

        // Cargar alumnos
        loadAlumnos();

        // Cargar números autorizados
        loadNumerosAutorizados();
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
                                // Log para debug
                                Log.d(TAG, "Documento alumno ID: " + document.getId());
                                Log.d(TAG, "Datos del documento: " + document.getData());

                                UserProfile alumno = UserProfile.fromMap(document.getData());
                                alumno.setUserId(document.getId());

                                // Log para verificar datos parseados
                                Log.d(TAG, "Alumno parseado - Nombre: " + alumno.getFullName() +
                                        ", DisplayName: " + alumno.getDisplayName() +
                                        ", Email: " + alumno.getEmail() +
                                        ", Control: " + alumno.getControlNumber() +
                                        ", Carrera: " + alumno.getCareer());

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

    // --- MÉTODOS PARA NÚMEROS DE CONTROL AUTORIZADOS ---

    private void loadNumerosAutorizados() {
        firebaseManager.obtenerNumerosAutorizados(task -> {
            if (task.isSuccessful()) {
                numerosAutorizadosList.clear();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    String numeroControl = document.getString("numeroControl");
                    if (numeroControl != null && !numeroControl.isEmpty()) {
                        numerosAutorizadosList.add(numeroControl);
                    }
                }
                numeroAutorizadoAdapter.notifyDataSetChanged();
                updateAutorizadosEmptyState();
            } else {
                Log.e(TAG, "Error cargando números autorizados", task.getException());
            }
        });
    }

    private void mostrarDialogoAgregarAutorizado() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_agregar_autorizado, null);
        builder.setView(dialogView);

        TextInputEditText etNumeroControl = dialogView.findViewById(R.id.et_numero_control);
        MaterialButton btnCancelar = dialogView.findViewById(R.id.btn_cancelar);
        MaterialButton btnAgregar = dialogView.findViewById(R.id.btn_agregar);

        AlertDialog dialog = builder.create();

        btnCancelar.setOnClickListener(v -> dialog.dismiss());
        btnAgregar.setOnClickListener(v -> {
            String numeroControl = etNumeroControl.getText().toString().trim();
            if (numeroControl.isEmpty()) {
                Toast.makeText(getContext(), "Por favor ingresa un número de control", Toast.LENGTH_SHORT).show();
                return;
            }

            // Verificar si ya existe
            if (numerosAutorizadosList.contains(numeroControl)) {
                Toast.makeText(getContext(), "Este número de control ya está registrado", Toast.LENGTH_SHORT).show();
                return;
            }

            firebaseManager.agregarNumeroAutorizado(numeroControl,
                    () -> {
                        Toast.makeText(getContext(), "Número de control agregado exitosamente", Toast.LENGTH_SHORT).show();
                        loadNumerosAutorizados();
                        dialog.dismiss();
                    },
                    e -> {
                        Log.e(TAG, "Error agregando número autorizado", e);
                        Toast.makeText(getContext(), "Error al agregar número de control: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        dialog.show();
    }

    private void eliminarNumeroAutorizado(String numeroControl) {
        new AlertDialog.Builder(getContext())
                .setTitle("Eliminar Número de Control")
                .setMessage("¿Estás seguro de que deseas eliminar el número de control " + numeroControl + "?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    firebaseManager.eliminarNumeroAutorizado(numeroControl,
                            () -> {
                                Toast.makeText(getContext(), "Número de control eliminado", Toast.LENGTH_SHORT).show();
                                loadNumerosAutorizados();
                            },
                            e -> {
                                Log.e(TAG, "Error eliminando número autorizado", e);
                                Toast.makeText(getContext(), "Error al eliminar número de control: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void updateAutorizadosEmptyState() {
        if (numerosAutorizadosList.isEmpty()) {
            tvNoAutorizados.setVisibility(View.VISIBLE);
            recyclerAutorizados.setVisibility(View.GONE);
        } else {
            tvNoAutorizados.setVisibility(View.GONE);
            recyclerAutorizados.setVisibility(View.VISIBLE);
        }
    }
}

