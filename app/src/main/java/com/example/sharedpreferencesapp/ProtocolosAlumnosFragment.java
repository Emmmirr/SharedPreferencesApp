package com.example.sharedpreferencesapp;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
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
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProtocolosAlumnosFragment extends Fragment {

    private static final String TAG = "ProtocolosAlumnosFrag";
    private LinearLayout layoutProtocolos;
    private TextView tvNoProtocolos;

    private FirebaseManager firebaseManager;
    private FirebaseFirestore db;
    private String currentUserId;

    // --- CAMBIO ---
    // Ya no se necesitan estas variables globales para la generación de PDF
    // private JSONObject protocoloPendiente;
    // private final ActivityResultLauncher<Intent> selectorCarpeta = ...;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_protocolos_alumnos, container, false);

        layoutProtocolos = view.findViewById(R.id.layoutProtocolosAlumnos);
        tvNoProtocolos = view.findViewById(R.id.tvNoProtocolosAlumnos);

        firebaseManager = new FirebaseManager();
        db = FirebaseFirestore.getInstance();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            this.currentUserId = user.getUid();
            cargarProtocolosDeAlumnos();
        } else {
            tvNoProtocolos.setText("Error de sesión. Por favor, inicie sesión nuevamente.");
            tvNoProtocolos.setVisibility(View.VISIBLE);
        }
    }

    private void cargarProtocolosDeAlumnos() {
        if (currentUserId == null) return;
        layoutProtocolos.removeAllViews();
        tvNoProtocolos.setText("Cargando protocolos de alumnos...");
        tvNoProtocolos.setVisibility(View.VISIBLE);

        // --- CAMBIO: Se ajusta la consulta para usar 'db' directamente y se añade 'orderBy' ---
        db.collection("protocolos")
                .whereEqualTo("supervisorId", currentUserId)
                .orderBy("fechaActualizacion", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        if (task.getResult().isEmpty()) {
                            tvNoProtocolos.setText("Aún no hay protocolos registrados por tus alumnos.");
                            return;
                        }

                        tvNoProtocolos.setVisibility(View.GONE);
                        for (QueryDocumentSnapshot protocoloDoc : task.getResult()) {
                            // --- CAMBIO: Ahora pasamos el UserProfile a crearCardProtocoloAlumno ---
                            // Se necesita buscar el perfil completo del estudiante para tener todos sus datos
                            String estudianteId = protocoloDoc.getString("estudianteId");
                            if (estudianteId != null && !estudianteId.isEmpty()) {
                                firebaseManager.buscarPerfilDeEstudiantePorId(estudianteId, profileTask -> {
                                    if(profileTask.isSuccessful() && profileTask.getResult() != null && profileTask.getResult().exists()) {
                                        UserProfile estudianteProfile = UserProfile.fromMap(profileTask.getResult().getData());
                                        crearCardProtocoloAlumno(protocoloDoc, estudianteProfile);
                                    }
                                });
                            }
                        }
                    } else {
                        Log.e(TAG, "Error al cargar protocolos", task.getException());
                        tvNoProtocolos.setText("Error al cargar protocolos.");
                    }
                });
    }

    private void crearCardProtocoloAlumno(DocumentSnapshot protocolo, UserProfile estudiante) {
        if (getContext() == null) return;
        View cardView = LayoutInflater.from(getContext()).inflate(R.layout.card_protocolo_alumno, layoutProtocolos, false);

        TextView tvNombreAlumno = cardView.findViewById(R.id.tvNombreAlumno);
        TextView tvNumControl = cardView.findViewById(R.id.tvNumControl);
        TextView tvNombreProyecto = cardView.findViewById(R.id.tvNombreProyecto);
        TextView tvEmpresa = cardView.findViewById(R.id.tvEmpresa);
        TextView tvBanco = cardView.findViewById(R.id.tvBanco);
        TextView tvFechaRegistro = cardView.findViewById(R.id.tvFechaRegistro);
        View btnVerPDF = cardView.findViewById(R.id.btnVerPDF);

        String nombreCompleto = estudiante.getFullName().isEmpty() ? estudiante.getDisplayName() : estudiante.getFullName();
        tvNombreAlumno.setText(nombreCompleto);
        tvNumControl.setText("Control: " + estudiante.getControlNumber());
        tvNombreProyecto.setText(protocolo.getString("nombreProyecto"));
        tvEmpresa.setText("Empresa: " + protocolo.getString("nombreEmpresa"));
        tvBanco.setText("Banco: " + protocolo.getString("tipoProyecto"));

        if (protocolo.contains("fechaActualizacion")) {
            try {
                long timestamp = protocolo.getLong("fechaActualizacion");
                String fechaFormateada = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date(timestamp));
                tvFechaRegistro.setText("Última act: " + fechaFormateada);
            } catch (Exception e) {
                tvFechaRegistro.setText("Fecha no disponible");
            }
        } else {
            tvFechaRegistro.setText("Fecha no disponible");
        }

        btnVerPDF.setOnClickListener(v -> {
            // --- CAMBIO: Convertimos el UserProfile a JSONObject para pasarlo al generador ---
            JSONObject protocoloJson = new JSONObject(protocolo.getData());
            JSONObject alumnoJson = new JSONObject(estudiante.toMap());

            try {
                protocoloJson.put("id", protocolo.getId());
                // Llamamos al nuevo método para visualizar
                generarYVisualizarPDF(protocoloJson, alumnoJson);
            } catch (JSONException e) {
                Toast.makeText(getContext(), "Error al preparar datos para PDF", Toast.LENGTH_SHORT).show();
            }
        });

        layoutProtocolos.addView(cardView);
    }

// Reemplaza el método existente con este en AMBOS fragmentos.
// Asegúrate de importar las clases necesarias:
// import android.os.Build;
// import android.os.Environment;
// import android.content.ContentValues;
// import android.content.ContentResolver;
// import android.provider.MediaStore;
// import java.io.OutputStream;
// import java.io.FileOutputStream;

    private void generarYVisualizarPDF(JSONObject protocolo, JSONObject alumno) {
        if (getContext() == null || getActivity() == null) return;
        Toast.makeText(getContext(), "📄 Generando PDF...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            Uri pdfUri = null;
            File pdfFile = null; // Variable para el método legacy
            boolean exito = false;

            try {
                String nombreProyecto = protocolo.optString("nombreProyecto", "Protocolo");
                String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                String nombreArchivo = "Protocolo_" + nombreProyecto.replaceAll("[^a-zA-Z0-9]", "_") + "_" + timestamp + ".pdf";

                // --- INICIO DE LA LÓGICA DE VERSIÓN DE ANDROID ---
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // MÉTODO MODERNO (Android 10 y superior)
                    ContentResolver resolver = getContext().getContentResolver();
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, nombreArchivo);
                    contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
                    contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                    pdfUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);
                    if (pdfUri == null) throw new IOException("No se pudo crear el archivo en Descargas (API 29+)");

                    try (OutputStream outputStream = resolver.openOutputStream(pdfUri)) {
                        PDFGeneratorExterno pdfGenerator = new PDFGeneratorExterno(getContext());
                        exito = pdfGenerator.generarPDFProtocoloEnOutputStream(protocolo, alumno, outputStream);
                    }

                    if (!exito) {
                        resolver.delete(pdfUri, null, null); // Limpiar si falla
                    }

                } else {
                    // MÉTODO LEGACY (Android 9 y anterior)
                    File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    if (!downloadsDir.exists()) {
                        downloadsDir.mkdirs();
                    }
                    pdfFile = new File(downloadsDir, nombreArchivo);

                    try (OutputStream outputStream = new FileOutputStream(pdfFile)) {
                        PDFGeneratorExterno pdfGenerator = new PDFGeneratorExterno(getContext());
                        exito = pdfGenerator.generarPDFProtocoloEnOutputStream(protocolo, alumno, outputStream);
                    }
                }
                // --- FIN DE LA LÓGICA DE VERSIÓN DE ANDROID ---

                if (!exito) {
                    throw new Exception("El generador de PDF reportó un error.");
                }

                // Obtener la Uri correcta para el Intent de visualización
                final Uri uriParaVisualizar;
                if (pdfFile != null) {
                    // Si usamos el método legacy, obtenemos la Uri con FileProvider
                    uriParaVisualizar = FileProvider.getUriForFile(
                            getContext(),
                            getContext().getApplicationContext().getPackageName() + ".provider",
                            pdfFile
                    );
                } else {
                    // Si usamos el método moderno, la Uri ya la tenemos
                    uriParaVisualizar = pdfUri;
                }

                // Crear y lanzar el Intent para ver el PDF
                Intent viewIntent = new Intent(Intent.ACTION_VIEW);
                viewIntent.setDataAndType(uriParaVisualizar, "application/pdf");
                viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "PDF guardado en Descargas.", Toast.LENGTH_SHORT).show();
                    try {
                        startActivity(Intent.createChooser(viewIntent, "Abrir PDF con:"));
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(getContext(), "No se encontró una aplicación para ver archivos PDF.", Toast.LENGTH_LONG).show();
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error en generarYVisualizarPDF", e);
                // Si hubo un error con el método moderno, intentamos limpiar
                if (pdfUri != null) {
                    try {
                        getContext().getContentResolver().delete(pdfUri, null, null);
                    } catch (Exception ex) {
                        Log.e(TAG, "Error al limpiar Uri fallida", ex);
                    }
                }
                getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "❌ Error al guardar el PDF: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    @Override
    public void onResume() {
        super.onResume();
        if(currentUserId != null) {
            cargarProtocolosDeAlumnos();
        }
    }
}