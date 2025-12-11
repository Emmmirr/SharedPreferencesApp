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
        recyclerView.setHasFixedSize(false); // Permitir que el RecyclerView calcule el tamaño dinámicamente
        adapter = new StudentAdapter(getContext(), filteredStudentsList, this);
        recyclerView.setAdapter(adapter);
        Log.d(TAG, "RecyclerView configurado - adapter itemCount inicial: " + adapter.getItemCount());

        // Asegurar que el RecyclerView tenga altura válida cuando el layout se complete
        recyclerView.getViewTreeObserver().addOnGlobalLayoutListener(new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (recyclerView.getHeight() > 0 && adapter != null && adapter.getItemCount() > 0) {
                    Log.d(TAG, "Layout completo - RecyclerView height: " + recyclerView.getHeight() +
                            ", adapter items: " + adapter.getItemCount());
                    // Forzar actualización si hay items pero no se están mostrando
                    if (recyclerView.getChildCount() == 0 && adapter.getItemCount() > 0) {
                        Log.d(TAG, "Forzando actualización después de layout completo");
                        adapter.notifyDataSetChanged();
                    }
                }
                // Remover el listener después de la primera ejecución
                recyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

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

        Log.d(TAG, "filterStudents - filteredStudentsList size: " + filteredStudentsList.size());

        if (adapter != null) {
            adapter.updateList(filteredStudentsList);
            Log.d(TAG, "Adapter actualizado en filterStudents, itemCount: " + adapter.getItemCount());

            // Forzar actualización del RecyclerView
            if (recyclerView != null) {
                recyclerView.post(() -> {
                    recyclerView.invalidate();
                    recyclerView.requestLayout();
                    Log.d(TAG, "RecyclerView invalidado en filterStudents");
                });
            }
        } else {
            Log.e(TAG, "Adapter es null en filterStudents!");
        }

        // Actualizar mensaje según si hay búsqueda activa
        if (filteredStudentsList.isEmpty()) {
            if (tvNoStudents != null) {
                tvNoStudents.setVisibility(View.VISIBLE);
                if (searchQuery.isEmpty()) {
                    tvNoStudents.setText("No hay estudiantes asignados");
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

        Log.d(TAG, "Cargando estudiantes asignados para maestro ID: " + currentUserId);

        // Cargar alumnos asignados: usar supervisorId y userType en la consulta
        // Filtrar isApproved manualmente para evitar problemas con índices compuestos
        db.collection("user_profiles")
                .whereEqualTo("supervisorId", currentUserId)
                .whereEqualTo("userType", "student")
                .get()
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);

                    if (task.isSuccessful()) {
                        allStudentsList.clear();

                        Log.d(TAG, "Consulta exitosa, documentos encontrados: " + task.getResult().size());

                        // Filtrar manualmente por isApproved (solo mostrar aprobados)
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                UserProfile student = UserProfile.fromMap(document.getData());

                                // Asegurar que el userId esté establecido
                                if (student.getUserId() == null || student.getUserId().isEmpty()) {
                                    student.setUserId(document.getId());
                                }

                                // Solo agregar estudiantes aprobados (asignados por el administrador)
                                if (student.isApproved()) {
                                    Log.d(TAG, "Estudiante aprobado encontrado: " +
                                            (student.getFullName().isEmpty() ? student.getDisplayName() : student.getFullName()) +
                                            ", ID: " + student.getUserId());
                                    allStudentsList.add(student);
                                } else {
                                    Log.d(TAG, "Estudiante no aprobado omitido: " + student.getUserId());
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parseando estudiante: " + document.getId(), e);
                            }
                        }

                        Log.d(TAG, "Total estudiantes aprobados cargados: " + allStudentsList.size());

                        // Actualizar la lista filtrada
                        filteredStudentsList.clear();
                        filteredStudentsList.addAll(allStudentsList);

                        Log.d(TAG, "Actualizando adapter con " + filteredStudentsList.size() + " estudiantes");
                        Log.d(TAG, "Adapter es null: " + (adapter == null));
                        Log.d(TAG, "RecyclerView es null: " + (recyclerView == null));

                        // Actualizar el adapter (el callback ya está en el hilo principal)
                        if (adapter != null) {
                            adapter.updateList(filteredStudentsList);
                            Log.d(TAG, "Adapter actualizado, itemCount: " + adapter.getItemCount());
                        } else {
                            Log.e(TAG, "Adapter es null!");
                        }

                        // Actualizar estado de vistas
                        updateEmptyState();

                        // Forzar actualización del RecyclerView
                        if (recyclerView != null) {
                            // Asegurar que el RecyclerView esté visible
                            recyclerView.setVisibility(View.VISIBLE);

                            // Esperar a que el layout se complete completamente
                            // Usar múltiples posts para asegurar que el layout se haya medido
                            recyclerView.post(() -> {
                                recyclerView.post(() -> {
                                    // Verificar que el RecyclerView tenga altura válida
                                    View parent = (View) recyclerView.getParent();
                                    if (parent != null) {
                                        Log.d(TAG, "FrameLayout parent height: " + parent.getHeight());
                                        if (parent.getHeight() > 0) {
                                            forceRecyclerViewUpdate();
                                        } else {
                                            // Si aún no tiene altura, esperar un poco más
                                            recyclerView.postDelayed(() -> {
                                                forceRecyclerViewUpdate();
                                            }, 200);
                                        }
                                    } else {
                                        forceRecyclerViewUpdate();
                                    }
                                });
                            });
                        } else {
                            Log.e(TAG, "RecyclerView es null!");
                        }

                    } else {
                        Log.e(TAG, "Error obteniendo estudiantes", task.getException());
                        if (task.getException() != null) {
                            Log.e(TAG, "Detalles del error: " + task.getException().getMessage());
                        }
                        Toast.makeText(getContext(), "Error al cargar estudiantes", Toast.LENGTH_SHORT).show();
                        tvNoStudents.setVisibility(View.VISIBLE);
                        tvNoStudents.setText("Error al cargar estudiantes. Verifique su conexión.");
                    }
                });
    }

    private void forceRecyclerViewUpdate() {
        if (recyclerView == null || adapter == null) {
            Log.e(TAG, "forceRecyclerViewUpdate - recyclerView o adapter es null");
            return;
        }

        Log.d(TAG, "forceRecyclerViewUpdate - Forzando actualización del RecyclerView");
        Log.d(TAG, "Adapter itemCount: " + adapter.getItemCount());

        // Verificar LayoutManager
        if (recyclerView.getLayoutManager() == null) {
            Log.e(TAG, "LayoutManager es null, configurando...");
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        }

        // Verificar dimensiones del RecyclerView y su parent
        View parent = (View) recyclerView.getParent();
        int parentHeight = parent != null ? parent.getHeight() : 0;
        int rvHeight = recyclerView.getHeight();

        Log.d(TAG, "RecyclerView dimensions antes: width=" + recyclerView.getWidth() +
                ", height=" + rvHeight + ", parent height=" + parentHeight);

        // Si el RecyclerView no tiene altura pero el parent sí, forzar layout
        if (rvHeight == 0 && parentHeight > 0) {
            Log.d(TAG, "Forzando layout del RecyclerView...");
            recyclerView.getLayoutParams().height = parentHeight;
            recyclerView.requestLayout();
        }

        // Forzar notificación
        adapter.notifyDataSetChanged();

        // Forzar invalidación y layout
        recyclerView.invalidate();
        recyclerView.requestLayout();

        // Verificar después de un momento
        recyclerView.post(() -> {
            Log.d(TAG, "RecyclerView dimensions después: width=" +
                    recyclerView.getWidth() + ", height=" + recyclerView.getHeight());
            Log.d(TAG, "onCreateViewHolder llamado: " +
                    (recyclerView.getChildCount() > 0 ? "SÍ (hay " + recyclerView.getChildCount() + " hijos)" : "NO"));
        });
    }

    private void updateEmptyState() {
        Log.d(TAG, "updateEmptyState - filteredStudentsList size: " + filteredStudentsList.size());

        if (filteredStudentsList.isEmpty()) {
            Log.d(TAG, "Lista vacía, mostrando mensaje");
            if (tvNoStudents != null) {
                tvNoStudents.setVisibility(View.VISIBLE);
                tvNoStudents.setText("No hay estudiantes asignados");
            }
            if (recyclerView != null) {
                recyclerView.setVisibility(View.GONE);
            }
        } else {
            Log.d(TAG, "Lista tiene elementos, ocultando mensaje y mostrando RecyclerView");
            if (tvNoStudents != null) {
                tvNoStudents.setVisibility(View.GONE);
            }
            if (recyclerView != null) {
                recyclerView.setVisibility(View.VISIBLE);
                Log.d(TAG, "RecyclerView visibility establecido a VISIBLE");
            }
        }
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
