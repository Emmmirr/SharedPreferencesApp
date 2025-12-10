package com.example.sharedpreferencesapp;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BottomSheetEditarEstado extends BottomSheetDialogFragment {

    private static final String TAG = "BottomSheetEditarEstado";

    // Fases de la residencia
    private static final String[] FASES = {
            "Requisitos",
            "Solicitud",
            "Carta de presentación",
            "Carta de aceptación",
            "Asignación de asesores",
            "Cronograma",
            "Reportes parciales",
            "Reporte final",
            "Carta de término",
            "Acta de calificación / liberación"
    };

    private static final String[] FASES_IDS = {
            "requisitos",
            "solicitud",
            "carta_presentacion",
            "carta_aceptacion",
            "asignacion_asesores",
            "cronograma",
            "reportes_parciales",
            "reporte_final",
            "carta_termino",
            "acta_calificacion"
    };

    // Estados posibles
    private static final String[] ESTADOS = {
            "No elegible",
            "Candidato",
            "En trámite",
            "En curso",
            "Concluida",
            "Liberada"
    };

    private String studentId;
    private Spinner spinnerEstatus;
    private TextInputEditText etHorasCompletadas;
    private TextInputEditText etHorasTotal;
    private LinearLayout layoutFases;
    private List<CheckBox> checkBoxesFases;
    private FirebaseManager firebaseManager;
    private OnEstadoGuardadoListener listener;

    public interface OnEstadoGuardadoListener {
        void onEstadoGuardado();
    }

    public static BottomSheetEditarEstado newInstance(String studentId) {
        BottomSheetEditarEstado fragment = new BottomSheetEditarEstado();
        Bundle args = new Bundle();
        args.putString("studentId", studentId);
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnEstadoGuardadoListener(OnEstadoGuardadoListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            studentId = getArguments().getString("studentId");
        }
        firebaseManager = new FirebaseManager();
        checkBoxesFases = new ArrayList<>();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new BottomSheetDialog(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_editar_estado, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        spinnerEstatus = view.findViewById(R.id.spinner_estatus);
        etHorasCompletadas = view.findViewById(R.id.et_horas_completadas);
        etHorasTotal = view.findViewById(R.id.et_horas_total);
        layoutFases = view.findViewById(R.id.layout_fases);

        // Configurar spinner de estatus
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                ESTADOS
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEstatus.setAdapter(adapter);

        // Cargar datos actuales
        cargarEstadoActual();

        // Crear checkboxes para fases
        crearCheckBoxesFases();

        // Botón cerrar
        ImageView ivCerrar = view.findViewById(R.id.iv_cerrar);
        ivCerrar.setOnClickListener(v -> dismiss());

        // Botón cancelar
        LinearLayout btnCancelar = view.findViewById(R.id.btn_cancelar);
        btnCancelar.setOnClickListener(v -> dismiss());

        // Botón guardar
        LinearLayout btnGuardar = view.findViewById(R.id.btn_guardar);
        btnGuardar.setOnClickListener(v -> guardarEstado());
    }

    private void cargarEstadoActual() {
        if (studentId == null) {
            Log.e(TAG, "StudentId es null");
            return;
        }

        firebaseManager.cargarEstadoResidencia(studentId, data -> {
            if (data != null) {
                // Establecer estatus
                String estatus = (String) data.getOrDefault("estatus", "Candidato");
                for (int i = 0; i < ESTADOS.length; i++) {
                    if (ESTADOS[i].equals(estatus)) {
                        spinnerEstatus.setSelection(i);
                        break;
                    }
                }

                // Establecer horas
                int horasCompletadas = ((Number) data.getOrDefault("horasCompletadas", 0)).intValue();
                int horasTotal = ((Number) data.getOrDefault("horasTotal", 500)).intValue();
                etHorasCompletadas.setText(String.valueOf(horasCompletadas));
                etHorasTotal.setText(String.valueOf(horasTotal));

                // Establecer fases completadas
                @SuppressWarnings("unchecked")
                Map<String, Object> fases = (Map<String, Object>) data.getOrDefault("fases", new HashMap<>());
                for (int i = 0; i < FASES_IDS.length; i++) {
                    boolean completada = fases.containsKey(FASES_IDS[i]) &&
                            Boolean.TRUE.equals(fases.get(FASES_IDS[i]));
                    if (i < checkBoxesFases.size()) {
                        checkBoxesFases.get(i).setChecked(completada);
                    }
                }
            }
        }, error -> {
            Log.e(TAG, "Error al cargar estado: " + error.getMessage());
            Toast.makeText(requireContext(), "Error al cargar el estado actual", Toast.LENGTH_SHORT).show();
        });
    }

    private void crearCheckBoxesFases() {
        layoutFases.removeAllViews();
        checkBoxesFases.clear();

        for (int i = 0; i < FASES.length; i++) {
            CheckBox checkBox = new CheckBox(requireContext());
            checkBox.setText(FASES[i]);
            checkBox.setTextSize(14);
            checkBox.setTextColor(getResources().getColor(R.color.text_primary, null));
            checkBox.setPadding(0, 12, 0, 12);
            layoutFases.addView(checkBox);
            checkBoxesFases.add(checkBox);
        }
    }

    private void guardarEstado() {
        if (studentId == null) {
            Toast.makeText(requireContext(), "Error: ID de estudiante no válido", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validar horas
        String horasCompletadasStr = etHorasCompletadas.getText().toString().trim();
        String horasTotalStr = etHorasTotal.getText().toString().trim();

        if (horasCompletadasStr.isEmpty() || horasTotalStr.isEmpty()) {
            Toast.makeText(requireContext(), "Por favor completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        int horasCompletadas = Integer.parseInt(horasCompletadasStr);
        int horasTotal = Integer.parseInt(horasTotalStr);

        if (horasCompletadas < 0 || horasTotal <= 0 || horasCompletadas > horasTotal) {
            Toast.makeText(requireContext(), "Las horas no son válidas", Toast.LENGTH_SHORT).show();
            return;
        }

        // Obtener estatus seleccionado
        String estatus = ESTADOS[spinnerEstatus.getSelectedItemPosition()];

        // Obtener fases completadas
        Map<String, Object> fases = new HashMap<>();
        for (int i = 0; i < FASES_IDS.length; i++) {
            fases.put(FASES_IDS[i], checkBoxesFases.get(i).isChecked());
        }

        // Crear mapa de datos
        Map<String, Object> estadoData = new HashMap<>();
        estadoData.put("estatus", estatus);
        estadoData.put("horasCompletadas", horasCompletadas);
        estadoData.put("horasTotal", horasTotal);
        estadoData.put("fases", fases);
        estadoData.put("updatedAt", System.currentTimeMillis());

        // Guardar en Firebase
        firebaseManager.guardarEstadoResidencia(studentId, estadoData, () -> {
            Toast.makeText(requireContext(), "Estado actualizado correctamente", Toast.LENGTH_SHORT).show();
            if (listener != null) {
                listener.onEstadoGuardado();
            }
            dismiss();
        }, error -> {
            Log.e(TAG, "Error al guardar estado: " + error.getMessage());
            Toast.makeText(requireContext(), "Error al guardar el estado", Toast.LENGTH_SHORT).show();
        });
    }
}

