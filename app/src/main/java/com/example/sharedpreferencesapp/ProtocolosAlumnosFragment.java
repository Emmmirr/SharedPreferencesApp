package com.example.sharedpreferencesapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ProtocolosAlumnosFragment extends Fragment {

    private static final String TAG = "ProtocolosAlumnosFrag";
    private LinearLayout layoutProtocolos;
    private TextView tvNoProtocolos;

    private FirebaseManager firebaseManager;
    private String currentUserId;
    private JSONObject protocoloPendiente;

    private final ActivityResultLauncher<Intent> selectorCarpeta = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null && protocoloPendiente != null) {
                        generarPDFEnUbicacion(protocoloPendiente, uri);
                    }
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_protocolos_alumnos, container, false);

        layoutProtocolos = view.findViewById(R.id.layoutProtocolosAlumnos);
        tvNoProtocolos = view.findViewById(R.id.tvNoProtocolosAlumnos);

        firebaseManager = new FirebaseManager();

        // Obtener el ID del usuario actual (maestro)
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            this.currentUserId = user.getUid();
            cargarProtocolosDeAlumnos();
        } else {
            tvNoProtocolos.setText("Error de sesión. Por favor, inicie sesión nuevamente.");
            tvNoProtocolos.setVisibility(View.VISIBLE);
        }

        return view;
    }

    private void cargarProtocolosDeAlumnos() {
        if (currentUserId == null) return;

        // Limpiar vista
        layoutProtocolos.removeAllViews();
        tvNoProtocolos.setText("Cargando protocolos de alumnos...");
        tvNoProtocolos.setVisibility(View.VISIBLE);

        // Paso 1: Obtener estudiantes aprobados por este maestro
        firebaseManager.cargarEstudiantesAprobados(currentUserId, task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                List<UserProfile> estudiantesAprobados = new ArrayList<>();

                for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                    UserProfile estudiante = UserProfile.fromMap(doc.getData());
                    if (estudiante.isApproved() &&
                            estudiante.getSupervisorId() != null &&
                            estudiante.getSupervisorId().equals(currentUserId)) {
                        estudiantesAprobados.add(estudiante);
                    }
                }

                if (estudiantesAprobados.isEmpty()) {
                    tvNoProtocolos.setText("No tiene alumnos aprobados con protocolos.");
                    tvNoProtocolos.setVisibility(View.VISIBLE);
                    return;
                }

                // Paso 2: Buscar protocolos para cada estudiante aprobado
                buscarProtocolosDeEstudiantes(estudiantesAprobados);

            } else {
                Log.e(TAG, "Error al cargar estudiantes aprobados", task.getException());
                tvNoProtocolos.setText("Error al cargar estudiantes aprobados.");
                tvNoProtocolos.setVisibility(View.VISIBLE);
            }
        });
    }

    private void buscarProtocolosDeEstudiantes(List<UserProfile> estudiantes) {
        if (estudiantes.isEmpty()) {
            tvNoProtocolos.setText("No se encontraron protocolos de alumnos.");
            tvNoProtocolos.setVisibility(View.VISIBLE);
            return;
        }

        final int[] estudiantesRestantes = {estudiantes.size()};
        final int[] protocolosEncontrados = {0};

        for (UserProfile estudiante : estudiantes) {
            firebaseManager.buscarProtocolosPorEstudiante(estudiante.getUserId(), task -> {
                estudiantesRestantes[0]--;

                if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                    for (DocumentSnapshot protocolo : task.getResult().getDocuments()) {
                        protocolosEncontrados[0]++;
                        crearCardProtocoloAlumno(protocolo, estudiante);
                    }
                }

                // Si ya revisamos todos los estudiantes, actualizar la UI según resultado
                if (estudiantesRestantes[0] == 0) {
                    if (protocolosEncontrados[0] == 0) {
                        tvNoProtocolos.setText("Los alumnos aprobados no tienen protocolos registrados.");
                        tvNoProtocolos.setVisibility(View.VISIBLE);
                    } else {
                        tvNoProtocolos.setVisibility(View.GONE);
                    }
                }
            });
        }
    }

    private void crearCardProtocoloAlumno(DocumentSnapshot protocolo, UserProfile estudiante) {
        View cardView = LayoutInflater.from(getContext()).inflate(R.layout.card_protocolo_alumno, layoutProtocolos, false);

        TextView tvNombreAlumno = cardView.findViewById(R.id.tvNombreAlumno);
        TextView tvNumControl = cardView.findViewById(R.id.tvNumControl);
        TextView tvNombreProyecto = cardView.findViewById(R.id.tvNombreProyecto);
        TextView tvEmpresa = cardView.findViewById(R.id.tvEmpresa);
        TextView tvBanco = cardView.findViewById(R.id.tvBanco);
        TextView tvFechaRegistro = cardView.findViewById(R.id.tvFechaRegistro);
        View btnVerPDF = cardView.findViewById(R.id.btnVerPDF);

        // Establecer datos del estudiante
        String nombreCompleto = estudiante.getFullName().isEmpty() ?
                estudiante.getDisplayName() : estudiante.getFullName();
        tvNombreAlumno.setText(nombreCompleto);
        tvNumControl.setText("Control: " + estudiante.getControlNumber());

        // Establecer datos del protocolo
        tvNombreProyecto.setText(protocolo.getString("nombreProyecto"));
        tvEmpresa.setText("Empresa: " + protocolo.getString("nombreEmpresa"));
        tvBanco.setText("Banco: " + protocolo.getString("banco"));

        // Fecha del protocolo (usando timestamp de creación o fecha actual)
        Object timestamp = protocolo.get("timestamp");
        if (timestamp != null) {
            Date fecha = protocolo.getDate("timestamp");
            if (fecha != null) {
                String fechaFormateada = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(fecha);
                tvFechaRegistro.setText("Fecha: " + fechaFormateada);
            } else {
                tvFechaRegistro.setText("Fecha: No disponible");
            }
        } else {
            tvFechaRegistro.setText("Fecha: No disponible");
        }

        // Configurar botón para generar PDF
        btnVerPDF.setOnClickListener(v -> {
            JSONObject protocoloJson = new JSONObject(protocolo.getData());
            try {
                protocoloJson.put("id", protocolo.getId());
                seleccionarUbicacionPDF(protocoloJson);
            } catch (JSONException e) {
                Toast.makeText(getContext(), "Error al preparar datos para PDF", Toast.LENGTH_SHORT).show();
            }
        });

        layoutProtocolos.addView(cardView);
    }

    private void seleccionarUbicacionPDF(JSONObject protocolo) {
        protocoloPendiente = protocolo;

        String nombreProyecto = protocolo.optString("nombreProyecto", "Protocolo");
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String nombreArchivo = "Protocolo_" + nombreProyecto.replaceAll("[^a-zA-Z0-9]", "_") + "_" + timestamp + ".pdf";

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_TITLE, nombreArchivo);

        try {
            selectorCarpeta.launch(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "❌ Error al abrir selector de archivos", Toast.LENGTH_SHORT).show();
        }
    }

    private void generarPDFEnUbicacion(JSONObject protocolo, Uri uri) {
        Toast.makeText(getContext(), "📄 Generando PDF...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            if (getActivity() == null) return;
            try {
                PDFGeneratorExterno pdfGenerator = new PDFGeneratorExterno(requireContext());
                boolean exito = pdfGenerator.generarPDFProtocoloEnUri(protocolo, uri);

                getActivity().runOnUiThread(() -> {
                    if (exito) {
                        String mensaje = "✅ PDF guardado exitosamente";
                        Toast.makeText(getContext(), mensaje, Toast.LENGTH_LONG).show();

                        new AlertDialog.Builder(getContext())
                                .setTitle("📄 PDF Creado")
                                .setMessage("El archivo PDF se ha guardado en la ubicación seleccionada.")
                                .setPositiveButton("OK", null)
                                .show();
                    } else {
                        Toast.makeText(getContext(), "❌ Error al generar PDF", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "❌ Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    @Override
    public void onResume() {
        super.onResume();
        cargarProtocolosDeAlumnos();
    }
}