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

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DocumentosTabFragment extends Fragment {

    private static final String TAG = "DocumentosTabFragment";
    private LinearLayout layoutDocumentos;
    private TextView tvNoDocumentos;

    private FirebaseManager firebaseManager;
    private FileManager fileManager;

    private String pendingUploadCalendarioId = null;
    private String pendingUploadCampoPdf = null;

    private final ActivityResultLauncher<String[]> selectorPDF = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri != null && pendingUploadCalendarioId != null && pendingUploadCampoPdf != null) {
                    try {
                        final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
                        requireContext().getContentResolver().takePersistableUriPermission(uri, takeFlags);
                        guardarUriPdfLocalmente(pendingUploadCalendarioId, pendingUploadCampoPdf, uri.toString());
                    } catch (SecurityException e) {
                        Log.e(TAG, "Error al tomar permisos", e);
                        Toast.makeText(getContext(), "Error de permisos al seleccionar el archivo.", Toast.LENGTH_LONG).show();
                    }
                }
                pendingUploadCalendarioId = null;
                pendingUploadCampoPdf = null;
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_documentos_tab, container, false);

        layoutDocumentos = view.findViewById(R.id.layoutDocumentosCalendarios);
        tvNoDocumentos = view.findViewById(R.id.tvNoDocumentosCalendarios);

        firebaseManager = new FirebaseManager();
        fileManager = new FileManager(requireContext());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Esta llamada es la correcta y la que debe permanecer.
        // Se ejecuta cada vez que el fragmento se vuelve visible, refrescando la UI.
        cargarCalendariosDocumentos();
    }

    private void cargarCalendariosDocumentos() {
        layoutDocumentos.removeAllViews();
        tvNoDocumentos.setVisibility(View.GONE);

        firebaseManager.cargarCalendarios(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                if(task.getResult().isEmpty()){
                    tvNoDocumentos.setVisibility(View.VISIBLE);
                } else {
                    tvNoDocumentos.setVisibility(View.GONE);
                    for (QueryDocumentSnapshot calendarioDoc : task.getResult()) {
                        crearCardDocumentos(calendarioDoc);
                    }
                }
            } else {
                tvNoDocumentos.setVisibility(View.VISIBLE);
                Log.e(TAG, "Error cargando calendarios para documentos", task.getException());
            }
        });
    }

    private void crearCardDocumentos(DocumentSnapshot calendarioFirebase) {
        View cardView = LayoutInflater.from(getContext()).inflate(R.layout.card_documentos_calendario, layoutDocumentos, false);
        TextView tvNombre = cardView.findViewById(R.id.tvNombreAlumnoDoc);
        TextView tvNumControl = cardView.findViewById(R.id.tvNumControlDoc);
        TextView[] labels = { cardView.findViewById(R.id.tvLabelDoc1), cardView.findViewById(R.id.tvLabelDoc2), cardView.findViewById(R.id.tvLabelDoc3) };
        Button[] btnsSubir = { cardView.findViewById(R.id.btnSubirDoc1), cardView.findViewById(R.id.btnSubirDoc2), cardView.findViewById(R.id.btnSubirDoc3) };
        ImageView[] btnsVer = { cardView.findViewById(R.id.btnVerDoc1), cardView.findViewById(R.id.btnVerDoc2), cardView.findViewById(R.id.btnVerDoc3) };

        String calendarioId = calendarioFirebase.getId();
        String alumnoId = calendarioFirebase.getString("alumnoId");

        if (alumnoId != null) {
            firebaseManager.buscarAlumnoPorId(alumnoId, task -> {
                if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                    DocumentSnapshot alumno = task.getResult();
                    tvNombre.setText(alumno.getString("nombre"));
                    tvNumControl.setText("No. Control: " + alumno.getString("numControl"));
                } else {
                    tvNombre.setText("Alumno no encontrado");
                }
            });
        }

        JSONObject calendarioLocal = fileManager.buscarCalendarioPorId(calendarioId);

        String[] camposPdfUri = {"pdfUriPrimeraEntrega", "pdfUriSegundaEntrega", "pdfUriResultado"};
        String[] camposFecha = {"fechaPrimeraEntrega", "fechaSegundaEntrega", "fechaResultado"};
        String[] camposLabel = {"labelPrimeraEntrega", "labelSegundaEntrega", "labelResultado"};
        String[] defaultLabels = {"1ª Entrega", "2ª Entrega", "Resultado Final"};

        for (int i = 0; i < 3; i++) {
            final String campoPdfKey = camposPdfUri[i];
            String label = calendarioFirebase.getString(camposLabel[i]) != null ? calendarioFirebase.getString(camposLabel[i]) : defaultLabels[i];
            String fechaLimiteString = calendarioFirebase.getString(camposFecha[i]);
            String pdfUriString = (calendarioLocal != null) ? calendarioLocal.optString(campoPdfKey, "") : "";

            labels[i].setText(label);
            boolean isFechaVencida = haVencidoFecha(fechaLimiteString);
            boolean isDocPrevioSubido = (i == 0) || (calendarioLocal != null && !calendarioLocal.optString(camposPdfUri[i - 1], "").isEmpty());

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

            if (pdfUriString.isEmpty()) {
                btnsVer[i].setVisibility(View.GONE);
            } else {
                btnsVer[i].setVisibility(View.VISIBLE);
                btnsVer[i].setOnClickListener(v -> verPDF(pdfUriString));
                if (btnsSubir[i].isEnabled()) btnsSubir[i].setText("Reemplazar");
            }
        }
        layoutDocumentos.addView(cardView);
    }

    private void guardarUriPdfLocalmente(String calendarioId, String campoPdfKey, String uriString) {
        JSONObject calendario = fileManager.buscarCalendarioPorId(calendarioId);
        if (calendario == null) {
            calendario = new JSONObject();
            try {
                calendario.put("id", calendarioId);
            } catch (JSONException e) {
                e.printStackTrace();
                return;
            }
        }
        try {
            calendario.put(campoPdfKey, uriString);
            if (fileManager.guardarCalendario(calendario)) {
                Toast.makeText(getContext(), "PDF guardado exitosamente.", Toast.LENGTH_SHORT).show();

                // CORRECCIÓN: Se elimina la siguiente línea. onResume() se encargará de esto.
                // cargarCalendariosDocumentos();

            } else {
                Toast.makeText(getContext(), "Error al guardar la referencia del PDF.", Toast.LENGTH_SHORT).show();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private boolean haVencidoFecha(String fechaLimiteString) {
        if (fechaLimiteString == null || fechaLimiteString.isEmpty()) {
            return false;
        }
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

    private void verPDF(String uriString) {
        if (uriString == null || uriString.isEmpty()) {
            Toast.makeText(getContext(), "No hay documento para visualizar.", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Uri uri = Uri.parse(uriString);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getContext(), "No se encontró una aplicación para abrir PDFs.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(TAG, "Error al intentar ver PDF: " + uriString, e);
            Toast.makeText(getContext(), "No se pudo abrir el archivo. Puede que haya sido movido o eliminado.", Toast.LENGTH_LONG).show();
        }
    }
}