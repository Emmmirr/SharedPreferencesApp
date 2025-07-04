package com.example.sharedpreferencesapp;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
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

import androidx.fragment.app.Fragment;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class GestionAlumnoFragment extends Fragment {

    private LinearLayout layoutAlumnos;
    private TextView tvNoAlumnos;
    private Button btnAgregarAlumno;
    private Button btnDescargarAlumnos;
    private FileManager fileManager;
    private PDFGenerator pdfGenerator;

    // Datos para los spinners
    private final String[] SEXOS = {"Masculino", "Femenino", "Otro"};
    private final String[] SEMESTRES = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15"};
    private final String[] CARRERAS = {
            "CONTABILIDAD",
            "INGENIERÍA EN INFORMÁTICA",
            "INGENIERÍA EN GESTIÓN EMPRESARIAL",
            "INGENIERÍA CIVIL",
            "INGENIERÍA EN SISTEMAS COMPUTACIONALES"
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
        btnDescargarAlumnos = view.findViewById(R.id.btnDescargarAlumnos);

        // Inicializar FileManager y PDFGenerator
        fileManager = new FileManager(requireContext());
        pdfGenerator = new PDFGenerator(requireContext());

        btnAgregarAlumno.setOnClickListener(v -> mostrarFormularioAlumno(null));
        btnDescargarAlumnos.setOnClickListener(v -> descargarPDFAlumnos());

        cargarAlumnos();

        return view;
    }

    private void cargarAlumnos() {
        layoutAlumnos.removeAllViews();

        List<JSONObject> alumnos = fileManager.cargarAlumnos();

        if (alumnos.isEmpty()) {
            tvNoAlumnos.setVisibility(View.VISIBLE);
        } else {
            tvNoAlumnos.setVisibility(View.GONE);

            for (JSONObject alumno : alumnos) {
                crearCardAlumno(alumno);
            }
        }
    }

    private void crearCardAlumno(JSONObject alumno) {
        View cardView = LayoutInflater.from(getContext()).inflate(R.layout.card_alumno, layoutAlumnos, false);

        TextView tvNombre = cardView.findViewById(R.id.tvNombre);
        TextView tvNumControl = cardView.findViewById(R.id.tvNumControl);
        TextView tvCarrera = cardView.findViewById(R.id.tvCarrera);
        TextView tvSemestre = cardView.findViewById(R.id.tvSemestre);
        Button btnEditar = cardView.findViewById(R.id.btnEditar);
        Button btnEliminar = cardView.findViewById(R.id.btnEliminar);
        Button btnDescargarPDF = cardView.findViewById(R.id.btnDescargarPDF);

        try {
            // Mostrar datos
            String id = alumno.getString("id");
            String nombre = alumno.optString("nombre", "");
            String numControl = alumno.optString("numControl", "");
            String carrera = alumno.optString("carrera", "");
            String semestre = alumno.optString("semestre", "");

            tvNombre.setText(nombre);
            tvNumControl.setText("No. Control: " + numControl);
            tvCarrera.setText(carrera);
            tvSemestre.setText("Semestre: " + semestre);

            btnEditar.setOnClickListener(v -> mostrarFormularioAlumno(id));
            btnEliminar.setOnClickListener(v -> eliminarAlumno(id));
            btnDescargarPDF.setOnClickListener(v -> descargarPDFAlumnoIndividual(alumno));

        } catch (JSONException e) {
            e.printStackTrace();
        }

        layoutAlumnos.addView(cardView);
    }

    private void mostrarFormularioAlumno(String alumnoId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_alumno, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        // Inicializar vistas
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

        // Configurar spinners
        configurarSpinners(spinnerSexo, spinnerSemestre, spinnerCarrera);

        // Configurar DatePicker para fecha de nacimiento
        configurarDatePicker(etFechaNacimiento);

        // Al editar carga los datos
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
                        etEmail, etDireccion);

                cargarAlumnos();
                dialog.dismiss();

                String mensaje = (alumnoId == null) ? "Alumno agregado exitosamente" : "Alumno actualizado exitosamente";
                Toast.makeText(getContext(), mensaje, Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void configurarSpinners(Spinner spinnerSexo, Spinner spinnerSemestre, Spinner spinnerCarrera) {
        // Configurar spinner de sexo
        ArrayAdapter<String> sexoAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, SEXOS);
        sexoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSexo.setAdapter(sexoAdapter);

        // Configurar spinner de semestre
        ArrayAdapter<String> semestreAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, SEMESTRES);
        semestreAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSemestre.setAdapter(semestreAdapter);

        // Configurar spinner de carrera
        ArrayAdapter<String> carreraAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, CARRERAS);
        carreraAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCarrera.setAdapter(carreraAdapter);
    }

    private void configurarDatePicker(EditText etFechaNacimiento) {
        etFechaNacimiento.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();

            // Si ya hay una fecha, usarla como fecha inicial
            String fechaActual = etFechaNacimiento.getText().toString();
            if (!fechaActual.isEmpty()) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    calendar.setTime(sdf.parse(fechaActual));
                } catch (Exception e) {
                    // Si hay error, usar fecha actual
                }
            }

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    getContext(),
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

            // Establecer fecha máxima (hoy)
            datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());

            // Establecer fecha mínima (hace 80 años)
            Calendar minDate = Calendar.getInstance();
            minDate.add(Calendar.YEAR, -80);
            datePickerDialog.getDatePicker().setMinDate(minDate.getTimeInMillis());

            datePickerDialog.show();
        });

        // Hacer que no se pueda escribir directamente
        etFechaNacimiento.setFocusable(false);
        etFechaNacimiento.setClickable(true);
    }

    private void cargarDatosExistentes(String alumnoId, EditText etNombre, EditText etCurp,
                                       EditText etFechaNacimiento, Spinner spinnerSexo,
                                       EditText etNumControl, Spinner spinnerSemestre,
                                       Spinner spinnerCarrera, EditText etEspecialidad,
                                       EditText etTelefono, EditText etEmail, EditText etDireccion) {

        JSONObject alumno = fileManager.buscarAlumnoPorId(alumnoId);
        if (alumno != null) {
            try {
                etNombre.setText(alumno.optString("nombre", ""));
                etCurp.setText(alumno.optString("curp", ""));
                etFechaNacimiento.setText(alumno.optString("fechaNacimiento", ""));
                etNumControl.setText(alumno.optString("numControl", ""));
                etEspecialidad.setText(alumno.optString("especialidad", ""));
                etTelefono.setText(alumno.optString("telefono", ""));
                etEmail.setText(alumno.optString("email", ""));
                etDireccion.setText(alumno.optString("direccion", ""));

                // Seleccionar valores en spinners
                String sexo = alumno.optString("sexo", "");
                String semestre = alumno.optString("semestre", "");
                String carrera = alumno.optString("carrera", "");

                int sexoPosition = Arrays.asList(SEXOS).indexOf(sexo);
                if (sexoPosition >= 0) spinnerSexo.setSelection(sexoPosition);

                int semestrePosition = Arrays.asList(SEMESTRES).indexOf(semestre);
                if (semestrePosition >= 0) spinnerSemestre.setSelection(semestrePosition);

                int carreraPosition = Arrays.asList(CARRERAS).indexOf(carrera);
                if (carreraPosition >= 0) spinnerCarrera.setSelection(carreraPosition);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private boolean validarFormulario(EditText etNombre, EditText etCurp, EditText etFechaNacimiento,
                                      EditText etNumControl, EditText etTelefono, EditText etEmail) {

        // Validar nombre (obligatorio)
        if (etNombre.getText().toString().trim().isEmpty()) {
            Toast.makeText(getContext(), "El nombre es obligatorio", Toast.LENGTH_SHORT).show();
            etNombre.requestFocus();
            return false;
        }

        // Validar número de control (obligatorio y numérico)
        String numControl = etNumControl.getText().toString().trim();
        if (numControl.isEmpty()) {
            Toast.makeText(getContext(), "El número de control es obligatorio", Toast.LENGTH_SHORT).show();
            etNumControl.requestFocus();
            return false;
        }

        // Validar CURP (si se proporciona)
        String curp = etCurp.getText().toString().trim().toUpperCase();
        if (!curp.isEmpty() && !validarCURP(curp)) {
            Toast.makeText(getContext(), "El formato del CURP no es válido", Toast.LENGTH_SHORT).show();
            etCurp.requestFocus();
            return false;
        }

        // Validar teléfono (si se proporciona)
        String telefono = etTelefono.getText().toString().trim();
        if (!telefono.isEmpty() && !validarTelefono(telefono)) {
            Toast.makeText(getContext(), "El formato del teléfono no es válido", Toast.LENGTH_SHORT).show();
            etTelefono.requestFocus();
            return false;
        }

        // Validar email (si se proporciona)
        String email = etEmail.getText().toString().trim();
        if (!email.isEmpty() && !validarEmail(email)) {
            Toast.makeText(getContext(), "El formato del email no es válido", Toast.LENGTH_SHORT).show();
            etEmail.requestFocus();
            return false;
        }

        return true;
    }

    private boolean validarCURP(String curp) {
        // Patrón para CURP mexicano
        String patronCURP = "^[A-Z]{1}[AEIOU]{1}[A-Z]{2}[0-9]{2}(0[1-9]|1[0-2])(0[1-9]|1[0-9]|2[0-9]|3[01])[HM]{1}(AS|BC|BS|CC|CS|CH|CL|CM|DF|DG|GT|GR|HG|JC|MC|MN|MS|NT|NL|OC|PL|QT|QR|SP|SL|SR|TC|TS|TL|VZ|YN|ZS|NE)[B-DF-HJ-NP-TV-Z]{3}[0-9A-Z]{1}[0-9]{1}$";
        return Pattern.matches(patronCURP, curp);
    }

    private boolean validarTelefono(String telefono) {
        // Permitir formatos: 1234567890, (123) 456-7890, 123-456-7890, etc.
        String telefonoLimpio = telefono.replaceAll("[^0-9]", "");
        return telefonoLimpio.length() == 10;
    }

    private boolean validarEmail(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private void guardarAlumno(String alumnoId, EditText etNombre, EditText etCurp,
                               EditText etFechaNacimiento, Spinner spinnerSexo, EditText etNumControl,
                               Spinner spinnerSemestre, Spinner spinnerCarrera, EditText etEspecialidad,
                               EditText etTelefono, EditText etEmail, EditText etDireccion) {

        try {
            JSONObject alumno = new JSONObject();

            // Si es nuevo generar el ID
            if (alumnoId == null) {
                alumnoId = "alumno_" + System.currentTimeMillis();
            }

            // Guardar datos del alumno
            alumno.put("id", alumnoId);
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

            // Guardar en archivo
            boolean exito;
            if (fileManager.buscarAlumnoPorId(alumnoId) != null) {
                // Actualizar existente
                exito = fileManager.actualizarAlumno(alumnoId, alumno);
            } else {
                // Agregar nuevo
                exito = fileManager.agregarAlumno(alumno);
            }

            if (!exito) {
                Toast.makeText(getContext(), "Error al guardar el alumno", Toast.LENGTH_SHORT).show();
            }

        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error al procesar los datos", Toast.LENGTH_SHORT).show();
        }
    }

    private void eliminarAlumno(String alumnoId) {
        new AlertDialog.Builder(getContext())
                .setTitle("Eliminar Alumno")
                .setMessage("¿Está seguro de que desea eliminar este alumno?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    boolean exito = fileManager.eliminarAlumno(alumnoId);
                    if (exito) {
                        cargarAlumnos();
                        Toast.makeText(getContext(), "Alumno eliminado", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Error al eliminar el alumno", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    /**
     * Descarga PDF con todos los alumnos
     */
    private void descargarPDFAlumnos() {
        if (!PermissionsHelper.hasStoragePermissions(requireContext())) {
            PermissionsHelper.requestStoragePermissions(requireActivity());
            return;
        }

        List<JSONObject> alumnos = fileManager.cargarAlumnos();
        
        if (alumnos.isEmpty()) {
            Toast.makeText(getContext(), "No hay alumnos registrados para descargar", Toast.LENGTH_SHORT).show();
            return;
        }

        // Generar PDF en hilo secundario
        new Thread(() -> {
            String pdfPath = pdfGenerator.generarPDFAlumnos(alumnos);
            
            // Volver al hilo principal para mostrar el resultado
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (pdfPath != null) {
                        Toast.makeText(getContext(), "PDF guardado en: " + pdfPath, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getContext(), "Error al generar PDF", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start();
    }

    /**
     * Descarga PDF de un alumno individual
     */
    private void descargarPDFAlumnoIndividual(JSONObject alumno) {
        if (!PermissionsHelper.hasStoragePermissions(requireContext())) {
            PermissionsHelper.requestStoragePermissions(requireActivity());
            return;
        }

        // Generar PDF en hilo secundario
        new Thread(() -> {
            String pdfPath = pdfGenerator.generarPDFAlumnoIndividual(alumno);
            
            // Volver al hilo principal para mostrar el resultado
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (pdfPath != null) {
                        Toast.makeText(getContext(), "PDF guardado en: " + pdfPath, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getContext(), "Error al generar PDF", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start();
    }

    /**
     * Maneja el resultado de solicitud de permisos
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PermissionsHelper.STORAGE_PERMISSION_REQUEST_CODE) {
            if (PermissionsHelper.arePermissionsGranted(grantResults)) {
                Toast.makeText(getContext(), "Permisos concedidos. Puede descargar PDFs ahora.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Permisos denegados. No se pueden generar PDFs.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}