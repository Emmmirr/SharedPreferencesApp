package com.example.sharedpreferencesapp;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class CargaAcademicaFragment extends Fragment {

    // Información general
    private TextInputEditText etNombreCompleto, etCurp, etFechaNacimiento;
    private RadioGroup rgSexo;
    private RadioButton rbMasculino, rbFemenino;

    // Información escolar
    private TextInputEditText etNumeroControl;
    private Spinner spSemestre, spCarrera;

    // Información de contacto
    private TextInputEditText etTelefono, etEmail, etDireccion, etEmergencyContact, etEmergencyPhone;
    private TextInputEditText etCondicionesMedicas;

    // Botón guardar
    private Button btnGuardar;

    // Para el calendario
    private Calendar calendar;

    // Profile Manager
    private ProfileManager profileManager;

    public CargaAcademicaFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_carga_academica, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Inicializar el ProfileManager
        profileManager = new ProfileManager(requireContext());

        // Inicializar Calendar para el selector de fecha
        calendar = Calendar.getInstance();

        // Inicializar vistas
        initViews(view);

        // Configurar spinners
        setupSpinners();

        // Configurar el selector de fecha
        setupDatePicker();

        // Cargar datos del estudiante si existen
        loadStudentData();

        // Configurar botón guardar
        btnGuardar.setOnClickListener(v -> saveStudentData());
    }

    private void initViews(View view) {
        // Información general
        etNombreCompleto = view.findViewById(R.id.et_nombre_completo);
        etCurp = view.findViewById(R.id.et_curp);
        etFechaNacimiento = view.findViewById(R.id.et_fecha_nacimiento);
        rgSexo = view.findViewById(R.id.rg_sexo);
        rbMasculino = view.findViewById(R.id.rb_masculino);
        rbFemenino = view.findViewById(R.id.rb_femenino);

        // Información escolar
        etNumeroControl = view.findViewById(R.id.et_numero_control);
        spSemestre = view.findViewById(R.id.sp_semestre);
        spCarrera = view.findViewById(R.id.sp_carrera);
        etCondicionesMedicas = view.findViewById(R.id.et_condiciones_medicas);

        // Información de contacto
        etTelefono = view.findViewById(R.id.et_telefono);
        etEmail = view.findViewById(R.id.et_email);
        etDireccion = view.findViewById(R.id.et_direccion);
        etEmergencyContact = view.findViewById(R.id.et_emergency_contact);
        etEmergencyPhone = view.findViewById(R.id.et_emergency_phone);

        // Botón guardar
        btnGuardar = view.findViewById(R.id.btn_guardar);
    }

    private void setupSpinners() {
        // Configurar spinner de semestres (1-15)
        String[] semestres = new String[15];
        for (int i = 0; i < 15; i++) {
            semestres[i] = String.valueOf(i + 1);
        }
        ArrayAdapter<String> semestreAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                semestres);
        semestreAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spSemestre.setAdapter(semestreAdapter);

        // Configurar spinner de carreras
        String[] carreras = new String[]{
                "Ing. Sistemas Computacionales",
                "Ing. Civil",
                "Ing. Gestión Empresarial",
                "Lic. Contabilidad",
                "Ing. Informática"
        };
        ArrayAdapter<String> carreraAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                carreras);
        carreraAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCarrera.setAdapter(carreraAdapter);
    }

    private void setupDatePicker() {
        etFechaNacimiento.setFocusable(false);
        etFechaNacimiento.setClickable(true);
        etFechaNacimiento.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    requireContext(),
                    (view, year, month, dayOfMonth) -> {
                        calendar.set(Calendar.YEAR, year);
                        calendar.set(Calendar.MONTH, month);
                        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        updateDateInView();
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH));

            // Establecer fecha máxima (hoy)
            datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());

            datePickerDialog.show();
        });
    }

    private void updateDateInView() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        etFechaNacimiento.setText(sdf.format(calendar.getTime()));
    }

    private void loadStudentData() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show();
            return;
        }

        profileManager.obtenerPerfilActual(
                profile -> {
                    if (profile != null && isAdded()) {
                        // Cargar información general
                        etNombreCompleto.setText(profile.getFullName());
                        etCurp.setText(profile.getCurp());
                        etFechaNacimiento.setText(profile.getDateOfBirth());

                        // Cargar información de género/sexo
                        if (profile.getGender() != null) {
                            if (profile.getGender().equalsIgnoreCase("Masculino")) {
                                rbMasculino.setChecked(true);
                            } else if (profile.getGender().equalsIgnoreCase("Femenino")) {
                                rbFemenino.setChecked(true);
                            }
                        }

                        // Cargar información escolar
                        etNumeroControl.setText(profile.getControlNumber());
                        etCondicionesMedicas.setText(profile.getMedicalConditions());

                        // Seleccionar carrera en el spinner
                        if (profile.getCareer() != null && !profile.getCareer().isEmpty()) {
                            ArrayAdapter<String> adapter = (ArrayAdapter<String>) spCarrera.getAdapter();
                            int position = adapter.getPosition(profile.getCareer());
                            if (position >= 0) {
                                spCarrera.setSelection(position);
                            }
                        }

                        // Cargar información de contacto
                        etTelefono.setText(profile.getPhoneNumber());
                        etEmail.setText(profile.getEmail());
                        etEmergencyContact.setText(profile.getEmergencyContactName());
                        etEmergencyPhone.setText(profile.getEmergencyContactPhone());

                        // Deshabilitar número de control ya que ya está asignado
                        if (!TextUtils.isEmpty(profile.getControlNumber())) {
                            etNumeroControl.setEnabled(false);
                        }

                        // Deshabilitar email ya que es un campo clave
                        etEmail.setEnabled(false);
                    }
                },
                error -> {
                    if (isAdded()) {
                        Toast.makeText(requireContext(),
                                "Error al cargar datos: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void saveStudentData() {
        // Validar campos obligatorios
        if (TextUtils.isEmpty(etNombreCompleto.getText())) {
            Toast.makeText(requireContext(), "El nombre completo es obligatorio", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(etCurp.getText())) {
            Toast.makeText(requireContext(), "La CURP es obligatoria", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(etFechaNacimiento.getText())) {
            Toast.makeText(requireContext(), "La fecha de nacimiento es obligatoria", Toast.LENGTH_SHORT).show();
            return;
        }

        if (rgSexo.getCheckedRadioButtonId() == -1) {
            Toast.makeText(requireContext(), "Debes seleccionar tu sexo", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(etNumeroControl.getText())) {
            Toast.makeText(requireContext(), "El número de control es obligatorio", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(etTelefono.getText())) {
            Toast.makeText(requireContext(), "El teléfono es obligatorio", Toast.LENGTH_SHORT).show();
            return;
        }

        // Obtener valores
        String fullName = etNombreCompleto.getText().toString().trim();
        String curp = etCurp.getText().toString().trim();
        String dateOfBirth = etFechaNacimiento.getText().toString().trim();
        String gender = rbMasculino.isChecked() ? "Masculino" : "Femenino";
        String controlNumber = etNumeroControl.getText().toString().trim();
        String career = spCarrera.getSelectedItem().toString();
        String phoneNumber = etTelefono.getText().toString().trim();
        String emergencyContactName = etEmergencyContact.getText().toString().trim();
        String emergencyContactPhone = etEmergencyPhone.getText().toString().trim();
        String medicalConditions = etCondicionesMedicas.getText().toString().trim();

        // Obtener el perfil actual
        profileManager.obtenerPerfilActual(
                profile -> {
                    if (profile != null && isAdded()) {
                        // Actualizar datos del perfil
                        profile.setFullName(fullName);
                        profile.setCurp(curp);
                        profile.setDateOfBirth(dateOfBirth);
                        profile.setGender(gender);
                        profile.setControlNumber(controlNumber);
                        profile.setCareer(career);
                        profile.setPhoneNumber(phoneNumber);
                        profile.setEmergencyContactName(emergencyContactName);
                        profile.setEmergencyContactPhone(emergencyContactPhone);
                        profile.setMedicalConditions(medicalConditions);

                        // Calcular si el perfil está completo
                        int completeness = profile.getProfileCompleteness();
                        profile.setProfileComplete(completeness == 100);

                        // Establecer la fecha de actualización
                        profile.setUpdatedAt(String.valueOf(System.currentTimeMillis()));

                        // Guardar el perfil actualizado
                        profileManager.actualizarPerfilActual(profile,
                                () -> {
                                    if (isAdded()) {
                                        Toast.makeText(requireContext(),
                                                "Datos guardados correctamente",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                },
                                error -> {
                                    if (isAdded()) {
                                        Toast.makeText(requireContext(),
                                                "Error al guardar datos: " + error.getMessage(),
                                                Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                },
                error -> {
                    if (isAdded()) {
                        Toast.makeText(requireContext(),
                                "Error al obtener perfil: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }
}