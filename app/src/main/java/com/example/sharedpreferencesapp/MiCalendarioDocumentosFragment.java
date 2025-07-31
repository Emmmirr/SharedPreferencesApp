package com.example.sharedpreferencesapp;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
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

import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MiCalendarioDocumentosFragment extends Fragment {

    private static final String TAG = "MiCalendarioDocsFrag";
    private LinearLayout layoutDocumentos;
    private TextView tvNoDocumentos;
    private FirebaseManager firebaseManager;
    private ProfileManager profileManager;

    private String currentUserId;
    private String supervisorId; // Guardaremos el ID del supervisor aquí

    private String pendingUploadCalendarioId = null;
    private String pendingUploadCampoPdf = null;

    private final ActivityResultLauncher<String[]> selectorPDF = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri != null && pendingUploadCalendarioId != null && pendingUploadCampoPdf != null) {
                    try {
                        final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
                        requireContext().getContentResolver().takePersistableUriPermission(uri, takeFlags);
                        Toast.makeText(getContext(), "Subiendo archivo...", Toast.LENGTH_LONG).show();

                        // Necesitamos el ID del supervisor para la ruta de subida
                        if (supervisorId == null) {
                            Toast.makeText(getContext(), "Error: No se encontró el supervisor.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        firebaseManager.subirPdfStorage(supervisorId, pendingUploadCalendarioId, pendingUploadCampoPdf, uri,
                                downloadUrl -> guardarUrlEnFirestore(pendingUploadCalendarioId, pendingUploadCampoPdf, downloadUrl),
                                e -> {
                                    Toast.makeText(getContext(), "Error al subir el archivo: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    Log.e(TAG, "Error al subir a Storage", e);
                                    resetPendingUpload();
                                }
                        );
                    } catch (SecurityException e) {
                        Log.e(TAG, "Error de permisos al seleccionar el archivo.", e);
                        Toast.makeText(getContext(), "Error de permisos. No se pudo acceder al archivo.", Toast.LENGTH_LONG).show();
                        resetPendingUpload();
                    }
                } else {
                    resetPendingUpload();
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mi_calendario_documentos, container, false);
        layoutDocumentos = view.findViewById(R.id.layoutDocumentosCalendarios);
        tvNoDocumentos = view.findViewById(R.id.tvNoDocumentosCalendarios);
        firebaseManager = new FirebaseManager();
        profileManager = new ProfileManager(requireContext());
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            this.currentUserId = user.getUid();
            cargarCalendarioAsignado();
        } else {
            tvNoDocumentos.setText("Error de sesión. Por favor, inicie sesión de nuevo.");
            tvNoDocumentos.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (currentUserId != null) {
            cargarCalendarioAsignado();
        }
    }

    private void cargarCalendarioAsignado() {
        if (currentUserId == null || getContext() == null) return;

        layoutDocumentos.removeAllViews();
        tvNoDocumentos.setVisibility(View.VISIBLE);
        tvNoDocumentos.setText("Buscando calendario asignado...");

        profileManager.obtenerPerfilActual(
                profile -> {
                    if (profile == null) {
                        handleError("No se pudo cargar tu perfil.");
                        return;
                    }

                    this.supervisorId = profile.getSupervisorId();
                    if (supervisorId == null || supervisorId.isEmpty()) {
                        tvNoDocumentos.setText("Aún no tienes un supervisor asignado.");
                        return;
                    }

                    String calendarioId = "calendario_" + currentUserId;
                    firebaseManager.buscarCalendarioPorId(supervisorId, calendarioId, task -> {
                        if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                            tvNoDocumentos.setVisibility(View.GONE);
                            crearCardDocumentos(task.getResult(), profile);
                        } else {
                            tvNoDocumentos.setText("Tu supervisor aún no te ha asignado un calendario de entregas.");
                        }
                    });
                },
                error -> handleError("Error al obtener perfil: " + error.getMessage())
        );
    }

    // El método ahora recibe el perfil del estudiante para mostrar su nombre
    private void crearCardDocumentos(DocumentSnapshot calendarioFirebase, UserProfile studentProfile) {
        if (getContext() == null) return;
        View cardView = LayoutInflater.from(getContext()).inflate(R.layout.card_documentos_calendario, layoutDocumentos, false);

        TextView tvNombre = cardView.findViewById(R.id.tvNombreAlumnoDoc);
        TextView tvNumControl = cardView.findViewById(R.id.tvNumControlDoc);

        // Usamos los datos del perfil del propio estudiante
        String nombre = studentProfile.getFullName().isEmpty() ? studentProfile.getDisplayName() : studentProfile.getFullName();
        tvNombre.setText(nombre);
        tvNumControl.setText("No. Control: " + studentProfile.getControlNumber());

        TextView[] labels = {cardView.findViewById(R.id.tvLabelDoc1), cardView.findViewById(R.id.tvLabelDoc2), cardView.findViewById(R.id.tvLabelDoc3)};
        Button[] btnsSubir = {cardView.findViewById(R.id.btnSubirDoc1), cardView.findViewById(R.id.btnSubirDoc2), cardView.findViewById(R.id.btnSubirDoc3)};
        ImageView[] btnsVer = {cardView.findViewById(R.id.btnVerDoc1), cardView.findViewById(R.id.btnVerDoc2), cardView.findViewById(R.id.btnVerDoc3)};

        String calendarioId = calendarioFirebase.getId();

        String[] camposPdfUri = {"pdfUriPrimeraEntrega", "pdfUriSegundaEntrega", "pdfUriResultado"};
        String[] camposFecha = {"fechaPrimeraEntrega", "fechaSegundaEntrega", "fechaResultado"};
        String[] camposLabel = {"labelPrimeraEntrega", "labelSegundaEntrega", "labelResultado"};
        String[] defaultLabels = {"1ª Entrega", "2ª Entrega", "Resultado Final"};

        for (int i = 0; i < 3; i++) {
            final String campoPdfKey = camposPdfUri[i];
            String label = calendarioFirebase.getString(camposLabel[i]);
            String fechaLimiteString = calendarioFirebase.getString(camposFecha[i]);
            String pdfUrlString = calendarioFirebase.getString(campoPdfKey);

            labels[i].setText(label != null && !label.isEmpty() ? label : defaultLabels[i]);

            boolean isFechaVencida = haVencidoFecha(fechaLimiteString);
            boolean isDocPrevioSubido = (i == 0) || (calendarioFirebase.getString(camposPdfUri[i - 1]) != null && !calendarioFirebase.getString(camposPdfUri[i - 1]).isEmpty());

            if (isDocPrevioSubido && !isFechaVencida) {
                btnsSubir[i].setEnabled(true);
                btnsSubir[i].setText("Subir PDF");
                btnsSubir[i].setOnClickListener(v -> iniciarSubidaPDF(calendarioId, campoPdfKey));
            } else {
                btnsSubir[i].setEnabled(false);
                if (isFechaVencida) btnsSubir[i].setText("Vencido");
                else if (!isDocPrevioSubido) btnsSubir[i].setText("Bloqueado");
                else btnsSubir[i].setText("Subir PDF");
            }

            if (pdfUrlString == null || pdfUrlString.isEmpty()) {
                btnsVer[i].setVisibility(View.GONE);
            } else {
                btnsVer[i].setVisibility(View.VISIBLE);
                btnsVer[i].setOnClickListener(v -> verPDF(pdfUrlString));
                if (btnsSubir[i].isEnabled()) btnsSubir[i].setText("Reemplazar");
            }
        }
        layoutDocumentos.addView(cardView);
    }

    private void guardarUrlEnFirestore(String calendarioId, String campoPdfKey, String downloadUrl) {
        if (supervisorId == null || getContext() == null) {
            Toast.makeText(getContext(), "Error de sesión o supervisor no encontrado.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> update = new HashMap<>();
        update.put(campoPdfKey, downloadUrl);

        // La actualización se hace en el documento del supervisor
        firebaseManager.guardarOActualizarCalendario(supervisorId, calendarioId, update,
                () -> {
                    Toast.makeText(getContext(), "Documento actualizado exitosamente.", Toast.LENGTH_SHORT).show();

                    AlarmScheduler scheduler = new AlarmScheduler(requireContext());
                    String[] camposPdfUri = {"pdfUriPrimeraEntrega", "pdfUriSegundaEntrega", "pdfUriResultado"};
                    for (int i = 0; i < camposPdfUri.length; i++) {
                        if (camposPdfUri[i].equals(campoPdfKey)) {
                            scheduler.stopShortIntervalCycle(calendarioId, i);
                            break;
                        }
                    }
                    cargarCalendarioAsignado(); // Recargar la vista
                },
                e -> {
                    Toast.makeText(getContext(), "Error al guardar en la base de datos.", Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Error al actualizar Firestore", e);
                }
        );
    }

    private boolean haVencidoFecha(String fechaLimiteString) {
        if (fechaLimiteString == null || fechaLimiteString.isEmpty()) return false;
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        try {
            Date fechaLimite = sdf.parse(fechaLimiteString);
            Calendar hoy = Calendar.getInstance();
            hoy.set(Calendar.HOUR_OF_DAY, 0);
            hoy.set(Calendar.MINUTE, 0);
            hoy.set(Calendar.SECOND, 0);
            hoy.set(Calendar.MILLISECOND, 0);
            return fechaLimite.before(hoy.getTime());
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void iniciarSubidaPDF(String calendarioId, String campoPdfKey) {
        this.pendingUploadCalendarioId = calendarioId;
        this.pendingUploadCampoPdf = campoPdfKey;
        selectorPDF.launch(new String[]{"application/pdf"});
    }

// En MiCalendarioDocumentosFragment.java

// En DocumentosTabFragment.java Y MiCalendarioDocumentosFragment.java
// Asegúrate de añadir esta importación al principio del archivo:
// import java.net.URLEncoder;

    private void verPDF(String urlString) {
        if (urlString == null || urlString.isEmpty()) {
            Toast.makeText(getContext(), "No hay documento para visualizar.", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "URL original de Firebase: " + urlString);

        try {
            // --- INICIO DE LA SOLUCIÓN DEFINITIVA ---
            // 1. Codificamos la URL de Firebase para que sea segura de usar como parámetro en otra URL.
            String encodedUrl = URLEncoder.encode(urlString, "UTF-8");

            // 2. Construimos la URL del visor de Google Docs, pasándole nuestra URL codificada.
            String googleDocsViewerUrl = "https://docs.google.com/gview?embedded=true&url=" + encodedUrl;

            Log.d(TAG, "Abriendo con el visor de Google: " + googleDocsViewerUrl);

            // 3. Creamos un Intent para ver la URL del visor.
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(googleDocsViewerUrl));
            startActivity(intent);
            // --- FIN DE LA SOLUCIÓN DEFINITIVA ---

        } catch (ActivityNotFoundException e) {
            Toast.makeText(getContext(), "No se encontró una aplicación para abrir este enlace (navegador web).", Toast.LENGTH_LONG).show();
            Log.e(TAG, "ActivityNotFoundException al intentar ver PDF: " + urlString, e);
        } catch (Exception e) {
            Log.e(TAG, "Error general al intentar ver PDF: " + urlString, e);
            Toast.makeText(getContext(), "No se pudo abrir el enlace del documento.", Toast.LENGTH_LONG).show();
        }
    }

    private void resetPendingUpload() {
        pendingUploadCalendarioId = null;
        pendingUploadCampoPdf = null;
    }

    private void handleError(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            tvNoDocumentos.setText(message);
            tvNoDocumentos.setVisibility(View.VISIBLE);
        }
        Log.e(TAG, message);
    }
}