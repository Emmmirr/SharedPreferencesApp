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
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudentsManagementFragment extends Fragment implements StudentAdapter.StudentActionListener {

    private static final String TAG = "StudentsManagementFrag";

    private RecyclerView recyclerView;
    private StudentAdapter adapter;
    private List<UserProfile> allStudentsList = new ArrayList<>();
    private List<UserProfile> filteredStudentsList = new ArrayList<>();
    private ProgressBar progressBar;
    private TextView tvNoStudents;
    private TextInputEditText etSearch;
    private TextView tabAll, tabPending, tabApproved;
    private FirebaseFirestore db;
    private String currentUserId;
    private String currentFilter = "all"; // "all", "pending", "approved"

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_students_management, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Inicializar vistas
        recyclerView = view.findViewById(R.id.recycler_students);
        progressBar = view.findViewById(R.id.progress_bar);
        tvNoStudents = view.findViewById(R.id.tv_no_students);
        etSearch = view.findViewById(R.id.etSearch);
        tabAll = view.findViewById(R.id.tabAll);
        tabPending = view.findViewById(R.id.tabPending);
        tabApproved = view.findViewById(R.id.tabApproved);

        ImageView btnBack = view.findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                if (getActivity() != null) {
                    getActivity().onBackPressed();
                }
            });
        }

        // Configurar RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new StudentAdapter(getContext(), filteredStudentsList, this);
        recyclerView.setAdapter(adapter);

        // Inicializar Firebase
        db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            currentUserId = currentUser.getUid();
            setupSearch();
            setupTabs();
            loadAssignedStudents();
        } else {
            tvNoStudents.setVisibility(View.VISIBLE);
            tvNoStudents.setText("Error: No hay sesión activa");
        }
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterStudents();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupTabs() {
        // Ocultar tabs ya que ahora solo mostramos alumnos asignados (todos aprobados)
        if (tabAll != null) tabAll.setVisibility(View.GONE);
        if (tabPending != null) tabPending.setVisibility(View.GONE);
        if (tabApproved != null) tabApproved.setVisibility(View.GONE);

        // Ya no hay filtros, todos los alumnos mostrados están asignados y aprobados
        currentFilter = "all";
    }

    private void updateTabSelection() {
        // Resetear todos los tabs
        tabAll.setTextColor(getResources().getColor(R.color.text_secondary));
        tabAll.setTextSize(16);
        tabAll.setTypeface(null, android.graphics.Typeface.NORMAL);

        tabPending.setTextColor(getResources().getColor(R.color.text_secondary));
        tabPending.setTextSize(16);
        tabPending.setTypeface(null, android.graphics.Typeface.NORMAL);

        tabApproved.setTextColor(getResources().getColor(R.color.text_secondary));
        tabApproved.setTextSize(16);
        tabApproved.setTypeface(null, android.graphics.Typeface.NORMAL);

        // Resaltar el tab seleccionado
        switch (currentFilter) {
            case "all":
                tabAll.setTextColor(getResources().getColor(R.color.primary));
                tabAll.setTextSize(16);
                tabAll.setTypeface(null, android.graphics.Typeface.BOLD);
                break;
            case "pending":
                tabPending.setTextColor(getResources().getColor(R.color.primary));
                tabPending.setTextSize(16);
                tabPending.setTypeface(null, android.graphics.Typeface.BOLD);
                break;
            case "approved":
                tabApproved.setTextColor(getResources().getColor(R.color.primary));
                tabApproved.setTextSize(16);
                tabApproved.setTypeface(null, android.graphics.Typeface.BOLD);
                break;
        }
    }

    private void filterStudents() {
        filteredStudentsList.clear();

        if (etSearch == null) {
            return;
        }

        String searchQuery = etSearch.getText().toString().toLowerCase().trim();

        for (UserProfile student : allStudentsList) {
            // Ya no hay filtros, todos los alumnos mostrados están asignados y aprobados
            boolean matchesFilter = true;

            if (!matchesFilter) continue;

            // Aplicar búsqueda
            if (searchQuery.isEmpty()) {
                filteredStudentsList.add(student);
            } else {
                String fullName = (student.getFullName().isEmpty() ?
                        student.getDisplayName() : student.getFullName()).toLowerCase();
                String controlNumber = student.getControlNumber() != null ? student.getControlNumber().toLowerCase() : "";
                String career = student.getCareer() != null ? student.getCareer().toLowerCase() : "";
                String email = student.getEmail() != null ? student.getEmail().toLowerCase() : "";

                if (fullName.contains(searchQuery) ||
                        controlNumber.contains(searchQuery) ||
                        career.contains(searchQuery) ||
                        email.contains(searchQuery)) {
                    filteredStudentsList.add(student);
                }
            }
        }

        if (adapter != null) {
            adapter.updateList(filteredStudentsList);

            // Forzar actualización del RecyclerView
            if (recyclerView != null) {
                recyclerView.post(() -> {
                    recyclerView.invalidate();
                    recyclerView.requestLayout();
                });
            }
        }

        // Mostrar mensaje si no hay estudiantes
        if (filteredStudentsList.isEmpty()) {
            if (tvNoStudents != null) {
                tvNoStudents.setVisibility(View.VISIBLE);
                if (searchQuery.isEmpty()) {
                    tvNoStudents.setText("No hay estudiantes");
                } else {
                    tvNoStudents.setText("No se encontraron estudiantes");
                }
            }
            if (recyclerView != null) {
                recyclerView.setVisibility(View.GONE);
            }
        } else {
            if (tvNoStudents != null) {
                tvNoStudents.setVisibility(View.GONE);
            }
            if (recyclerView != null) {
                recyclerView.setVisibility(View.VISIBLE);
            }
        }
    }

    private void loadAssignedStudents() {
        progressBar.setVisibility(View.VISIBLE);
        tvNoStudents.setVisibility(View.GONE);

        // Solo cargar alumnos asignados y aprobados (asignados por el administrador)
        db.collection("user_profiles")
                .whereEqualTo("supervisorId", currentUserId)
                .whereEqualTo("isApproved", true)
                .get()
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);

                    if (task.isSuccessful()) {
                        allStudentsList.clear();

                        // Filtrar manualmente por userType
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                String userType = document.getString("userType");
                                if (userType != null && userType.equals("student")) {
                                    UserProfile student = UserProfile.fromMap(document.getData());
                                    // Asegurar que el userId esté establecido
                                    if (student.getUserId() == null || student.getUserId().isEmpty()) {
                                        student.setUserId(document.getId());
                                    }
                                    allStudentsList.add(student);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parseando estudiante: " + document.getId(), e);
                            }
                        }

                        // Verificar que las vistas estén inicializadas antes de filtrar
                        if (etSearch != null && tabAll != null) {
                            filterStudents();
                        }

                    } else {
                        Log.e(TAG, "Error obteniendo estudiantes", task.getException());
                        Toast.makeText(getContext(), "Error al cargar estudiantes", Toast.LENGTH_SHORT).show();
                        tvNoStudents.setVisibility(View.VISIBLE);
                    }
                });
    }

    @Override
    public void onApproveClicked(UserProfile student, int position) {
        // Ya no se puede aprobar desde aquí, el administrador es quien asigna
        Toast.makeText(getContext(), "Los alumnos son asignados por el administrador", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onViewProtocolClicked(UserProfile student) {
        // Aquí se implementaría la lógica para ver el protocolo del estudiante
        Toast.makeText(getContext(), "Ver protocolo: Funcionalidad en desarrollo", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onStudentClicked(UserProfile student) {
        // Implementar vista detallada de estudiante
        Toast.makeText(getContext(), "Ver detalles de: " +
                        (student.getFullName().isEmpty() ? student.getDisplayName() : student.getFullName()),
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onMenuClicked(UserProfile student, View view) {
        // Mostrar menú de opciones (sin opción de aprobar)
        String[] options = {"Ver detalles", "Ver protocolo"};

        new AlertDialog.Builder(getContext())
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            onStudentClicked(student);
                            break;
                        case 1:
                            onViewProtocolClicked(student);
                            break;
                    }
                })
                .show();
    }
}
