package com.example.sharedpreferencesapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import java.util.HashSet;
import java.util.Set;

public class GestionAlumnoFragment extends Fragment {

    private LinearLayout layoutAlumnos;
    private TextView tvNoAlumnos;
    private Button btnAgregarAlumno;
    private SharedPreferences sharedPreferences;

    public GestionAlumnoFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_gestion_alumno, container, false);

        layoutAlumnos = view.findViewById(R.id.layoutAlumnos);
        tvNoAlumnos = view.findViewById(R.id.tvNoAlumnos);
        btnAgregarAlumno = view.findViewById(R.id.btnAgregarAlumno);

        sharedPreferences = requireContext().getSharedPreferences("AlumnosPrefs", Context.MODE_PRIVATE);

        btnAgregarAlumno.setOnClickListener(v -> mostrarFormularioAlumno(null));

        cargarAlumnos();

        return view;
    }

    private void cargarAlumnos() {
        layoutAlumnos.removeAllViews();

        Set<String> alumnosSet = sharedPreferences.getStringSet("lista_alumnos", new HashSet<>());

        if (alumnosSet.isEmpty()) {
            tvNoAlumnos.setVisibility(View.VISIBLE);
        } else {
            tvNoAlumnos.setVisibility(View.GONE);

            for (String alumnoId : alumnosSet) {
                crearCardAlumno(alumnoId);
            }
        }
    }

    private void crearCardAlumno(String alumnoId) {
        View cardView = LayoutInflater.from(getContext()).inflate(R.layout.card_alumno, layoutAlumnos, false);

        TextView tvNombre = cardView.findViewById(R.id.tvNombre);
        TextView tvNumControl = cardView.findViewById(R.id.tvNumControl);
        TextView tvCarrera = cardView.findViewById(R.id.tvCarrera);
        TextView tvSemestre = cardView.findViewById(R.id.tvSemestre);
        Button btnEditar = cardView.findViewById(R.id.btnEditar);
        Button btnEliminar = cardView.findViewById(R.id.btnEliminar);

        // Mostrar datos
        String nombre = sharedPreferences.getString(alumnoId + "_nombre", "");
        String numControl = sharedPreferences.getString(alumnoId + "_numControl", "");
        String carrera = sharedPreferences.getString(alumnoId + "_carrera", "");
        String semestre = sharedPreferences.getString(alumnoId + "_semestre", "");

        tvNombre.setText(nombre);
        tvNumControl.setText("No. Control: " + numControl);
        tvCarrera.setText(carrera);
        tvSemestre.setText("Semestre: " + semestre);

        btnEditar.setOnClickListener(v -> mostrarFormularioAlumno(alumnoId));
        btnEliminar.setOnClickListener(v -> eliminarAlumno(alumnoId));

        layoutAlumnos.addView(cardView);
    }

    private void mostrarFormularioAlumno(String alumnoId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_alumno, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        TextView tvTitulo = dialogView.findViewById(R.id.tvTituloFormulario);
        EditText etNombre = dialogView.findViewById(R.id.etNombre);
        EditText etCurp = dialogView.findViewById(R.id.etCurp);
        EditText etFechaNacimiento = dialogView.findViewById(R.id.etFechaNacimiento);
        EditText etGenero = dialogView.findViewById(R.id.etGenero);
        EditText etNumControl = dialogView.findViewById(R.id.etNumControl);
        EditText etSemestre = dialogView.findViewById(R.id.etSemestre);
        EditText etCarrera = dialogView.findViewById(R.id.etCarrera);
        EditText etEspecialidad = dialogView.findViewById(R.id.etEspecialidad);
        EditText etTelefono = dialogView.findViewById(R.id.etTelefono);
        EditText etEmail = dialogView.findViewById(R.id.etEmail);
        EditText etDireccion = dialogView.findViewById(R.id.etDireccion);

        Button btnCancelar = dialogView.findViewById(R.id.btnCancelar);
        Button btnGuardar = dialogView.findViewById(R.id.btnGuardar);

        // Al editar carga los datos
        if (alumnoId != null) {
            tvTitulo.setText("Editar Alumno");
            etNombre.setText(sharedPreferences.getString(alumnoId + "_nombre", ""));
            etCurp.setText(sharedPreferences.getString(alumnoId + "_curp", ""));
            etFechaNacimiento.setText(sharedPreferences.getString(alumnoId + "_fechaNacimiento", ""));
            etGenero.setText(sharedPreferences.getString(alumnoId + "_genero", ""));
            etNumControl.setText(sharedPreferences.getString(alumnoId + "_numControl", ""));
            etSemestre.setText(sharedPreferences.getString(alumnoId + "_semestre", ""));
            etCarrera.setText(sharedPreferences.getString(alumnoId + "_carrera", ""));
            etEspecialidad.setText(sharedPreferences.getString(alumnoId + "_especialidad", ""));
            etTelefono.setText(sharedPreferences.getString(alumnoId + "_telefono", ""));
            etEmail.setText(sharedPreferences.getString(alumnoId + "_email", ""));
            etDireccion.setText(sharedPreferences.getString(alumnoId + "_direccion", ""));
        }

        btnCancelar.setOnClickListener(v -> dialog.dismiss());

        btnGuardar.setOnClickListener(v -> {
            if (etNombre.getText().toString().trim().isEmpty() ||
                    etNumControl.getText().toString().trim().isEmpty()) {
                Toast.makeText(getContext(), "Complete los campos obligatorios", Toast.LENGTH_SHORT).show();
                return;
            }

            guardarAlumno(alumnoId, etNombre, etCurp, etFechaNacimiento, etGenero,
                    etNumControl, etSemestre, etCarrera, etEspecialidad,
                    etTelefono, etEmail, etDireccion);

            cargarAlumnos();
            dialog.dismiss();

            String mensaje = (alumnoId == null) ? "Alumno agregado exitosamente" : "Alumno actualizado exitosamente";
            Toast.makeText(getContext(), mensaje, Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    private void guardarAlumno(String alumnoId, EditText etNombre, EditText etCurp,
                               EditText etFechaNacimiento, EditText etGenero, EditText etNumControl,
                               EditText etSemestre, EditText etCarrera, EditText etEspecialidad,
                               EditText etTelefono, EditText etEmail, EditText etDireccion) {

        SharedPreferences.Editor editor = sharedPreferences.edit();

        // Si es nuevo generar el ID
        if (alumnoId == null) {
            alumnoId = "alumno_" + System.currentTimeMillis();

            // Agregar a la lista de alumnos
            Set<String> alumnosSet = new HashSet<>(sharedPreferences.getStringSet("lista_alumnos", new HashSet<>()));
            alumnosSet.add(alumnoId);
            editor.putStringSet("lista_alumnos", alumnosSet);
        }

        // Guardar datos del alumno
        editor.putString(alumnoId + "_nombre", etNombre.getText().toString().trim());
        editor.putString(alumnoId + "_curp", etCurp.getText().toString().trim());
        editor.putString(alumnoId + "_fechaNacimiento", etFechaNacimiento.getText().toString().trim());
        editor.putString(alumnoId + "_genero", etGenero.getText().toString().trim());
        editor.putString(alumnoId + "_numControl", etNumControl.getText().toString().trim());
        editor.putString(alumnoId + "_semestre", etSemestre.getText().toString().trim());
        editor.putString(alumnoId + "_carrera", etCarrera.getText().toString().trim());
        editor.putString(alumnoId + "_especialidad", etEspecialidad.getText().toString().trim());
        editor.putString(alumnoId + "_telefono", etTelefono.getText().toString().trim());
        editor.putString(alumnoId + "_email", etEmail.getText().toString().trim());
        editor.putString(alumnoId + "_direccion", etDireccion.getText().toString().trim());

        editor.apply();
    }

    private void eliminarAlumno(String alumnoId) {
        new AlertDialog.Builder(getContext())
                .setTitle("Eliminar Alumno")
                .setMessage("¿Está seguro de que desea eliminar este alumno?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    SharedPreferences.Editor editor = sharedPreferences.edit();

                    // Remover de la lista
                    Set<String> alumnosSet = new HashSet<>(sharedPreferences.getStringSet("lista_alumnos", new HashSet<>()));
                    alumnosSet.remove(alumnoId);
                    editor.putStringSet("lista_alumnos", alumnosSet);

                    // Eliminar todos los datos del alumno
                    editor.remove(alumnoId + "_nombre");
                    editor.remove(alumnoId + "_curp");
                    editor.remove(alumnoId + "_fechaNacimiento");
                    editor.remove(alumnoId + "_genero");
                    editor.remove(alumnoId + "_numControl");
                    editor.remove(alumnoId + "_semestre");
                    editor.remove(alumnoId + "_carrera");
                    editor.remove(alumnoId + "_especialidad");
                    editor.remove(alumnoId + "_telefono");
                    editor.remove(alumnoId + "_email");
                    editor.remove(alumnoId + "_direccion");

                    editor.apply();
                    cargarAlumnos();

                    Toast.makeText(getContext(), "Alumno eliminado", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}