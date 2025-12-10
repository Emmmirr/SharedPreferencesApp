package com.example.sharedpreferencesapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EstadoResidenciaAdminTabFragment extends Fragment {

    private static final String ARG_STUDENT_ID = "studentId";
    private String studentId;
    private FirebaseFirestore db;
    private FirebaseManager firebaseManager;

    private TextView tvEstatusActual, tvHorasCompletadas, tvHorasTotal, tvPorcentajeHoras;
    private ProgressBar progressHoras;
    private LinearLayout timelineContainer;

    // Fases de la residencia (mismas que en EstadoResidenciaFragment)
    private static final String[] FASES = {
            "Requisitos", "Solicitud", "Carta de presentación", "Carta de aceptación",
            "Asignación de asesores", "Cronograma", "Reportes parciales", "Reporte final",
            "Carta de término", "Acta de calificación / liberación"
    };

    private static final String[] FASES_IDS = {
            "requisitos", "solicitud", "carta_presentacion", "carta_aceptacion",
            "asignacion_asesores", "cronograma", "reportes_parciales", "reporte_final",
            "carta_termino", "acta_calificacion"
    };

    public static EstadoResidenciaAdminTabFragment newInstance(String studentId) {
        EstadoResidenciaAdminTabFragment fragment = new EstadoResidenciaAdminTabFragment();
        Bundle args = new Bundle();
        args.putString(ARG_STUDENT_ID, studentId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            studentId = getArguments().getString(ARG_STUDENT_ID);
        }
        db = FirebaseFirestore.getInstance();
        firebaseManager = new FirebaseManager();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_estado_residencia_admin_tab, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvEstatusActual = view.findViewById(R.id.tvEstatusActual);
        tvHorasCompletadas = view.findViewById(R.id.tvHorasCompletadas);
        tvHorasTotal = view.findViewById(R.id.tvHorasTotal);
        tvPorcentajeHoras = view.findViewById(R.id.tvPorcentajeHoras);
        progressHoras = view.findViewById(R.id.progressHoras);
        timelineContainer = view.findViewById(R.id.timelineContainer);

        LinearLayout btnEditarEstado = view.findViewById(R.id.btn_editar_estado);
        btnEditarEstado.setOnClickListener(v -> {
            BottomSheetEditarEstado bottomSheet = BottomSheetEditarEstado.newInstance(studentId);
            bottomSheet.setOnEstadoGuardadoListener(() -> {
                cargarEstadoResidencia();
            });
            bottomSheet.show(getParentFragmentManager(), "BottomSheetEditarEstado");
        });

        cargarEstadoResidencia();
    }

    private void cargarEstadoResidencia() {
        if (studentId == null) return;

        firebaseManager.cargarEstadoResidencia(studentId, data -> {
            actualizarUI(data);
        }, error -> {
            // Si no hay datos, inicializar con valores por defecto
            Map<String, Object> defaultData = new HashMap<>();
            defaultData.put("estatus", "Candidato");
            defaultData.put("horasCompletadas", 0);
            defaultData.put("horasTotal", 500);
            defaultData.put("fases", new HashMap<>());
            actualizarUI(defaultData);
        });
    }

    private void actualizarUI(Map<String, Object> data) {
        // Actualizar estatus
        String estatus = (String) data.getOrDefault("estatus", "Candidato");
        tvEstatusActual.setText(estatus);

        // Actualizar progreso de horas
        int horasCompletadas = ((Number) data.getOrDefault("horasCompletadas", 0)).intValue();
        int horasTotal = ((Number) data.getOrDefault("horasTotal", 500)).intValue();
        int porcentaje = horasTotal > 0 ? (horasCompletadas * 100) / horasTotal : 0;

        tvHorasCompletadas.setText(horasCompletadas + " horas");
        tvHorasTotal.setText("/ " + horasTotal + " horas");
        progressHoras.setProgress(porcentaje);
        tvPorcentajeHoras.setText(porcentaje + "% completado");

        // Actualizar timeline
        @SuppressWarnings("unchecked")
        Map<String, Object> fasesData = (Map<String, Object>) data.getOrDefault("fases", new HashMap<>());
        actualizarTimeline(estatus, fasesData);
    }

    private void actualizarTimeline(String estatus, Map<String, Object> fasesData) {
        timelineContainer.removeAllViews();

        // Reutilizar la lógica de EstadoResidenciaFragment para crear las vistas de timeline
        // Por simplicidad, aquí solo mostraremos las fases con su estado
        for (int i = 0; i < FASES.length; i++) {
            boolean completada = fasesData.containsKey(FASES_IDS[i]) &&
                    Boolean.TRUE.equals(fasesData.get(FASES_IDS[i]));

            View faseView = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_timeline_fase_nuevo, timelineContainer, false);

            TextView tvNombreFase = faseView.findViewById(R.id.tvNombreFase);
            TextView tvEstadoPill = faseView.findViewById(R.id.tvEstadoPill);

            tvNombreFase.setText(FASES[i]);
            tvEstadoPill.setText(completada ? "Completada" : "Pendiente");

            timelineContainer.addView(faseView);
        }
    }
}

