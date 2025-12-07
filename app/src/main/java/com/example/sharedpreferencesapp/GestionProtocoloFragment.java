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
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class GestionProtocoloFragment extends Fragment implements ProtocolAdapter.ProtocolActionListener {

    private static final String TAG = "GestionProtocoloFrag";

    private RecyclerView recyclerView;
    private ProtocolAdapter adapter;
    private List<DocumentSnapshot> allProtocolsList = new ArrayList<>();
    private List<DocumentSnapshot> filteredProtocolsList = new ArrayList<>();
    private ProgressBar progressBar;
    private TextView tvNoProtocolos;
    private TextInputEditText etSearch;
    private FirebaseFirestore db;
    private FirebaseManager firebaseManager;
    private String currentUserId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_protocolos_management, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Inicializar vistas
        recyclerView = view.findViewById(R.id.recycler_protocolos);
        progressBar = view.findViewById(R.id.progress_bar);
        tvNoProtocolos = view.findViewById(R.id.tv_no_protocolos);
        etSearch = view.findViewById(R.id.etSearch);

        ImageView btnBack = view.findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                if (getActivity() != null) {
                    getActivity().onBackPressed();
                }
            });
        }

        // Configurar RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ProtocolAdapter(getContext(), filteredProtocolsList, this);
        recyclerView.setAdapter(adapter);

        // Inicializar Firebase
        db = FirebaseFirestore.getInstance();
        firebaseManager = new FirebaseManager();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            currentUserId = currentUser.getUid();
            setupSearch();
            loadProtocols();
        } else {
            tvNoProtocolos.setVisibility(View.VISIBLE);
            tvNoProtocolos.setText("Error: No hay sesión activa");
        }
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterProtocols();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterProtocols() {
        filteredProtocolsList.clear();

        if (etSearch == null) {
            // Si no hay búsqueda, mostrar todos
            filteredProtocolsList.addAll(allProtocolsList);
            updateAdapterAndViews();
            return;
        }

        String searchQuery = etSearch.getText().toString().toLowerCase().trim();

        // Si la búsqueda está vacía, mostrar todos
        if (searchQuery.isEmpty()) {
            filteredProtocolsList.addAll(allProtocolsList);
        } else {
            // Filtrar por búsqueda
            for (DocumentSnapshot protocol : allProtocolsList) {
                String nombreEstudiante = protocol.getString("nombreEstudiante");
                if (nombreEstudiante == null || nombreEstudiante.isEmpty()) {
                    nombreEstudiante = protocol.getString("nombreAlumno");
                }
                if (nombreEstudiante == null) nombreEstudiante = "";

                String numeroControl = protocol.getString("numeroControl");
                if (numeroControl == null || numeroControl.isEmpty()) {
                    numeroControl = protocol.getString("numControl");
                }
                if (numeroControl == null) numeroControl = "";

                String nombreProyecto = protocol.getString("nombreProyecto");
                if (nombreProyecto == null) nombreProyecto = "";

                String nombreEmpresa = protocol.getString("nombreEmpresa");
                if (nombreEmpresa == null) nombreEmpresa = "";

                if (nombreEstudiante.toLowerCase().contains(searchQuery) ||
                        numeroControl.toLowerCase().contains(searchQuery) ||
                        nombreProyecto.toLowerCase().contains(searchQuery) ||
                        nombreEmpresa.toLowerCase().contains(searchQuery)) {
                    filteredProtocolsList.add(protocol);
                }
            }
        }

        updateAdapterAndViews();
    }

    private void updateAdapterAndViews() {
        Log.d(TAG, "updateAdapterAndViews - filteredProtocolsList size: " + filteredProtocolsList.size());
        Log.d(TAG, "Adapter es null: " + (adapter == null));
        Log.d(TAG, "RecyclerView es null: " + (recyclerView == null));

        if (adapter != null) {
            Log.d(TAG, "Actualizando adapter con " + filteredProtocolsList.size() + " protocolos");
            adapter.updateList(filteredProtocolsList);

            if (recyclerView != null) {
                Log.d(TAG, "RecyclerView visible: " + (recyclerView.getVisibility() == View.VISIBLE));
                recyclerView.post(() -> {
                    recyclerView.invalidate();
                    recyclerView.requestLayout();
                    Log.d(TAG, "RecyclerView invalidado y layout solicitado");
                });
            }
        } else {
            Log.e(TAG, "Adapter es null, no se puede actualizar");
        }

        // Mostrar mensaje si no hay protocolos
        if (filteredProtocolsList.isEmpty()) {
            Log.d(TAG, "Lista filtrada vacía, mostrando mensaje");
            if (tvNoProtocolos != null) {
                tvNoProtocolos.setVisibility(View.VISIBLE);
                String searchQuery = (etSearch != null) ? etSearch.getText().toString().trim() : "";
                if (searchQuery.isEmpty()) {
                    tvNoProtocolos.setText("No hay protocolos");
                } else {
                    tvNoProtocolos.setText("No se encontraron protocolos");
                }
            }
            if (recyclerView != null) {
                recyclerView.setVisibility(View.GONE);
            }
        } else {
            Log.d(TAG, "Lista filtrada tiene " + filteredProtocolsList.size() + " protocolos, mostrando RecyclerView");
            if (tvNoProtocolos != null) {
                tvNoProtocolos.setVisibility(View.GONE);
            }
            if (recyclerView != null) {
                recyclerView.setVisibility(View.VISIBLE);
                Log.d(TAG, "RecyclerView configurado como VISIBLE");
            }
        }
    }

    private void loadProtocols() {
        progressBar.setVisibility(View.VISIBLE);
        tvNoProtocolos.setVisibility(View.GONE);

        Log.d(TAG, "Cargando protocolos para supervisorId: " + currentUserId);

        // Intentar primero con orderBy, si falla intentar sin orderBy
        db.collection("protocolos")
                .whereEqualTo("supervisorId", currentUserId)
                .orderBy("fechaActualizacion", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        // Si falla con orderBy, intentar sin orderBy (puede ser problema de índice)
                        Log.w(TAG, "Error con orderBy, intentando sin orderBy", task.getException());
                        db.collection("protocolos")
                                .whereEqualTo("supervisorId", currentUserId)
                                .get()
                                .addOnCompleteListener(task2 -> {
                                    progressBar.setVisibility(View.GONE);
                                    handleProtocolsResult(task2);
                                });
                    } else {
                        progressBar.setVisibility(View.GONE);
                        handleProtocolsResult(task);
                    }
                });
    }

    private void handleProtocolsResult(com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> task) {
        if (task.isSuccessful()) {
            allProtocolsList.clear();
            int count = 0;

            for (QueryDocumentSnapshot document : task.getResult()) {
                allProtocolsList.add(document);
                count++;
                Log.d(TAG, "Protocolo encontrado - ID: " + document.getId() + ", estudianteId: " + document.getString("estudianteId"));
            }

            Log.d(TAG, "Total protocolos cargados: " + count);

            // Actualizar la lista filtrada y el adapter
            filterProtocols();

        } else {
            Log.e(TAG, "Error obteniendo protocolos", task.getException());
            Toast.makeText(getContext(), "Error al cargar protocolos: " + (task.getException() != null ? task.getException().getMessage() : "Desconocido"), Toast.LENGTH_SHORT).show();
            tvNoProtocolos.setVisibility(View.VISIBLE);
            tvNoProtocolos.setText("Error al cargar protocolos");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (currentUserId != null) {
            loadProtocols();
        }
    }

    @Override
    public void onViewPdfClicked(DocumentSnapshot protocol) {
        String estudianteId = protocol.getString("estudianteId");
        if (estudianteId == null || estudianteId.isEmpty()) {
            Toast.makeText(getContext(), "Error: ID de estudiante no encontrado en el protocolo.", Toast.LENGTH_SHORT).show();
            return;
        }

        firebaseManager.buscarPerfilDeEstudiantePorId(estudianteId, task -> {
            if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                generarYVisualizarPDF(new JSONObject(protocol.getData()), new JSONObject(task.getResult().getData()));
            } else {
                Toast.makeText(getContext(), "No se pudo encontrar el perfil del alumno para generar el PDF.", Toast.LENGTH_LONG).show();
                Log.e(TAG, "Perfil de estudiante no encontrado para ID: " + estudianteId, task.getException());
            }
        });
    }

    @Override
    public void onProtocolClicked(DocumentSnapshot protocol) {
        String nombreProyecto = protocol.getString("nombreProyecto");
        if (nombreProyecto == null || nombreProyecto.isEmpty()) {
            nombreProyecto = "Protocolo";
        }
        Toast.makeText(getContext(), "Protocolo: " + nombreProyecto, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onMenuClicked(DocumentSnapshot protocol, View view) {
        String[] options = {"Ver detalles", "Ver PDF"};

        new AlertDialog.Builder(getContext())
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            onProtocolClicked(protocol);
                            break;
                        case 1:
                            onViewPdfClicked(protocol);
                            break;
                    }
                })
                .show();
    }

    private void generarYVisualizarPDF(JSONObject protocolo, JSONObject alumno) {
        if (getContext() == null || getActivity() == null) return;
        Toast.makeText(getContext(), "📄 Generando PDF...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            Uri pdfUri = null;
            File pdfFile = null;
            boolean exito = false;

            try {
                String nombreProyecto = protocolo.optString("nombreProyecto", "Protocolo");
                String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                String nombreArchivo = "Protocolo_" + nombreProyecto.replaceAll("[^a-zA-Z0-9]", "_") + "_" + timestamp + ".pdf";

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
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
                        resolver.delete(pdfUri, null, null);
                    }

                } else {
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

                if (!exito) {
                    throw new Exception("El generador de PDF reportó un error.");
                }

                final Uri uriParaVisualizar;
                if (pdfFile != null) {
                    uriParaVisualizar = FileProvider.getUriForFile(
                            getContext(),
                            getContext().getApplicationContext().getPackageName() + ".provider",
                            pdfFile
                    );
                } else {
                    uriParaVisualizar = pdfUri;
                }

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
}
