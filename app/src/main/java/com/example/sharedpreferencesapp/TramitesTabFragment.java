package com.example.sharedpreferencesapp;

import android.content.Context;
import android.content.SharedPreferences;
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
        TextView ivEstado = cardView.findViewById(R.id.ivEstado);
        LinearLayout btnVer = cardView.findViewById(R.id.btnVer);
        LinearLayout btnDescargar = cardView.findViewById(R.id.btnDescargar);
        LinearLayout btnLlenar = cardView.findViewById(R.id.btnLlenar);

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

        // Actualizar estado visual
        String estado = docData != null ? (String) docData.getOrDefault("estado", "pendiente") : "pendiente";
        actualizarEstadoVisual(ivEstado, estado, docData != null && docData.containsKey("urlDocumento"));

        // Configurar botones
        boolean tieneDocumento = docData != null && docData.containsKey("urlDocumento");
        btnVer.setVisibility(tieneDocumento ? View.VISIBLE : View.GONE);

        btnVer.setOnClickListener(v -> verDocumento(finalId, docData));
        btnDescargar.setOnClickListener(v -> descargarFormato(finalId, finalNombre));
        btnLlenar.setOnClickListener(v -> abrirWizard(finalId, finalNombre, docData));

        listaDocumentos.addView(cardView);
    }

    private void actualizarEstadoVisual(TextView ivEstado, String estado, boolean tieneDocumento) {
        switch (estado) {
            case "completado":
                ivEstado.setText("✓");
                ivEstado.setTextColor(getResources().getColor(R.color.status_approved, null));
                break;
            case "aprobado":
                ivEstado.setText("✓✓");
                ivEstado.setTextColor(getResources().getColor(R.color.status_approved, null));
                break;
            case "en_proceso":
                ivEstado.setText("⏱");
                ivEstado.setTextColor(getResources().getColor(R.color.status_pending, null));
                break;
            default:
                if (tieneDocumento) {
                    ivEstado.setText("○");
                    ivEstado.setTextColor(getResources().getColor(R.color.status_ongoing, null));
                } else {
                    ivEstado.setText("○");
                    ivEstado.setTextColor(getResources().getColor(R.color.text_secondary, null));
                }
                break;
        }
    }

    private void verDocumento(String documentoId, Map<String, Object> docData) {
        if (docData != null && docData.containsKey("urlDocumento")) {
            String url = (String) docData.get("urlDocumento");
            abrirVisorPDF(url);
        } else {
            Toast.makeText(getContext(), "No hay documento para visualizar", Toast.LENGTH_SHORT).show();
        }
    }

    private void abrirVisorPDF(String urlString) {
        if (urlString == null || urlString.isEmpty()) {
            Toast.makeText(getContext(), "No hay documento para visualizar.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String encodedUrl = java.net.URLEncoder.encode(urlString, "UTF-8");
            String googleDocsViewerUrl = "https://docs.google.com/gview?embedded=true&url=" + encodedUrl;

            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW);
            intent.setData(android.net.Uri.parse(googleDocsViewerUrl));
            startActivity(intent);

        } catch (android.content.ActivityNotFoundException e) {
            Toast.makeText(getContext(), "No se encontró una aplicación para abrir este enlace.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "ActivityNotFoundException al intentar ver PDF: " + urlString, e);
        } catch (Exception e) {
            Log.e(TAG, "Error general al intentar ver PDF: " + urlString, e);
            Toast.makeText(getContext(), "No se pudo abrir el enlace del documento.", Toast.LENGTH_LONG).show();
        }
    }

    private void descargarFormato(String documentoId, String nombre) {
        if (getContext() == null) return;

        Toast.makeText(getContext(), "Descargando formato: " + nombre, Toast.LENGTH_SHORT).show();

        // Intentar descargar desde Storage (primero .docx, luego .pdf)
        com.google.firebase.storage.FirebaseStorage storage = com.google.firebase.storage.FirebaseStorage.getInstance();

        // Intentar primero con .docx, luego con .pdf
        String pathDocx = "formatos_plantillas/" + documentoId + ".docx";
        String pathPdf = "formatos_plantillas/" + documentoId + ".pdf";

        com.google.firebase.storage.StorageReference storageRefDocx = storage.getReference(pathDocx);
        com.google.firebase.storage.StorageReference storageRefPdf = storage.getReference(pathPdf);

        // Crear archivo temporal para descarga
        final java.io.File localFile = new java.io.File(getContext().getExternalFilesDir(null), nombre.replaceAll("[^a-zA-Z0-9]", "_") + ".docx");
        final java.io.File localFilePdf = new java.io.File(getContext().getExternalFilesDir(null), nombre.replaceAll("[^a-zA-Z0-9]", "_") + ".pdf");

        // Intentar descargar .docx primero
        storageRefDocx.getFile(localFile)
                .addOnSuccessListener(taskSnapshot -> {
                    Toast.makeText(getContext(), "Formato descargado exitosamente", Toast.LENGTH_SHORT).show();
                    // Abrir el archivo descargado
                    android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW);
                    android.net.Uri uri = androidx.core.content.FileProvider.getUriForFile(
                            requireContext(),
                            requireContext().getApplicationContext().getPackageName() + ".provider",
                            localFile
                    );
                    intent.setDataAndType(uri, "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
                    intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    try {
                        startActivity(intent);
                    } catch (android.content.ActivityNotFoundException e) {
                        Toast.makeText(getContext(), "No se encontró una aplicación para abrir el documento Word", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(exception -> {
                    // Si falla .docx, intentar con .pdf
                    Log.d(TAG, "No se encontró .docx, intentando con .pdf");
                    storageRefPdf.getFile(localFilePdf)
                            .addOnSuccessListener(taskSnapshot -> {
                                Toast.makeText(getContext(), "Formato descargado exitosamente", Toast.LENGTH_SHORT).show();
                                android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW);
                                android.net.Uri uri = androidx.core.content.FileProvider.getUriForFile(
                                        requireContext(),
                                        requireContext().getApplicationContext().getPackageName() + ".provider",
                                        localFilePdf
                                );
                                intent.setDataAndType(uri, "application/pdf");
                                intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                try {
                                    startActivity(intent);
                                } catch (android.content.ActivityNotFoundException e) {
                                    Toast.makeText(getContext(), "No se encontró una aplicación para abrir el PDF", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnFailureListener(exception2 -> {
                                Log.e(TAG, "Error descargando formato", exception2);
                                Toast.makeText(getContext(), "No se pudo descargar el formato. Puede que no esté disponible aún.", Toast.LENGTH_LONG).show();
                            });
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

