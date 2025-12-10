package com.example.sharedpreferencesapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TramitesTabFragment extends Fragment {

    private static final String TAG = "TramitesTabFragment";
    private static final String ARG_CATEGORIA = "categoria";

    private int categoria; // 0: antes_de_iniciar, 1: durante, 2: al_finalizar
    private LinearLayout listaDocumentos;
    private TextView tvNoDocumentos;
    private FirebaseFirestore db;
    private String currentUserId;

    // Documentos por categoría
    private static final String[][] DOCUMENTOS = {
            // Antes de iniciar
            {"Solicitud de residencias", "solicitud_residencias"},
            {"Carta de presentación", "carta_presentacion"},
            {"Carta de aceptación", "carta_aceptacion"},
            {"Asignación de asesor interno", "asignacion_asesor"}
    };

    private static final String[][] DOCUMENTOS_DURANTE = {
            // Durante
            {"Cronograma", "cronograma"},
            {"Formato de evaluación y seguimiento", "formato_evaluacion"},
            {"Reportes parciales", "reportes_parciales"}
    };

    private static final String[][] DOCUMENTOS_FINALIZAR = {
            // Al finalizar
            {"Carta de término", "carta_termino"},
            {"Acta de calificación", "acta_calificacion"}
    };

    public static TramitesTabFragment newInstance(int categoria) {
        TramitesTabFragment fragment = new TramitesTabFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_CATEGORIA, categoria);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            categoria = getArguments().getInt(ARG_CATEGORIA, 0);
        }
        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tramites_tab, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        listaDocumentos = view.findViewById(R.id.listaDocumentos);
        tvNoDocumentos = view.findViewById(R.id.tvNoDocumentos);

        cargarDocumentos();
    }

    private void cargarDocumentos() {
        if (currentUserId == null) {
            Log.e(TAG, "Usuario no autenticado");
            return;
        }

        // Obtener lista de documentos según la categoría
        final String[][] documentosArray = obtenerDocumentosPorCategoria();

        if (documentosArray.length == 0) {
            tvNoDocumentos.setVisibility(View.VISIBLE);
            listaDocumentos.setVisibility(View.GONE);
            return;
        }

        tvNoDocumentos.setVisibility(View.GONE);
        listaDocumentos.setVisibility(View.VISIBLE);
        listaDocumentos.removeAllViews();

        // Cargar estado de documentos desde Firebase
        db.collection("user_profiles")
                .document(currentUserId)
                .collection("tramites_formatos")
                .document("documentos")
                .get()
                .addOnCompleteListener(task -> {
                    Map<String, Object> documentosData = new HashMap<>();
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                        documentosData = task.getResult().getData();
                    }

                    // Crear cards para cada documento
                    for (String[] documento : documentosArray) {
                        String nombre = documento[0];
                        String id = documento[1];
                        crearCardDocumento(nombre, id, documentosData);
                    }
                });
    }

    private String[][] obtenerDocumentosPorCategoria() {
        switch (categoria) {
            case 0:
                return DOCUMENTOS;
            case 1:
                return DOCUMENTOS_DURANTE;
            case 2:
                return DOCUMENTOS_FINALIZAR;
            default:
                return new String[0][];
        }
    }

    private void crearCardDocumento(String nombre, String id, Map<String, Object> documentosData) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View cardView = inflater.inflate(R.layout.item_documento_checklist, listaDocumentos, false);

        TextView tvNombre = cardView.findViewById(R.id.tvNombreDocumento);
        ImageView ivCheckEstado = cardView.findViewById(R.id.ivCheckEstado);
        LinearLayout layoutBotones = cardView.findViewById(R.id.layoutBotones);
        LinearLayout btnVer = cardView.findViewById(R.id.btnVer);
        LinearLayout btnDescargar = cardView.findViewById(R.id.btnDescargar);
        LinearLayout btnLlenar = cardView.findViewById(R.id.btnLlenar);
        com.google.android.material.card.MaterialCardView cardDocumento = cardView.findViewById(R.id.cardDocumento);

        tvNombre.setText(nombre);

        // Obtener estado del documento
        Map<String, Object> docDataTemp = null;
        if (documentosData != null && documentosData.containsKey(id)) {
            Object docObj = documentosData.get(id);
            if (docObj instanceof Map) {
                docDataTemp = (Map<String, Object>) docObj;
            }
        }
        final Map<String, Object> docData = docDataTemp;
        final String finalId = id;
        final String finalNombre = nombre;

        // Verificar si está marcado como completado por el usuario
        final boolean[] estaCompletado = {docData != null &&
                Boolean.TRUE.equals(docData.getOrDefault("completado", false))};

        // Actualizar estado visual inicial
        actualizarEstadoVisual(ivCheckEstado, cardDocumento, estaCompletado[0]);

        // Configurar botones
        boolean tieneDocumento = docData != null && docData.containsKey("urlDocumento");
        btnVer.setVisibility(tieneDocumento ? View.VISIBLE : View.GONE);

        // Click en el círculo de check para marcar/desmarcar como completado
        ivCheckEstado.setOnClickListener(v -> {
            v.getParent().requestDisallowInterceptTouchEvent(true);
            boolean nuevoEstado = !estaCompletado[0];
            marcarComoCompletado(finalId, nuevoEstado);
            actualizarEstadoVisual(ivCheckEstado, cardDocumento, nuevoEstado);
            estaCompletado[0] = nuevoEstado;
        });

        // Click en la card para expandir/colapsar botones
        LinearLayout cardContent = cardView.findViewById(R.id.cardContent);
        cardContent.setOnClickListener(v -> {
            boolean isExpanded = layoutBotones.getVisibility() == View.VISIBLE;
            layoutBotones.setVisibility(isExpanded ? View.GONE : View.VISIBLE);
        });

        btnVer.setOnClickListener(v -> {
            v.getParent().requestDisallowInterceptTouchEvent(true);
            verDocumento(finalId, docData);
        });
        btnDescargar.setOnClickListener(v -> {
            v.getParent().requestDisallowInterceptTouchEvent(true);
            descargarFormato(finalId, finalNombre);
        });
        btnLlenar.setOnClickListener(v -> {
            v.getParent().requestDisallowInterceptTouchEvent(true);
            abrirWizard(finalId, finalNombre, docData);
        });

        listaDocumentos.addView(cardView);
    }

    private void actualizarEstadoVisual(ImageView ivCheckEstado, com.google.android.material.card.MaterialCardView card, boolean completado) {
        if (completado) {
            ivCheckEstado.setImageResource(R.drawable.check_circle_completed);
            card.setCardBackgroundColor(getResources().getColor(R.color.green_pastel, null));
        } else {
            ivCheckEstado.setImageResource(R.drawable.check_circle_pending);
            card.setCardBackgroundColor(getResources().getColor(R.color.white, null));
        }
    }

    private void marcarComoCompletado(String documentoId, boolean completado) {
        if (currentUserId == null) {
            Toast.makeText(getContext(), "Usuario no autenticado", Toast.LENGTH_SHORT).show();
            return;
        }

        // Actualizar en Firebase
        db.collection("user_profiles")
                .document(currentUserId)
                .collection("tramites_formatos")
                .document("documentos")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        Map<String, Object> documentosData = new HashMap<>();
                        if (task.getResult().exists()) {
                            documentosData = task.getResult().getData();
                        }

                        // Obtener o crear el documento
                        Map<String, Object> docData = new HashMap<>();
                        if (documentosData.containsKey(documentoId)) {
                            Object docObj = documentosData.get(documentoId);
                            if (docObj instanceof Map) {
                                docData = (Map<String, Object>) docObj;
                            }
                        }

                        // Actualizar el estado de completado
                        docData.put("completado", completado);
                        documentosData.put(documentoId, docData);

                        // Guardar en Firebase
                        db.collection("user_profiles")
                                .document(currentUserId)
                                .collection("tramites_formatos")
                                .document("documentos")
                                .set(documentosData)
                                .addOnCompleteListener(saveTask -> {
                                    if (saveTask.isSuccessful()) {
                                        Log.d(TAG, "Estado de completado actualizado para: " + documentoId);
                                    } else {
                                        Log.e(TAG, "Error al actualizar estado de completado", saveTask.getException());
                                        Toast.makeText(getContext(), "Error al guardar el estado", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                });
    }

    private void verDocumento(String documentoId, Map<String, Object> docData) {
        if (docData != null && docData.containsKey("urlDocumento")) {
            String url = (String) docData.get("urlDocumento");
            // Verificar si es Word o PDF
            boolean esWord = url.toLowerCase().endsWith(".docx") ||
                    (docData.containsKey("urlWord") && url.equals(docData.get("urlWord")));
            if (esWord) {
                abrirVisorWord(url);
            } else {
                abrirVisorPDF(url);
            }
        } else {
            Toast.makeText(getContext(), "No hay documento para visualizar", Toast.LENGTH_SHORT).show();
        }
    }

    private void abrirVisorWord(String urlString) {
        if (urlString == null || urlString.isEmpty()) {
            Toast.makeText(getContext(), "No hay documento para visualizar.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
            builder.setTitle("Documento disponible");
            builder.setMessage("¿Qué deseas hacer con el documento Word?");

            builder.setPositiveButton("Ver", (dialog, which) -> {
                try {
                    String encodedUrl = java.net.URLEncoder.encode(urlString, "UTF-8");
                    // Google Docs Viewer puede abrir Word también
                    String googleDocsViewerUrl = "https://docs.google.com/gview?embedded=true&url=" + encodedUrl;

                    android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW);
                    intent.setData(android.net.Uri.parse(googleDocsViewerUrl));
                    try {
                        startActivity(intent);
                    } catch (android.content.ActivityNotFoundException e) {
                        // Intentar abrir directamente con una app que pueda manejar Word
                        try {
                            android.content.Intent intent2 = new android.content.Intent(android.content.Intent.ACTION_VIEW);
                            intent2.setData(android.net.Uri.parse(urlString));
                            startActivity(intent2);
                        } catch (Exception ex) {
                            Toast.makeText(getContext(), "No se encontró una aplicación para abrir este documento.", Toast.LENGTH_LONG).show();
                        }
                    }
                } catch (java.io.UnsupportedEncodingException e) {
                    Log.e(TAG, "Error codificando URL", e);
                    Toast.makeText(getContext(), "Error al procesar el enlace", Toast.LENGTH_SHORT).show();
                }
            });

            builder.setNeutralButton("Descargar", (dialog, which) -> {
                descargarWordDesdeURL(urlString);
            });

            builder.setNegativeButton("Cancelar", null);
            builder.show();

        } catch (Exception e) {
            Log.e(TAG, "Error general al intentar ver Word: " + urlString, e);
            Toast.makeText(getContext(), "No se pudo abrir el enlace del documento.", Toast.LENGTH_LONG).show();
        }
    }

    private void abrirVisorPDF(String urlString) {
        if (urlString == null || urlString.isEmpty()) {
            Toast.makeText(getContext(), "No hay documento para visualizar.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Crear un diálogo con opciones: Ver y Descargar
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
            builder.setTitle("Documento disponible");
            builder.setMessage("¿Qué deseas hacer con el documento?");

            builder.setPositiveButton("Ver", (dialog, which) -> {
                try {
                    String encodedUrl = java.net.URLEncoder.encode(urlString, "UTF-8");
                    String googleDocsViewerUrl = "https://docs.google.com/gview?embedded=true&url=" + encodedUrl;

                    android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW);
                    intent.setData(android.net.Uri.parse(googleDocsViewerUrl));
                    try {
                        startActivity(intent);
                    } catch (android.content.ActivityNotFoundException e) {
                        Toast.makeText(getContext(), "No se encontró una aplicación para abrir este enlace.", Toast.LENGTH_LONG).show();
                    }
                } catch (java.io.UnsupportedEncodingException e) {
                    Log.e(TAG, "Error codificando URL", e);
                    Toast.makeText(getContext(), "Error al procesar el enlace", Toast.LENGTH_SHORT).show();
                }
            });

            builder.setNeutralButton("Descargar PDF", (dialog, which) -> {
                descargarPDFDesdeURL(urlString);
            });

            builder.setNegativeButton("Cancelar", null);
            builder.show();

        } catch (Exception e) {
            Log.e(TAG, "Error general al intentar ver PDF: " + urlString, e);
            Toast.makeText(getContext(), "No se pudo abrir el enlace del documento.", Toast.LENGTH_LONG).show();
        }
    }

    private void descargarPDFDesdeURL(String urlString) {
        if (getContext() == null) return;

        Toast.makeText(getContext(), "Descargando PDF...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL(urlString);
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                connection.connect();

                if (connection.getResponseCode() != java.net.HttpURLConnection.HTTP_OK) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Error al descargar el PDF", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                // Crear archivo PDF en descargas
                String nombreArchivo = "documento_" + System.currentTimeMillis() + ".pdf";
                java.io.File pdfFile;

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    android.content.ContentResolver resolver = getContext().getContentResolver();
                    android.content.ContentValues contentValues = new android.content.ContentValues();
                    contentValues.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, nombreArchivo);
                    contentValues.put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
                    contentValues.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS);

                    android.net.Uri uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);
                    if (uri != null) {
                        try (java.io.InputStream input = connection.getInputStream();
                             java.io.OutputStream output = resolver.openOutputStream(uri)) {

                            byte[] buffer = new byte[4096];
                            int bytesRead;
                            while ((bytesRead = input.read(buffer)) != -1) {
                                output.write(buffer, 0, bytesRead);
                            }

                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(getContext(), "PDF descargado exitosamente en Descargas", Toast.LENGTH_SHORT).show();
                            });
                        }
                    }
                } else {
                    java.io.File downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS);
                    if (!downloadsDir.exists()) {
                        downloadsDir.mkdirs();
                    }
                    pdfFile = new java.io.File(downloadsDir, nombreArchivo);

                    try (java.io.InputStream input = connection.getInputStream();
                         java.io.FileOutputStream output = new java.io.FileOutputStream(pdfFile)) {

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = input.read(buffer)) != -1) {
                            output.write(buffer, 0, bytesRead);
                        }

                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "PDF descargado exitosamente en Descargas", Toast.LENGTH_SHORT).show();
                        });
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "Error descargando PDF", e);
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Error al descargar el PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void descargarWordDesdeURL(String urlString) {
        if (getContext() == null) return;

        Toast.makeText(getContext(), "Descargando documento Word...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL(urlString);
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                connection.connect();

                if (connection.getResponseCode() != java.net.HttpURLConnection.HTTP_OK) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Error al descargar el documento", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                String nombreArchivo = "documento_" + System.currentTimeMillis() + ".docx";
                java.io.File wordFile;

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    android.content.ContentResolver resolver = getContext().getContentResolver();
                    android.content.ContentValues contentValues = new android.content.ContentValues();
                    contentValues.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, nombreArchivo);
                    contentValues.put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
                    contentValues.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS);

                    android.net.Uri uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);
                    if (uri != null) {
                        try (java.io.InputStream input = connection.getInputStream();
                             java.io.OutputStream output = resolver.openOutputStream(uri)) {

                            byte[] buffer = new byte[4096];
                            int bytesRead;
                            while ((bytesRead = input.read(buffer)) != -1) {
                                output.write(buffer, 0, bytesRead);
                            }
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(getContext(), "Documento Word descargado en la carpeta Descargas", Toast.LENGTH_LONG).show();
                            });
                        }
                    } else {
                        throw new java.io.IOException("No se pudo crear el archivo en Descargas (API 29+)");
                    }
                } else {
                    java.io.File downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS);
                    if (!downloadsDir.exists()) {
                        downloadsDir.mkdirs();
                    }
                    wordFile = new java.io.File(downloadsDir, nombreArchivo);

                    try (java.io.InputStream input = connection.getInputStream();
                         java.io.OutputStream output = new java.io.FileOutputStream(wordFile)) {

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = input.read(buffer)) != -1) {
                            output.write(buffer, 0, bytesRead);
                        }
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "Documento Word descargado en la carpeta Descargas", Toast.LENGTH_LONG).show();
                        });
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "Error al descargar Word desde URL", e);
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Error al descargar el documento: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void descargarFormato(String documentoId, String nombre) {
        if (getContext() == null) return;

        Toast.makeText(getContext(), "Obteniendo enlace de descarga...", Toast.LENGTH_SHORT).show();

        // Obtener URL de descarga desde la configuración global (no por usuario)
        db.collection("tramites_formatos_config")
                .document("documentos")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                        Map<String, Object> documentosConfig = task.getResult().getData();

                        // Buscar el documento específico - el campo es directamente el string de la URL
                        if (documentosConfig != null && documentosConfig.containsKey(documentoId)) {
                            Object urlObj = documentosConfig.get(documentoId);
                            String urlDescarga = null;

                            // Si es un string directamente
                            if (urlObj instanceof String) {
                                urlDescarga = (String) urlObj;
                            }
                            // Si es un Map con urlDescarga (para compatibilidad)
                            else if (urlObj instanceof Map) {
                                Map<String, Object> docConfig = (Map<String, Object>) urlObj;
                                urlDescarga = (String) docConfig.get("urlDescarga");
                            }

                            if (urlDescarga != null && !urlDescarga.isEmpty()) {
                                // Abrir el enlace en el navegador
                                android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW);
                                intent.setData(android.net.Uri.parse(urlDescarga));
                                try {
                                    startActivity(intent);
                                    Toast.makeText(getContext(), "Abriendo enlace de descarga", Toast.LENGTH_SHORT).show();
                                } catch (android.content.ActivityNotFoundException e) {
                                    Toast.makeText(getContext(), "No se pudo abrir el enlace", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(getContext(), "No hay enlace de descarga disponible para este formato", Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Toast.makeText(getContext(), "No se encontró información del formato", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getContext(), "Error al obtener información del formato", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void abrirWizard(String documentoId, String nombre, Map<String, Object> docData) {
        if (getActivity() != null) {
            WizardTramiteFragment wizardFragment = WizardTramiteFragment.newInstance(documentoId, nombre);
            getActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, wizardFragment)
                    .addToBackStack(null)
                    .commit();
        }
    }
}

