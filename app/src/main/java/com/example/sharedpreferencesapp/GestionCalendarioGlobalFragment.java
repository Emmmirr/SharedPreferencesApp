package com.example.sharedpreferencesapp;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class GestionCalendarioGlobalFragment extends Fragment {

    private static final String TAG = "GestionCalendarioGlobal";

    private TextView tvFecha1, tvFecha2, tvFecha3;
    private TextView tvLabel1, tvLabel2, tvLabel3;
    private LinearLayout btnCalendario1, btnCalendario2, btnCalendario3;
    private LinearLayout btnGuardarTodas;
    private LinearLayout btnBorrarFechas;
    private ImageView btnBack;
    private ProgressBar progressBar;

    private FirebaseManager firebaseManager;
    private String[] fechasTemp = new String[3];
    private SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    private String[] nombresCamposFecha = {"fechaPrimeraEntrega", "fechaSegundaEntrega", "fechaResultado"};
    private String[] nombresCamposLabel = {"labelPrimeraEntrega", "labelSegundaEntrega", "labelResultado"};
    private String[] defaultLabels = {"1ª Entrega", "2ª Entrega", "Resultado Final"};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_gestion_calendario_global, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firebaseManager = new FirebaseManager();

        // Inicializar vistas
        tvFecha1 = view.findViewById(R.id.tv_fecha_1);
        tvFecha2 = view.findViewById(R.id.tv_fecha_2);
        tvFecha3 = view.findViewById(R.id.tv_fecha_3);
        tvLabel1 = view.findViewById(R.id.tv_label_1);
        tvLabel2 = view.findViewById(R.id.tv_label_2);
        tvLabel3 = view.findViewById(R.id.tv_label_3);
        btnCalendario1 = view.findViewById(R.id.btn_calendario_1);
        btnCalendario2 = view.findViewById(R.id.btn_calendario_2);
        btnCalendario3 = view.findViewById(R.id.btn_calendario_3);
        btnGuardarTodas = view.findViewById(R.id.btn_guardar_todas);
        btnBorrarFechas = view.findViewById(R.id.btn_borrar_fechas);
        btnBack = view.findViewById(R.id.btn_back);
        progressBar = view.findViewById(R.id.progress_bar);

        // Configurar listeners
        btnCalendario1.setOnClickListener(v -> mostrarDatePicker(0));
        btnCalendario2.setOnClickListener(v -> mostrarDatePicker(1));
        btnCalendario3.setOnClickListener(v -> mostrarDatePicker(2));

        btnGuardarTodas.setOnClickListener(v -> guardarTodasLasFechas());

        tvLabel1.setOnClickListener(v -> editarLabel(0));
        tvLabel2.setOnClickListener(v -> editarLabel(1));
        tvLabel3.setOnClickListener(v -> editarLabel(2));

        btnBorrarFechas.setOnClickListener(v -> confirmarBorrarFechas());

        btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        // Inicializar botón como deshabilitado
        btnGuardarTodas.setEnabled(false);
        btnGuardarTodas.setAlpha(0.5f);

        // Cargar fechas existentes
        cargarCalendarioGlobal();
    }

    private void mostrarDatePicker(int indice) {
        Calendar calendar = Calendar.getInstance();

        // Si hay una fecha anterior, usarla como mínima (hacer final para usar en lambda)
        final String fechaMinima;
        if (indice > 0 && fechasTemp[indice - 1] != null && !fechasTemp[indice - 1].isEmpty()) {
            fechaMinima = fechasTemp[indice - 1];
        } else {
            fechaMinima = null;
        }

        // Hacer el índice final para usar en lambda
        final int fechaIndex = indice;

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(year, month, dayOfMonth);

                    // Validar que no sea sábado ni domingo
                    if (esFindeSemana(selectedDate)) {
                        Toast.makeText(getContext(), "No se pueden seleccionar sábados ni domingos", Toast.LENGTH_LONG).show();
                        return;
                    }

                    // Validar que la fecha sea posterior a la anterior
                    if (fechaMinima != null && !fechaMinima.isEmpty()) {
                        try {
                            Date fechaMin = formatoFecha.parse(fechaMinima);
                            if (fechaMin != null && !selectedDate.getTime().after(fechaMin)) {
                                Toast.makeText(getContext(), "La fecha debe ser posterior a la fecha anterior", Toast.LENGTH_SHORT).show();
                                return;
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error al validar fecha mínima", e);
                        }
                    }

                    String fechaFormateada = formatoFecha.format(selectedDate.getTime());
                    fechasTemp[fechaIndex] = fechaFormateada;

                    TextView tvFecha = fechaIndex == 0 ? tvFecha1 : (fechaIndex == 1 ? tvFecha2 : tvFecha3);
                    tvFecha.setText(fechaFormateada);
                    tvFecha.setTextColor(getResources().getColor(R.color.primary));

                    // Verificar si las 3 fechas están completas para habilitar el guardado
                    verificarFechasCompletas();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        // Establecer fecha mínima si existe
        if (fechaMinima != null) {
            try {
                Date fechaMin = formatoFecha.parse(fechaMinima);
                if (fechaMin != null) {
                    Calendar calMin = Calendar.getInstance();
                    calMin.setTime(fechaMin);
                    calMin.add(Calendar.DAY_OF_MONTH, 1);
                    datePickerDialog.getDatePicker().setMinDate(calMin.getTimeInMillis());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error al parsear fecha mínima", e);
            }
        } else {
            // Establecer fecha mínima como hoy
            datePickerDialog.getDatePicker().setMinDate(calendar.getTimeInMillis());
        }

        datePickerDialog.show();
    }

    private boolean esFindeSemana(Calendar fecha) {
        int diaSemana = fecha.get(Calendar.DAY_OF_WEEK);
        return diaSemana == Calendar.SATURDAY || diaSemana == Calendar.SUNDAY;
    }

    private void verificarFechasCompletas() {
        boolean todasCompletas = true;
        for (int i = 0; i < 3; i++) {
            if (fechasTemp[i] == null || fechasTemp[i].isEmpty()) {
                todasCompletas = false;
                break;
            }
        }

        // Habilitar/deshabilitar botón de guardar según si todas las fechas están completas
        boolean habilitado = todasCompletas;
        btnGuardarTodas.setEnabled(habilitado);

        if (habilitado) {
            btnGuardarTodas.setAlpha(1.0f);
        } else {
            btnGuardarTodas.setAlpha(0.5f);
        }
    }

    private void guardarTodasLasFechas() {
        // Validar que todas las fechas estén completas
        for (int i = 0; i < 3; i++) {
            if (fechasTemp[i] == null || fechasTemp[i].isEmpty()) {
                Toast.makeText(getContext(), "Debes completar las 3 fechas antes de guardar", Toast.LENGTH_LONG).show();
                return;
            }
        }

        // Guardar todas las fechas juntas
        String label1 = tvLabel1.getText().toString();
        String label2 = tvLabel2.getText().toString();
        String label3 = tvLabel3.getText().toString();

        Map<String, Object> data = new HashMap<>();
        data.put(nombresCamposFecha[0], fechasTemp[0]);
        data.put(nombresCamposFecha[1], fechasTemp[1]);
        data.put(nombresCamposFecha[2], fechasTemp[2]);
        data.put(nombresCamposLabel[0], label1);
        data.put(nombresCamposLabel[1], label2);
        data.put(nombresCamposLabel[2], label3);
        data.put("fechaCreacion", formatoFecha.format(new Date()));

        progressBar.setVisibility(View.VISIBLE);
        firebaseManager.guardarCalendarioGlobal(
                data,
                () -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Todas las fechas guardadas exitosamente", Toast.LENGTH_SHORT).show();
                    cargarCalendarioGlobal();
                },
                e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Error al guardar las fechas: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error al guardar fechas", e);
                }
        );
    }

    private void editarLabel(int indice) {
        TextView tvLabel = indice == 0 ? tvLabel1 : (indice == 1 ? tvLabel2 : tvLabel3);
        String labelActual = tvLabel.getText().toString();

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Editar Nombre de la Actividad");
        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(labelActual);
        builder.setView(input);
        builder.setPositiveButton("Guardar", (dialog, which) -> {
            String nuevoLabel = input.getText().toString().trim();
            if (nuevoLabel.isEmpty()) {
                Toast.makeText(getContext(), "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> update = new HashMap<>();
            update.put(nombresCamposLabel[indice], nuevoLabel);

            progressBar.setVisibility(View.VISIBLE);
            firebaseManager.guardarCalendarioGlobal(
                    update,
                    () -> {
                        progressBar.setVisibility(View.GONE);
                        tvLabel.setText(nuevoLabel);
                        Toast.makeText(getContext(), "Nombre actualizado", Toast.LENGTH_SHORT).show();
                    },
                    e -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Error al actualizar", Toast.LENGTH_SHORT).show();
                    }
            );
        });
        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void confirmarBorrarFechas() {
        new AlertDialog.Builder(getContext())
                .setTitle("Borrar Fechas")
                .setMessage("¿Estás seguro que quieres eliminar todas las fechas del calendario global?")
                .setPositiveButton("Sí, Borrar", (dialog, which) -> borrarTodasLasFechas())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void borrarTodasLasFechas() {
        Map<String, Object> data = new HashMap<>();
        data.put("fechaPrimeraEntrega", "");
        data.put("fechaSegundaEntrega", "");
        data.put("fechaResultado", "");
        data.put("labelPrimeraEntrega", defaultLabels[0]);
        data.put("labelSegundaEntrega", defaultLabels[1]);
        data.put("labelResultado", defaultLabels[2]);

        progressBar.setVisibility(View.VISIBLE);
        firebaseManager.guardarCalendarioGlobal(
                data,
                () -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Fechas eliminadas", Toast.LENGTH_SHORT).show();
                    cargarCalendarioGlobal();
                },
                e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Error al eliminar fechas", Toast.LENGTH_SHORT).show();
                }
        );
    }

    private void cargarCalendarioGlobal() {
        progressBar.setVisibility(View.VISIBLE);
        firebaseManager.obtenerCalendarioGlobal(task -> {
            progressBar.setVisibility(View.GONE);
            if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                DocumentSnapshot doc = task.getResult();
                actualizarUI(doc);
            } else {
                // Si no existe, inicializar con valores por defecto
                inicializarValoresPorDefecto();
            }
        });
    }

    private void actualizarUI(DocumentSnapshot doc) {
        TextView[] textViewsFecha = {tvFecha1, tvFecha2, tvFecha3};
        TextView[] textViewsLabel = {tvLabel1, tvLabel2, tvLabel3};

        for (int i = 0; i < 3; i++) {
            String fecha = doc.getString(nombresCamposFecha[i]);
            String label = doc.getString(nombresCamposLabel[i]);

            if (fecha != null && !fecha.isEmpty()) {
                fechasTemp[i] = fecha;
                textViewsFecha[i].setText(fecha);
                textViewsFecha[i].setTextColor(getResources().getColor(R.color.primary));
            } else {
                fechasTemp[i] = null;
                textViewsFecha[i].setText("Sin fecha asignada");
                textViewsFecha[i].setTextColor(getResources().getColor(R.color.text_secondary));
            }

            if (label != null && !label.isEmpty()) {
                textViewsLabel[i].setText(label);
            } else {
                textViewsLabel[i].setText(defaultLabels[i]);
            }
        }

        // Verificar si todas las fechas están completas
        verificarFechasCompletas();
    }

    private void inicializarValoresPorDefecto() {
        TextView[] textViewsFecha = {tvFecha1, tvFecha2, tvFecha3};
        TextView[] textViewsLabel = {tvLabel1, tvLabel2, tvLabel3};

        for (int i = 0; i < 3; i++) {
            fechasTemp[i] = null;
            textViewsFecha[i].setText("Sin fecha asignada");
            textViewsLabel[i].setText(defaultLabels[i]);
        }
    }
}

