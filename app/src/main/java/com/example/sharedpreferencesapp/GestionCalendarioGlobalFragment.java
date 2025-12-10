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
    private LinearLayout btnGuardar1, btnGuardar2, btnGuardar3;
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
        btnGuardar1 = view.findViewById(R.id.btn_guardar_1);
        btnGuardar2 = view.findViewById(R.id.btn_guardar_2);
        btnGuardar3 = view.findViewById(R.id.btn_guardar_3);
        btnBorrarFechas = view.findViewById(R.id.btn_borrar_fechas);
        btnBack = view.findViewById(R.id.btn_back);
        progressBar = view.findViewById(R.id.progress_bar);

        // Configurar listeners
        btnCalendario1.setOnClickListener(v -> mostrarDatePicker(0));
        btnCalendario2.setOnClickListener(v -> mostrarDatePicker(1));
        btnCalendario3.setOnClickListener(v -> mostrarDatePicker(2));

        btnGuardar1.setOnClickListener(v -> guardarFecha(0));
        btnGuardar2.setOnClickListener(v -> guardarFecha(1));
        btnGuardar3.setOnClickListener(v -> guardarFecha(2));

        tvLabel1.setOnClickListener(v -> editarLabel(0));
        tvLabel2.setOnClickListener(v -> editarLabel(1));
        tvLabel3.setOnClickListener(v -> editarLabel(2));

        btnBorrarFechas.setOnClickListener(v -> confirmarBorrarFechas());

        btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        // Cargar fechas existentes
        cargarCalendarioGlobal();
    }

    private void mostrarDatePicker(int indice) {
        Calendar calendar = Calendar.getInstance();

        // Si hay una fecha anterior, usarla como mínima
        String fechaMinima = null;
        if (indice > 0 && fechasTemp[indice - 1] != null && !fechasTemp[indice - 1].isEmpty()) {
            fechaMinima = fechasTemp[indice - 1];
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(year, month, dayOfMonth);
                    String fechaFormateada = formatoFecha.format(selectedDate.getTime());
                    fechasTemp[indice] = fechaFormateada;

                    TextView tvFecha = indice == 0 ? tvFecha1 : (indice == 1 ? tvFecha2 : tvFecha3);
                    tvFecha.setText(fechaFormateada);
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
                    calendar.setTime(fechaMin);
                    datePickerDialog.getDatePicker().setMinDate(calendar.getTimeInMillis());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error al parsear fecha mínima", e);
            }
        }

        datePickerDialog.show();
    }

    private void guardarFecha(int indice) {
        if (fechasTemp[indice] == null || fechasTemp[indice].isEmpty()) {
            Toast.makeText(getContext(), "Por favor selecciona una fecha primero", Toast.LENGTH_SHORT).show();
            return;
        }

        String label = indice == 0 ? tvLabel1.getText().toString() :
                (indice == 1 ? tvLabel2.getText().toString() : tvLabel3.getText().toString());

        Map<String, Object> data = new HashMap<>();
        data.put(nombresCamposFecha[indice], fechasTemp[indice]);
        data.put(nombresCamposLabel[indice], label);
        data.put("fechaCreacion", formatoFecha.format(new Date()));

        progressBar.setVisibility(View.VISIBLE);
        firebaseManager.guardarCalendarioGlobal(
                data,
                () -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Fecha guardada exitosamente", Toast.LENGTH_SHORT).show();
                    cargarCalendarioGlobal();
                },
                e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Error al guardar la fecha: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error al guardar fecha", e);
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
            } else {
                fechasTemp[i] = null;
                textViewsFecha[i].setText("Sin fecha asignada");
            }

            if (label != null && !label.isEmpty()) {
                textViewsLabel[i].setText(label);
            } else {
                textViewsLabel[i].setText(defaultLabels[i]);
            }
        }
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

