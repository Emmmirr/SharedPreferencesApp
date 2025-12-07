package com.example.sharedpreferencesapp;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;

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

    private FirebaseManager firebaseManager;
    private FileManager fileManager;
    private AlarmScheduler alarmScheduler;

    private String currentUserId;
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
        // Constructor público vacío requerido
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lista_calendarios, container, false);

        layoutCalendarios = view.findViewById(R.id.layoutCalendarios);
        tvNoCalendarios = view.findViewById(R.id.tvNoCalendarios);
        Button btnAgregar = view.findViewById(R.id.btnAgregarCalendario);

        firebaseManager = new FirebaseManager();
        fileManager = new FileManager(requireContext());
        alarmScheduler = new AlarmScheduler(requireContext());

        btnAgregar.setOnClickListener(v -> mostrarDialogCalendario(null));

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Se verifica si la app tiene permiso para programar alarmas exactas (necesario en Android 12+)
        checkAndRequestExactAlarmPermission();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            this.currentUserId = user.getUid();
            cargarCalendarios();
        } else {
            tvNoCalendarios.setText("Error de sesión. Por favor, inicie sesión de nuevo.");
            tvNoCalendarios.setVisibility(View.VISIBLE);
            layoutCalendarios.setVisibility(View.GONE);
            view.findViewById(R.id.btnAgregarCalendario).setEnabled(false);
        }
    }

    /**
     * Verifica el permiso para programar alarmas exactas. Si no está concedido,
     * muestra un diálogo para guiar al usuario a la pantalla de ajustes.
     */
    private void checkAndRequestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
            if (!alarmManager.canScheduleExactAlarms()) {
                new AlertDialog.Builder(getContext())
                        .setTitle("Permiso Necesario")
                        .setMessage("Para poder enviarte recordatorios de tus fechas de entrega, la aplicación necesita permiso para programar alarmas. Serás dirigido a los ajustes para activarlo.")
                        .setPositiveButton("Ir a Ajustes", (dialog, which) -> {
                            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                            startActivity(intent);
                        })
                        .setNegativeButton("Cancelar", null)
                        .show();
            }
        }
    }

    private void mostrarDialogCalendario(String calendarioId) {
        if (currentUserId == null) {
            Toast.makeText(getContext(), "Error de sesión.", Toast.LENGTH_SHORT).show();
            return;
        }

// Usamos el método para cargar solo los estudiantes aprobados por el maestro actual
        firebaseManager.cargarEstudiantesAprobados(currentUserId, task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                Toast.makeText(getContext(), "Error al cargar alumnos aprobados.", Toast.LENGTH_SHORT).show();
                return;
            }

            ArrayList<String> alumnosDisplay = new ArrayList<>();
            ArrayList<String> alumnosIds = new ArrayList<>();
            alumnosDisplay.add("Selecciona un alumno...");
            alumnosIds.add(null);

            // Iteramos sobre los documentos de UserProfile de los estudiantes
            for (QueryDocumentSnapshot studentDoc : task.getResult()) {
                // Creamos un objeto UserProfile para acceder fácilmente a los datos
                UserProfile estudiante = UserProfile.fromMap(studentDoc.getData());

                String nombre = estudiante.getFullName().isEmpty() ? estudiante.getDisplayName() : estudiante.getFullName();
                String numControl = estudiante.getControlNumber();

                alumnosDisplay.add(nombre + " (" + numControl + ")");
                alumnosIds.add(estudiante.getUserId()); // Usamos el ID de usuario del estudiante
            }

            if (alumnosIds.size() <= 1) {
                Toast.makeText(getContext(), "No tienes alumnos aprobados para asignarles un calendario.", Toast.LENGTH_LONG).show();
                return;
            }

            // El resto del método sigue igual...
            construirYMostrarDialogo(calendarioId, alumnosDisplay, alumnosIds);
        });
    }

    private void construirYMostrarDialogo(String calendarioId, ArrayList<String> alumnosDisplay, ArrayList<String> alumnosIds) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_calendario, null);
        builder.setView(dialogView);

        TextView tvTitulo = dialogView.findViewById(R.id.tvTituloFormulario);
        Spinner spinnerAlumno = dialogView.findViewById(R.id.spinnerAlumno);
        TextView tvNombreAlumnoSeleccionado = dialogView.findViewById(R.id.tvNombreAlumnoSeleccionado);
        LinearLayout btnCalendarioAnteproyecto = dialogView.findViewById(R.id.btnCalendarioAnteproyecto);
        LinearLayout btnCalendarioViabilidad = dialogView.findViewById(R.id.btnCalendarioViabilidad);
        LinearLayout btnCalendarioModificacion = dialogView.findViewById(R.id.btnCalendarioModificacion);
        LinearLayout btnGuardarAnteproyecto = dialogView.findViewById(R.id.btnGuardarAnteproyecto);
        LinearLayout btnGuardarViabilidad = dialogView.findViewById(R.id.btnGuardarViabilidad);
        LinearLayout btnGuardarModificacion = dialogView.findViewById(R.id.btnGuardarModificacion);
        TextView tvFechaAnteproyecto = dialogView.findViewById(R.id.tvFechaAnteproyecto);
        TextView tvFechaViabilidad = dialogView.findViewById(R.id.tvFechaViabilidad);
        TextView tvFechaModificacion = dialogView.findViewById(R.id.tvFechaModificacion);
        TextView tvLabelAnteproyecto = dialogView.findViewById(R.id.tvLabelAnteproyecto);
        TextView tvLabelViabilidad = dialogView.findViewById(R.id.tvLabelViabilidad);
        TextView tvLabelModificacion = dialogView.findViewById(R.id.tvLabelModificacion);
        LinearLayout btnBorrarFechas = dialogView.findViewById(R.id.btnBorrarFechas);

        final String[] fechasTemp = new String[3];
        SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        TextView[] textViewsFecha = {tvFechaAnteproyecto, tvFechaViabilidad, tvFechaModificacion};
        LinearLayout[] iconosCalendario = {btnCalendarioAnteproyecto, btnCalendarioViabilidad, btnCalendarioModificacion};
        LinearLayout[] iconosGuardar = {btnGuardarAnteproyecto, btnGuardarViabilidad, btnGuardarModificacion};
        String[] nombresCamposFecha = {"fechaPrimeraEntrega", "fechaSegundaEntrega", "fechaResultado"};
        TextView[] textViewsLabel = {tvLabelAnteproyecto, tvLabelViabilidad, tvLabelModificacion};
        String[] nombresCamposLabel = {"labelPrimeraEntrega", "labelSegundaEntrega", "labelResultado"};
        String[] defaultLabels = {"1 Entrega evaluacion", "2 Entrega de Evaluacion", "3 Entrega de Resultado"};

        ArrayAdapter<String> alumnosAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, alumnosDisplay);
        alumnosAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAlumno.setAdapter(alumnosAdapter);

        for (int i = 0; i < iconosCalendario.length; i++) {
            final int indice = i;
            iconosCalendario[i].setOnClickListener(v -> {
                if (alumnoActualId == null) {
                    Toast.makeText(getContext(), "Primero selecciona un alumno", Toast.LENGTH_SHORT).show();
                    return;
                }
                // Para la primera fecha (índice 0), no hay fecha mínima
                // Para fechas posteriores, usar la fecha anterior solo si existe y no está vacía
                String fechaMinima = null;
                if (indice > 0 && fechasTemp[indice - 1] != null && !fechasTemp[indice - 1].isEmpty()) {
                    fechaMinima = fechasTemp[indice - 1];
                }
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
                guardarFechaIndividualFirebase(fechasTemp, indice, nombresCamposFecha[indice], formatoFecha, textViewsLabel[indice].getText().toString());
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
                    .setPositiveButton("Sí, Borrar", (dialog, which) -> borrarTodasLasFechasFirebase(fechasTemp, textViewsFecha, iconosGuardar, textViewsLabel, defaultLabels))
                    .setNegativeButton("Cancelar", null).show();
        });
        spinnerAlumno.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    alumnoActualId = alumnosIds.get(position);
                    calendarioActualId = "calendario_" + alumnoActualId;
                    cargarDatosCalendarioExistenteFirebase(fechasTemp, textViewsFecha, iconosGuardar, textViewsLabel, defaultLabels);
                } else {
                    alumnoActualId = null;
                    calendarioActualId = null;
                    cargarDatosCalendarioExistenteFirebase(fechasTemp, textViewsFecha, iconosGuardar, textViewsLabel, defaultLabels);
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

    private void guardarFechaIndividualFirebase(String[] fechasTemp, int indice, String nombreCampoFecha, SimpleDateFormat formatoFecha, String label) {
        if (currentUserId == null || this.alumnoActualId == null) {
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

        firebaseManager.guardarOActualizarCalendario(currentUserId, calendarioId, data,
                () -> {
                    Toast.makeText(getContext(), "Fecha guardada exitosamente", Toast.LENGTH_SHORT).show();

                    String[] camposPdf = {"pdfUriPrimeraEntrega", "pdfUriSegundaEntrega", "pdfUriResultado"};

                    // Se cancela cualquier alarma previa para esta fecha y se programa una nueva.
                    // Se pasa el 'indice' para asegurar un requestCode único.
                    alarmScheduler.cancelAlarmsForDate(calendarioId, indice);
                    alarmScheduler.scheduleAlarmsForDate(currentUserId, calendarioId, fechasTemp[indice], label, camposPdf[indice], indice);

                    cargarCalendarios();
                },
                e -> Toast.makeText(getContext(), "Error al guardar la fecha: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

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

            firebaseManager.guardarOActualizarCalendario(currentUserId, calendarioId, update,
                    () -> {
                        textViewLabel.setText(nuevoLabel);
                        cargarCalendarios();
                        Toast.makeText(getContext(), "Nombre actualizado", Toast.LENGTH_SHORT).show();
                    },
                    e -> Toast.makeText(getContext(), "Error al actualizar.", Toast.LENGTH_SHORT).show()
            );
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void cargarCalendarios() {
        if (currentUserId == null) return;

        layoutCalendarios.removeAllViews();
        tvNoCalendarios.setText("No hay calendarios registrados.");
        tvNoCalendarios.setVisibility(View.VISIBLE);

        firebaseManager.cargarCalendarios(currentUserId, task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                if(task.getResult().isEmpty()){
                    tvNoCalendarios.setVisibility(View.VISIBLE);
                } else {
                    tvNoCalendarios.setVisibility(View.GONE);
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

    private void crearCardCalendario(DocumentSnapshot calendario) {
        if (currentUserId == null || getContext() == null) return;

        View cardView = LayoutInflater.from(getContext()).inflate(R.layout.card_calendario, layoutCalendarios, false);
        TextView tvNombreAlumno = cardView.findViewById(R.id.tvNombreAlumno);
        TextView tvNumControl = cardView.findViewById(R.id.tvNumControl);
        TextView tvCarrera = cardView.findViewById(R.id.tvCarrera);
        TextView tvProximaFecha = cardView.findViewById(R.id.tvProximaFecha);
        TextView tvProgreso = cardView.findViewById(R.id.tvProgreso);
        TextView tvFechaCreacion = cardView.findViewById(R.id.tvFechaCreacion);
        LinearLayout btnPDF = cardView.findViewById(R.id.btnPDFCalendario);
        LinearLayout btnEditar = cardView.findViewById(R.id.btnEditarCalendario);
        LinearLayout btnEliminar = cardView.findViewById(R.id.btnEliminarCalendario);

        String calendarioId = calendario.getId();
        String alumnoId = calendario.getString("alumnoId");

        if (alumnoId != null) {
            firebaseManager.buscarPerfilDeEstudiantePorId(alumnoId, task -> {
                if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                    // Creamos un UserProfile para acceder a los datos fácilmente
                    UserProfile alumnoProfile = UserProfile.fromMap(task.getResult().getData());

                    String nombre = alumnoProfile.getFullName().isEmpty() ? alumnoProfile.getDisplayName() : alumnoProfile.getFullName();
                    tvNombreAlumno.setText(nombre);
                    tvNumControl.setText("No. Control: " + (alumnoProfile.getControlNumber() != null ? alumnoProfile.getControlNumber() : ""));
                    tvCarrera.setText("Carrera: " + (alumnoProfile.getCareer() != null ? alumnoProfile.getCareer() : ""));

                } else {
                    tvNombreAlumno.setText("Perfil de Alumno no Encontrado");
                    tvNumControl.setText("ID: " + alumnoId);
                    Log.e(TAG, "No se encontró el perfil para el alumno con ID: " + alumnoId, task.getException());
                }
            });
            // --- FIN DE CÓDIGO MODIFICADO ---
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

        // Configurar texto e iconos de los botones
        TextView tvButtonTextPdf = btnPDF.findViewById(R.id.tv_button_text_pdf);
        if (tvButtonTextPdf != null) {
            tvButtonTextPdf.setText("Ver PDF");
        }
        ImageView ivButtonIconPdf = btnPDF.findViewById(R.id.iv_button_icon_pdf);
        if (ivButtonIconPdf != null) {
            ivButtonIconPdf.setImageResource(R.drawable.ic_document);
            ivButtonIconPdf.setVisibility(View.VISIBLE);
        }

        TextView tvButtonTextEdit = btnEditar.findViewById(R.id.tv_button_text_edit);
        if (tvButtonTextEdit != null) {
            tvButtonTextEdit.setText("Editar");
        }
        ImageView ivButtonIconEdit = btnEditar.findViewById(R.id.iv_button_icon_edit);
        if (ivButtonIconEdit != null) {
            ivButtonIconEdit.setImageResource(R.drawable.ic_assignment);
            ivButtonIconEdit.setVisibility(View.VISIBLE);
        }

        TextView tvButtonTextDelete = btnEliminar.findViewById(R.id.tv_button_text_delete);
        if (tvButtonTextDelete != null) {
            tvButtonTextDelete.setText("Eliminar");
        }
        ImageView ivButtonIconDelete = btnEliminar.findViewById(R.id.iv_button_icon_delete);
        if (ivButtonIconDelete != null) {
            ivButtonIconDelete.setImageResource(R.drawable.ic_logout);
            ivButtonIconDelete.setVisibility(View.VISIBLE);
        }

        btnPDF.setOnClickListener(v -> {
            JSONObject calJson = new JSONObject(calendario.getData());
            seleccionarUbicacionPDF(calJson);
        });
        btnEditar.setOnClickListener(v -> mostrarDialogCalendario(calendarioId));
        btnEliminar.setOnClickListener(v -> {
            new AlertDialog.Builder(getContext())
                    .setTitle("Eliminar Calendario").setMessage("¿Eliminar este calendario?")
                    .setPositiveButton("Eliminar", (dialog, which) -> {

                        // Al eliminar un calendario, se cancelan todas sus alarmas asociadas.
                        for (int i = 0; i < 3; i++) {
                            alarmScheduler.cancelAlarmsForDate(calendarioId, i);
                        }

                        firebaseManager.eliminarCalendario(currentUserId, calendarioId,
                                () -> {
                                    fileManager.eliminarRegistroLocalCalendario(calendarioId);
                                    cargarCalendarios();
                                    Toast.makeText(getContext(), "Calendario eliminado", Toast.LENGTH_SHORT).show();
                                },
                                e -> Toast.makeText(getContext(), "Error al eliminar: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }).setNegativeButton("Cancelar", null).show();
        });

        layoutCalendarios.addView(cardView);
    }

    private void cargarDatosCalendarioParaEdicionFirebase(String calendarioId, Spinner spinnerAlumno, TextView tvNombreAlumnoSeleccionado) {
        if (currentUserId == null) return;
        firebaseManager.buscarCalendarioPorId(currentUserId, calendarioId, task -> {
            if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                DocumentSnapshot calendario = task.getResult();
                String alumnoId = calendario.getString("alumnoId");
                this.alumnoActualId = alumnoId;
                this.calendarioActualId = calendarioId;

                firebaseManager.buscarPerfilDeEstudiantePorId(alumnoId, alumnoTask -> {
                    if(alumnoTask.isSuccessful() && alumnoTask.getResult() != null && alumnoTask.getResult().exists()){
                        UserProfile alumnoProfile = UserProfile.fromMap(alumnoTask.getResult().getData());
                        String nombre = alumnoProfile.getFullName().isEmpty() ? alumnoProfile.getDisplayName() : alumnoProfile.getFullName();
                        tvNombreAlumnoSeleccionado.setText(nombre + "\nNo. Control: " + alumnoProfile.getControlNumber());
                        tvNombreAlumnoSeleccionado.setVisibility(View.VISIBLE);
                        spinnerAlumno.setVisibility(View.GONE);
                    }
                });
            }
        });
    }

    private void cargarDatosCalendarioExistenteFirebase(String[] fechasTemp, TextView[] textViewsFecha, LinearLayout[] iconosGuardar,
                                                        TextView[] textViewsLabel, String[] defaultLabels) {
        if (currentUserId == null) return;
        for (int i = 0; i < textViewsFecha.length; i++) {
            fechasTemp[i] = "";
            textViewsFecha[i].setText("Fecha: Sin asignar");
            textViewsFecha[i].setTextColor(getResources().getColor(R.color.text_secondary));
            iconosGuardar[i].setEnabled(false);
            iconosGuardar[i].setAlpha(0.5f);
            textViewsLabel[i].setText(defaultLabels[i]);
        }
        if (calendarioActualId == null) return;

        firebaseManager.buscarCalendarioPorId(currentUserId, calendarioActualId, task -> {
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
                            textViewsFecha[i].setTextColor(getResources().getColor(R.color.status_approved));
                            iconosGuardar[i].setAlpha(1.0f);
                            iconosGuardar[i].setEnabled(true);
                        }
                        String label = calendario.getString(camposLabel[i]);
                        textViewsLabel[i].setText(label != null ? label : defaultLabels[i]);
                    }
                }
            }
        });
    }

    private void borrarTodasLasFechasFirebase(String[] fechasTemp, TextView[] textViews, LinearLayout[] iconosGuardar, TextView[] textViewsLabel, String[] defaultLabels) {
        if (currentUserId == null || calendarioActualId == null) return;
        Map<String, Object> updates = new HashMap<>();
        updates.put("fechaPrimeraEntrega", "");
        updates.put("fechaSegundaEntrega", "");
        updates.put("fechaResultado", "");

        firebaseManager.guardarOActualizarCalendario(currentUserId, calendarioActualId, updates, () -> {
            Toast.makeText(getContext(), "Todas las fechas han sido eliminadas", Toast.LENGTH_SHORT).show();

            // Al borrar las fechas, también se cancelan todas las alarmas asociadas.
            for (int i = 0; i < 3; i++) {
                alarmScheduler.cancelAlarmsForDate(calendarioActualId, i);
            }

            cargarDatosCalendarioExistenteFirebase(fechasTemp, textViews, iconosGuardar, textViewsLabel, defaultLabels);
            cargarCalendarios();
        }, e -> Toast.makeText(getContext(), "Error al borrar fechas.", Toast.LENGTH_SHORT).show());
    }

    private void mostrarDatePickerIndividualIcono(int indice, String[] fechasTemp, TextView[] textViews,
                                                  LinearLayout[] iconosGuardar, SimpleDateFormat formatoFecha, String fechaMinima) {
        if (getContext() == null) return;
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
        DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(), (view, year, month, dayOfMonth) -> {
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
                textViews[indice].setTextColor(getResources().getColor(R.color.primary));
                iconosGuardar[indice].setEnabled(true);
                iconosGuardar[indice].setAlpha(1.0f);
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
        // Verificar si la fecha seleccionada es fin de semana
        Calendar calSeleccionada = Calendar.getInstance();
        calSeleccionada.setTime(fechaSeleccionada);
        if (esFindeSemana(calSeleccionada)) return false;

        // Si no hay fecha mínima (primera fecha), la validación pasa
        if (fechaMinima == null || fechaMinima.isEmpty()) return true;

        // Si hay fecha mínima, verificar que la fecha seleccionada sea posterior
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
        if (currentUserId == null) return;

        calendarioPendiente = calendario;
        String alumnoId = calendario.optString("alumnoId", "");

        firebaseManager.buscarAlumnoPorId(currentUserId, alumnoId, task -> {
            String nombreAlumno = "Calendario";
            if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                nombreAlumno = task.getResult().getString("nombre");
            }

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
        });
    }

    private void generarPDFEnUbicacion(JSONObject calendario, Uri uri) {
        if (getActivity() == null) return;
        Toast.makeText(getContext(), "Generando PDF...", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                PDFGeneratorCalendario pdfGenerator = new PDFGeneratorCalendario(requireContext());
                boolean exito = pdfGenerator.generarPDFCalendarioEnUri(calendario, uri);
                getActivity().runOnUiThread(() -> {
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
                getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
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