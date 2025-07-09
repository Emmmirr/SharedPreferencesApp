package com.example.sharedpreferencesapp;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
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

        // Botones de fechas
        Button btnFechaAnteproyecto = dialogView.findViewById(R.id.btnFechaAnteproyecto);
        Button btnFechaViabilidad = dialogView.findViewById(R.id.btnFechaViabilidad);
        Button btnFechaModificacion = dialogView.findViewById(R.id.btnFechaModificacion);
        Button btnFechaViabilidadFinal = dialogView.findViewById(R.id.btnFechaViabilidadFinal);
        Button btnFechaInicioResidencia = dialogView.findViewById(R.id.btnFechaInicioResidencia);
        Button btnFechaPrimerSeguimiento = dialogView.findViewById(R.id.btnFechaPrimerSeguimiento);
        Button btnFechaSegundoSeguimiento = dialogView.findViewById(R.id.btnFechaSegundoSeguimiento);
        Button btnFechaEntregaFinal = dialogView.findViewById(R.id.btnFechaEntregaFinal);

        Button btnBorrarFechas = dialogView.findViewById(R.id.btnBorrarFechas);

        // Variables para fechas
        final String[] fechas = new String[8]; // Array para almacenar fechas
        SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        Button[] botonesFecha = {
                btnFechaAnteproyecto, btnFechaViabilidad, btnFechaModificacion,
                btnFechaViabilidadFinal, btnFechaInicioResidencia, btnFechaPrimerSeguimiento,
                btnFechaSegundoSeguimiento, btnFechaEntregaFinal
        };

        // Configurar spinner alumnos
        ArrayList<String> alumnos = new ArrayList<>();
        ArrayList<String> alumnosIds = new ArrayList<>();
        List<JSONObject> alumnosData = fileManager.cargarAlumnos();

        alumnos.add("Selecciona un alumno..."); // Posición 0

        for (JSONObject alumno : alumnosData) {
            try {
                String nombre = alumno.optString("nombre", "");
                String numControl = alumno.optString("numControl", "");
                String id = alumno.getString("id");
                alumnos.add(nombre + " (" + numControl + ")");
                alumnosIds.add(id);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        if (alumnosIds.isEmpty()) {
            alumnos.clear();
            alumnos.add("No hay alumnos registrados");
            alumnosIds.add("");
        }

        ArrayAdapter<String> alumnosAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, alumnos);
        alumnosAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAlumno.setAdapter(alumnosAdapter);

        // Configurar listeners para fechas
        btnFechaAnteproyecto.setOnClickListener(v -> mostrarDatePicker(btnFechaAnteproyecto, null, fechas, 0, formatoFecha));
        btnFechaViabilidad.setOnClickListener(v -> mostrarDatePicker(btnFechaViabilidad, fechas[0], fechas, 1, formatoFecha));
        btnFechaModificacion.setOnClickListener(v -> mostrarDatePicker(btnFechaModificacion, fechas[1], fechas, 2, formatoFecha));
        btnFechaViabilidadFinal.setOnClickListener(v -> mostrarDatePicker(btnFechaViabilidadFinal, fechas[2], fechas, 3, formatoFecha));
        btnFechaInicioResidencia.setOnClickListener(v -> mostrarDatePicker(btnFechaInicioResidencia, fechas[3], fechas, 4, formatoFecha));
        btnFechaPrimerSeguimiento.setOnClickListener(v -> mostrarDatePicker(btnFechaPrimerSeguimiento, fechas[4], fechas, 5, formatoFecha));
        btnFechaSegundoSeguimiento.setOnClickListener(v -> mostrarDatePicker(btnFechaSegundoSeguimiento, fechas[5], fechas, 6, formatoFecha));
        btnFechaEntregaFinal.setOnClickListener(v -> mostrarDatePicker(btnFechaEntregaFinal, fechas[6], fechas, 7, formatoFecha));

        // Configurar listener para borrar fechas
        btnBorrarFechas.setOnClickListener(v -> {
            new AlertDialog.Builder(getContext())
                    .setTitle("Borrar Fechas")
                    .setMessage("¿Estás seguro que quieres eliminar todas las fechas?")
                    .setIcon(R.drawable.libro)
                    .setPositiveButton("Sí, Borrar", (dialog, which) -> {
                        borrarTodasLasFechas(fechas, botonesFecha);
                        Toast.makeText(getContext(), "Todas las fechas han sido eliminadas", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });

        // Si es edición, cargar datos
        if (calendarioId != null) {
            tvTitulo.setText("Editar Calendario");
            cargarDatosCalendario(calendarioId, spinnerAlumno, alumnosIds, fechas,
                    btnFechaAnteproyecto, btnFechaViabilidad, btnFechaModificacion,
                    btnFechaViabilidadFinal, btnFechaInicioResidencia, btnFechaPrimerSeguimiento,
                    btnFechaSegundoSeguimiento, btnFechaEntregaFinal);
        }

        AlertDialog dialog = builder.create();

        dialogView.findViewById(R.id.btnGuardar).setOnClickListener(v -> {
            if (spinnerAlumno.getSelectedItemPosition() == 0) {
                Toast.makeText(getContext(), "Selecciona un alumno", Toast.LENGTH_SHORT).show();
                return;
            }

            if (alumnosIds.isEmpty()) {
                Toast.makeText(getContext(), "No hay alumnos registrados", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                String alumnoId = alumnosIds.get(spinnerAlumno.getSelectedItemPosition() - 1);
                JSONObject calendario = new JSONObject();

                String finalCalendarioId = calendarioId;
                if (finalCalendarioId == null) {
                    finalCalendarioId = "calendario_" + alumnoId;
                }

                calendario.put("id", finalCalendarioId);
                calendario.put("alumnoId", alumnoId);
                calendario.put("fechaAnteproyecto", fechas[0] != null ? fechas[0] : "");
                calendario.put("fechaViabilidad", fechas[1] != null ? fechas[1] : "");
                calendario.put("fechaModificacion", fechas[2] != null ? fechas[2] : "");
                calendario.put("fechaViabilidadFinal", fechas[3] != null ? fechas[3] : "");
                calendario.put("fechaInicioResidencia", fechas[4] != null ? fechas[4] : "");
                calendario.put("fechaPrimerSeguimiento", fechas[5] != null ? fechas[5] : "");
                calendario.put("fechaSegundoSeguimiento", fechas[6] != null ? fechas[6] : "");
                calendario.put("fechaEntregaFinal", fechas[7] != null ? fechas[7] : "");
                calendario.put("fechaCreacion", formatoFecha.format(new Date()));

                boolean exito = fileManager.guardarCalendario(calendario);

                if (exito) {
                    cargarCalendarios();
                    dialog.dismiss();
                    String mensaje = (calendarioId == null) ? "Calendario agregado exitosamente" : "Calendario actualizado exitosamente";
                    Toast.makeText(getContext(), mensaje, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Error al guardar el calendario", Toast.LENGTH_SHORT).show();
                }

            } catch (JSONException e) {
                e.printStackTrace();
                Toast.makeText(getContext(), "Error al procesar los datos", Toast.LENGTH_SHORT).show();
            } catch (IndexOutOfBoundsException e) {
                e.printStackTrace();
                Toast.makeText(getContext(), "Error: Selección inválida", Toast.LENGTH_SHORT).show();
            }
        });

        dialogView.findViewById(R.id.btnCancelar).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void borrarTodasLasFechas(String[] fechas, Button[] botones) {
        // Limpiar el array de fechas
        for (int i = 0; i < fechas.length; i++) {
            fechas[i] = null;
        }

        // Resetear todos los botones a su estado original
        for (Button boton : botones) {
            boton.setText("Seleccionar");
            boton.setBackgroundColor(0xFFE2E8F0); // Color gris original
            boton.setTextColor(0xFF4299E1); // Color azul original
        }
    }

    // ⬅️ MÉTODO ACTUALIZADO CON VALIDACIÓN DE DÍAS LABORALES
    private void mostrarDatePicker(Button boton, String fechaMinima, String[] fechas, int indice, SimpleDateFormat formatoFecha) {
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
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(year, month, dayOfMonth);

                    // ⬅️ VALIDAR QUE NO SEA FIN DE SEMANA
                    if (esFindeSemana(selectedDate)) {
                        Toast.makeText(getContext(), "No se pueden seleccionar sábados ni domingos (días no laborales)", Toast.LENGTH_LONG).show();
                        return;
                    }

                    // Validar que sea posterior a la fecha mínima
                    if (validarFecha(selectedDate.getTime(), fechaMinima, formatoFecha)) {
                        String fechaSeleccionada = formatoFecha.format(selectedDate.getTime());
                        fechas[indice] = fechaSeleccionada;
                        boton.setText(fechaSeleccionada);
                        boton.setBackgroundColor(0xFF48BB78);
                        boton.setTextColor(0xFFFFFFFF);
                    } else {
                        Toast.makeText(getContext(), "La fecha debe ser posterior a la fecha anterior", Toast.LENGTH_SHORT).show();
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        if (fechaMinima != null && !fechaMinima.isEmpty()) {
            try {
                Date fechaMin = formatoFecha.parse(fechaMinima);
                if (fechaMin != null) {
                    Calendar calMin = Calendar.getInstance();
                    calMin.setTime(fechaMin);
                    calMin.add(Calendar.DAY_OF_MONTH, 1);
                    datePickerDialog.getDatePicker().setMinDate(calMin.getTimeInMillis());
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        datePickerDialog.show();
    }

    // VALIDAR DÍAS DE FIN DE SEMANA
    private boolean esFindeSemana(Calendar fecha) {
        int diaSemana = fecha.get(Calendar.DAY_OF_WEEK);
        // Calendar.SATURDAY = 7, Calendar.SUNDAY = 1
        return diaSemana == Calendar.SATURDAY || diaSemana == Calendar.SUNDAY;
    }

    // VALIDAR FECHAS LABORALES
    private boolean validarFecha(Date fechaSeleccionada, String fechaMinima, SimpleDateFormat formatoFecha) {
        // Primero validar que no sea fin de semana
        Calendar calFecha = Calendar.getInstance();
        calFecha.setTime(fechaSeleccionada);

        if (esFindeSemana(calFecha)) {
            return false;
        }

        // Luego validar que sea posterior a la fecha mínima
        if (fechaMinima == null || fechaMinima.isEmpty()) {
            return true;
        }

        try {
            Date fechaMin = formatoFecha.parse(fechaMinima);
            return fechaSeleccionada.after(fechaMin);
        } catch (ParseException e) {
            e.printStackTrace();
            return true;
        }
    }

    // OBTENER EL SIGUIENTE DÍA LABORAL
    private Calendar obtenerSiguienteDiaLaboral(Calendar fecha) {
        Calendar siguienteDia = (Calendar) fecha.clone();

        do {
            siguienteDia.add(Calendar.DAY_OF_MONTH, 1);
        } while (esFindeSemana(siguienteDia));

        return siguienteDia;
    }

    private void cargarDatosCalendario(String calendarioId, Spinner spinnerAlumno, ArrayList<String> alumnosIds,
                                       String[] fechas, Button... botones) {
        JSONObject calendario = null;

        List<JSONObject> calendarios = fileManager.cargarCalendarios();
        for (JSONObject cal : calendarios) {
            if (cal.optString("id", "").equals(calendarioId)) {
                calendario = cal;
                break;
            }
        }

        if (calendario != null) {
            try {
                String alumnoId = calendario.optString("alumnoId", "");

                for (int i = 0; i < alumnosIds.size(); i++) {
                    if (alumnosIds.get(i).equals(alumnoId)) {
                        spinnerAlumno.setSelection(i + 1);
                        break;
                    }
                }

                String[] camposFecha = {"fechaAnteproyecto", "fechaViabilidad", "fechaModificacion",
                        "fechaViabilidadFinal", "fechaInicioResidencia", "fechaPrimerSeguimiento",
                        "fechaSegundoSeguimiento", "fechaEntregaFinal"};

                for (int i = 0; i < camposFecha.length && i < botones.length; i++) {
                    String fecha = calendario.optString(camposFecha[i], "");
                    if (!fecha.isEmpty()) {
                        fechas[i] = fecha;
                        botones[i].setText(fecha);
                        botones[i].setBackgroundColor(0xFF48BB78);
                        botones[i].setTextColor(0xFFFFFFFF);
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void cargarCalendarios() {
        layoutCalendarios.removeAllViews();

        List<JSONObject> calendarios = fileManager.cargarCalendarios();

        Log.d("CargarCalendarios", "Total calendarios encontrados: " + calendarios.size());

        if (calendarios.isEmpty()) {
            tvNoCalendarios.setVisibility(View.VISIBLE);
        } else {
            tvNoCalendarios.setVisibility(View.GONE);

            for (int i = 0; i < calendarios.size(); i++) {
                JSONObject calendario = calendarios.get(i);
                Log.d("CargarCalendarios", "Procesando calendario " + (i+1) + ": " + calendario.optString("id", "sin_id"));
                crearCardCalendario(calendario);
            }
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

            Log.d("CalendarioCard", "Creando card para calendario: " + calendarioId + ", alumno: " + alumnoId);

            JSONObject alumno = fileManager.buscarAlumnoPorId(alumnoId);
            String nombreAlumno = "Sin alumno";
            String numControl = "";
            String carrera = "";

            if (alumno != null) {
                nombreAlumno = limpiarTextoVista(alumno.optString("nombre", "Sin alumno"));
                numControl = limpiarTextoVista(alumno.optString("numControl", ""));
                carrera = limpiarTextoVista(alumno.optString("carrera", ""));
            }

            String[] fechasArray = {
                    calendario.optString("fechaAnteproyecto", ""),
                    calendario.optString("fechaViabilidad", ""),
                    calendario.optString("fechaModificacion", ""),
                    calendario.optString("fechaViabilidadFinal", ""),
                    calendario.optString("fechaInicioResidencia", ""),
                    calendario.optString("fechaPrimerSeguimiento", ""),
                    calendario.optString("fechaSegundoSeguimiento", ""),
                    calendario.optString("fechaEntregaFinal", "")
            };

            String[] etiquetas = {
                    "Anteproyecto", "Viabilidad", "Modificación", "Viabilidad Final",
                    "Inicio", "1er Seguimiento", "2do Seguimiento", "Entrega Final"
            };

            String proximaFecha = calcularProximaFecha(fechasArray, etiquetas);
            String progreso = calcularProgreso(fechasArray);

            tvNombreAlumno.setText(nombreAlumno);
            tvNumControl.setText("No. Control: " + numControl);
            tvCarrera.setText("Carrera: " + carrera);
            tvProximaFecha.setText(proximaFecha);
            tvProgreso.setText(progreso);
            tvFechaCreacion.setText("Creado: " + limpiarTextoVista(calendario.optString("fechaCreacion", "")));

            btnPDF.setOnClickListener(v -> seleccionarUbicacionPDF(calendario));

            btnEditar.setOnClickListener(v -> mostrarDialogCalendario(calendarioId));

            btnEliminar.setOnClickListener(v -> {
                new AlertDialog.Builder(getContext())
                        .setTitle("Eliminar Calendario")
                        .setMessage("¿Eliminar este calendario?")
                        .setPositiveButton("Eliminar", (dialog, which) -> {
                            boolean exito = fileManager.eliminarCalendario(alumnoId);
                            if (exito) {
                                cargarCalendarios();
                                Toast.makeText(getContext(), "Calendario eliminado", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getContext(), "Error al eliminar el calendario", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("Cancelar", null)
                        .show();
            });

        } catch (JSONException e) {
            e.printStackTrace();
            Log.e("CalendarioCard", "Error creando card de calendario", e);
        }

        layoutCalendarios.addView(cardView);
    }

    private String calcularProximaFecha(String[] fechas, String[] etiquetas) {
        SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        Date hoy = new Date();

        for (int i = 0; i < fechas.length; i++) {
            if (!fechas[i].isEmpty()) {
                try {
                    Date fecha = formatoFecha.parse(fechas[i]);
                    if (fecha != null && fecha.after(hoy)) {
                        return "Próximo: " + limpiarTextoVista(etiquetas[i]) + " (" + fechas[i] + ")";
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
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
        return "Progreso: " + completadas + "/8 fechas configuradas";
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
                        String mensaje = "PDF de calendario guardado exitosamente";
                        Toast.makeText(getContext(), mensaje, Toast.LENGTH_LONG).show();

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
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private String limpiarTextoVista(String texto) {
        if (texto == null || texto.isEmpty()) {
            return "";
        }

        try {
            byte[] bytes = texto.getBytes("UTF-8");
            String textoLimpio = new String(bytes, "UTF-8");

            textoLimpio = textoLimpio.replace("Ã¡", "á")
                    .replace("Ã©", "é")
                    .replace("Ã­", "í")
                    .replace("Ã³", "ó")
                    .replace("Ãº", "ú")
                    .replace("Ã±", "ñ")
                    .replace("Ã¼", "ü")
                    .replace("Ã‰", "É")
                    .replace("Ã", "Á")
                    .replace("\u00C3\u201D", "Ó")
                    .replace("Ãš", "Ú")
                    .replace("\u00C3\u2018", "Ñ")
                    .replace("\u2019", "'")
                    .replace("\u201C", "\"")
                    .replace("\u201D", "\"")
                    .replace("\u2013", "-")
                    .replace("Â", "")
                    .replace("\u00A0", " ")
                    .replace("\u00C3\u00A1", "á")
                    .replace("\u00C3\u00A9", "é")
                    .replace("\u00C3\u00AD", "í")
                    .replace("\u00C3\u00B3", "ó")
                    .replace("\u00C3\u00FA", "ú")
                    .replace("\u00C3\u00B1", "ñ")
                    .replace("\u00C3\u00BC", "ü");

            return textoLimpio;
        } catch (Exception e) {
            Log.w("LimpiarTextoVista", "Error procesando texto: " + texto, e);
            return texto;
        }
    }
}