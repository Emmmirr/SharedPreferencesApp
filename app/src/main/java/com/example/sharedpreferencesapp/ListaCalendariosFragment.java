package com.example.sharedpreferencesapp;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ListaCalendariosFragment extends Fragment {

    private static final String TAG = "ListaCalendariosFrag";
    private LinearLayout layoutCalendarios;
    private TextView tvNoCalendarios;

    // MODIFICADO: Se añade FirebaseManager y se mantiene FileManager para ciertas operaciones.
    private FirebaseManager firebaseManager;
    private FileManager fileManager; // Mantenido para PDFGenerator y búsqueda de alumnos para PDF.

    private JSONObject calendarioPendiente;
    private String calendarioActualId = null;
    private String alumnoActualId = null;

    private final ActivityResultLauncher<Intent> selectorCarpeta = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null && calendarioPendiente != null) {
                        generarPDFEnUbicacion(calendarioPendiente, uri);
                    }
                }
            }
    );

    public ListaCalendariosFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lista_calendarios, container, false);

        layoutCalendarios = view.findViewById(R.id.layoutCalendarios);
        tvNoCalendarios = view.findViewById(R.id.tvNoCalendarios);
        Button btnAgregar = view.findViewById(R.id.btnAgregarCalendario);

        // AÑADIDO: Inicialización de Managers
        firebaseManager = new FirebaseManager();
        fileManager = new FileManager(requireContext());

        btnAgregar.setOnClickListener(v -> mostrarDialogCalendario(null));
        cargarCalendarios();

        return view;
    }

    // MODIFICADO: Ahora carga los alumnos de Firebase antes de mostrar el diálogo
    private void mostrarDialogCalendario(String calendarioId) {
        firebaseManager.cargarAlumnos(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                Toast.makeText(getContext(), "Error al cargar alumnos.", Toast.LENGTH_SHORT).show();
                return;
            }

            ArrayList<String> alumnosDisplay = new ArrayList<>();
            ArrayList<String> alumnosIds = new ArrayList<>();
            alumnosDisplay.add("Selecciona un alumno...");
            alumnosIds.add(null); // Placeholder para el prompt

            for (QueryDocumentSnapshot alumnoDoc : task.getResult()) {
                alumnosDisplay.add(alumnoDoc.getString("nombre") + " (" + alumnoDoc.getString("numControl") + ")");
                alumnosIds.add(alumnoDoc.getId());
            }

            if (alumnosIds.size() <= 1) {
                Toast.makeText(getContext(), "Primero debe registrar un alumno.", Toast.LENGTH_LONG).show();
                return;
            }

            construirYMostrarDialogo(calendarioId, alumnosDisplay, alumnosIds);
        });
    }

    private void construirYMostrarDialogo(String calendarioId, ArrayList<String> alumnosDisplay, ArrayList<String> alumnosIds) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_calendario, null);
        builder.setView(dialogView);

        // --- Bindeo de Vistas (sin cambios) ---
        TextView tvTitulo = dialogView.findViewById(R.id.tvTituloFormulario);
        Spinner spinnerAlumno = dialogView.findViewById(R.id.spinnerAlumno);
        TextView tvNombreAlumnoSeleccionado = dialogView.findViewById(R.id.tvNombreAlumnoSeleccionado);
        ImageView btnCalendarioAnteproyecto = dialogView.findViewById(R.id.btnCalendarioAnteproyecto);
        ImageView btnCalendarioViabilidad = dialogView.findViewById(R.id.btnCalendarioViabilidad);
        ImageView btnCalendarioModificacion = dialogView.findViewById(R.id.btnCalendarioModificacion);
        ImageView btnGuardarAnteproyecto = dialogView.findViewById(R.id.btnGuardarAnteproyecto);
        ImageView btnGuardarViabilidad = dialogView.findViewById(R.id.btnGuardarViabilidad);
        ImageView btnGuardarModificacion = dialogView.findViewById(R.id.btnGuardarModificacion);
        TextView tvFechaAnteproyecto = dialogView.findViewById(R.id.tvFechaAnteproyecto);
        TextView tvFechaViabilidad = dialogView.findViewById(R.id.tvFechaViabilidad);
        TextView tvFechaModificacion = dialogView.findViewById(R.id.tvFechaModificacion);
        TextView tvLabelAnteproyecto = dialogView.findViewById(R.id.tvLabelAnteproyecto);
        TextView tvLabelViabilidad = dialogView.findViewById(R.id.tvLabelViabilidad);
        TextView tvLabelModificacion = dialogView.findViewById(R.id.tvLabelModificacion);
        Button btnBorrarFechas = dialogView.findViewById(R.id.btnBorrarFechas);

        final String[] fechasTemp = new String[3];
        SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        TextView[] textViewsFecha = {tvFechaAnteproyecto, tvFechaViabilidad, tvFechaModificacion};
        ImageView[] iconosCalendario = {btnCalendarioAnteproyecto, btnCalendarioViabilidad, btnCalendarioModificacion};
        ImageView[] iconosGuardar = {btnGuardarAnteproyecto, btnGuardarViabilidad, btnGuardarModificacion};
        String[] nombresCamposFecha = {"fechaPrimeraEntrega", "fechaSegundaEntrega", "fechaResultado"};
        TextView[] textViewsLabel = {tvLabelAnteproyecto, tvLabelViabilidad, tvLabelModificacion};
        String[] nombresCamposLabel = {"labelPrimeraEntrega", "labelSegundaEntrega", "labelResultado"};
        String[] defaultLabels = {"1 Entrega evaluacion", "2 Entrega de Evaluacion", "3 Entrega de Resultado"};

        ArrayAdapter<String> alumnosAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, alumnosDisplay);
        alumnosAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAlumno.setAdapter(alumnosAdapter);

        // --- Listeners (lógica interna modificada para Firebase) ---
        for (int i = 0; i < iconosCalendario.length; i++) {
            final int indice = i;
            iconosCalendario[i].setOnClickListener(v -> {
                if (alumnoActualId == null) {
                    Toast.makeText(getContext(), "Primero selecciona un alumno", Toast.LENGTH_SHORT).show();
                    return;
                }
                String fechaMinima = indice > 0 ? fechasTemp[indice - 1] : null;
                mostrarDatePickerIndividualIcono(indice, fechasTemp, textViewsFecha, iconosGuardar, formatoFecha, fechaMinima);
            });
        }
        for (int i = 0; i < iconosGuardar.length; i++) {
            final int indice = i;
            iconosGuardar[i].setOnClickListener(v -> {
                if (alumnoActualId == null) {
                    Toast.makeText(getContext(), "Primero selecciona un alumno", Toast.LENGTH_SHORT).show();
                    return;
                }
                guardarFechaIndividualFirebase(fechasTemp, indice, nombresCamposFecha[indice], formatoFecha);
            });
        }
        for(int i = 0; i < textViewsLabel.length; i++) {
            final int indice = i;
            textViewsLabel[i].setOnClickListener(v -> {
                if(calendarioActualId == null) {
                    Toast.makeText(getContext(), "Primero guarda una fecha para este alumno.", Toast.LENGTH_SHORT).show();
                    return;
                }
                mostrarDialogEditarLabelFirebase(textViewsLabel[indice], calendarioActualId, nombresCamposLabel[indice]);
            });
        }
        btnBorrarFechas.setOnClickListener(v -> {
            if (calendarioActualId == null) {
                Toast.makeText(getContext(), "No hay fechas que borrar para este alumno", Toast.LENGTH_SHORT).show();
                return;
            }
            new AlertDialog.Builder(getContext())
                    .setTitle("Borrar Fechas").setMessage("¿Estás seguro que quieres eliminar todas las fechas?")
                    .setIcon(R.drawable.libro)
                    .setPositiveButton("Sí, Borrar", (dialog, which) -> borrarTodasLasFechasFirebase(fechasTemp, textViewsFecha, iconosGuardar))
                    .setNegativeButton("Cancelar", null).show();
        });
        spinnerAlumno.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    alumnoActualId = alumnosIds.get(position);
                    calendarioActualId = "calendario_" + alumnoActualId; // ID predecible
                    cargarDatosCalendarioExistenteFirebase(fechasTemp, textViewsFecha, iconosGuardar, textViewsLabel, defaultLabels);
                } else {
                    alumnoActualId = null;
                    calendarioActualId = null;
                    cargarDatosCalendarioExistenteFirebase(fechasTemp, textViewsFecha, iconosGuardar, textViewsLabel, defaultLabels); // Limpia la UI
                }
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                alumnoActualId = null;
                calendarioActualId = null;
            }
        });

        if (calendarioId != null) {
            tvTitulo.setText("Editar Calendario");
            cargarDatosCalendarioParaEdicionFirebase(calendarioId, spinnerAlumno, tvNombreAlumnoSeleccionado);
            cargarDatosCalendarioExistenteFirebase(fechasTemp, textViewsFecha, iconosGuardar, textViewsLabel, defaultLabels);
        }

        AlertDialog dialog = builder.create();
        dialogView.findViewById(R.id.btnCerrar).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    // MODIFICADO: Guarda o actualiza un campo de fecha en Firebase
    private void guardarFechaIndividualFirebase(String[] fechasTemp, int indice, String nombreCampoFecha, SimpleDateFormat formatoFecha) {
        if (this.alumnoActualId == null) {
            Toast.makeText(getContext(), "Error: No se ha seleccionado un alumno.", Toast.LENGTH_SHORT).show();
            return;
        }

        String calendarioId = "calendario_" + this.alumnoActualId;
        this.calendarioActualId = calendarioId;

        Map<String, Object> data = new HashMap<>();
        data.put("id", calendarioId);
        data.put("alumnoId", this.alumnoActualId);
        data.put(nombreCampoFecha, fechasTemp[indice]);
        data.put("fechaCreacion", formatoFecha.format(new Date()));

        firebaseManager.guardarOActualizarCalendario(calendarioId, data,
                () -> {
                    Toast.makeText(getContext(), "Fecha guardada exitosamente", Toast.LENGTH_SHORT).show();
                    cargarCalendarios();
                },
                e -> Toast.makeText(getContext(), "Error al guardar la fecha: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // MODIFICADO: Actualiza un campo de label en Firebase
    private void mostrarDialogEditarLabelFirebase(TextView textViewLabel, String calendarioId, String campoLabelKey) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Editar Nombre de la Actividad");
        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(textViewLabel.getText().toString());
        builder.setView(input);
        builder.setPositiveButton("Guardar", (dialog, which) -> {
            String nuevoLabel = input.getText().toString().trim();
            if(nuevoLabel.isEmpty()){
                Toast.makeText(getContext(), "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show();
                return;
            }
            Map<String, Object> update = new HashMap<>();
            update.put(campoLabelKey, nuevoLabel);
            firebaseManager.actualizarCalendario(calendarioId, update, () -> {
                textViewLabel.setText(nuevoLabel);
                cargarCalendarios();
                Toast.makeText(getContext(), "Nombre actualizado", Toast.LENGTH_SHORT).show();
            });
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    // MODIFICADO: Carga la lista de calendarios desde Firebase
    private void cargarCalendarios() {
        layoutCalendarios.removeAllViews();
        tvNoCalendarios.setVisibility(View.GONE);
        firebaseManager.cargarCalendarios(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                if(task.getResult().isEmpty()){
                    tvNoCalendarios.setVisibility(View.VISIBLE);
                } else {
                    for (QueryDocumentSnapshot calendarioDoc : task.getResult()) {
                        crearCardCalendario(calendarioDoc);
                    }
                }
            } else {
                tvNoCalendarios.setVisibility(View.VISIBLE);
                Log.e(TAG, "Error cargando calendarios", task.getException());
            }
        });
    }

    // MODIFICADO: Crea una card a partir de un DocumentSnapshot de Firebase
    private void crearCardCalendario(DocumentSnapshot calendario) {
        View cardView = LayoutInflater.from(getContext()).inflate(R.layout.card_calendario, layoutCalendarios, false);
        TextView tvNombreAlumno = cardView.findViewById(R.id.tvNombreAlumno);
        TextView tvNumControl = cardView.findViewById(R.id.tvNumControl);
        TextView tvCarrera = cardView.findViewById(R.id.tvCarrera);
        TextView tvProximaFecha = cardView.findViewById(R.id.tvProximaFecha);
        TextView tvProgreso = cardView.findViewById(R.id.tvProgreso);
        TextView tvFechaCreacion = cardView.findViewById(R.id.tvFechaCreacion);
        Button btnPDF = cardView.findViewById(R.id.btnPDFCalendario);
        Button btnEditar = cardView.findViewById(R.id.btnEditarCalendario);
        Button btnEliminar = cardView.findViewById(R.id.btnEliminarCalendario);

        String calendarioId = calendario.getId();
        String alumnoId = calendario.getString("alumnoId");

        // Carga asíncrona de los datos del alumno
        if (alumnoId != null) {
            firebaseManager.buscarAlumnoPorId(alumnoId, task -> {
                if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                    DocumentSnapshot alumno = task.getResult();
                    tvNombreAlumno.setText(limpiarTextoVista(alumno.getString("nombre")));
                    tvNumControl.setText("No. Control: " + limpiarTextoVista(alumno.getString("numControl")));
                    tvCarrera.setText("Carrera: " + limpiarTextoVista(alumno.getString("carrera")));
                } else {
                    tvNombreAlumno.setText("Alumno no encontrado");
                }
            });
        }

        String[] fechasArray = {
                calendario.getString("fechaPrimeraEntrega"),
                calendario.getString("fechaSegundaEntrega"),
                calendario.getString("fechaResultado")
        };
        String[] etiquetas = {
                calendario.getString("labelPrimeraEntrega") != null ? calendario.getString("labelPrimeraEntrega") : "1ª Entrega",
                calendario.getString("labelSegundaEntrega") != null ? calendario.getString("labelSegundaEntrega") : "2ª Entrega",
                calendario.getString("labelResultado") != null ? calendario.getString("labelResultado") : "Resultado"
        };

        tvProximaFecha.setText(calcularProximaFecha(fechasArray, etiquetas));
        tvProgreso.setText(calcularProgreso(fechasArray));
        tvFechaCreacion.setText("Creado: " + limpiarTextoVista(calendario.getString("fechaCreacion")));

        btnPDF.setOnClickListener(v -> {
            // Convierte el DocumentSnapshot a JSONObject para mantener la compatibilidad del generador PDF
            JSONObject calJson = new JSONObject(calendario.getData());
            seleccionarUbicacionPDF(calJson);
        });
        btnEditar.setOnClickListener(v -> mostrarDialogCalendario(calendarioId));
        btnEliminar.setOnClickListener(v -> new AlertDialog.Builder(getContext())
                .setTitle("Eliminar Calendario").setMessage("¿Eliminar este calendario?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    firebaseManager.eliminarCalendario(calendarioId,
                            () -> {
                                fileManager.eliminarRegistroLocalCalendario(calendarioId); // Elimina archivo local de URIs de PDF
                                cargarCalendarios();
                                Toast.makeText(getContext(), "Calendario eliminado", Toast.LENGTH_SHORT).show();
                            },
                            e -> Toast.makeText(getContext(), "Error al eliminar: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }).setNegativeButton("Cancelar", null).show());

        layoutCalendarios.addView(cardView);
    }

    // --- MÉTODOS AUXILIARES (Lógica de UI o adaptados a Firebase) ---

    private void cargarDatosCalendarioParaEdicionFirebase(String calendarioId, Spinner spinnerAlumno, TextView tvNombreAlumnoSeleccionado) {
        firebaseManager.buscarCalendarioPorId(calendarioId, task -> {
            if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                DocumentSnapshot calendario = task.getResult();
                String alumnoId = calendario.getString("alumnoId");
                this.alumnoActualId = alumnoId;
                this.calendarioActualId = calendarioId;

                firebaseManager.buscarAlumnoPorId(alumnoId, alumnoTask -> {
                    if(alumnoTask.isSuccessful() && alumnoTask.getResult() != null && alumnoTask.getResult().exists()){
                        DocumentSnapshot alumno = alumnoTask.getResult();
                        tvNombreAlumnoSeleccionado.setText(alumno.getString("nombre") + "\nNo. Control: " + alumno.getString("numControl"));
                        tvNombreAlumnoSeleccionado.setVisibility(View.VISIBLE);
                        spinnerAlumno.setVisibility(View.GONE);
                    }
                });
            }
        });
    }

    private void cargarDatosCalendarioExistenteFirebase(String[] fechasTemp, TextView[] textViewsFecha, ImageView[] iconosGuardar,
                                                        TextView[] textViewsLabel, String[] defaultLabels) {
        for (int i = 0; i < textViewsFecha.length; i++) {
            fechasTemp[i] = "";
            textViewsFecha[i].setText("Fecha: Sin asignar");
            textViewsFecha[i].setTextColor(0xFF4299E1);
            iconosGuardar[i].setEnabled(false);
            iconosGuardar[i].setAlpha(0.5f);
            iconosGuardar[i].setColorFilter(0xFF1976D2);
            textViewsLabel[i].setText(defaultLabels[i]);
        }
        if (calendarioActualId == null) return;

        firebaseManager.buscarCalendarioPorId(calendarioActualId, task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                DocumentSnapshot calendario = task.getResult();
                if (calendario.exists()) {
                    String[] camposFecha = {"fechaPrimeraEntrega", "fechaSegundaEntrega", "fechaResultado"};
                    String[] camposLabel = {"labelPrimeraEntrega", "labelSegundaEntrega", "labelResultado"};
                    for (int i = 0; i < camposFecha.length; i++) {
                        String fecha = calendario.getString(camposFecha[i]);
                        if (fecha != null && !fecha.isEmpty()) {
                            fechasTemp[i] = fecha;
                            textViewsFecha[i].setText("Fecha: " + fecha);
                            textViewsFecha[i].setTextColor(0xFF2E7D32);
                            iconosGuardar[i].setAlpha(1.0f);
                        }
                        String label = calendario.getString(camposLabel[i]);
                        textViewsLabel[i].setText(label != null ? label : defaultLabels[i]);
                    }
                }
            }
        });
    }

    private void borrarTodasLasFechasFirebase(String[] fechasTemp, TextView[] textViews, ImageView[] iconosGuardar) {
        if (calendarioActualId == null) return;
        Map<String, Object> updates = new HashMap<>();
        updates.put("fechaPrimeraEntrega", "");
        updates.put("fechaSegundaEntrega", "");
        updates.put("fechaResultado", "");

        firebaseManager.actualizarCalendario(calendarioActualId, updates, () -> {
            Toast.makeText(getContext(), "Todas las fechas han sido eliminadas", Toast.LENGTH_SHORT).show();
            cargarDatosCalendarioExistenteFirebase(fechasTemp, textViews, iconosGuardar, textViews, new String[]{"1 Entrega", "2 Entrega", "Resultado"});
            cargarCalendarios();
        });
    }

    private void mostrarDatePickerIndividualIcono(int indice, String[] fechasTemp, TextView[] textViews,
                                                  ImageView[] iconosGuardar, SimpleDateFormat formatoFecha, String fechaMinima) {
        Calendar calendar = Calendar.getInstance();
        if (fechaMinima != null && !fechaMinima.isEmpty()) {
            try {
                Date fechaMin = formatoFecha.parse(fechaMinima);
                if (fechaMin != null) {
                    Calendar calMin = Calendar.getInstance();
                    calMin.setTime(fechaMin);
                    calMin.add(Calendar.DAY_OF_MONTH, 1);
                    calendar = calMin;
                }
            } catch (ParseException e) { e.printStackTrace(); }
        }
        DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
            Calendar selectedDate = Calendar.getInstance();
            selectedDate.set(year, month, dayOfMonth);
            if (esFindeSemana(selectedDate)) {
                Toast.makeText(getContext(), "No se pueden seleccionar sábados ni domingos", Toast.LENGTH_LONG).show();
                return;
            }
            if (validarFecha(selectedDate.getTime(), fechaMinima, formatoFecha)) {
                String fechaSeleccionada = formatoFecha.format(selectedDate.getTime());
                fechasTemp[indice] = fechaSeleccionada;
                textViews[indice].setText("Fecha: " + fechaSeleccionada);
                textViews[indice].setTextColor(0xFF1976D2);
                iconosGuardar[indice].setEnabled(true);
                iconosGuardar[indice].setAlpha(1.0f);
                iconosGuardar[indice].setColorFilter(0xFF1976D2);
            } else {
                Toast.makeText(getContext(), "La fecha debe ser posterior a la fecha anterior", Toast.LENGTH_SHORT).show();
            }
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        if (fechaMinima != null && !fechaMinima.isEmpty()) {
            try {
                Date fechaMin = formatoFecha.parse(fechaMinima);
                if (fechaMin != null) {
                    Calendar calMin = Calendar.getInstance();
                    calMin.setTime(fechaMin);
                    calMin.add(Calendar.DAY_OF_MONTH, 1);
                    datePickerDialog.getDatePicker().setMinDate(calMin.getTimeInMillis());
                }
            } catch (ParseException e) { e.printStackTrace(); }
        }
        datePickerDialog.show();
    }

    private boolean esFindeSemana(Calendar fecha) {
        int diaSemana = fecha.get(Calendar.DAY_OF_WEEK);
        return diaSemana == Calendar.SATURDAY || diaSemana == Calendar.SUNDAY;
    }

    private boolean validarFecha(Date fechaSeleccionada, String fechaMinima, SimpleDateFormat formatoFecha) {
        if (esFindeSemana(Calendar.getInstance())) return false;
        if (fechaMinima == null || fechaMinima.isEmpty()) return true;
        try {
            return fechaSeleccionada.after(formatoFecha.parse(fechaMinima));
        } catch (ParseException e) {
            e.printStackTrace();
            return true;
        }
    }

    private String calcularProximaFecha(String[] fechas, String[] etiquetas) {
        SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        Calendar hoy = Calendar.getInstance();
        hoy.set(Calendar.HOUR_OF_DAY, 0);
        hoy.set(Calendar.MINUTE, 0);
        hoy.set(Calendar.SECOND, 0);
        hoy.set(Calendar.MILLISECOND, 0);
        for (int i = 0; i < fechas.length; i++) {
            if (fechas[i] != null && !fechas[i].isEmpty()) {
                try {
                    Date fecha = formatoFecha.parse(fechas[i]);
                    if (fecha != null && !fecha.before(hoy.getTime())) {
                        return "Próximo: " + limpiarTextoVista(etiquetas[i]) + " (" + fechas[i] + ")";
                    }
                } catch (ParseException e) { e.printStackTrace(); }
            }
        }
        return "Sin fechas próximas";
    }

    private String calcularProgreso(String[] fechas) {
        int completadas = 0;
        for (String fecha : fechas) {
            if (fecha != null && !fecha.isEmpty()) {
                completadas++;
            }
        }
        return "Progreso: " + completadas + "/3 fechas configuradas";
    }

    private void seleccionarUbicacionPDF(JSONObject calendario) {
        calendarioPendiente = calendario;
        String alumnoId = calendario.optString("alumnoId", "");
        // Se usa fileManager para buscar el alumno para el nombre del archivo PDF
        JSONObject alumno = fileManager.buscarAlumnoPorId(alumnoId);
        String nombreAlumno = alumno != null ? alumno.optString("nombre", "Calendario") : "Calendario";
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String nombreArchivo = "Calendario_" + nombreAlumno.replaceAll("[^a-zA-Z0-9]", "_") + "_" + timestamp + ".pdf";
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_TITLE, nombreArchivo);
        try {
            selectorCarpeta.launch(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error al abrir selector de archivos", Toast.LENGTH_SHORT).show();
        }
    }

    private void generarPDFEnUbicacion(JSONObject calendario, Uri uri) {
        Toast.makeText(getContext(), "Generando PDF...", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                PDFGeneratorCalendario pdfGenerator = new PDFGeneratorCalendario(requireContext());
                boolean exito = pdfGenerator.generarPDFCalendarioEnUri(calendario, uri);
                requireActivity().runOnUiThread(() -> {
                    if (exito) {
                        Toast.makeText(getContext(), "PDF de calendario guardado exitosamente", Toast.LENGTH_LONG).show();
                        new AlertDialog.Builder(getContext())
                                .setTitle("PDF Creado")
                                .setMessage("El archivo PDF se ha guardado en la ubicación seleccionada.")
                                .setIcon(R.drawable.libro)
                                .setPositiveButton("OK", null)
                                .show();
                    } else {
                        Toast.makeText(getContext(), "Error al generar PDF", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private String limpiarTextoVista(String texto) {
        if (texto == null || texto.isEmpty()) return "";
        try {
            return new String(texto.getBytes("ISO-8859-1"), "UTF-8").replace("Â", "");
        } catch (Exception e) {
            Log.w("LimpiarTextoVista", "Error procesando texto: " + texto, e);
            return texto;
        }
    }
}