package com.example.sharedpreferencesapp;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DocumentosTabFragment extends Fragment {

    private static final String TAG = "DocumentosTabFragment";
    private LinearLayout layoutDocumentos;
    private TextView tvNoDocumentos;
    private FirebaseManager firebaseManager;
    private AlarmScheduler alarmScheduler; // <-- AÑADIDO
    private String currentUserId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_documentos_tab, container, false);
        layoutDocumentos = view.findViewById(R.id.layoutDocumentosCalendarios);
        tvNoDocumentos = view.findViewById(R.id.tvNoDocumentosCalendarios);
        firebaseManager = new FirebaseManager();
        alarmScheduler = new AlarmScheduler(requireContext()); // <-- AÑADIDO E INICIALIZADO
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            this.currentUserId = user.getUid();
            cargarCalendariosDocumentos();
        } else {
            tvNoDocumentos.setText("Error de sesión. Por favor, inicie sesión de nuevo.");
            tvNoDocumentos.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (currentUserId !=  null) {
            cargarCalendariosDocumentos();
        }
    }

    private void cargarCalendariosDocumentos() {
        if (currentUserId == null) {
            Log.w(TAG, "Intento de cargar calendarios sin un UID de usuario.");
            return;
        }
        layoutDocumentos.removeAllViews();
        tvNoDocumentos.setVisibility(View.VISIBLE);
        tvNoDocumentos.setText("No hay calendarios de alumnos asignados.");
        firebaseManager.cargarCalendarios(currentUserId, task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                if (task.getResult().isEmpty()) {
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
        if (getContext() == null) return;
        View cardView = LayoutInflater.from(getContext()).inflate(R.layout.card_documentos_calendario, layoutDocumentos, false);

        TextView tvNombre = cardView.findViewById(R.id.tvNombreAlumnoDoc);
        TextView tvNumControl = cardView.findViewById(R.id.tvNumControlDoc);

        // --- INICIO DE SECCIÓN MODIFICADA ---
        TextView[] labels = {cardView.findViewById(R.id.tvLabelDoc1), cardView.findViewById(R.id.tvLabelDoc2), cardView.findViewById(R.id.tvLabelDoc3)};
        Button[] btnsSubir = {cardView.findViewById(R.id.btnSubirDoc1), cardView.findViewById(R.id.btnSubirDoc2), cardView.findViewById(R.id.btnSubirDoc3)};
        ImageView[] btnsVer = {cardView.findViewById(R.id.btnVerDoc1), cardView.findViewById(R.id.btnVerDoc2), cardView.findViewById(R.id.btnVerDoc3)};
        ImageView[] btnsRevisar = {cardView.findViewById(R.id.btnRevisarDoc1), cardView.findViewById(R.id.btnRevisarDoc2), cardView.findViewById(R.id.btnRevisarDoc3)}; // <-- NUEVO

        String calendarioId = calendarioFirebase.getId();
        String alumnoId = calendarioFirebase.getString("alumnoId");

        if (alumnoId != null) {
            firebaseManager.buscarPerfilDeEstudiantePorId(alumnoId, task -> {
                if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                    UserProfile alumnoProfile = UserProfile.fromMap(task.getResult().getData());
                    String nombre = alumnoProfile.getFullName().isEmpty() ? alumnoProfile.getDisplayName() : alumnoProfile.getFullName();
                    tvNombre.setText(nombre);
                    tvNumControl.setText("No. Control: " + alumnoProfile.getControlNumber());
                } else {
                    tvNombre.setText("Perfil de Alumno no Encontrado");
                    tvNumControl.setText("ID: " + alumnoId);
                }
            });
        }

        String[] camposPdfUri = {"pdfUriPrimeraEntrega", "pdfUriSegundaEntrega", "pdfUriResultado"};
        String[] camposLabel = {"labelPrimeraEntrega", "labelSegundaEntrega", "labelResultado"};
        String[] defaultLabels = {"1ª Entrega", "2ª Entrega", "Resultado Final"};
        // --- NUEVO: Campos para el estado de aprobación y nueva fecha ---
        String[] camposEstado = {"estadoPrimeraEntrega", "estadoSegundaEntrega", "estadoResultado"};

        for (int i = 0; i < 3; i++) {
            final int index = i; // Usar en lambdas
            String label = calendarioFirebase.getString(camposLabel[i]);
            String pdfUrlString = calendarioFirebase.getString(camposPdfUri[i]);
            String estado = calendarioFirebase.getString(camposEstado[i]); // "Aprobado", "Rechazado", o null

            labels[i].setText(label != null && !label.isEmpty() ? label : defaultLabels[i]);

            // Lógica de UI para el maestro (solo lectura)
            btnsSubir[i].setVisibility(View.GONE);

            if (pdfUrlString == null || pdfUrlString.isEmpty()) {
                btnsVer[i].setVisibility(View.GONE);
                btnsRevisar[i].setVisibility(View.GONE); // No se puede revisar si no hay nada
                labels[i].setText(labels[i].getText() + " (Pendiente de entrega)");
            } else {
                btnsVer[i].setVisibility(View.VISIBLE);
                btnsVer[i].setOnClickListener(v -> verPDF(pdfUrlString));

                // Configurar el botón de revisión
                btnsRevisar[i].setVisibility(View.VISIBLE);
                if ("Aprobado".equals(estado)) {
                    btnsRevisar[i].setImageResource(android.R.drawable.ic_menu_myplaces); // Ícono de check/aprobado
                    btnsRevisar[i].setColorFilter(getResources().getColor(android.R.color.holo_green_dark));
                    labels[i].setText(labels[i].getText() + " (Aprobado)");
                    btnsRevisar[i].setOnClickListener(null); // Deshabilitar click si ya está aprobado
                } else {
                    btnsRevisar[i].setImageResource(android.R.drawable.ic_menu_manage); // Ícono de revisión
                    btnsRevisar[i].setColorFilter(getResources().getColor(R.color.primary)); // Color por defecto

                    if ("Rechazado".equals(estado)) {
                        labels[i].setText(labels[i].getText() + " (Rechazado, esperando re-entrega)");
                    } else {
                        labels[i].setText(labels[i].getText() + " (Entregado, pendiente de revisión)");
                    }
                    btnsRevisar[i].setOnClickListener(v -> mostrarDialogoRevision(calendarioId, alumnoId, index));
                }
            }
        }
        layoutDocumentos.addView(cardView);
    }
    // --- FIN DE SECCIÓN MODIFICADA ---

    // --- INICIO DE NUEVOS MÉTODOS ---
    private void mostrarDialogoRevision(String calendarioId, String alumnoId, int entregaIndex) {
        final CharSequence[] options = {"Aprobar Entrega", "Rechazar y Asignar Nueva Fecha", "Cancelar"};

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Revisar Documento");
        builder.setItems(options, (dialog, item) -> {
            if (options[item].equals("Aprobar Entrega")) {
                actualizarEstadoEntrega(calendarioId, entregaIndex, "Aprobado", null);
            } else if (options[item].equals("Rechazar y Asignar Nueva Fecha")) {
                mostrarSelectorDeFecha(calendarioId, alumnoId, entregaIndex);
            } else if (options[item].equals("Cancelar")) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    private void mostrarSelectorDeFecha(String calendarioId, String alumnoId, int entregaIndex) {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(),
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    String nuevaFecha = sdf.format(calendar.getTime());

                    // Guardamos la nueva fecha y reiniciamos el estado
                    actualizarEstadoEntrega(calendarioId, entregaIndex, "Rechazado", nuevaFecha);

                    // Reprogramamos las alarmas para el alumno
                    reprogramarAlarmas(calendarioId, alumnoId, entregaIndex, nuevaFecha);

                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis()); // Solo fechas futuras
        datePickerDialog.show();
    }

    private void actualizarEstadoEntrega(String calendarioId, int entregaIndex, String estado, @Nullable String nuevaFecha) {
        String[] camposFecha = {"fechaPrimeraEntrega", "fechaSegundaEntrega", "fechaResultado"};
        String[] camposEstado = {"estadoPrimeraEntrega", "estadoSegundaEntrega", "estadoResultado"};
        String[] camposPdfUri = {"pdfUriPrimeraEntrega", "pdfUriSegundaEntrega", "pdfUriResultado"};

        Map<String, Object> updates = new HashMap<>();
        updates.put(camposEstado[entregaIndex], estado);

        if (nuevaFecha != null) { // Esto significa que fue rechazado
            updates.put(camposFecha[entregaIndex], nuevaFecha);
            // Limpiamos la URL del PDF para que el alumno pueda subir uno nuevo
            updates.put(camposPdfUri[entregaIndex], "");
        }

        firebaseManager.guardarOActualizarCalendario(currentUserId, calendarioId, updates,
                () -> {
                    Toast.makeText(getContext(), "Estado de entrega actualizado.", Toast.LENGTH_SHORT).show();
                    cargarCalendariosDocumentos(); // Recargar la vista para reflejar los cambios
                },
                e -> Toast.makeText(getContext(), "Error al actualizar: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void reprogramarAlarmas(String calendarioId, String alumnoId, int entregaIndex, String nuevaFecha) {
        // Obtenemos la etiqueta de la entrega para que la notificación sea correcta
        firebaseManager.buscarCalendarioPorId(currentUserId, calendarioId, task -> {
            if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                String[] camposLabel = {"labelPrimeraEntrega", "labelSegundaEntrega", "labelResultado"};
                String[] defaultLabels = {"1ª Entrega", "2ª Entrega", "Resultado Final"};
                String label = task.getResult().getString(camposLabel[entregaIndex]);
                if (label == null || label.isEmpty()) {
                    label = defaultLabels[entregaIndex];
                }

                String[] camposPdf = {"pdfUriPrimeraEntrega", "pdfUriSegundaEntrega", "pdfUriResultado"};

                // Cancelamos las alarmas viejas y programamos las nuevas
                alarmScheduler.cancelAlarmsForDate(calendarioId, entregaIndex);
                alarmScheduler.scheduleAlarmsForDate(alumnoId, calendarioId, nuevaFecha, label, camposPdf[entregaIndex], entregaIndex);

                Toast.makeText(getContext(), "Nuevos recordatorios programados para el alumno.", Toast.LENGTH_SHORT).show();
            }
        });
    }
    // --- FIN DE NUEVOS MÉTODOS ---

// En DocumentosTabFragment.java

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
}