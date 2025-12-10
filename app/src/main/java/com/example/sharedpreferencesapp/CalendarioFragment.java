package com.example.sharedpreferencesapp;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
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

public class CalendarioFragment extends Fragment {

    private static final String TAG = "CalendarioFragment";

    // Vistas principales
    private LinearLayout layoutDetalleFecha;
    private LinearLayout layoutFechasLista;
    private TextView tvNoCalendario;
    private View scrollViewDetalle;

    // Vistas del detalle superior
    private TextView tvNombreActividad;
    private TextView tvDiaNumero;
    private TextView tvMesAno;
    private TextView tvDiasRestantes;
    private CalendarView calendarView;
    private LinearLayout btnSubirDocumento;
    private TextView tvTextoSubir;
    private ImageView ivIconoSubir;
    private LinearLayout btnVerDocumento;

    // Referencias a las cards de fechas para cambiar el color
    private View[] fechaCardViews = new View[3];

    // Datos del calendario
    private DocumentSnapshot calendarioGlobalDocument; // Fechas globales
    private DocumentSnapshot calendarioEstudianteDocument; // Documentos del estudiante
    private UserProfile studentProfile;
    private String currentUserId;

    // Fecha actualmente seleccionada (0, 1, o 2)
    private int selectedFechaIndex = -1;

    // Campos de fecha
    private String[] camposFecha = {"fechaPrimeraEntrega", "fechaSegundaEntrega", "fechaResultado"};
    private String[] camposLabel = {"labelPrimeraEntrega", "labelSegundaEntrega", "labelResultado"};
    private String[] camposPdfUri = {"pdfUriPrimeraEntrega", "pdfUriSegundaEntrega", "pdfUriResultado"};
    private String[] defaultLabels = {"1ª Entrega", "2ª Entrega", "Resultado Final"};

    // Para la subida de archivos
    private String pendingUploadCampoPdf = null;

    // Colores para las cards de fechas (variaciones del primary - colores pasteles)
    private int[] fechaCardColors = {
            R.color.primary_light_2,
            R.color.primary_light_3,
            R.color.primary_light_4
    };

    private FirebaseManager firebaseManager;
    private ProfileManager profileManager;

    private final ActivityResultLauncher<String[]> selectorPDF = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri != null && pendingUploadCampoPdf != null && selectedFechaIndex >= 0) {
                    try {
                        final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
                        requireContext().getContentResolver().takePersistableUriPermission(uri, takeFlags);
                        Toast.makeText(getContext(), "Subiendo archivo...", Toast.LENGTH_LONG).show();

                        if (currentUserId == null) {
                            Toast.makeText(getContext(), "Error: No se encontró el usuario.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Los documentos se guardan en la colección del estudiante
                        String calendarioId = "calendario_" + currentUserId;
                        firebaseManager.subirPdfStorage(currentUserId, calendarioId, pendingUploadCampoPdf, uri,
                                downloadUrl -> guardarUrlEnFirestore(pendingUploadCampoPdf, downloadUrl),
                                e -> {
                                    Toast.makeText(getContext(), "Error al subir el archivo: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    Log.e(TAG, "Error al subir a Storage", e);
                                    pendingUploadCampoPdf = null;
                                }
                        );
                    } catch (SecurityException e) {
                        Log.e(TAG, "Error de permisos al seleccionar el archivo.", e);
                        Toast.makeText(getContext(), "Error de permisos. No se pudo acceder al archivo.", Toast.LENGTH_LONG).show();
                        pendingUploadCampoPdf = null;
                    }
                } else {
                    pendingUploadCampoPdf = null;
                }
            }
    );

    public CalendarioFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_calendario, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firebaseManager = new FirebaseManager();
        profileManager = new ProfileManager(requireContext());

        initViews(view);
        cargarCalendarioAsignado();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (currentUserId != null) {
            cargarCalendarioAsignado();
        }
    }

    private void initViews(View view) {
        layoutDetalleFecha = view.findViewById(R.id.layoutDetalleFecha);
        layoutFechasLista = view.findViewById(R.id.layoutFechasLista);
        tvNoCalendario = view.findViewById(R.id.tvNoCalendario);
        // Buscar el ScrollView que contiene layoutDetalleFecha
        scrollViewDetalle = layoutDetalleFecha != null ? (View) layoutDetalleFecha.getParent() : null;

        tvNombreActividad = view.findViewById(R.id.tvNombreActividad);
        tvDiaNumero = view.findViewById(R.id.tvDiaNumero);
        tvMesAno = view.findViewById(R.id.tvMesAno);
        tvDiasRestantes = view.findViewById(R.id.tvDiasRestantes);
        calendarView = view.findViewById(R.id.calendarView);
        btnSubirDocumento = view.findViewById(R.id.btnSubirDocumento);
        tvTextoSubir = view.findViewById(R.id.tvTextoSubir);
        ivIconoSubir = view.findViewById(R.id.ivIconoSubir);
        btnVerDocumento = view.findViewById(R.id.btnVerDocumento);

        // Deshabilitar completamente la interacción del calendario (solo visual)
        calendarView.setEnabled(false);
        calendarView.setClickable(false);
        calendarView.setFocusable(false);

        btnSubirDocumento.setOnClickListener(v -> {
            if (selectedFechaIndex >= 0) {
                iniciarSubidaPDF();
            }
        });

        btnVerDocumento.setOnClickListener(v -> {
            if (selectedFechaIndex >= 0 && calendarioEstudianteDocument != null) {
                String pdfUrl = calendarioEstudianteDocument.getString(camposPdfUri[selectedFechaIndex]);
                if (!TextUtils.isEmpty(pdfUrl)) {
                    verPDF(pdfUrl);
                }
            }
        });
    }

    private void cargarCalendarioAsignado() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            tvNoCalendario.setText("Error de sesión. Por favor, inicie sesión de nuevo.");
            tvNoCalendario.setVisibility(View.VISIBLE);
            layoutDetalleFecha.setVisibility(View.GONE);
            layoutFechasLista.setVisibility(View.GONE);
            if (calendarView != null) {
                calendarView.setVisibility(View.GONE);
            }
            return;
        }

        currentUserId = user.getUid();

        profileManager.obtenerPerfilActual(
                profile -> {
                    if (profile == null) {
                        handleError("No se pudo cargar tu perfil.");
                        return;
                    }

                    studentProfile = profile;

                    // Cargar calendario global (fechas) y calendario del estudiante (documentos)
                    firebaseManager.obtenerCalendarioGlobal(taskGlobal -> {
                        if (taskGlobal.isSuccessful() && taskGlobal.getResult() != null && taskGlobal.getResult().exists()) {
                            calendarioGlobalDocument = taskGlobal.getResult();

                            // Verificar si hay al menos una fecha asignada ANTES de mostrar los contenedores
                            boolean hayFechas = false;
                            for (int i = 0; i < 3; i++) {
                                String fecha = calendarioGlobalDocument.getString(camposFecha[i]);
                                if (fecha != null && !fecha.isEmpty()) {
                                    hayFechas = true;
                                    break;
                                }
                            }

                            if (!hayFechas) {
                                // El documento existe pero no tiene fechas asignadas
                                tvNoCalendario.setText("Aún no hay fechas asignadas. El administrador configurará las fechas próximamente.");
                                tvNoCalendario.setVisibility(View.VISIBLE);
                                // Ocultar el ScrollView que contiene layoutDetalleFecha
                                if (scrollViewDetalle != null) {
                                    scrollViewDetalle.setVisibility(View.GONE);
                                }
                                layoutFechasLista.setVisibility(View.GONE);
                                if (calendarView != null) {
                                    calendarView.setVisibility(View.GONE);
                                }
                                return;
                            }

                            // Hay fechas, cargar también el calendario del estudiante para obtener los PDFs subidos
                            String calendarioId = "calendario_" + currentUserId;
                            firebaseManager.buscarCalendarioPorId(currentUserId, calendarioId, taskEstudiante -> {
                                if (taskEstudiante.isSuccessful() && taskEstudiante.getResult() != null && taskEstudiante.getResult().exists()) {
                                    calendarioEstudianteDocument = taskEstudiante.getResult();
                                } else {
                                    calendarioEstudianteDocument = null; // No hay documentos aún
                                }

                                tvNoCalendario.setVisibility(View.GONE);
                                // Mostrar el ScrollView que contiene layoutDetalleFecha
                                if (scrollViewDetalle != null) {
                                    scrollViewDetalle.setVisibility(View.VISIBLE);
                                }
                                layoutDetalleFecha.setVisibility(View.VISIBLE);
                                layoutFechasLista.setVisibility(View.VISIBLE);
                                if (calendarView != null) {
                                    calendarView.setVisibility(View.VISIBLE);
                                }
                                cargarFechas();
                                // Seleccionar la primera fecha por defecto
                                if (selectedFechaIndex < 0) {
                                    seleccionarFecha(0);
                                }
                            });
                        } else {
                            tvNoCalendario.setText("Aún no hay fechas asignadas. El administrador configurará las fechas próximamente.");
                            tvNoCalendario.setVisibility(View.VISIBLE);
                            // Ocultar el ScrollView que contiene layoutDetalleFecha
                            if (scrollViewDetalle != null) {
                                scrollViewDetalle.setVisibility(View.GONE);
                            }
                            layoutFechasLista.setVisibility(View.GONE);
                            if (calendarView != null) {
                                calendarView.setVisibility(View.GONE);
                            }
                        }
                    });
                },
                error -> handleError("Error al obtener perfil: " + error.getMessage())
        );
    }

    private void cargarFechas() {
        if (calendarioGlobalDocument == null) return;

        layoutFechasLista.removeAllViews();
        // Limpiar referencias anteriores
        for (int j = 0; j < fechaCardViews.length; j++) {
            fechaCardViews[j] = null;
        }

        // Verificar si hay al menos una fecha asignada
        boolean hayFechas = false;
        for (int i = 0; i < 3; i++) {
            String fecha = calendarioGlobalDocument.getString(camposFecha[i]);
            if (fecha != null && !fecha.isEmpty()) {
                hayFechas = true;
                break;
            }
        }

        if (!hayFechas) {
            // No hay fechas asignadas, mostrar mensaje
            tvNoCalendario.setText("Aún no hay fechas asignadas. El administrador configurará las fechas próximamente.");
            tvNoCalendario.setVisibility(View.VISIBLE);
            layoutDetalleFecha.setVisibility(View.GONE);
            layoutFechasLista.setVisibility(View.GONE);
            if (calendarView != null) {
                calendarView.setVisibility(View.GONE);
            }
            return;
        }

        // Hay fechas, mostrar el contenedor
        tvNoCalendario.setVisibility(View.GONE);
        layoutDetalleFecha.setVisibility(View.VISIBLE);
        layoutFechasLista.setVisibility(View.VISIBLE);
        if (calendarView != null) {
            calendarView.setVisibility(View.VISIBLE);
        }

        for (int i = 0; i < 3; i++) {
            // Fechas y labels vienen del calendario global
            String label = calendarioGlobalDocument.getString(camposLabel[i]);
            String fecha = calendarioGlobalDocument.getString(camposFecha[i]);

            // PDFs vienen del calendario del estudiante (si existe)
            String pdfUrl = null;
            if (calendarioEstudianteDocument != null) {
                pdfUrl = calendarioEstudianteDocument.getString(camposPdfUri[i]);
            }

            if (fecha == null || fecha.isEmpty()) {
                continue; // No mostrar fechas sin asignar
            }

            View fechaItem = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_fecha_calendario, layoutFechasLista, false);

            androidx.cardview.widget.CardView cardFecha = fechaItem.findViewById(R.id.cardFecha);
            TextView tvNombreItem = fechaItem.findViewById(R.id.tvNombreActividadItem);
            TextView tvDiaNumeroItem = fechaItem.findViewById(R.id.tvDiaNumeroItem);
            TextView tvMesAnoItem = fechaItem.findViewById(R.id.tvMesAnoItem);

            // Verificar si hay documento subido para cambiar el color
            boolean tieneDocumento = !TextUtils.isEmpty(pdfUrl);
            if (tieneDocumento) {
                // Verde pastel si tiene documento
                cardFecha.setCardBackgroundColor(getResources().getColor(R.color.green_pastel));
            } else {
                // Color normal según el índice
                cardFecha.setCardBackgroundColor(getResources().getColor(fechaCardColors[i % fechaCardColors.length]));
            }

            // Guardar referencia a la card
            fechaCardViews[i] = cardFecha;

            // Establecer nombre
            tvNombreItem.setText(label != null && !label.isEmpty() ? label : defaultLabels[i]);

            // Formatear y establecer fecha
            String[] fechaFormateada = formatearFecha(fecha);
            tvDiaNumeroItem.setText(fechaFormateada[0]);
            tvMesAnoItem.setText(fechaFormateada[1]);

            // Seleccionar esta fecha al hacer clic
            final int fechaIndex = i;
            cardFecha.setOnClickListener(v -> seleccionarFecha(fechaIndex));

            layoutFechasLista.addView(fechaItem);
        }
    }

    private void seleccionarFecha(int index) {
        if (calendarioGlobalDocument == null || index < 0 || index >= 3) return;

        selectedFechaIndex = index;

        // Fechas y labels vienen del calendario global
        String label = calendarioGlobalDocument.getString(camposLabel[index]);
        String fecha = calendarioGlobalDocument.getString(camposFecha[index]);

        // PDFs vienen del calendario del estudiante (si existe)
        String pdfUrl = null;
        if (calendarioEstudianteDocument != null) {
            pdfUrl = calendarioEstudianteDocument.getString(camposPdfUri[index]);
        }

        // Actualizar nombre de actividad
        tvNombreActividad.setText(label != null && !label.isEmpty() ? label : defaultLabels[index]);

        // Formatear y actualizar fecha
        if (fecha != null && !fecha.isEmpty()) {
            String[] fechaFormateada = formatearFecha(fecha);
            tvDiaNumero.setText(fechaFormateada[0]);
            tvMesAno.setText(fechaFormateada[1]);

            // Actualizar calendario visual con la fecha seleccionada
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                Date fechaDate = sdf.parse(fecha);
                if (fechaDate != null) {
                    calendarView.setDate(fechaDate.getTime(), false, true);
                }
            } catch (ParseException e) {
                Log.e(TAG, "Error al parsear fecha para calendario: " + fecha, e);
            }
        } else {
            tvDiaNumero.setText("--");
            tvMesAno.setText("Sin asignar");
        }

        // Calcular y mostrar días restantes
        int diasRestantes = calcularDiasRestantes(fecha);
        if (diasRestantes >= 0) {
            tvDiasRestantes.setText("Días restantes: " + diasRestantes);
        } else {
            tvDiasRestantes.setText("Fecha vencida");
        }

        // Verificar si hay documento subido
        boolean tieneDocumento = !TextUtils.isEmpty(pdfUrl);
        boolean fechaVencida = diasRestantes < 0;
        boolean puedeSubir = puedeSubirDocumento(index);

        // Actualizar botón de subir/sustituir
        if (puedeSubir && !fechaVencida) {
            btnSubirDocumento.setVisibility(View.VISIBLE);
            btnSubirDocumento.setEnabled(true);

            // Cambiar texto según si hay documento o no
            if (tieneDocumento) {
                tvTextoSubir.setText("Sustituir Documento");
            } else {
                tvTextoSubir.setText("Subir Documento");
            }
        } else {
            btnSubirDocumento.setVisibility(View.GONE);
        }

        // Mostrar/ocultar botón de ver documento
        if (tieneDocumento) {
            btnVerDocumento.setVisibility(View.VISIBLE);
        } else {
            btnVerDocumento.setVisibility(View.GONE);
        }
    }

    private String[] formatearFecha(String fechaString) {
        if (fechaString == null || fechaString.isEmpty()) {
            return new String[]{"--", "Sin asignar"};
        }

        SimpleDateFormat sdfInput = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        // Nombres de meses en español (abreviados)
        String[] meses = {"Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"};

        try {
            Date fecha = sdfInput.parse(fechaString);
            if (fecha != null) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(fecha);

                int dia = cal.get(Calendar.DAY_OF_MONTH);
                int mes = cal.get(Calendar.MONTH);
                int ano = cal.get(Calendar.YEAR);

                String diaStr = String.valueOf(dia);
                String mesAno = meses[mes] + ", " + ano;

                return new String[]{diaStr, mesAno};
            }
        } catch (ParseException e) {
            Log.e(TAG, "Error al formatear fecha: " + fechaString, e);
        }

        return new String[]{"--", "Error"};
    }

    private int calcularDiasRestantes(String fechaString) {
        if (fechaString == null || fechaString.isEmpty()) {
            return -1;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        try {
            Date fechaLimite = sdf.parse(fechaString);
            Calendar hoy = Calendar.getInstance();
            hoy.set(Calendar.HOUR_OF_DAY, 0);
            hoy.set(Calendar.MINUTE, 0);
            hoy.set(Calendar.SECOND, 0);
            hoy.set(Calendar.MILLISECOND, 0);

            Calendar fechaLimiteCal = Calendar.getInstance();
            fechaLimiteCal.setTime(fechaLimite);
            fechaLimiteCal.set(Calendar.HOUR_OF_DAY, 0);
            fechaLimiteCal.set(Calendar.MINUTE, 0);
            fechaLimiteCal.set(Calendar.SECOND, 0);
            fechaLimiteCal.set(Calendar.MILLISECOND, 0);

            long diff = fechaLimiteCal.getTimeInMillis() - hoy.getTimeInMillis();
            return (int) (diff / (1000 * 60 * 60 * 24));
        } catch (ParseException e) {
            Log.e(TAG, "Error al parsear fecha: " + fechaString, e);
            return -1;
        }
    }

    private boolean puedeSubirDocumento(int index) {
        // La primera fecha siempre se puede subir si no está vencida
        if (index == 0) {
            return true;
        }

        // Para las siguientes fechas, verificar que la anterior tenga documento
        if (calendarioEstudianteDocument == null) return false;

        String pdfUrlAnterior = calendarioEstudianteDocument.getString(camposPdfUri[index - 1]);
        return !TextUtils.isEmpty(pdfUrlAnterior);
    }

    private void iniciarSubidaPDF() {
        if (selectedFechaIndex < 0 || calendarioGlobalDocument == null) return;

        String campoPdfKey = camposPdfUri[selectedFechaIndex];
        pendingUploadCampoPdf = campoPdfKey;
        selectorPDF.launch(new String[]{"application/pdf"});
    }

    private void guardarUrlEnFirestore(String campoPdfKey, String downloadUrl) {
        if (currentUserId == null || getContext() == null) {
            Toast.makeText(getContext(), "Error de sesión.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Los documentos se guardan en la colección del estudiante
        String calendarioId = "calendario_" + currentUserId;
        Map<String, Object> update = new HashMap<>();
        update.put(campoPdfKey, downloadUrl);

        firebaseManager.guardarOActualizarCalendario(currentUserId, calendarioId, update,
                () -> {
                    Toast.makeText(getContext(), "Documento actualizado exitosamente.", Toast.LENGTH_SHORT).show();

                    // Recargar el calendario del estudiante para actualizar los PDFs
                    firebaseManager.buscarCalendarioPorId(currentUserId, calendarioId, task -> {
                        if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                            calendarioEstudianteDocument = task.getResult();
                        }

                        AlarmScheduler scheduler = new AlarmScheduler(requireContext());
                        int fechaIndex = -1;
                        for (int i = 0; i < camposPdfUri.length; i++) {
                            if (camposPdfUri[i].equals(campoPdfKey)) {
                                scheduler.stopShortIntervalCycle(calendarioId, i);
                                fechaIndex = i;
                                break;
                            }
                        }

                        // Cambiar el color de la card a verde pastel
                        if (fechaIndex >= 0 && fechaIndex < fechaCardViews.length && fechaCardViews[fechaIndex] != null) {
                            androidx.cardview.widget.CardView card = (androidx.cardview.widget.CardView) fechaCardViews[fechaIndex];
                            card.setCardBackgroundColor(getResources().getColor(R.color.green_pastel));
                        }

                        // Actualizar la vista de la fecha seleccionada
                        if (selectedFechaIndex == fechaIndex) {
                            seleccionarFecha(fechaIndex);
                        }

                        // Recargar fechas para actualizar la UI
                        cargarFechas();
                    });
                    cargarCalendarioAsignado();
                },
                e -> {
                    Toast.makeText(getContext(), "Error al guardar en la base de datos.", Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Error al actualizar Firestore", e);
                }
        );
    }

    private void verPDF(String urlString) {
        if (urlString == null || urlString.isEmpty()) {
            Toast.makeText(getContext(), "No hay documento para visualizar.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String encodedUrl = URLEncoder.encode(urlString, "UTF-8");
            String googleDocsViewerUrl = "https://docs.google.com/gview?embedded=true&url=" + encodedUrl;

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(googleDocsViewerUrl));
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getContext(), "No se encontró una aplicación para abrir este enlace.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "ActivityNotFoundException al intentar ver PDF: " + urlString, e);
        } catch (Exception e) {
            Log.e(TAG, "Error general al intentar ver PDF: " + urlString, e);
            Toast.makeText(getContext(), "No se pudo abrir el enlace del documento.", Toast.LENGTH_LONG).show();
        }
    }

    private void handleError(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            tvNoCalendario.setText(message);
            tvNoCalendario.setVisibility(View.VISIBLE);
            layoutDetalleFecha.setVisibility(View.GONE);
            layoutFechasLista.setVisibility(View.GONE);
            if (calendarView != null) {
                calendarView.setVisibility(View.GONE);
            }
        }
        Log.e(TAG, message);
    }
}
