package com.example.sharedpreferencesapp;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public class GestionAlumnoFragment extends Fragment {

    private static final String TAG = "GestionAlumnoFragment";
    private LinearLayout layoutAlumnos;
    private TextView tvNoAlumnos;
    private Button btnAgregarAlumno;

    private FirebaseManager firebaseManager;

    // AÑADIDO: Variable para guardar el ID del usuario actual
    private String currentUserId;

    // Datos para los spinners (sin cambios)
    private final String[] SEXOS = {"Masculino", "Femenino", "Otro"};
    private final String[] SEMESTRES = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15"};
    private final String[] CARRERAS = {
            "CONTABILIDAD",
            "INGENIERIA EN INFORMATICA",
            "INGENIERIA EN GESTION EMPRESARIAL",
            "INGENIERIA CIVIL",
            "INGENIERIA EN SISTEMAS COMPUTACIONALES"
    };

    public GestionAlumnoFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_gestion_alumno, container, false);

        layoutAlumnos = view.findViewById(R.id.layoutAlumnos);
        tvNoAlumnos = view.findViewById(R.id.tvNoAlumnos);
        btnAgregarAlumno = view.findViewById(R.id.btnAgregarAlumno);

        firebaseManager = new FirebaseManager();

        btnAgregarAlumno.setOnClickListener(v -> mostrarFormularioAlumno(null));

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Obtenemos el usuario actual aquí. Es el paso más importante.
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            this.currentUserId = user.getUid();
            // Una vez que tenemos el ID, cargamos los datos del usuario.
            cargarAlumnos();
        } else {
            // Si por alguna razón no hay usuario, bloqueamos la UI.
            tvNoAlumnos.setText("Error de sesión. Por favor, inicie sesión de nuevo.");
            tvNoAlumnos.setVisibility(View.VISIBLE);
            layoutAlumnos.setVisibility(View.GONE);
            btnAgregarAlumno.setEnabled(false);
        }
    }

    private void cargarAlumnos() {
        // Salvaguarda para evitar crashes si el ID es nulo.
        if (currentUserId == null) return;

        layoutAlumnos.removeAllViews();
        tvNoAlumnos.setText("No hay alumnos registrados.");
        tvNoAlumnos.setVisibility(View.VISIBLE);

        // MODIFICADO: Se pasa el ID del usuario al método.
        firebaseManager.cargarAlumnos(currentUserId, task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                if (task.getResult().isEmpty()) {
                    tvNoAlumnos.setVisibility(View.VISIBLE);
                } else {
                    tvNoAlumnos.setVisibility(View.GONE);
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        crearCardAlumno(document);
                    }
                }
            } else {
                tvNoAlumnos.setVisibility(View.VISIBLE);
                Toast.makeText(getContext(), "Error al cargar alumnos.", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error al cargar alumnos", task.getException());
            }
        });
    }

    private void crearCardAlumno(DocumentSnapshot alumnoDoc) {
        View cardView = LayoutInflater.from(getContext()).inflate(R.layout.card_alumno, layoutAlumnos, false);

        TextView tvNombre = cardView.findViewById(R.id.tvNombre);
        TextView tvNumControl = cardView.findViewById(R.id.tvNumControl);
        TextView tvCarrera = cardView.findViewById(R.id.tvCarrera);
        TextView tvSemestre = cardView.findViewById(R.id.tvSemestre);
        Button btnEditar = cardView.findViewById(R.id.btnEditar);
        Button btnEliminar = cardView.findViewById(R.id.btnEliminar);

        String id = alumnoDoc.getId();
        String nombre = alumnoDoc.getString("nombre");
        String numControl = alumnoDoc.getString("numControl");
        String carrera = alumnoDoc.getString("carrera");
        String semestre = alumnoDoc.getString("semestre");

        tvNombre.setText(nombre);
        tvNumControl.setText("No. Control: " + numControl);
        tvCarrera.setText(carrera);
        tvSemestre.setText("Semestre: " + semestre);

        btnEditar.setOnClickListener(v -> mostrarFormularioAlumno(id));
        btnEliminar.setOnClickListener(v -> eliminarAlumno(id));

        layoutAlumnos.addView(cardView);
    }

    private void mostrarFormularioAlumno(String alumnoId) {
        // Salvaguarda de sesión
        if (currentUserId == null) {
            Toast.makeText(getContext(), "Error: No se ha iniciado sesión.", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_alumno, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        TextView tvTitulo = dialogView.findViewById(R.id.tvTituloFormulario);
        EditText etNombre = dialogView.findViewById(R.id.etNombre);
        EditText etCurp = dialogView.findViewById(R.id.etCurp);
        EditText etFechaNacimiento = dialogView.findViewById(R.id.etFechaNacimiento);
        Spinner spinnerSexo = dialogView.findViewById(R.id.spinnerSexo);
        EditText etNumControl = dialogView.findViewById(R.id.etNumControl);
        Spinner spinnerSemestre = dialogView.findViewById(R.id.spinnerSemestre);
        Spinner spinnerCarrera = dialogView.findViewById(R.id.spinnerCarrera);
        EditText etEspecialidad = dialogView.findViewById(R.id.etEspecialidad);
        EditText etTelefono = dialogView.findViewById(R.id.etTelefono);
        EditText etEmail = dialogView.findViewById(R.id.etEmail);
        EditText etDireccion = dialogView.findViewById(R.id.etDireccion);
        Button btnCancelar = dialogView.findViewById(R.id.btnCancelar);
        Button btnGuardar = dialogView.findViewById(R.id.btnGuardar);

        configurarSpinners(spinnerSexo, spinnerSemestre, spinnerCarrera);
        configurarDatePicker(etFechaNacimiento);

        if (alumnoId != null) {
            tvTitulo.setText("Editar Alumno");
            cargarDatosExistentes(alumnoId, etNombre, etCurp, etFechaNacimiento,
                    spinnerSexo, etNumControl, spinnerSemestre,
                    spinnerCarrera, etEspecialidad, etTelefono,
                    etEmail, etDireccion);
        }

        btnCancelar.setOnClickListener(v -> dialog.dismiss());

        btnGuardar.setOnClickListener(v -> {
            if (validarFormulario(etNombre, etCurp, etFechaNacimiento, etNumControl,
                    etTelefono, etEmail)) {
                guardarAlumno(alumnoId, etNombre, etCurp, etFechaNacimiento,
                        spinnerSexo, etNumControl, spinnerSemestre,
                        spinnerCarrera, etEspecialidad, etTelefono,
                        etEmail, etDireccion, dialog);
            }
        });

        dialog.show();
    }

    private void cargarDatosExistentes(String alumnoId, EditText etNombre, EditText etCurp,
                                       EditText etFechaNacimiento, Spinner spinnerSexo,
                                       EditText etNumControl, Spinner spinnerSemestre,
                                       Spinner spinnerCarrera, EditText etEspecialidad,
                                       EditText etTelefono, EditText etEmail, EditText etDireccion) {
        if (currentUserId == null) return;

        // MODIFICADO: Se pasa el ID del usuario
        firebaseManager.buscarAlumnoPorId(currentUserId, alumnoId, task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                DocumentSnapshot alumno = task.getResult();
                if (alumno.exists()) {
                    etNombre.setText(alumno.getString("nombre"));
                    etCurp.setText(alumno.getString("curp"));
                    etFechaNacimiento.setText(alumno.getString("fechaNacimiento"));
                    etNumControl.setText(alumno.getString("numControl"));
                    etEspecialidad.setText(alumno.getString("especialidad"));
                    etTelefono.setText(alumno.getString("telefono"));
                    etEmail.setText(alumno.getString("email"));
                    etDireccion.setText(alumno.getString("direccion"));

                    String sexo = alumno.getString("sexo");
                    String semestre = alumno.getString("semestre");
                    String carrera = alumno.getString("carrera");

                    int sexoPosition = Arrays.asList(SEXOS).indexOf(sexo);
                    if (sexoPosition >= 0) spinnerSexo.setSelection(sexoPosition);

                    int semestrePosition = Arrays.asList(SEMESTRES).indexOf(semestre);
                    if (semestrePosition >= 0) spinnerSemestre.setSelection(semestrePosition);

                    int carreraPosition = Arrays.asList(CARRERAS).indexOf(carrera);
                    if (carreraPosition >= 0) spinnerCarrera.setSelection(carreraPosition);
                }
            } else {
                Toast.makeText(getContext(), "Error al cargar datos del alumno.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void guardarAlumno(String alumnoId, EditText etNombre, EditText etCurp,
                               EditText etFechaNacimiento, Spinner spinnerSexo, EditText etNumControl,
                               Spinner spinnerSemestre, Spinner spinnerCarrera, EditText etEspecialidad,
                               EditText etTelefono, EditText etEmail, EditText etDireccion, AlertDialog dialog) {
        if (currentUserId == null) return;

        Map<String, Object> alumno = new HashMap<>();
        alumno.put("nombre", etNombre.getText().toString().trim());
        alumno.put("curp", etCurp.getText().toString().trim().toUpperCase());
        alumno.put("fechaNacimiento", etFechaNacimiento.getText().toString().trim());
        alumno.put("sexo", spinnerSexo.getSelectedItem().toString());
        alumno.put("numControl", etNumControl.getText().toString().trim());
        alumno.put("semestre", spinnerSemestre.getSelectedItem().toString());
        alumno.put("carrera", spinnerCarrera.getSelectedItem().toString());
        alumno.put("especialidad", etEspecialidad.getText().toString().trim());
        alumno.put("telefono", etTelefono.getText().toString().trim());
        alumno.put("email", etEmail.getText().toString().trim());
        alumno.put("direccion", etDireccion.getText().toString().trim());

        // MODIFICADO: Se pasa el ID del usuario
        firebaseManager.guardarAlumno(currentUserId, alumnoId, alumno,
                () -> { // onSuccess
                    dialog.dismiss();
                    cargarAlumnos();
                    String mensaje = (alumnoId == null) ? "Alumno agregado exitosamente" : "Alumno actualizado exitosamente";
                    Toast.makeText(getContext(), mensaje, Toast.LENGTH_SHORT).show();
                },
                e -> { // onFailure
                    Toast.makeText(getContext(), "Error al guardar el alumno: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void eliminarAlumno(String alumnoId) {
        if (currentUserId == null) return;

        new AlertDialog.Builder(getContext())
                .setTitle("Eliminar Alumno")
                .setMessage("¿Está seguro de que desea eliminar este alumno?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    // MODIFICADO: Se pasa el ID del usuario
                    firebaseManager.eliminarAlumno(currentUserId, alumnoId,
                            () -> { // onSuccess
                                cargarAlumnos();
                                Toast.makeText(getContext(), "Alumno eliminado", Toast.LENGTH_SHORT).show();
                            },
                            e -> { // onFailure
                                Toast.makeText(getContext(), "Error al eliminar el alumno", Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // --- MÉTODOS DE UI (SIN CAMBIOS) ---

    private void configurarSpinners(Spinner spinnerSexo, Spinner spinnerSemestre, Spinner spinnerCarrera) {
        ArrayAdapter<String> sexoAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, SEXOS);
        sexoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSexo.setAdapter(sexoAdapter);

        ArrayAdapter<String> semestreAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, SEMESTRES);
        semestreAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSemestre.setAdapter(semestreAdapter);

        ArrayAdapter<String> carreraAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, CARRERAS);
        carreraAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCarrera.setAdapter(carreraAdapter);
    }

    private void configurarDatePicker(EditText etFechaNacimiento) {
        etFechaNacimiento.setOnClickListener(v -> {
            if (getContext() == null) return;
            Calendar calendar = Calendar.getInstance();
            String fechaActual = etFechaNacimiento.getText().toString();
            if (!fechaActual.isEmpty()) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    calendar.setTime(sdf.parse(fechaActual));
                } catch (Exception e) {
                    // Si hay error, usar fecha actual
                }
            }
            DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(),
                    (view, year, month, dayOfMonth) -> {
                        Calendar selectedDate = Calendar.getInstance();
                        selectedDate.set(year, month, dayOfMonth);
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                        etFechaNacimiento.setText(sdf.format(selectedDate.getTime()));
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
            Calendar minDate = Calendar.getInstance();
            minDate.add(Calendar.YEAR, -80);
            datePickerDialog.getDatePicker().setMinDate(minDate.getTimeInMillis());
            datePickerDialog.show();
        });
        etFechaNacimiento.setFocusable(false);
        etFechaNacimiento.setClickable(true);
    }

    private boolean validarFormulario(EditText etNombre, EditText etCurp, EditText etFechaNacimiento,
                                      EditText etNumControl, EditText etTelefono, EditText etEmail) {
        if (etNombre.getText().toString().trim().isEmpty()) {
            Toast.makeText(getContext(), "El nombre es obligatorio", Toast.LENGTH_SHORT).show();
            etNombre.requestFocus();
            return false;
        }
        String numControl = etNumControl.getText().toString().trim();
        if (numControl.isEmpty()) {
            Toast.makeText(getContext(), "El número de control es obligatorio", Toast.LENGTH_SHORT).show();
            etNumControl.requestFocus();
            return false;
        }
        String curp = etCurp.getText().toString().trim().toUpperCase();
        if (!curp.isEmpty() && !validarCURP(curp)) {
            Toast.makeText(getContext(), "El formato del CURP no es válido", Toast.LENGTH_SHORT).show();
            etCurp.requestFocus();
            return false;
        }
        String telefono = etTelefono.getText().toString().trim();
        if (!telefono.isEmpty() && !validarTelefono(telefono)) {
            Toast.makeText(getContext(), "El formato del teléfono no es válido", Toast.LENGTH_SHORT).show();
            etTelefono.requestFocus();
            return false;
        }
        String email = etEmail.getText().toString().trim();
        if (!email.isEmpty() && !validarEmail(email)) {
            Toast.makeText(getContext(), "El formato del email no es válido", Toast.LENGTH_SHORT).show();
            etEmail.requestFocus();
            return false;
        }
        return true;
    }

    private boolean validarCURP(String curp) {
        String patronCURP = "^[A-Z]{1}[AEIOU]{1}[A-Z]{2}[0-9]{2}(0[1-9]|1[0-2])(0[1-9]|1[0-9]|2[0-9]|3[01])[HM]{1}(AS|BC|BS|CC|CS|CH|CL|CM|DF|DG|GT|GR|HG|JC|MC|MN|MS|NT|NL|OC|PL|QT|QR|SP|SL|SR|TC|TS|TL|VZ|YN|ZS|NE)[B-DF-HJ-NP-TV-Z]{3}[0-9A-Z]{1}[0-9]{1}$";
        return Pattern.matches(patronCURP, curp);
    }

    private boolean validarTelefono(String telefono) {
        String telefonoLimpio = telefono.replaceAll("[^0-9]", "");
        return telefonoLimpio.length() == 10;
    }

    private boolean validarEmail(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
}