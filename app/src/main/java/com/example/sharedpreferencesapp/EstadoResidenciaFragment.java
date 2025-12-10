package com.example.sharedpreferencesapp;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EstadoResidenciaFragment extends Fragment {

    private static final String TAG = "EstadoResidenciaFragment";

    // UI Elements
    private TextView tvEstatusActual;
    private TextView tvEstatusDescripcion;
    private TextView tvHorasCompletadas;
    private TextView tvHorasTotal;
    private TextView tvPorcentajeHoras;
    private ProgressBar progressHoras;
    private LinearLayout timelineContainer;
    private TextView badgeTotalProcesos;

    // Data
    private FirebaseFirestore db;
    private String currentUserId;

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
    private static final String ESTATUS_NO_ELEGIBLE = "No elegible";
    private static final String ESTATUS_CANDIDATO = "Candidato";
    private static final String ESTATUS_EN_TRAMITE = "En trámite";
    private static final String ESTATUS_EN_CURSO = "En curso";
    private static final String ESTATUS_CONCLUIDA = "Concluida";
    private static final String ESTATUS_LIBERADA = "Liberada";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_estado_residencia, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Inicializar Firebase
        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        // Inicializar vistas
        tvEstatusActual = view.findViewById(R.id.tvEstatusActual);
        tvEstatusDescripcion = view.findViewById(R.id.tvEstatusDescripcion);
        tvHorasCompletadas = view.findViewById(R.id.tvHorasCompletadas);
        tvHorasTotal = view.findViewById(R.id.tvHorasTotal);
        progressHoras = view.findViewById(R.id.progressHoras);
        tvPorcentajeHoras = view.findViewById(R.id.tvPorcentajeHoras);
        timelineContainer = view.findViewById(R.id.timelineContainer);
        badgeTotalProcesos = view.findViewById(R.id.badgeTotalProcesos);

        // Cargar datos
        cargarEstadoResidencia();
    }

    private void cargarEstadoResidencia() {
        if (currentUserId == null) {
            Log.e(TAG, "Usuario no autenticado");
            return;
        }

        db.collection("user_profiles")
                .document(currentUserId)
                .collection("estado_residencia")
                .document("datos")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                        DocumentSnapshot doc = task.getResult();
                        Map<String, Object> data = doc.getData();
                        if (data != null) {
                            actualizarUI(data);
                        } else {
                            // Si no hay datos, inicializar con valores por defecto
                            inicializarEstadoPorDefecto();
                        }
                    } else {
                        // Si no existe el documento, inicializar con valores por defecto
                        inicializarEstadoPorDefecto();
                    }
                });
    }

    private void inicializarEstadoPorDefecto() {
        // Establecer valores por defecto
        String estatus = ESTATUS_CANDIDATO;
        int horasCompletadas = 0;
        int horasTotal = 500;
        Map<String, Object> fasesEstado = new HashMap<>();

        // Crear mapa con datos por defecto
        Map<String, Object> datos = new HashMap<>();
        datos.put("estatus", estatus);
        datos.put("horasCompletadas", horasCompletadas);
        datos.put("horasTotal", horasTotal);
        datos.put("fases", fasesEstado);

        actualizarUI(datos);
    }

    private void actualizarUI(Map<String, Object> data) {
        // Actualizar estatus
        String estatus = (String) data.getOrDefault("estatus", ESTATUS_CANDIDATO);
        tvEstatusActual.setText(estatus);
        tvEstatusDescripcion.setText(obtenerDescripcionEstatus(estatus));

        // Actualizar progreso de horas
        int horasCompletadas = ((Number) data.getOrDefault("horasCompletadas", 0)).intValue();
        int horasTotal = ((Number) data.getOrDefault("horasTotal", 500)).intValue();
        int porcentaje = horasTotal > 0 ? (horasCompletadas * 100) / horasTotal : 0;

        tvHorasCompletadas.setText(horasCompletadas + " horas");
        tvHorasTotal.setText("/ " + horasTotal + " horas");
        progressHoras.setProgress(porcentaje);
        tvPorcentajeHoras.setText(porcentaje + "% completado");

        // Actualizar timeline - pasar el estatus y los datos completos
        @SuppressWarnings("unchecked")
        Map<String, Object> fasesData = (Map<String, Object>) data.getOrDefault("fases", new HashMap<>());
        actualizarTimeline(estatus, fasesData);
    }

    private String obtenerDescripcionEstatus(String estatus) {
        switch (estatus) {
            case ESTATUS_NO_ELEGIBLE:
                return "No cumples con los requisitos necesarios";
            case ESTATUS_CANDIDATO:
                return "Estás en proceso de evaluación";
            case ESTATUS_EN_TRAMITE:
                return "Tu solicitud está siendo procesada";
            case ESTATUS_EN_CURSO:
                return "Estás realizando tu residencia profesional";
            case ESTATUS_CONCLUIDA:
                return "Has completado tu residencia";
            case ESTATUS_LIBERADA:
                return "Tu residencia ha sido liberada exitosamente";
            default:
                return "Estado no definido";
        }
    }

    private void actualizarTimeline(String estatus, Map<String, Object> fasesData) {
        timelineContainer.removeAllViews();

        // Determinar la fase actual basándose en el estatus y las fases completadas
        int faseActualIndex = determinarFaseActual(estatus, fasesData);

        // Actualizar badge con el total de procesos
        badgeTotalProcesos.setText(String.valueOf(FASES.length));

        for (int i = 0; i < FASES.length; i++) {
            String faseId = FASES_IDS[i];
            String faseNombre = FASES[i];

            // Obtener estado de la fase
            String estadoFase = (String) fasesData.getOrDefault(faseId, "pendiente");
            if (estadoFase == null) {
                estadoFase = "pendiente";
            }

            // Obtener fecha de la fase si existe
            String fechaFase = null;
            @SuppressWarnings("unchecked")
            Map<String, Object> faseInfo = (Map<String, Object>) fasesData.get(faseId + "_info");
            if (faseInfo != null) {
                fechaFase = (String) faseInfo.get("fecha");
            }

            // Determinar si está completada, en curso o pendiente
            boolean completada = "completada".equals(estadoFase);
            boolean enCurso = (i == faseActualIndex && !completada);

            // Crear vista de la fase
            boolean esPrimera = (i == 0);
            boolean esUltima = (i == FASES.length - 1);
            View faseView = crearVistaFase(faseNombre, estadoFase, completada, enCurso, esPrimera, esUltima, fechaFase, i);
            timelineContainer.addView(faseView);
        }
    }

    private String obtenerDescripcionFase(String faseNombre, int indice) {
        switch (indice) {
            case 0: // Requisitos
                return "Asegúrate de cumplir con todos los requisitos necesarios para iniciar tu residencia profesional:\n\n" +
                        "• Acreditación del Servicio Social.\n" +
                        "• Acreditación de todas las actividades complementarias.\n" +
                        "• Tener aprobado al menos el 80% de créditos de su plan de estudios.\n" +
                        "• No contar con ninguna asignatura en condiciones de \"curso especial\".";
            case 1: // Solicitud
                return "Envía tu solicitud de residencia profesional para ser evaluada por el comité correspondiente.";
            case 2: // Carta de presentación
                return "Prepara y envía tu carta de presentación a la empresa o institución donde realizarás tu residencia.";
            case 3: // Carta de aceptación
                return "Espera la carta de aceptación de la empresa o institución donde realizarás tu residencia.";
            case 4: // Asignación de asesores
                return "Se te asignará un asesor interno que te guiará durante todo el proceso de tu residencia.";
            case 5: // Cronograma
                return "Establece un cronograma de actividades y fechas importantes para tu residencia profesional.";
            case 6: // Reportes parciales
                return "Realiza y entrega los reportes parciales según el cronograma establecido.";
            case 7: // Reporte final
                return "Prepara y entrega tu reporte final con todos los resultados y conclusiones de tu residencia.";
            case 8: // Carta de término
                return "Obtén la carta de término que certifica la conclusión de tu residencia profesional.";
            case 9: // Acta de calificación
                return "Recibe tu acta de calificación y liberación que acredita la finalización exitosa de tu residencia.";
            default:
                return "Fase del proceso de residencia profesional.";
        }
    }

    private int determinarFaseActual(String estatus, Map<String, Object> fasesData) {
        // Determinar la fase actual basándose en el estatus
        switch (estatus) {
            case ESTATUS_NO_ELEGIBLE:
            case ESTATUS_CANDIDATO:
                return 0; // Requisitos
            case ESTATUS_EN_TRAMITE:
                return 1; // Solicitud
            case ESTATUS_EN_CURSO:
                // Buscar la primera fase no completada
                for (int i = 0; i < FASES_IDS.length; i++) {
                    String estado = (String) fasesData.getOrDefault(FASES_IDS[i], "pendiente");
                    if (!"completada".equals(estado)) {
                        return i;
                    }
                }
                return 5; // Cronograma (por defecto si está en curso)
            case ESTATUS_CONCLUIDA:
            case ESTATUS_LIBERADA:
                return FASES.length - 1; // Última fase
            default:
                return 0;
        }
    }

    private View crearVistaFase(String nombreFase, String estado, boolean completada, boolean enCurso, boolean esPrimera, boolean esUltima, String fecha, int indiceFase) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View faseView = inflater.inflate(R.layout.item_timeline_fase_nuevo, timelineContainer, false);

        TextView tvNombreFase = faseView.findViewById(R.id.tvNombreFase);
        TextView tvInfoFase = faseView.findViewById(R.id.tvInfoFase);
        TextView tvFechaFase = faseView.findViewById(R.id.tvFechaFase);
        TextView tvEstadoPill = faseView.findViewById(R.id.tvEstadoPill);
        ImageView ivIconoPill = faseView.findViewById(R.id.ivIconoPill);
        LinearLayout pillEstado = faseView.findViewById(R.id.pillEstado);
        View indicatorPoint = faseView.findViewById(R.id.indicatorPoint);
        View lineTop = faseView.findViewById(R.id.lineTop);
        View lineBottom = faseView.findViewById(R.id.lineBottom);
        androidx.cardview.widget.CardView cardFase = faseView.findViewById(R.id.cardFase);
        LinearLayout cardBackground = faseView.findViewById(R.id.cardContent);

        tvNombreFase.setText(nombreFase);

        // Configurar fecha en la columna izquierda
        if (fecha != null && !fecha.isEmpty()) {
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM", new Locale("es", "MX"));
                Date date = inputFormat.parse(fecha);
                if (date != null) {
                    tvFechaFase.setText(outputFormat.format(date));
                } else {
                    tvFechaFase.setText(fecha.length() > 7 ? fecha.substring(0, 7) : fecha);
                }
            } catch (Exception e) {
                // Si no se puede parsear, usar la fecha tal cual (limitada)
                tvFechaFase.setText(fecha.length() > 7 ? fecha.substring(0, 7) : fecha);
            }
        } else {
            // Si no hay fecha, mostrar "--"
            tvFechaFase.setText("--");
        }

        // Configurar el pill según el estado
        if (completada) {
            // Si está completada, mostrar fecha o "Completada"
            if (fecha != null && !fecha.isEmpty()) {
                try {
                    SimpleDateFormat inputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM", new Locale("es", "MX"));
                    Date date = inputFormat.parse(fecha);
                    if (date != null) {
                        tvEstadoPill.setText("Completada - " + outputFormat.format(date));
                    } else {
                        tvEstadoPill.setText("Completada");
                    }
                } catch (Exception e) {
                    tvEstadoPill.setText("Completada");
                }
            } else {
                tvEstadoPill.setText("Completada");
            }
            ivIconoPill.setImageResource(R.drawable.ic_check);
            ivIconoPill.setColorFilter(getResources().getColor(R.color.primary));
        } else if (enCurso) {
            // Si está en curso, mostrar "En curso"
            tvEstadoPill.setText("En curso");
            ivIconoPill.setImageResource(R.drawable.ic_calendar);
            ivIconoPill.setColorFilter(getResources().getColor(R.color.primary));

            // Si hay fecha, mostrarla en la información adicional
            if (fecha != null && !fecha.isEmpty()) {
                try {
                    SimpleDateFormat inputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM, yyyy", new Locale("es", "MX"));
                    Date date = inputFormat.parse(fecha);
                    if (date != null) {
                        tvInfoFase.setText("Fecha: " + outputFormat.format(date));
                        tvInfoFase.setVisibility(View.VISIBLE);
                    }
                } catch (Exception e) {
                    tvInfoFase.setVisibility(View.GONE);
                }
            }
        } else {
            // Si está pendiente, mostrar "Fecha pendiente"
            tvEstadoPill.setText("Fecha pendiente");
            ivIconoPill.setImageResource(R.drawable.ic_calendar);
            ivIconoPill.setColorFilter(getResources().getColor(R.color.text_secondary));
            tvInfoFase.setVisibility(View.GONE);
        }

        // Configurar estado visual del punto indicador
        if (completada) {
            indicatorPoint.setBackgroundResource(R.drawable.timeline_point_completed);
        } else if (enCurso) {
            indicatorPoint.setBackgroundResource(R.drawable.timeline_point_current);
        } else {
            indicatorPoint.setBackgroundResource(R.drawable.timeline_point_pending);
        }

        // Ocultar líneas en los extremos
        if (esPrimera) {
            lineTop.setVisibility(View.GONE);
        }
        if (esUltima) {
            lineBottom.setVisibility(View.GONE);
        }

        // Agregar click listener a la card para mostrar Bottom Sheet
        final int indiceFinal = indiceFase;
        cardFase.setOnClickListener(v -> mostrarBottomSheetFase(nombreFase, completada, enCurso, fecha, indiceFinal));

        return faseView;
    }

    private void mostrarBottomSheetFase(String nombreFase, boolean completada, boolean enCurso, String fecha, int indiceFase) {
        if (getContext() == null) return;

        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getContext());
        View bottomSheetView = LayoutInflater.from(getContext()).inflate(R.layout.bottom_sheet_fase_info, null);
        bottomSheetDialog.setContentView(bottomSheetView);

        // Obtener referencias de las vistas
        TextView tvTituloFase = bottomSheetView.findViewById(R.id.tvTituloFase);
        TextView tvDescripcionFase = bottomSheetView.findViewById(R.id.tvDescripcionFase);
        TextView tvEstadoPillInfo = bottomSheetView.findViewById(R.id.tvEstadoPillInfo);
        ImageView ivIconoPillInfo = bottomSheetView.findViewById(R.id.ivIconoPillInfo);
        TextView tvFechaInfo = bottomSheetView.findViewById(R.id.tvFechaInfo);
        ImageView ivCerrar = bottomSheetView.findViewById(R.id.ivCerrar);

        // Configurar título
        tvTituloFase.setText(nombreFase);

        // Configurar descripción
        String descripcion = obtenerDescripcionFase(nombreFase, indiceFase);
        tvDescripcionFase.setText(descripcion);

        // Configurar estado en el pill
        if (completada) {
            if (fecha != null && !fecha.isEmpty()) {
                try {
                    SimpleDateFormat inputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM", new Locale("es", "MX"));
                    Date date = inputFormat.parse(fecha);
                    if (date != null) {
                        tvEstadoPillInfo.setText("Completada - " + outputFormat.format(date));
                    } else {
                        tvEstadoPillInfo.setText("Completada");
                    }
                } catch (Exception e) {
                    tvEstadoPillInfo.setText("Completada");
                }
            } else {
                tvEstadoPillInfo.setText("Completada");
            }
            ivIconoPillInfo.setImageResource(R.drawable.ic_check);
            ivIconoPillInfo.setColorFilter(getResources().getColor(R.color.primary));
        } else if (enCurso) {
            tvEstadoPillInfo.setText("En curso");
            ivIconoPillInfo.setImageResource(R.drawable.ic_calendar);
            ivIconoPillInfo.setColorFilter(getResources().getColor(R.color.primary));
        } else {
            tvEstadoPillInfo.setText("Fecha pendiente");
            ivIconoPillInfo.setImageResource(R.drawable.ic_calendar);
            ivIconoPillInfo.setColorFilter(getResources().getColor(R.color.text_secondary));
        }

        // Configurar fecha adicional si está disponible
        if (fecha != null && !fecha.isEmpty() && !completada) {
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("dd 'de' MMMM 'de' yyyy", new Locale("es", "MX"));
                Date date = inputFormat.parse(fecha);
                if (date != null) {
                    tvFechaInfo.setText("Fecha estimada: " + outputFormat.format(date));
                    tvFechaInfo.setVisibility(View.VISIBLE);
                }
            } catch (Exception e) {
                tvFechaInfo.setVisibility(View.GONE);
            }
        } else {
            tvFechaInfo.setVisibility(View.GONE);
        }

        // Configurar botón de cerrar
        ivCerrar.setOnClickListener(v -> bottomSheetDialog.dismiss());

        // Mostrar el Bottom Sheet
        bottomSheetDialog.show();
    }
}

