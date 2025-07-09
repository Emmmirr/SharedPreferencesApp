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

    // Variables para el calendario actual
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

        // ⬅️ ICONOS DE CALENDARIO
        ImageView btnCalendarioAnteproyecto = dialogView.findViewById(R.id.btnCalendarioAnteproyecto);
        ImageView btnCalendarioViabilidad = dialogView.findViewById(R.id.btnCalendarioViabilidad);
        ImageView btnCalendarioModificacion = dialogView.findViewById(R.id.btnCalendarioModificacion);
        ImageView btnCalendarioViabilidadFinal = dialogView.findViewById(R.id.btnCalendarioViabilidadFinal);
        ImageView btnCalendarioInicioResidencia = dialogView.findViewById(R.id.btnCalendarioInicioResidencia);
        ImageView btnCalendarioPrimerSeguimiento = dialogView.findViewById(R.id.btnCalendarioPrimerSeguimiento);
        ImageView btnCalendarioSegundoSeguimiento = dialogView.findViewById(R.id.btnCalendarioSegundoSeguimiento);
        ImageView btnCalendarioEntregaFinal = dialogView.findViewById(R.id.btnCalendarioEntregaFinal);

        // ⬅️ ICONOS GUARDAR INDIVIDUALES
        ImageView btnGuardarAnteproyecto = dialogView.findViewById(R.id.btnGuardarAnteproyecto);
        ImageView btnGuardarViabilidad = dialogView.findViewById(R.id.btnGuardarViabilidad);
        ImageView btnGuardarModificacion = dialogView.findViewById(R.id.btnGuardarModificacion);
        ImageView btnGuardarViabilidadFinal = dialogView.findViewById(R.id.btnGuardarViabilidadFinal);
        ImageView btnGuardarInicioResidencia = dialogView.findViewById(R.id.btnGuardarInicioResidencia);
        ImageView btnGuardarPrimerSeguimiento = dialogView.findViewById(R.id.btnGuardarPrimerSeguimiento);
        ImageView btnGuardarSegundoSeguimiento = dialogView.findViewById(R.id.btnGuardarSegundoSeguimiento);
        ImageView btnGuardarEntregaFinal = dialogView.findViewById(R.id.btnGuardarEntregaFinal);

        // TextViews para mostrar las fechas
        TextView tvFechaAnteproyecto = dialogView.findViewById(R.id.tvFechaAnteproyecto);
        TextView tvFechaViabilidad = dialogView.findViewById(R.id.tvFechaViabilidad);
        TextView tvFechaModificacion = dialogView.findViewById(R.id.tvFechaModificacion);
        TextView tvFechaViabilidadFinal = dialogView.findViewById(R.id.tvFechaViabilidadFinal);
        TextView tvFechaInicioResidencia = dialogView.findViewById(R.id.tvFechaInicioResidencia);
        TextView tvFechaPrimerSeguimiento = dialogView.findViewById(R.id.tvFechaPrimerSeguimiento);
        TextView tvFechaSegundoSeguimiento = dialogView.findViewById(R.id.tvFechaSegundoSeguimiento);
        TextView tvFechaEntregaFinal = dialogView.findViewById(R.id.tvFechaEntregaFinal);

        Button btnBorrarFechas = dialogView.findViewById(R.id.btnBorrarFechas);

        // Variables para fechas temporales
        final String[] fechasTemp = new String[8];
        SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        // Arrays para manejar elementos
        TextView[] textViewsFecha = {
                tvFechaAnteproyecto, tvFechaViabilidad, tvFechaModificacion,
                tvFechaViabilidadFinal, tvFechaInicioResidencia, tvFechaPrimerSeguimiento,
                tvFechaSegundoSeguimiento, tvFechaEntregaFinal
        };

        ImageView[] iconosCalendario = {
                btnCalendarioAnteproyecto, btnCalendarioViabilidad, btnCalendarioModificacion,
                btnCalendarioViabilidadFinal, btnCalendarioInicioResidencia, btnCalendarioPrimerSeguimiento,
                btnCalendarioSegundoSeguimiento, btnCalendarioEntregaFinal
        };

        // ⬅️ CAMBIO: AHORA SON ImageView EN LUGAR DE Button
        ImageView[] iconosGuardar = {
                btnGuardarAnteproyecto, btnGuardarViabilidad, btnGuardarModificacion,
                btnGuardarViabilidadFinal, btnGuardarInicioResidencia, btnGuardarPrimerSeguimiento,
                btnGuardarSegundoSeguimiento, btnGuardarEntregaFinal
        };

        String[] nombresCampos = {
                "fechaAnteproyecto", "fechaViabilidad", "fechaModificacion",
                "fechaViabilidadFinal", "fechaInicioResidencia", "fechaPrimerSeguimiento",
                "fechaSegundoSeguimiento", "fechaEntregaFinal"
        };

        // Configurar spinner alumnos
        ArrayList<String> alumnos = new ArrayList<>();
        ArrayList<String> alumnosIds = new ArrayList<>();
        List<JSONObject> alumnosData = fileManager.cargarAlumnos();

        alumnos.add("Selecciona un alumno...");

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

        // ⬅️ CONFIGURAR LISTENERS PARA ICONOS DE CALENDARIO
        for (int i = 0; i < iconosCalendario.length; i++) {
            final int indice = i;
            iconosCalendario[i].setOnClickListener(v -> {
                if (spinnerAlumno.getSelectedItemPosition() == 0) {
                    Toast.makeText(getContext(), "Primero selecciona un alumno", Toast.LENGTH_SHORT).show();
                    return;
                }

                String fechaMinima = indice > 0 ? fechasTemp[indice - 1] : null;
                mostrarDatePickerIndividualIcono(indice, fechasTemp, textViewsFecha, iconosGuardar, formatoFecha, fechaMinima);
            });
        }

        // ⬅️ CONFIGURAR LISTENERS PARA ICONOS GUARDAR INDIVIDUALES
        for (int i = 0; i < iconosGuardar.length; i++) {
            final int indice = i;
            iconosGuardar[i].setOnClickListener(v -> {
                if (spinnerAlumno.getSelectedItemPosition() == 0) {
                    Toast.makeText(getContext(), "Primero selecciona un alumno", Toast.LENGTH_SHORT).show();
                    return;
                }

                guardarFechaIndividualIcono(spinnerAlumno, alumnosIds, fechasTemp, indice, nombresCampos[indice],
                        textViewsFecha[indice], iconosGuardar[indice], formatoFecha);
            });
        }

        // Configurar listener para borrar fechas
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
                    .setNegativeButton("Cancelar", null)
                    .show();
        });

        // ⬅️ CONFIGURAR LISTENER DEL SPINNER
        spinnerAlumno.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (position > 0 && !alumnosIds.isEmpty()) {
                    alumnoActualId = alumnosIds.get(position - 1);
                    calendarioActualId = "calendario_" + alumnoActualId;
                    cargarDatosCalendarioExistenteIcono(fechasTemp, textViewsFecha, iconosGuardar);
                } else {
                    alumnoActualId = null;
                    calendarioActualId = null;
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                alumnoActualId = null;
                calendarioActualId = null;
            }
        });

        // Si es edición, cargar datos
        if (calendarioId != null) {
            tvTitulo.setText("Editar Calendario");
            cargarDatosCalendarioParaEdicionIcono(calendarioId, spinnerAlumno, alumnosIds, fechasTemp, textViewsFecha, iconosGuardar);
        }

        AlertDialog dialog = builder.create();

        dialogView.findViewById(R.id.btnCerrar).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    // ⬅️ MÉTODO ACTUALIZADO PARA ICONOS
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
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(year, month, dayOfMonth);

                    // Validar que no sea fin de semana
                    if (esFindeSemana(selectedDate)) {
                        Toast.makeText(getContext(), "No se pueden seleccionar sábados ni domingos (días no laborales)", Toast.LENGTH_LONG).show();
                        return;
                    }

                    // Validar que sea posterior a la fecha mínima
                    if (validarFecha(selectedDate.getTime(), fechaMinima, formatoFecha)) {
                        String fechaSeleccionada = formatoFecha.format(selectedDate.getTime());
                        fechasTemp[indice] = fechaSeleccionada;

                        // ⬅️ ACTUALIZAR TextView Y HABILITAR ICONO GUARDAR
                        textViews[indice].setText("Fecha: " + fechaSeleccionada);
                        textViews[indice].setTextColor(0xFF1976D2);
                        iconosGuardar[indice].setEnabled(true);
                        iconosGuardar[indice].setAlpha(1.0f); // Opacidad completa
                        iconosGuardar[indice].setColorFilter(0xFF1976D2); // Azul cuando está habilitado
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

    // ⬅️ MÉTODO PARA GUARDAR CON ICONOS
    private void guardarFechaIndividualIcono(Spinner spinnerAlumno, ArrayList<String> alumnosIds, String[] fechasTemp,
                                             int indice, String nombreCampo, TextView textView, ImageView iconoGuardar,
                                             SimpleDateFormat formatoFecha) {
        try {
            String alumnoId = alumnosIds.get(spinnerAlumno.getSelectedItemPosition() - 1);
            String calendarioId = "calendario_" + alumnoId;

            // Buscar o crear calendario
            JSONObject calendario = fileManager.buscarCalendarioPorId(calendarioId);
            if (calendario == null) {
                calendario = new JSONObject();
                calendario.put("id", calendarioId);
                calendario.put("alumnoId", alumnoId);
                calendario.put("fechaCreacion", formatoFecha.format(new Date()));

                // Inicializar todas las fechas vacías
                String[] campos = {"fechaAnteproyecto", "fechaViabilidad", "fechaModificacion",
                        "fechaViabilidadFinal", "fechaInicioResidencia", "fechaPrimerSeguimiento",
                        "fechaSegundoSeguimiento", "fechaEntregaFinal"};
                for (String campo : campos) {
                    calendario.put(campo, "");
                }
            }

            // Actualizar solo el campo específico
            calendario.put(nombreCampo, fechasTemp[indice]);

            boolean exito = fileManager.guardarCalendario(calendario);

            if (exito) {
                // ⬅️ CAMBIAR A VERDE Y MOSTRAR CHECK
                textView.setTextColor(0xFF2E7D32); // Verde cuando está guardado
                iconoGuardar.setEnabled(false);
                iconoGuardar.setColorFilter(0xFF4CAF50); // Verde
                iconoGuardar.setImageResource(android.R.drawable.ic_menu_save); // Cambiar a check

                // Restaurar después de 2 segundos
                iconoGuardar.postDelayed(() -> {
                    iconoGuardar.setImageResource(android.R.drawable.ic_menu_save);
                    iconoGuardar.setColorFilter(0xFF4CAF50); // Mantener verde
                }, 2000);

                Toast.makeText(getContext(), "Fecha guardada exitosamente", Toast.LENGTH_SHORT).show();
                cargarCalendarios(); // Actualizar la lista
            } else {
                Toast.makeText(getContext(), "Error al guardar la fecha", Toast.LENGTH_SHORT).show();
            }

        } catch (JSONException | IndexOutOfBoundsException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error al procesar los datos", Toast.LENGTH_SHORT).show();
        }
    }

    // ⬅️ MÉTODO PARA CARGAR DATOS CON ICONOS
    private void cargarDatosCalendarioExistenteIcono(String[] fechasTemp, TextView[] textViews, ImageView[] iconosGuardar) {
        if (calendarioActualId == null) return;

        JSONObject calendario = fileManager.buscarCalendarioPorId(calendarioActualId);
        if (calendario != null) {
            String[] campos = {"fechaAnteproyecto", "fechaViabilidad", "fechaModificacion",
                    "fechaViabilidadFinal", "fechaInicioResidencia", "fechaPrimerSeguimiento",
                    "fechaSegundoSeguimiento", "fechaEntregaFinal"};

            for (int i = 0; i < campos.length; i++) {
                String fecha = calendario.optString(campos[i], "");
                if (!fecha.isEmpty()) {
                    fechasTemp[i] = fecha;
                    textViews[i].setText("Fecha: " + fecha);
                    textViews[i].setTextColor(0xFF2E7D32); // Verde para fechas guardadas
                    iconosGuardar[i].setEnabled(false);
                    iconosGuardar[i].setColorFilter(0xFF4CAF50); // Verde
                    iconosGuardar[i].setAlpha(1.0f);
                }
            }
        }
    }

    // ⬅️ MÉTODO PARA BORRAR FECHAS CON ICONOS
    private void borrarTodasLasFechasIndividualIcono(String[] fechasTemp, TextView[] textViews, ImageView[] iconosGuardar) {
        if (calendarioActualId == null) return;

        try {
            JSONObject calendario = fileManager.buscarCalendarioPorId(calendarioActualId);
            if (calendario != null) {
                // Limpiar todas las fechas en el calendario
                String[] campos = {"fechaAnteproyecto", "fechaViabilidad", "fechaModificacion",
                        "fechaViabilidadFinal", "fechaInicioResidencia", "fechaPrimerSeguimiento",
                        "fechaSegundoSeguimiento", "fechaEntregaFinal"};

                for (String campo : campos) {
                    calendario.put(campo, "");
                }

                fileManager.guardarCalendario(calendario);
            }

            // Limpiar arrays temporales y UI
            for (int i = 0; i < fechasTemp.length; i++) {
                fechasTemp[i] = null;
                textViews[i].setText("Fecha: Sin asignar");
                textViews[i].setTextColor(0xFF4299E1);
                iconosGuardar[i].setEnabled(false);
                iconosGuardar[i].setColorFilter(0xFF1976D2);
                iconosGuardar[i].setAlpha(0.5f);
            }

            cargarCalendarios(); // Actualizar lista

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // Método para cargar datos en modo edición con iconos
    private void cargarDatosCalendarioParaEdicionIcono(String calendarioId, Spinner spinnerAlumno, ArrayList<String> alumnosIds,
                                                       String[] fechasTemp, TextView[] textViews, ImageView[] iconosGuardar) {
        JSONObject calendario = fileManager.buscarCalendarioPorId(calendarioId);
        if (calendario != null) {
            try {
                String alumnoId = calendario.optString("alumnoId", "");
                alumnoActualId = alumnoId;
                calendarioActualId = calendarioId;

                // Seleccionar alumno en spinner
                for (int i = 0; i < alumnosIds.size(); i++) {
                    if (alumnosIds.get(i).equals(alumnoId)) {
                        spinnerAlumno.setSelection(i + 1);
                        break;
                    }
                }

                // Cargar fechas
                cargarDatosCalendarioExistenteIcono(fechasTemp, textViews, iconosGuardar);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //DATEPICKER INDIVIDUAL
    private void mostrarDatePickerIndividual(int indice, String[] fechasTemp, TextView[] textViews,
                                             Button[] botonesGuardar, SimpleDateFormat formatoFecha, String fechaMinima) {
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

                    // Validar que no sea fin de semana
                    if (esFindeSemana(selectedDate)) {
                        Toast.makeText(getContext(), "No se pueden seleccionar sábados ni domingos (días no laborales)", Toast.LENGTH_LONG).show();
                        return;
                    }

                    // Validar que sea posterior a la fecha mínima
                    if (validarFecha(selectedDate.getTime(), fechaMinima, formatoFecha)) {
                        String fechaSeleccionada = formatoFecha.format(selectedDate.getTime());
                        fechasTemp[indice] = fechaSeleccionada;

                        // Actualizar TextView y habilitar botón guardar
                        textViews[indice].setText("Fecha: " + fechaSeleccionada);
                        textViews[indice].setTextColor(0xFF1976D2);
                        botonesGuardar[indice].setEnabled(true);
                        botonesGuardar[indice].setBackgroundColor(0xFF1976D2);
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

    //FECHA INDIVIDUAL
    private void guardarFechaIndividual(Spinner spinnerAlumno, ArrayList<String> alumnosIds, String[] fechasTemp,
                                        int indice, String nombreCampo, TextView textView, Button botonGuardar,
                                        SimpleDateFormat formatoFecha) {
        try {
            String alumnoId = alumnosIds.get(spinnerAlumno.getSelectedItemPosition() - 1);
            String calendarioId = "calendario_" + alumnoId;

            // Buscar o crear calendario
            JSONObject calendario = fileManager.buscarCalendarioPorId(calendarioId);
            if (calendario == null) {
                calendario = new JSONObject();
                calendario.put("id", calendarioId);
                calendario.put("alumnoId", alumnoId);
                calendario.put("fechaCreacion", formatoFecha.format(new Date()));

                // Inicializar todas las fechas vacías
                String[] campos = {"fechaAnteproyecto", "fechaViabilidad", "fechaModificacion",
                        "fechaViabilidadFinal", "fechaInicioResidencia", "fechaPrimerSeguimiento",
                        "fechaSegundoSeguimiento", "fechaEntregaFinal"};
                for (String campo : campos) {
                    calendario.put(campo, "");
                }
            }

            // Actualizar solo el campo específico
            calendario.put(nombreCampo, fechasTemp[indice]);

            boolean exito = fileManager.guardarCalendario(calendario);

            if (exito) {
                textView.setTextColor(0xFF2E7D32); // Verde cuando está guardado
                botonGuardar.setEnabled(false);
                botonGuardar.setBackgroundColor(0xFF4CAF50); // Verde
                botonGuardar.setText("✓");

                // Restaurar después de 2 segundos
                botonGuardar.postDelayed(() -> {
                    botonGuardar.setText("Guardar");
                    botonGuardar.setBackgroundColor(0xFF1976D2);
                }, 2000);

                Toast.makeText(getContext(), "Fecha guardada exitosamente", Toast.LENGTH_SHORT).show();
                cargarCalendarios(); // Actualizar la lista
            } else {
                Toast.makeText(getContext(), "Error al guardar la fecha", Toast.LENGTH_SHORT).show();
            }

        } catch (JSONException | IndexOutOfBoundsException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error al procesar los datos", Toast.LENGTH_SHORT).show();
        }
    }

    //CARGAR DATOS DE CALENDARIO EXISTENTE
    private void cargarDatosCalendarioExistente(String[] fechasTemp, TextView[] textViews, Button[] botonesGuardar) {
        if (calendarioActualId == null) return;

        JSONObject calendario = fileManager.buscarCalendarioPorId(calendarioActualId);
        if (calendario != null) {
            String[] campos = {"fechaAnteproyecto", "fechaViabilidad", "fechaModificacion",
                    "fechaViabilidadFinal", "fechaInicioResidencia", "fechaPrimerSeguimiento",
                    "fechaSegundoSeguimiento", "fechaEntregaFinal"};

            for (int i = 0; i < campos.length; i++) {
                String fecha = calendario.optString(campos[i], "");
                if (!fecha.isEmpty()) {
                    fechasTemp[i] = fecha;
                    textViews[i].setText("Fecha: " + fecha);
                    textViews[i].setTextColor(0xFF2E7D32); // Verde para fechas guardadas
                    botonesGuardar[i].setEnabled(false);
                    botonesGuardar[i].setBackgroundColor(0xFF4CAF50);
                }
            }
        }
    }

    //BORRAR TODAS LAS FECHAS INDIVIDUAL
    private void borrarTodasLasFechasIndividual(String[] fechasTemp, TextView[] textViews, Button[] botonesGuardar) {
        if (calendarioActualId == null) return;

        try {
            JSONObject calendario = fileManager.buscarCalendarioPorId(calendarioActualId);
            if (calendario != null) {// Limpiar todas las fechas en el calendario
                String[] campos = {"fechaAnteproyecto", "fechaViabilidad", "fechaModificacion",
                        "fechaViabilidadFinal", "fechaInicioResidencia", "fechaPrimerSeguimiento",
                        "fechaSegundoSeguimiento", "fechaEntregaFinal"};

                for (String campo : campos) {
                    calendario.put(campo, "");
                }

                fileManager.guardarCalendario(calendario);
            }

            // Limpiar arrays temporales y UI
            for (int i = 0; i < fechasTemp.length; i++) {
                fechasTemp[i] = null;
                textViews[i].setText("Fecha: Sin asignar");
                textViews[i].setTextColor(0xFF4299E1);
                botonesGuardar[i].setEnabled(false);
                botonesGuardar[i].setBackgroundColor(0xFF1976D2);
                botonesGuardar[i].setText("Guardar");
            }

            cargarCalendarios(); // Actualizar lista

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // Método para cargar datos en modo edición
    private void cargarDatosCalendarioParaEdicion(String calendarioId, Spinner spinnerAlumno, ArrayList<String> alumnosIds,
                                                  String[] fechasTemp, TextView[] textViews, Button[] botonesGuardar) {
        JSONObject calendario = fileManager.buscarCalendarioPorId(calendarioId);
        if (calendario != null) {
            try {
                String alumnoId = calendario.optString("alumnoId", "");
                alumnoActualId = alumnoId;
                calendarioActualId = calendarioId;

                // Seleccionar alumno en spinner
                for (int i = 0; i < alumnosIds.size(); i++) {
                    if (alumnosIds.get(i).equals(alumnoId)) {
                        spinnerAlumno.setSelection(i + 1);
                        break;
                    }
                }

                // Cargar fechas
                cargarDatosCalendarioExistente(fechasTemp, textViews, botonesGuardar);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // VALIDAR DÍAS DE FIN DE SEMANA
    private boolean esFindeSemana(Calendar fecha) {
        int diaSemana = fecha.get(Calendar.DAY_OF_WEEK);
        return diaSemana == Calendar.SATURDAY || diaSemana == Calendar.SUNDAY;
    }

    // VALIDAR FECHAS LABORALES
    private boolean validarFecha(Date fechaSeleccionada, String fechaMinima, SimpleDateFormat formatoFecha) {
        Calendar calFecha = Calendar.getInstance();
        calFecha.setTime(fechaSeleccionada);

        if (esFindeSemana(calFecha)) {
            return false;
        }

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