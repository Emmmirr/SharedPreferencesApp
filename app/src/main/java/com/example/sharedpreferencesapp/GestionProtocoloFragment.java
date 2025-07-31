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

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class GestionProtocoloFragment extends Fragment {

    private static final String TAG = "GestionProtocoloFrag";
    private LinearLayout layoutProtocolos;
    private TextView tvNoProtocolos;

    private FirebaseFirestore db;
    private FirebaseManager firebaseManager; // Se añade para buscar perfiles de estudiantes
    private String currentUserId;

    // --- CAMBIO: Ya no se necesita el ActivityResultLauncher para guardar ---
    // private final ActivityResultLauncher<Intent> selectorCarpeta = ...;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_gestion_protocolo, container, false);

        layoutProtocolos = view.findViewById(R.id.layoutProtocolos);
        tvNoProtocolos = view.findViewById(R.id.tvNoProtocolos);

        db = FirebaseFirestore.getInstance();
        firebaseManager = new FirebaseManager(); // Se inicializa FirebaseManager

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
            tvNoProtocolos.setText("Error de sesión. Por favor, inicie sesión de nuevo.");
            tvNoProtocolos.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (currentUserId != null) {
            cargarProtocolosDeAlumnos();
        }
    }

    private void cargarProtocolosDeAlumnos() {
        if (currentUserId == null) return;

        layoutProtocolos.removeAllViews();
        tvNoProtocolos.setText("Cargando protocolos...");
        tvNoProtocolos.setVisibility(View.VISIBLE);

        db.collection("protocolos")
                .whereEqualTo("supervisorId", currentUserId)
                .orderBy("fechaActualizacion", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        if (task.getResult().isEmpty()) {
                            tvNoProtocolos.setText("Aún no hay protocolos registrados por tus alumnos.");
                        } else {
                            tvNoProtocolos.setVisibility(View.GONE);
                            for (QueryDocumentSnapshot documento : task.getResult()) {
                                crearCardProtocolo(documento);
                            }
                        }
                    } else {
                        tvNoProtocolos.setText("Error al cargar protocolos.");
                        Log.e(TAG, "Error cargando protocolos", task.getException());
                    }
                });
    }

    private void crearCardProtocolo(DocumentSnapshot protocolo) {
        if (getContext() == null) return;

        View cardView = LayoutInflater.from(getContext()).inflate(R.layout.card_protocolo_alumno, layoutProtocolos, false);

        TextView tvNombreAlumno = cardView.findViewById(R.id.tvNombreAlumno);
        TextView tvNumControl = cardView.findViewById(R.id.tvNumControl);
        TextView tvNombreProyecto = cardView.findViewById(R.id.tvNombreProyecto);
        TextView tvEmpresa = cardView.findViewById(R.id.tvEmpresa);
        TextView tvBanco = cardView.findViewById(R.id.tvBanco);
        TextView tvFechaRegistro = cardView.findViewById(R.id.tvFechaRegistro);
        Button btnVerPDF = cardView.findViewById(R.id.btnVerPDF);

        // Cambiamos el texto del botón para que sea más claro
        btnVerPDF.setText("Ver PDF");

        // Rellenar la card con los datos
        tvNombreAlumno.setText(protocolo.getString("nombreEstudiante"));
        tvNumControl.setText("Control: " + protocolo.getString("numeroControl"));
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

        // --- INICIO DE LÓGICA MODIFICADA PARA EL BOTÓN "VER PDF" ---
        btnVerPDF.setOnClickListener(v -> {
            String estudianteId = protocolo.getString("estudianteId");
            if (estudianteId == null || estudianteId.isEmpty()) {
                Toast.makeText(getContext(), "Error: ID de estudiante no encontrado en el protocolo.", Toast.LENGTH_SHORT).show();
                return;
            }

            // 1. Buscamos el perfil completo del alumno en Firestore.
            firebaseManager.buscarPerfilDeEstudiantePorId(estudianteId, task -> {
                if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                    // 2. Si lo encontramos, pasamos los datos del protocolo y del alumno al nuevo método.
                    Map<String, Object> studentData = task.getResult().getData();
                    generarYVisualizarPDF(new JSONObject(protocolo.getData()), new JSONObject(studentData));
                } else {
                    Toast.makeText(getContext(), "No se pudo encontrar el perfil del alumno para generar el PDF.", Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Perfil de estudiante no encontrado para ID: " + estudianteId, task.getException());
                }
            });
        });
        // --- FIN DE LÓGICA MODIFICADA ---

        layoutProtocolos.addView(cardView);
    }

    // --- INICIO DE NUEVO MÉTODO PARA GENERAR Y VISUALIZAR PDF ---
// Reemplaza el método existente con este en AMBOS fragmentos.
// Asegúrate de importar las clases necesarias (ContentValues, ContentResolver).
// import android.content.ContentValues;
// import android.content.ContentResolver;
// import android.provider.MediaStore;
// import java.io.OutputStream;

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
    // --- FIN DE NUEVO MÉTODO ---

    // Los métodos `seleccionarUbicacionPDF` y `generarPDFEnUbicacion` han sido reemplazados
    // por la nueva lógica de `generarYVisualizarPDF`.
}