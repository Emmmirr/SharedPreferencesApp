package com.example.sharedpreferencesapp;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

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
    private ProfileManager profileManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_student_home, container, false);

        // Inicializar ProfileManager
        profileManager = new ProfileManager(requireContext());

        // Inicializar vistas
        initializeViews(view);

        // Establecer fecha actual
        setCurrentDate();

        // Cargar datos del estudiante
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

        // Configurar listener para botón de ver protocolo
        btnVerProtocolo.setOnClickListener(v -> {
            // Navegar al fragmento de protocolo
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
        // Obtener usuario actual
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "Usuario no autenticado");
            return;
        }

        // Mostrar mensaje de bienvenida con datos básicos mientras carga
        tvWelcomeMessage.setText("Bienvenido, Estudiante");

        // Cargar datos completos desde Firestore
        profileManager.obtenerPerfilActual(
                profile -> {
                    if (profile != null) {
                        // Actualizar mensaje de bienvenida
                        String displayName = profile.getFullName().isEmpty() ? profile.getDisplayName() : profile.getFullName();
                        tvWelcomeMessage.setText("Bienvenido, " + displayName);

                        // Verificar si tiene supervisor asignado
                        String supervisorId = profile.getSupervisorId();
                        String supervisorName = profile.getSupervisorName();

                        if (supervisorId == null || supervisorId.isEmpty()) {
                            // No tiene supervisor asignado
                            showNoSupervisorState();
                            return;
                        }

                        // Tiene supervisor, verificar si está aprobado
                        boolean isApproved = profile.isApproved();

                        if (isApproved) {
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
        // Recargar datos cuando se vuelve al fragmento
        loadStudentData();
    }
}