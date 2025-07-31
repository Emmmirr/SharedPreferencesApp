package com.example.sharedpreferencesapp;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

public class FechasAsignadasFragment extends Fragment {

    private static final String TAG = "FechasAsignadasFragment";

    private TextView tvSupervisorInfo, tvNoCalendario;
    private TextView tvLabelFecha1, tvFecha1;
    private TextView tvLabelFecha2, tvFecha2;
    private TextView tvLabelFecha3, tvFecha3;
    private LinearLayout layoutFechasContainer;

    private FirebaseManager firebaseManager;
    private ProfileManager profileManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_fechas_asignadas, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firebaseManager = new FirebaseManager();
        profileManager = new ProfileManager(requireContext());

        initViews(view);
        cargarCalendarioAsignado();
    }

    private void initViews(View view) {
        tvSupervisorInfo = view.findViewById(R.id.tvSupervisorInfo);
        tvNoCalendario = view.findViewById(R.id.tvNoCalendario);
        layoutFechasContainer = view.findViewById(R.id.layoutFechasContainer);

        tvLabelFecha1 = view.findViewById(R.id.tvLabelFecha1);
        tvFecha1 = view.findViewById(R.id.tvFecha1);
        tvLabelFecha2 = view.findViewById(R.id.tvLabelFecha2);
        tvFecha2 = view.findViewById(R.id.tvFecha2);
        tvLabelFecha3 = view.findViewById(R.id.tvLabelFecha3);
        tvFecha3 = view.findViewById(R.id.tvFecha3);
    }

    private void cargarCalendarioAsignado() {
        FirebaseUser studentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (studentUser == null || getContext() == null) return;

        String studentId = studentUser.getUid();

        profileManager.obtenerPerfilActual(
                profile -> {
                    if (profile == null) {
                        handleError("No se pudo cargar tu perfil.");
                        return;
                    }

                    String supervisorId = profile.getSupervisorId();
                    String supervisorName = profile.getSupervisorName();

                    if (supervisorId == null || supervisorId.isEmpty()) {
                        tvSupervisorInfo.setText("Aún no tienes un supervisor asignado.");
                        tvNoCalendario.setVisibility(View.VISIBLE);
                        layoutFechasContainer.setVisibility(View.GONE);
                        return;
                    }

                    tvSupervisorInfo.setText("Asignado por: " + (supervisorName.isEmpty() ? "Supervisor" : supervisorName));

                    String calendarioId = "calendario_" + studentId;
                    firebaseManager.buscarCalendarioPorId(supervisorId, calendarioId, task -> {
                        if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                            actualizarUIConDatos(task.getResult());
                        } else {
                            tvNoCalendario.setVisibility(View.VISIBLE);
                            layoutFechasContainer.setVisibility(View.GONE);
                        }
                    });
                },
                error -> handleError("Error al obtener perfil: " + error.getMessage())
        );
    }

    private void actualizarUIConDatos(DocumentSnapshot calendario) {
        tvNoCalendario.setVisibility(View.GONE);
        layoutFechasContainer.setVisibility(View.VISIBLE);

        // Nombres de campo y etiquetas por defecto
        String[] camposFecha = {"fechaPrimeraEntrega", "fechaSegundaEntrega", "fechaResultado"};
        String[] camposLabel = {"labelPrimeraEntrega", "labelSegundaEntrega", "labelResultado"};
        String[] defaultLabels = {"1ª Entrega", "2ª Entrega", "Resultado Final"};
        TextView[] textViewsLabel = {tvLabelFecha1, tvLabelFecha2, tvLabelFecha3};
        TextView[] textViewsFecha = {tvFecha1, tvFecha2, tvFecha3};

        for (int i = 0; i < camposFecha.length; i++) {
            String label = calendario.getString(camposLabel[i]);
            String fecha = calendario.getString(camposFecha[i]);

            textViewsLabel[i].setText(label != null && !label.isEmpty() ? label : defaultLabels[i]);
            textViewsFecha[i].setText("Fecha: " + (fecha != null && !fecha.isEmpty() ? fecha : "Sin asignar"));
        }
    }

    private void handleError(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            tvSupervisorInfo.setText("Error");
            tvNoCalendario.setText(message);
            tvNoCalendario.setVisibility(View.VISIBLE);
            layoutFechasContainer.setVisibility(View.GONE);
        }
        Log.e(TAG, message);
    }
}