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
import androidx.fragment.app.Fragment;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class GestionCalendarioFragment extends Fragment {

    private LinearLayout layoutCalendarios;
    private TextView tvNoCalendarios;
    private FileManager fileManager;
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

    public GestionCalendarioFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_gestion_calendario, container, false);

        layoutCalendarios = view.findViewById(R.id.layoutCalendarios);
        tvNoCalendarios = view.findViewById(R.id.tvNoCalendarios);
        Button btnAgregar = view.findViewById(R.id.btnAgregarCalendario);

        fileManager = new FileManager(requireContext());

        btnAgregar.setOnClickListener(v -> mostrarDialogCalendario(null));
        cargarCalendarios();

        return view;
    }

    private void mostrarDialogCalendario(String calendarioId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_calendario, null);
        builder.setView(dialogView);

        TextView tvTitulo = dialogView.findViewById(R.id.tvTituloFormulario);
        Spinner spinnerAlumno = dialogView.findViewById(R.id.spinnerAlumno);
        TextView tvNombreAlumnoSeleccionado = dialogView.findViewById(R.id.tvNombreAlumnoSeleccionado);

        // MODIFICADO: Referencias a los nuevos TextView que funcionan como botones de fecha
        TextView btnFechaAnteproyecto = dialogView.findViewById(R.id.btnFechaAnteproyecto);
        TextView btnFechaViabilidad = dialogView.findViewById(R.id.btnFechaViabilidad);
        TextView btnFechaModificacion = dialogView.findViewById(R.id.btnFechaModificacion);

        ImageView btnGuardarAnteproyecto = dialogView.findViewById(R.id.btnGuardarAnteproyecto);
        ImageView btnGuardarViabilidad = dialogView.findViewById(R.id.btnGuardarViabilidad);
        ImageView btnGuardarModificacion = dialogView.findViewById(R.id.btnGuardarModificacion);

        TextView tvLabelAnteproyecto = dialogView.findViewById(R.id.tvLabelAnteproyecto);
        TextView tvLabelViabilidad = dialogView.findViewById(R.id.tvLabelViabilidad);
        TextView tvLabelModificacion = dialogView.findViewById(R.id.tvLabelModificacion);

        Button btnBorrarFechas = dialogView.findViewById(R.id.btnBorrarFechas);

        final String[] fechasTemp = new String[3];
        SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        // MODIFICADO: El array 'textViewsFecha' ahora contiene los nuevos TextView/Botones
        TextView[] textViewsFecha = {btnFechaAnteproyecto, btnFechaViabilidad, btnFechaModificacion};
        ImageView[] iconosGuardar = {btnGuardarAnteproyecto, btnGuardarViabilidad, btnGuardarModificacion};
        String[] nombresCamposFecha = {"fechaPrimeraEntrega", "fechaSegundaEntrega", "fechaResultado"};

        TextView[] textViewsLabel = {tvLabelAnteproyecto, tvLabelViabilidad, tvLabelModificacion};
        String[] nombresCamposLabel = {"labelPrimeraEntrega", "labelSegundaEntrega", "labelResultado"};
        String[] defaultLabels = {"1 Entrega evaluacion", "2 Entrega de Evaluacion", "3 Entrega de Resultado"};


        ArrayList<String> alumnos = new ArrayList<>();
        ArrayList<String> alumnosIds = new ArrayList<>();
        List<JSONObject> alumnosData = fileManager.cargarAlumnos();
        alumnos.add("Selecciona un alumno...");
        for (JSONObject alumno : alumnosData) {
            try {
                alumnos.add(alumno.optString("nombre", "") + " (" + alumno.optString("numControl", "") + ")");
                alumnosIds.add(alumno.getString("id"));
            } catch (JSONException e) { e.printStackTrace(); }
        }
        if (alumnosIds.isEmpty()) {
            alumnos.clear();
            alumnos.add("No hay alumnos registrados");
            alumnosIds.add("");
        }
        ArrayAdapter<String> alumnosAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, alumnos);
        alumnosAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAlumno.setAdapter(alumnosAdapter);

        // --- LISTENERS ---

        // MODIFICADO: El listener se asigna al nuevo TextView/Botón de fecha
        for (int i = 0; i < textViewsFecha.length; i++) {
            final int indice = i;
            textViewsFecha[i].setOnClickListener(v -> {
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
                guardarFechaIndividualIcono(fechasTemp, indice, nombresCamposFecha[indice],
                        nombresCamposLabel, defaultLabels, formatoFecha);
            });
        }

        for(int i = 0; i < textViewsLabel.length; i++) {
            final int indice = i;
            textViewsLabel[i].setOnClickListener(v -> {
                if(calendarioActualId == null) {
                    Toast.makeText(getContext(), "Primero selecciona un alumno para editar", Toast.LENGTH_SHORT).show();
                    return;
                }
                mostrarDialogEditarLabel(textViewsLabel[indice], calendarioActualId, nombresCamposLabel[indice]);
            });
        }

        btnBorrarFechas.setOnClickListener(v -> {
            if (alumnoActualId == null) {
                Toast.makeText(getContext(), "Primero selecciona un alumno", Toast.LENGTH_SHORT).show();
                return;
            }
            new AlertDialog.Builder(getContext())
                    .setTitle("Borrar Fechas")
                    .setMessage("¿Estás seguro que quieres eliminar todas las fechas?")
                    .setIcon(R.drawable.libro)
                    .setPositiveButton("Sí, Borrar", (dialog, which) -> {
                        borrarTodasLasFechasIndividualIcono(fechasTemp, textViewsFecha, iconosGuardar);
                        Toast.makeText(getContext(), "Todas las fechas han sido eliminadas", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancelar", null).show();
        });

        spinnerAlumno.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (position > 0 && !alumnosIds.isEmpty()) {
                    alumnoActualId = alumnosIds.get(position - 1);
                    calendarioActualId = "calendario_" + alumnoActualId;
                } else {
                    alumnoActualId = null;
                    calendarioActualId = null;
                }
                cargarDatosCalendarioExistenteIcono(fechasTemp, textViewsFecha, iconosGuardar, textViewsLabel, defaultLabels);
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                alumnoActualId = null;
                calendarioActualId = null;
            }
        });

        if (calendarioId != null) {
            tvTitulo.setText("Editar Calendario");
            cargarDatosCalendarioParaEdicionIcono(calendarioId, spinnerAlumno, tvNombreAlumnoSeleccionado);
            cargarDatosCalendarioExistenteIcono(fechasTemp, textViewsFecha, iconosGuardar, textViewsLabel, defaultLabels);
        }

        AlertDialog dialog = builder.create();
        dialogView.findViewById(R.id.btnCerrar).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void mostrarDatePickerIndividualIcono(int indice, String[] fechasTemp, TextView[] textViews,
                                                  ImageView[] iconosGuardar, SimpleDateFormat formatoFecha, String fechaMinima) {
        // Calendar para la vista inicial del DatePicker
        Calendar calendar = Calendar.getInstance();

        // Calendar para establecer la fecha mínima seleccionable
        Calendar minDateCal = Calendar.getInstance(); // Por defecto, hoy

        if (fechaMinima != null && !fechaMinima.isEmpty()) {
            try {
                Date fechaMinDate = formatoFecha.parse(fechaMinima);
                if (fechaMinDate != null) {
                    Calendar tempCal = Calendar.getInstance();
                    tempCal.setTime(fechaMinDate);
                    tempCal.add(Calendar.DAY_OF_MONTH, 1);
                    calendar = tempCal; // La vista inicial será el día después de la fecha anterior
                    minDateCal = (Calendar) tempCal.clone(); // La fecha mínima seleccionable también
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
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
                textViews[indice].setText(fechaSeleccionada);
                textViews[indice].setTextColor(0xFF1976D2); // Color azul para indicar cambio pendiente de guardar
                iconosGuardar[indice].setEnabled(true);
                iconosGuardar[indice].setAlpha(1.0f);
                iconosGuardar[indice].setColorFilter(0xFF1976D2);
            } else {
                Toast.makeText(getContext(), "La fecha debe ser posterior a la fecha anterior", Toast.LENGTH_SHORT).show();
            }
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

        //Fecha minima
        datePickerDialog.getDatePicker().setMinDate(minDateCal.getTimeInMillis());
        datePickerDialog.show();
    }


    private void cargarDatosCalendarioExistenteIcono(String[] fechasTemp, TextView[] textViewsFecha, ImageView[] iconosGuardar,
                                                     TextView[] textViewsLabel, String[] defaultLabels) {
        for (int i = 0; i < textViewsFecha.length; i++) {
            fechasTemp[i] = "";
            textViewsFecha[i].setText("Asignar"); //Texto por defecti
            textViewsFecha[i].setTextColor(0xFF4299E1); // Color azul claro por defecto
            iconosGuardar[i].setEnabled(false);
            iconosGuardar[i].setAlpha(0.5f);
            iconosGuardar[i].setColorFilter(0xFF1976D2);
            textViewsLabel[i].setText(defaultLabels[i]);
        }
        if (calendarioActualId == null) return;
        JSONObject calendario = fileManager.buscarCalendarioPorId(calendarioActualId);
        if (calendario != null) {
            String[] camposFecha = {"fechaPrimeraEntrega", "fechaSegundaEntrega", "fechaResultado"};
            String[] camposLabel = {"labelPrimeraEntrega", "labelSegundaEntrega", "labelResultado"};
            for (int i = 0; i < camposFecha.length; i++) {
                String fecha = calendario.optString(camposFecha[i], "");
                if (!fecha.isEmpty()) {
                    fechasTemp[i] = fecha;
                    textViewsFecha[i].setText(fecha);
                    textViewsFecha[i].setTextColor(0xFF2E7D32);
                    iconosGuardar[i].setAlpha(1.0f);
                }
                textViewsLabel[i].setText(calendario.optString(camposLabel[i], defaultLabels[i]));
            }
        }
    }


    private void borrarTodasLasFechasIndividualIcono(String[] fechasTemp, TextView[] textViews, ImageView[] iconosGuardar) {
        if (calendarioActualId != null) {
            try {
                JSONObject calendario = fileManager.buscarCalendarioPorId(calendarioActualId);
                if (calendario != null) {
                    String[] campos = {"fechaPrimeraEntrega", "fechaSegundaEntrega", "fechaResultado"};
                    for (String campo : campos) calendario.put(campo, "");
                    fileManager.guardarCalendario(calendario);
                }
            } catch (JSONException e) { e.printStackTrace(); }
        }
        for (int i = 0; i < fechasTemp.length; i++) {
            fechasTemp[i] = null;
            textViews[i].setText("Asignar"); // MODIFICADO: Restablece el texto del botón
            textViews[i].setTextColor(0xFF4299E1);
            iconosGuardar[i].setEnabled(false);
            iconosGuardar[i].setColorFilter(0xFF1976D2);
            iconosGuardar[i].setAlpha(0.5f);
        }
        cargarCalendarios();
    }


    // --- MÉTODOS SIN CAMBIOS IMPORTANTES (Se mantienen igual que en tu versión) ---

    private void guardarFechaIndividualIcono(String[] fechasTemp,
                                             int indice, String nombreCampoFecha, String[] nombresCamposLabel,
                                             String[] defaultLabels, SimpleDateFormat formatoFecha) {
        try {
            if(this.alumnoActualId == null) {
                Toast.makeText(getContext(), "Error: No se ha seleccionado un alumno.", Toast.LENGTH_SHORT).show();
                return;
            }

            String calendarioId = "calendario_" + this.alumnoActualId;
            JSONObject calendario = fileManager.buscarCalendarioPorId(calendarioId);

            if (calendario == null) {
                calendario = new JSONObject();
                calendario.put("id", calendarioId);
                calendario.put("alumnoId", this.alumnoActualId);
                calendario.put("fechaCreacion", formatoFecha.format(new Date()));
                String[] camposFecha = {"fechaPrimeraEntrega", "fechaSegundaEntrega", "fechaResultado"};
                for (String campo : camposFecha) calendario.put(campo, "");
                for (int i = 0; i < nombresCamposLabel.length; i++) {
                    calendario.put(nombresCamposLabel[i], defaultLabels[i]);
                }
            }
            calendario.put(nombreCampoFecha, fechasTemp[indice]);
            if (fileManager.guardarCalendario(calendario)) {
                Toast.makeText(getContext(), "Fecha guardada exitosamente", Toast.LENGTH_SHORT).show();
                cargarCalendarios();
            } else {
                Toast.makeText(getContext(), "Error al guardar la fecha", Toast.LENGTH_SHORT).show();
            }
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error al procesar los datos", Toast.LENGTH_SHORT).show();
        }
    }


    private void cargarDatosCalendarioParaEdicionIcono(String calendarioId, Spinner spinnerAlumno, TextView tvNombreAlumnoSeleccionado) {
        JSONObject calendario = fileManager.buscarCalendarioPorId(calendarioId);
        if (calendario != null) {
            try {
                String alumnoId = calendario.optString("alumnoId", "");
                this.alumnoActualId = alumnoId;
                this.calendarioActualId = calendarioId;

                JSONObject alumno = fileManager.buscarAlumnoPorId(alumnoId);
                if (alumno != null) {
                    String nombre = alumno.optString("nombre", "Desconocido");
                    String numControl = alumno.optString("numControl", "");
                    tvNombreAlumnoSeleccionado.setText(nombre + "\nNo. Control: " + numControl);
                    tvNombreAlumnoSeleccionado.setVisibility(View.VISIBLE);
                    spinnerAlumno.setVisibility(View.GONE);
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private void mostrarDialogEditarLabel(TextView textViewLabel, String calendarioId, String campoLabelKey) {
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
            JSONObject calendario = fileManager.buscarCalendarioPorId(calendarioId);
            if (calendario != null) {
                try {
                    calendario.put(campoLabelKey, nuevoLabel);
                    if(fileManager.guardarCalendario(calendario)){
                        textViewLabel.setText(nuevoLabel);
                        cargarCalendarios();
                        Toast.makeText(getContext(), "Nombre actualizado", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Error al guardar el nombre", Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) { e.printStackTrace(); }
            }
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());
        builder.show();
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

    private void cargarCalendarios() {
        layoutCalendarios.removeAllViews();
        List<JSONObject> calendarios = fileManager.cargarCalendarios();
        tvNoCalendarios.setVisibility(calendarios.isEmpty() ? View.VISIBLE : View.GONE);
        for (JSONObject calendario : calendarios) {
            crearCardCalendario(calendario);
        }
    }

    private void crearCardCalendario(JSONObject calendario) {
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

        try {
            String calendarioId = calendario.getString("id");
            String alumnoId = calendario.optString("alumnoId", "");
            JSONObject alumno = fileManager.buscarAlumnoPorId(alumnoId);
            String nombreAlumno = "Sin alumno", numControl = "", carrera = "";
            if (alumno != null) {
                nombreAlumno = limpiarTextoVista(alumno.optString("nombre", "Sin alumno"));
                numControl = limpiarTextoVista(alumno.optString("numControl", ""));
                carrera = limpiarTextoVista(alumno.optString("carrera", ""));
            }
            String[] fechasArray = {
                    calendario.optString("fechaPrimeraEntrega", ""),
                    calendario.optString("fechaSegundaEntrega", ""),
                    calendario.optString("fechaResultado", "")
            };
            String[] etiquetas = {
                    calendario.optString("labelPrimeraEntrega", "1ª Entrega"),
                    calendario.optString("labelSegundaEntrega", "2ª Entrega"),
                    calendario.optString("labelResultado", "Resultado")
            };
            tvNombreAlumno.setText(nombreAlumno);
            tvNumControl.setText("No. Control: " + numControl);
            tvCarrera.setText("Carrera: " + carrera);
            tvProximaFecha.setText(calcularProximaFecha(fechasArray, etiquetas));
            tvProgreso.setText(calcularProgreso(fechasArray));
            tvFechaCreacion.setText("Creado: " + limpiarTextoVista(calendario.optString("fechaCreacion", "")));
            btnPDF.setOnClickListener(v -> seleccionarUbicacionPDF(calendario));
            btnEditar.setOnClickListener(v -> mostrarDialogCalendario(calendarioId));
            btnEliminar.setOnClickListener(v -> new AlertDialog.Builder(getContext())
                    .setTitle("Eliminar Calendario").setMessage("¿Eliminar este calendario?")
                    .setPositiveButton("Eliminar", (dialog, which) -> {
                        if (fileManager.eliminarCalendario(alumnoId)) {
                            cargarCalendarios();
                            Toast.makeText(getContext(), "Calendario eliminado", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), "Error al eliminar", Toast.LENGTH_SHORT).show();
                        }
                    }).setNegativeButton("Cancelar", null).show());
        } catch (JSONException e) { e.printStackTrace(); }
        layoutCalendarios.addView(cardView);
    }

    private String calcularProximaFecha(String[] fechas, String[] etiquetas) {
        SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        Calendar hoy = Calendar.getInstance();
        hoy.set(Calendar.HOUR_OF_DAY, 0);
        hoy.set(Calendar.MINUTE, 0);
        hoy.set(Calendar.SECOND, 0);
        hoy.set(Calendar.MILLISECOND, 0);
        for (int i = 0; i < fechas.length; i++) {
            if (!fechas[i].isEmpty()) {
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
            if (!fecha.isEmpty()) {
                completadas++;
            }
        }
        return "Progreso: " + completadas + "/3 fechas configuradas";
    }

    private void seleccionarUbicacionPDF(JSONObject calendario) {
        calendarioPendiente = calendario;
        String alumnoId = calendario.optString("alumnoId", "");
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