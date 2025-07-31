package com.example.sharedpreferencesapp;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class StudentHomeFragment extends Fragment {

    private static final String TAG = "StudentHomeFragment";

    // Referencias UI
    private TextView tvWelcomeMessage;
    private CardView cardEstadoSolicitud;
    private TextView tvEstadoSolicitud;
    private TextView tvSupervisorInfo;
    private LinearLayout layoutAccionesAprobado;
    private Button btnVerProtocolo;
    private TextView tvFechaActual;

    // --- INICIO DE NUEVAS REFERENCIAS UI ---
    private LinearLayout layoutProximaEntregaInfo;
    private TextView tvProximaEntregaLabel, tvProximaEntregaFecha, tvNoEntregas;
    private ProgressBar progressProximaEntrega;
    // --- FIN DE NUEVAS REFERENCIAS UI ---

    private ProfileManager profileManager;
    private FirebaseManager firebaseManager; // Añadido

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_student_home, container, false);

        profileManager = new ProfileManager(requireContext());
        firebaseManager = new FirebaseManager(); // Inicializar FirebaseManager

        initializeViews(view);
        setCurrentDate();
        loadStudentData();

        return view;
    }

    private void initializeViews(View view) {
        tvWelcomeMessage = view.findViewById(R.id.tvWelcomeMessage);
        cardEstadoSolicitud = view.findViewById(R.id.cardEstadoSolicitud);
        tvEstadoSolicitud = view.findViewById(R.id.tvEstadoSolicitud);
        tvSupervisorInfo = view.findViewById(R.id.tvSupervisorInfo);
        layoutAccionesAprobado = view.findViewById(R.id.layoutAccionesAprobado);
        btnVerProtocolo = view.findViewById(R.id.btnVerProtocolo);
        tvFechaActual = view.findViewById(R.id.tvFechaActual);

        // --- INICIO DE INICIALIZACIÓN DE VISTAS NUEVAS ---
        layoutProximaEntregaInfo = view.findViewById(R.id.layoutProximaEntregaInfo);
        tvProximaEntregaLabel = view.findViewById(R.id.tvProximaEntregaLabel);
        tvProximaEntregaFecha = view.findViewById(R.id.tvProximaEntregaFecha);
        tvNoEntregas = view.findViewById(R.id.tvNoEntregas);
        progressProximaEntrega = view.findViewById(R.id.progressProximaEntrega);
        // --- FIN DE INICIALIZACIÓN DE VISTAS NUEVAS ---

        btnVerProtocolo.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new ProtocoloFragment())
                        .addToBackStack(null)
                        .commit();
            }
        });
    }

    private void setCurrentDate() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", new Locale("es", "MX"));
        String currentDate = dateFormat.format(calendar.getTime());
        tvFechaActual.setText(currentDate);
    }

    private void loadStudentData() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "Usuario no autenticado");
            return;
        }

        tvWelcomeMessage.setText("Bienvenido, Estudiante");

        profileManager.obtenerPerfilActual(
                profile -> {
                    if (profile != null) {
                        String displayName = profile.getFullName().isEmpty() ? profile.getDisplayName() : profile.getFullName();
                        tvWelcomeMessage.setText("Bienvenido, " + displayName);

                        String supervisorId = profile.getSupervisorId();
                        String supervisorName = profile.getSupervisorName();

                        if (supervisorId == null || supervisorId.isEmpty()) {
                            showNoSupervisorState();
                            // --- CAMBIO: Mostrar mensaje en tarjeta de entregas también ---
                            updateProximasEntregasUI(null, "Necesitas un supervisor para ver tus entregas.");
                            return;
                        }

                        // --- CAMBIO: Llamar al método para cargar el calendario ---
                        cargarProximaEntrega(supervisorId, profile.getUserId());

                        if (profile.isApproved()) {
                            showApprovedState(supervisorName);
                        } else {
                            showPendingState(supervisorName);
                        }
                    } else {
                        showErrorState();
                    }
                },
                error -> {
                    Log.e(TAG, "Error al cargar perfil", error);
                    Toast.makeText(getContext(), "Error al cargar datos: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    showErrorState();
                }
        );
    }

    // --- INICIO DE NUEVO MÉTODO ---
    private void cargarProximaEntrega(String supervisorId, String studentId) {
        String calendarioId = "calendario_" + studentId;

        firebaseManager.buscarCalendarioPorId(supervisorId, calendarioId, task -> {
            if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                DocumentSnapshot calendario = task.getResult();
                encontrarYMostrarProximaFecha(calendario);
            } else {
                // El calendario no existe o hubo un error
                updateProximasEntregasUI(null, "Tu supervisor aún no ha configurado tu calendario.");
                Log.e(TAG, "No se encontró el calendario del alumno", task.getException());
            }
        });
    }

// En StudentHomeFragment.java

    private void encontrarYMostrarProximaFecha(DocumentSnapshot calendario) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        Calendar hoy = Calendar.getInstance();
        // Normalizar 'hoy' para comparar solo el día
        hoy.set(Calendar.HOUR_OF_DAY, 0);
        hoy.set(Calendar.MINUTE, 0);
        hoy.set(Calendar.SECOND, 0);
        hoy.set(Calendar.MILLISECOND, 0);

        String[] camposFecha = {"fechaPrimeraEntrega", "fechaSegundaEntrega", "fechaResultado"};
        String[] camposLabel = {"labelPrimeraEntrega", "labelSegundaEntrega", "labelResultado"};
        String[] camposEstado = {"estadoPrimeraEntrega", "estadoSegundaEntrega", "estadoResultado"};
        String[] defaultLabels = {"1ª Entrega", "2ª Entrega", "Resultado Final"};

        // --- INICIO DE LÓGICA DE BÚSQUEDA MEJORADA ---

        // Iteramos a través de las entregas para encontrar la primera que no esté aprobada.
        for (int i = 0; i < camposFecha.length; i++) {
            String estado = calendario.getString(camposEstado[i]);
            String fechaStr = calendario.getString(camposFecha[i]);

            // Si la entrega NO está marcada como "Aprobado"
            if (!"Aprobado".equals(estado)) {
                // Verificamos si tiene una fecha asignada
                if (fechaStr != null && !fechaStr.isEmpty()) {
                    try {
                        Date fecha = sdf.parse(fechaStr);
                        // Y si la fecha no ha pasado
                        if (fecha != null && !fecha.before(hoy.getTime())) {

                            // ¡Esta es nuestra próxima entrega pendiente!
                            String label = calendario.getString(camposLabel[i]);
                            String proximoLabel = (label != null && !label.isEmpty()) ? label : defaultLabels[i];

                            Map<String, String> entrega = new HashMap<>();
                            entrega.put("label", proximoLabel);
                            entrega.put("fecha", fechaStr);

                            // Si la entrega fue rechazada, añadimos un mensaje especial
                            if ("Rechazado".equals(estado)) {
                                entrega.put("estado", " (Requiere re-entrega)");
                            } else {
                                entrega.put("estado", "");
                            }

                            updateProximasEntregasUI(entrega, null);
                            return; // Salimos del bucle en cuanto encontramos la primera
                        }
                    } catch (ParseException e) {
                        Log.e(TAG, "Error al parsear fecha: " + fechaStr, e);
                    }
                }
            }
        }
        // --- FIN DE LÓGICA DE BÚSQUEDA MEJORADA ---

        // Si el bucle termina, significa que todas las entregas futuras ya están aprobadas,
        // o no hay ninguna fecha futura asignada.
        updateProximasEntregasUI(null, "¡Felicidades! No tienes entregas pendientes.");
    }

// En StudentHomeFragment.java

    private void updateProximasEntregasUI(Map<String, String> entrega, String mensaje) {
        if (getContext() == null) return;

        progressProximaEntrega.setVisibility(View.GONE);
        if (entrega != null) {
            layoutProximaEntregaInfo.setVisibility(View.VISIBLE);
            tvNoEntregas.setVisibility(View.GONE);

            // --- CAMBIO: Añadimos el estado al texto de la etiqueta ---
            String estado = entrega.get("estado") != null ? entrega.get("estado") : "";
            tvProximaEntregaLabel.setText(entrega.get("label") + estado);

            tvProximaEntregaFecha.setText("Fecha límite: " + entrega.get("fecha"));
        } else {
            layoutProximaEntregaInfo.setVisibility(View.GONE);
            tvNoEntregas.setVisibility(View.VISIBLE);
            tvNoEntregas.setText(mensaje != null ? mensaje : "No hay entregas pendientes.");
        }
    }
    // --- FIN DE NUEVO MÉTODO ---

    private void showApprovedState(String supervisorName) {
        cardEstadoSolicitud.setVisibility(View.VISIBLE);
        cardEstadoSolicitud.setCardBackgroundColor(getResources().getColor(R.color.approved_bg, null));
        tvEstadoSolicitud.setText("¡APROBADO! 🎉");
        tvSupervisorInfo.setText("Has sido aceptado por el Maestro: " + supervisorName);
        layoutAccionesAprobado.setVisibility(View.VISIBLE);
    }

    private void showPendingState(String supervisorName) {
        cardEstadoSolicitud.setVisibility(View.VISIBLE);
        cardEstadoSolicitud.setCardBackgroundColor(getResources().getColor(R.color.pending_bg, null));
        tvEstadoSolicitud.setText("EN ESPERA ⏳");
        tvSupervisorInfo.setText("Esperando aprobación del Maestro: " + supervisorName);
        layoutAccionesAprobado.setVisibility(View.GONE);
    }

    private void showNoSupervisorState() {
        cardEstadoSolicitud.setVisibility(View.VISIBLE);
        cardEstadoSolicitud.setCardBackgroundColor(getResources().getColor(R.color.error_bg, null));
        tvEstadoSolicitud.setText("SIN SUPERVISOR ❌");
        tvSupervisorInfo.setText("No tienes un maestro supervisor asignado. Por favor, actualiza tu perfil.");
        layoutAccionesAprobado.setVisibility(View.GONE);
    }

    private void showErrorState() {
        cardEstadoSolicitud.setVisibility(View.VISIBLE);
        cardEstadoSolicitud.setCardBackgroundColor(getResources().getColor(R.color.error_bg, null));
        tvEstadoSolicitud.setText("ERROR ❌");
        tvSupervisorInfo.setText("Ocurrió un error al cargar tus datos. Por favor, intenta nuevamente.");
        layoutAccionesAprobado.setVisibility(View.GONE);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadStudentData();
    }
}