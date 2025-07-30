package com.example.sharedpreferencesapp;

import android.os.Bundle;
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
    private List<UserProfile> studentsList = new ArrayList<>();
    private ProgressBar progressBar;
    private TextView tvNoStudents;
    private FirebaseFirestore db;
    private String currentUserId;

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

        // Configurar RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new StudentAdapter(getContext(), studentsList, this);
        recyclerView.setAdapter(adapter);

        // Inicializar Firebase
        db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            currentUserId = currentUser.getUid();
            loadAssignedStudents();
        }
    }

    private void loadAssignedStudents() {
        progressBar.setVisibility(View.VISIBLE);
        tvNoStudents.setVisibility(View.GONE);

        db.collection("users")
                .whereEqualTo("supervisorId", currentUserId)
                .whereEqualTo("userType", "student")
                .get()
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);

                    if (task.isSuccessful()) {
                        studentsList.clear();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            UserProfile student = UserProfile.fromMap(document.getData());
                            studentsList.add(student);
                        }

                        adapter.notifyDataSetChanged();

                        // Mostrar mensaje si no hay estudiantes
                        if (studentsList.isEmpty()) {
                            tvNoStudents.setVisibility(View.VISIBLE);
                        } else {
                            tvNoStudents.setVisibility(View.GONE);
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
        // Actualizar estado de aprobación
        Map<String, Object> updates = new HashMap<>();
        updates.put("isApproved", true);

        db.collection("users")
                .document(student.getUserId())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    // Actualizar modelo local
                    student.setApproved(true);
                    adapter.updateStudent(student, position);
                    Toast.makeText(getContext(), "Estudiante aprobado", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error aprobando estudiante", e);
                    Toast.makeText(getContext(), "Error al aprobar estudiante", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onViewProtocolClicked(UserProfile student) {
        // Aquí se implementaría la lógica para ver el protocolo del estudiante
        Toast.makeText(getContext(), "Ver protocolo: Funcionalidad en desarrollo", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onStudentClicked(UserProfile student) {
        // Implementar vista detallada de estudiante
        Toast.makeText(getContext(), "Ver detalles de: " + student.getFullName(), Toast.LENGTH_SHORT).show();
    }
}