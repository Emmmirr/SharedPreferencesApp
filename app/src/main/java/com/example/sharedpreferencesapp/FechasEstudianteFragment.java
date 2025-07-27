package com.example.sharedpreferencesapp;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FechasEstudianteFragment extends Fragment {

    private TextView tvTituloCalendario;
    private CardView cardEntrega1, cardEntrega2, cardEntrega3;
    private TextView tvFechaEntrega1, tvFechaEntrega2, tvFechaEntrega3;
    private Button btnFechaEntrega1, btnFechaEntrega2, btnFechaEntrega3;
    private Button btnDocEntrega1, btnDocEntrega2, btnDocEntrega3;
    private Button btnBorrarFechas, btnCerrar;

    private FirebaseFirestore db;
    private String userId;
    private Calendar calendar;
    private SimpleDateFormat dateFormat;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_fechas_estudiante, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Inicializar Firebase
        db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            userId = user.getUid();
        } else {
            Toast.makeText(requireContext(), "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show();
            return;
        }

        // Inicializar formato de fecha
        calendar = Calendar.getInstance();
        dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        // Inicializar vistas
        initViews(view);

        // Cargar fechas guardadas
        loadFechasEstudiante();

        // Configurar listeners de los botones
        setupButtonListeners();
    }

    private void initViews(View view) {
        tvTituloCalendario = view.findViewById(R.id.tvTituloCalendario);

        cardEntrega1 = view.findViewById(R.id.cardEntrega1);
        cardEntrega2 = view.findViewById(R.id.cardEntrega2);
        cardEntrega3 = view.findViewById(R.id.cardEntrega3);

        tvFechaEntrega1 = view.findViewById(R.id.tvFechaEntrega1);
        tvFechaEntrega2 = view.findViewById(R.id.tvFechaEntrega2);
        tvFechaEntrega3 = view.findViewById(R.id.tvFechaEntrega3);

        btnFechaEntrega1 = view.findViewById(R.id.btnFechaEntrega1);
        btnFechaEntrega2 = view.findViewById(R.id.btnFechaEntrega2);
        btnFechaEntrega3 = view.findViewById(R.id.btnFechaEntrega3);

        btnDocEntrega1 = view.findViewById(R.id.btnDocEntrega1);
        btnDocEntrega2 = view.findViewById(R.id.btnDocEntrega2);
        btnDocEntrega3 = view.findViewById(R.id.btnDocEntrega3);

        btnBorrarFechas = view.findViewById(R.id.btnBorrarFechas);
        btnCerrar = view.findViewById(R.id.btnCerrar);
    }

    private void setupButtonListeners() {
        // Botones para seleccionar fechas
        btnFechaEntrega1.setOnClickListener(v -> showDatePickerDialog(1));
        btnFechaEntrega2.setOnClickListener(v -> showDatePickerDialog(2));
        btnFechaEntrega3.setOnClickListener(v -> showDatePickerDialog(3));

        // Botones para subir documentos
        btnDocEntrega1.setOnClickListener(v -> navigateToDocumentos(1));
        btnDocEntrega2.setOnClickListener(v -> navigateToDocumentos(2));
        btnDocEntrega3.setOnClickListener(v -> navigateToDocumentos(3));

        // Botón para borrar fechas
        btnBorrarFechas.setOnClickListener(v -> borrarFechas());

        // Botón para cerrar (opcional si es un diálogo)
        btnCerrar.setOnClickListener(v -> {
            // En un fragmento, puedes ocultar alguna vista o navegar hacia atrás
            // Si es un diálogo, puedes cerrarlo
        });
    }

    private void showDatePickerDialog(int entrega) {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                    String fechaSeleccionada = dateFormat.format(calendar.getTime());
                    guardarFechaEntrega(entrega, fechaSeleccionada);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));

        datePickerDialog.show();
    }

    private void guardarFechaEntrega(int entrega, String fecha) {
        DocumentReference docRef = db.collection("cronogramas").document(userId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("entrega" + entrega, fecha);

        docRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    if (isAdded()) {
                        switch (entrega) {
                            case 1:
                                tvFechaEntrega1.setText("Fecha: " + fecha);
                                break;
                            case 2:
                                tvFechaEntrega2.setText("Fecha: " + fecha);
                                break;
                            case 3:
                                tvFechaEntrega3.setText("Fecha: " + fecha);
                                break;
                        }
                        Toast.makeText(requireContext(), "Fecha guardada correctamente", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        // Si el documento no existe, crearlo
                        if (e.getMessage() != null && e.getMessage().contains("No document to update")) {
                            Map<String, Object> cronograma = new HashMap<>();
                            cronograma.put("userId", userId);
                            cronograma.put("entrega" + entrega, fecha);

                            docRef.set(cronograma)
                                    .addOnSuccessListener(aVoid -> {
                                        if (isAdded()) {
                                            switch (entrega) {
                                                case 1:
                                                    tvFechaEntrega1.setText("Fecha: " + fecha);
                                                    break;
                                                case 2:
                                                    tvFechaEntrega2.setText("Fecha: " + fecha);
                                                    break;
                                                case 3:
                                                    tvFechaEntrega3.setText("Fecha: " + fecha);
                                                    break;
                                            }
                                            Toast.makeText(requireContext(), "Fecha guardada correctamente", Toast.LENGTH_SHORT).show();
                                        }
                                    })
                                    .addOnFailureListener(e2 -> {
                                        if (isAdded()) {
                                            Toast.makeText(requireContext(), "Error: " + e2.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        } else {
                            Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void loadFechasEstudiante() {
        db.collection("cronogramas").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && isAdded()) {
                        String fecha1 = documentSnapshot.getString("entrega1");
                        String fecha2 = documentSnapshot.getString("entrega2");
                        String fecha3 = documentSnapshot.getString("entrega3");

                        if (fecha1 != null && !fecha1.isEmpty()) {
                            tvFechaEntrega1.setText("Fecha: " + fecha1);
                        } else {
                            tvFechaEntrega1.setText("Fecha: Sin asignar");
                        }

                        if (fecha2 != null && !fecha2.isEmpty()) {
                            tvFechaEntrega2.setText("Fecha: " + fecha2);
                        } else {
                            tvFechaEntrega2.setText("Fecha: Sin asignar");
                        }

                        if (fecha3 != null && !fecha3.isEmpty()) {
                            tvFechaEntrega3.setText("Fecha: " + fecha3);
                        } else {
                            tvFechaEntrega3.setText("Fecha: Sin asignar");
                        }
                    } else if (isAdded()) {
                        // No hay datos guardados todavía
                        tvFechaEntrega1.setText("Fecha: Sin asignar");
                        tvFechaEntrega2.setText("Fecha: Sin asignar");
                        tvFechaEntrega3.setText("Fecha: Sin asignar");
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Error al cargar fechas: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        tvFechaEntrega1.setText("Fecha: Sin asignar");
                        tvFechaEntrega2.setText("Fecha: Sin asignar");
                        tvFechaEntrega3.setText("Fecha: Sin asignar");
                    }
                });
    }

    private void borrarFechas() {
        DocumentReference docRef = db.collection("cronogramas").document(userId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("entrega1", "");
        updates.put("entrega2", "");
        updates.put("entrega3", "");

        docRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    if (isAdded()) {
                        tvFechaEntrega1.setText("Fecha: Sin asignar");
                        tvFechaEntrega2.setText("Fecha: Sin asignar");
                        tvFechaEntrega3.setText("Fecha: Sin asignar");
                        Toast.makeText(requireContext(), "Fechas borradas correctamente", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Error al borrar fechas: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void navigateToDocumentos(int entrega) {
        // Navegar a la pestaña de documentos y pasar el número de entrega
        if (getActivity() instanceof MainActivity) {
            // Suponiendo que tienes un método en MainActivity para cambiar a la pestaña de documentos
            // ((MainActivity) getActivity()).navigateToDocumentos(entrega);
            Toast.makeText(requireContext(), "Funcionalidad de subir documentos por implementar", Toast.LENGTH_SHORT).show();
        }
    }
}