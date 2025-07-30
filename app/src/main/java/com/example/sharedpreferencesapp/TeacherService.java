package com.example.sharedpreferencesapp;

import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio para gestionar operaciones relacionadas con los maestros
 */
public class TeacherService {
    private static final String TAG = "TeacherService";
    private final FirebaseFirestore db;

    public TeacherService() {
        db = FirebaseFirestore.getInstance();
    }

    /**
     * Modelo simple para representar a un maestro en la UI
     */
    public static class Teacher {
        private final String id;
        private final String fullName;
        private final String email;

        public Teacher(String id, String fullName, String email) {
            this.id = id;
            this.fullName = fullName;
            this.email = email;
        }

        public String getId() { return id; }
        public String getFullName() { return fullName; }
        public String getEmail() { return email; }

        @Override
        public String toString() {
            return fullName; // Esto es lo que se mostrará en el Spinner
        }
    }

    /**
     * Obtiene la lista de maestros disponibles
     */
    public void getAvailableTeachers(OnTeachersLoadedListener listener) {
        // Usando user_profiles en lugar de users para coincidir con tu estructura de base de datos
        Log.d(TAG, "Iniciando búsqueda de maestros en Firestore...");
        db.collection("user_profiles")
                .whereEqualTo("userType", "admin") // Asumimos que los maestros tienen userType="admin"
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Teacher> teachers = new ArrayList<>();
                        Log.d(TAG, "Consulta exitosa, documentos: " + task.getResult().size());

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String id = document.getId();

                            // Obtener nombre del profesor con fallbacks por si algún campo está vacío
                            String fullName = document.getString("fullName");
                            if (fullName == null || fullName.isEmpty()) {
                                fullName = document.getString("displayName");
                            }
                            if (fullName == null || fullName.isEmpty()) {
                                // Si todavía no tenemos nombre, usamos el email o un valor predeterminado
                                fullName = document.getString("email");
                                if (fullName == null || fullName.isEmpty()) {
                                    fullName = "Maestro " + id.substring(0, Math.min(id.length(), 5));
                                }
                            }

                            String email = document.getString("email");
                            if (email == null) email = "";

                            Log.d(TAG, "Maestro encontrado: ID=" + id + ", nombre=" + fullName + ", email=" + email);
                            teachers.add(new Teacher(id, fullName, email));
                        }

                        Log.d(TAG, "Total maestros encontrados: " + teachers.size());
                        listener.onTeachersLoaded(teachers);
                    } else {
                        Log.w(TAG, "Error al obtener maestros", task.getException());
                        listener.onError(task.getException());
                    }
                });
    }

    /**
     * Crea maestros de prueba para desarrollo
     */
    public void crearMaestrosDePrueba() {
        Map<String, Object> maestro1 = new HashMap<>();
        maestro1.put("userId", "maestro1");
        maestro1.put("email", "maestro1@example.com");
        maestro1.put("displayName", "Profesor Juan Pérez");
        maestro1.put("fullName", "Juan Pérez");
        maestro1.put("userType", "admin");
        maestro1.put("authMethod", "email");
        maestro1.put("createdAt", String.valueOf(System.currentTimeMillis()));

        db.collection("user_profiles").document("maestro1")
                .set(maestro1)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Maestro 1 creado exitosamente"))
                .addOnFailureListener(e -> Log.e(TAG, "Error al crear maestro 1", e));

        Map<String, Object> maestro2 = new HashMap<>();
        maestro2.put("userId", "maestro2");
        maestro2.put("email", "maestro2@example.com");
        maestro2.put("displayName", "Profesora María López");
        maestro2.put("fullName", "María López");
        maestro2.put("userType", "admin");
        maestro2.put("authMethod", "email");
        maestro2.put("createdAt", String.valueOf(System.currentTimeMillis()));

        db.collection("user_profiles").document("maestro2")
                .set(maestro2)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Maestro 2 creado exitosamente"))
                .addOnFailureListener(e -> Log.e(TAG, "Error al crear maestro 2", e));
    }

    /**
     * Interface para manejar la carga asíncrona de maestros
     */
    public interface OnTeachersLoadedListener {
        void onTeachersLoaded(List<Teacher> teachers);
        void onError(Exception e);
    }
}