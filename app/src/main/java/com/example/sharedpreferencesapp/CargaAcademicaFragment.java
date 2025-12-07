package com.example.sharedpreferencesapp;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
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

    private static final int WIZARD_THRESHOLD = 50; // Umbral para mostrar wizard (< 50%)

    // Contenedores de vistas
    private View wizardView;
    private View profileView;
    private View editFormView;

    // Vistas del wizard
    private TextView tvWizardStep;
    private ProgressBar progressWizard;
    private androidx.cardview.widget.CardView cardStep1, cardStep2, cardStep3;
    private LinearLayout btnWizardBack, btnWizardNext, btnWizardFinish;
    private int currentWizardStep = 1;

    // Campos del wizard - Paso 1
    private TextInputEditText etWizardFullName, etWizardCurp, etWizardDateBirth;
    private RadioGroup rgWizardGender;
    private RadioButton rbWizardMasculino, rbWizardFemenino;

    // Campos del wizard - Paso 2
    private TextInputEditText etWizardControlNumber, etWizardMedicalConditions;
    private Spinner spWizardCareer, spWizardSemester;

    // Campos del wizard - Paso 3
    private TextInputEditText etWizardPhone, etWizardEmail, etWizardAddress;
    private TextInputEditText etWizardEmergencyContact, etWizardEmergencyPhone;

    // Vistas de solo lectura
    private LinearLayout btnEditProfile;
    private TextView tvFullName, tvCurp, tvDateOfBirth, tvGender;
    private TextView tvControlNumber, tvCareer, tvSemester, tvMedicalConditions;
    private TextView tvPhone, tvEmail, tvAddress, tvEmergencyContact, tvEmergencyPhone;

    // Formulario de edición (reutiliza fragment_carga_academica.xml)
    private TextInputEditText etNombreCompleto, etCurp, etFechaNacimiento;
    private RadioGroup rgSexo;
    private RadioButton rbMasculino, rbFemenino;
    private TextInputEditText etNumeroControl;
    private Spinner spSemestre, spCarrera;
    private TextInputEditText etTelefono, etEmail, etDireccion, etEmergencyContact, etEmergencyPhone;
    private TextInputEditText etCondicionesMedicas;
    private android.widget.Button btnGuardar;

    // Para el calendario
    private Calendar calendar;

    // Profile Manager
    private ProfileManager profileManager;
    private UserProfile currentProfile;

    public CargaAcademicaFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Usar un FrameLayout como contenedor principal
        View rootView = inflater.inflate(R.layout.fragment_carga_academica_container, container, false);
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        profileManager = new ProfileManager(requireContext());
        calendar = Calendar.getInstance();

        // Cargar perfil y decidir qué vista mostrar
        loadProfileAndShowView();
    }

    private void loadProfileAndShowView() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show();
            return;
        }

        profileManager.obtenerPerfilActual(
                profile -> {
                    if (profile != null && isAdded()) {
                        currentProfile = profile;
                        int completeness = profile.getProfileCompleteness();

                        // Si el perfil está muy incompleto, mostrar wizard
                        if (completeness < WIZARD_THRESHOLD) {
                            showWizardView();
                        } else {
                            // Si está completo, mostrar vista de solo lectura
                            showProfileView();
                        }
                    }
                },
                error -> {
                    if (isAdded()) {
                        // Si no hay perfil, mostrar wizard
                        showWizardView();
                    }
                }
        );
    }

    private void showWizardView() {
        if (getView() == null) return;

        ViewGroup container = getView().findViewById(R.id.container);
        if (container == null) return;

        container.removeAllViews();

        wizardView = LayoutInflater.from(requireContext())
                .inflate(R.layout.fragment_student_wizard, container, false);
        container.addView(wizardView);

        initWizardViews();
        setupWizardSpinners();
        setupWizardDatePicker();
        loadWizardData();
        setupWizardNavigation();
    }

    private void showProfileView() {
        if (getView() == null) return;

        ViewGroup container = getView().findViewById(R.id.container);
        if (container == null) return;

        container.removeAllViews();

        profileView = LayoutInflater.from(requireContext())
                .inflate(R.layout.fragment_student_profile_view, container, false);
        container.addView(profileView);

        initProfileViews();
        loadProfileData();
        setupEditButton();
    }

    private void showEditFormView() {
        if (getView() == null) return;

        ViewGroup container = getView().findViewById(R.id.container);
        if (container == null) return;

        container.removeAllViews();

        editFormView = LayoutInflater.from(requireContext())
                .inflate(R.layout.fragment_carga_academica, container, false);
        container.addView(editFormView);

        initEditFormViews();
        setupSpinners();
        setupDatePicker();
        loadEditFormData();
        setupSaveButton();
    }

    // ========== WIZARD METHODS ==========

    private void initWizardViews() {
        tvWizardStep = wizardView.findViewById(R.id.tv_wizard_step);
        progressWizard = wizardView.findViewById(R.id.progress_wizard);
        cardStep1 = wizardView.findViewById(R.id.card_step1);
        cardStep2 = wizardView.findViewById(R.id.card_step2);
        cardStep3 = wizardView.findViewById(R.id.card_step3);
        btnWizardBack = wizardView.findViewById(R.id.btn_wizard_back);
        btnWizardNext = wizardView.findViewById(R.id.btn_wizard_next);
        btnWizardFinish = wizardView.findViewById(R.id.btn_wizard_finish);

        // Paso 1
        etWizardFullName = wizardView.findViewById(R.id.et_wizard_full_name);
        etWizardCurp = wizardView.findViewById(R.id.et_wizard_curp);
        etWizardDateBirth = wizardView.findViewById(R.id.et_wizard_date_birth);
        rgWizardGender = wizardView.findViewById(R.id.rg_wizard_gender);
        rbWizardMasculino = wizardView.findViewById(R.id.rb_wizard_masculino);
        rbWizardFemenino = wizardView.findViewById(R.id.rb_wizard_femenino);

        // Paso 2
        etWizardControlNumber = wizardView.findViewById(R.id.et_wizard_control_number);
        spWizardCareer = wizardView.findViewById(R.id.sp_wizard_career);
        spWizardSemester = wizardView.findViewById(R.id.sp_wizard_semester);
        etWizardMedicalConditions = wizardView.findViewById(R.id.et_wizard_medical_conditions);

        // Paso 3
        etWizardPhone = wizardView.findViewById(R.id.et_wizard_phone);
        etWizardEmail = wizardView.findViewById(R.id.et_wizard_email);
        etWizardAddress = wizardView.findViewById(R.id.et_wizard_address);
        etWizardEmergencyContact = wizardView.findViewById(R.id.et_wizard_emergency_contact);
        etWizardEmergencyPhone = wizardView.findViewById(R.id.et_wizard_emergency_phone);
    }

    private void setupWizardSpinners() {
        // Spinner de semestres
        String[] semestres = new String[15];
        for (int i = 0; i < 15; i++) {
            semestres[i] = String.valueOf(i + 1);
        }
        ArrayAdapter<String> semestreAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                semestres);
        semestreAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spWizardSemester.setAdapter(semestreAdapter);

        // Spinner de carreras
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
        spWizardCareer.setAdapter(carreraAdapter);
    }

    private void setupWizardDatePicker() {
        etWizardDateBirth.setFocusable(false);
        etWizardDateBirth.setClickable(true);
        etWizardDateBirth.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    requireContext(),
                    (view, year, month, dayOfMonth) -> {
                        calendar.set(Calendar.YEAR, year);
                        calendar.set(Calendar.MONTH, month);
                        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                        etWizardDateBirth.setText(sdf.format(calendar.getTime()));
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
            datePickerDialog.show();
        });
    }

    private void loadWizardData() {
        if (currentProfile != null) {
            etWizardFullName.setText(currentProfile.getFullName());
            etWizardCurp.setText(currentProfile.getCurp());
            etWizardDateBirth.setText(currentProfile.getDateOfBirth());
            if (currentProfile.getGender() != null) {
                if (currentProfile.getGender().equalsIgnoreCase("Masculino")) {
                    rbWizardMasculino.setChecked(true);
                } else if (currentProfile.getGender().equalsIgnoreCase("Femenino")) {
                    rbWizardFemenino.setChecked(true);
                }
            }
            etWizardControlNumber.setText(currentProfile.getControlNumber());
            etWizardMedicalConditions.setText(currentProfile.getMedicalConditions());
            if (currentProfile.getCareer() != null && !currentProfile.getCareer().isEmpty()) {
                ArrayAdapter<String> adapter = (ArrayAdapter<String>) spWizardCareer.getAdapter();
                int position = adapter.getPosition(currentProfile.getCareer());
                if (position >= 0) {
                    spWizardCareer.setSelection(position);
                }
            }
            etWizardPhone.setText(currentProfile.getPhoneNumber());
            etWizardEmail.setText(currentProfile.getEmail());
            // etWizardAddress no tiene campo correspondiente en UserProfile, se deja vacío
            etWizardEmergencyContact.setText(currentProfile.getEmergencyContactName());
            etWizardEmergencyPhone.setText(currentProfile.getEmergencyContactPhone());
        }
        updateWizardStep();
    }

    private void setupWizardNavigation() {
        btnWizardNext.setOnClickListener(v -> {
            if (validateCurrentStep()) {
                if (currentWizardStep < 3) {
                    currentWizardStep++;
                    updateWizardStep();
                }
            }
        });

        btnWizardBack.setOnClickListener(v -> {
            if (currentWizardStep > 1) {
                currentWizardStep--;
                updateWizardStep();
            }
        });

        btnWizardFinish.setOnClickListener(v -> {
            if (validateCurrentStep()) {
                saveWizardData();
            }
        });
    }

    private boolean validateCurrentStep() {
        switch (currentWizardStep) {
            case 1:
                if (TextUtils.isEmpty(etWizardFullName.getText())) {
                    Toast.makeText(requireContext(), "El nombre completo es obligatorio", Toast.LENGTH_SHORT).show();
                    return false;
                }
                if (TextUtils.isEmpty(etWizardCurp.getText())) {
                    Toast.makeText(requireContext(), "La CURP es obligatoria", Toast.LENGTH_SHORT).show();
                    return false;
                }
                if (TextUtils.isEmpty(etWizardDateBirth.getText())) {
                    Toast.makeText(requireContext(), "La fecha de nacimiento es obligatoria", Toast.LENGTH_SHORT).show();
                    return false;
                }
                if (rgWizardGender.getCheckedRadioButtonId() == -1) {
                    Toast.makeText(requireContext(), "Debes seleccionar tu sexo", Toast.LENGTH_SHORT).show();
                    return false;
                }
                return true;
            case 2:
                if (TextUtils.isEmpty(etWizardControlNumber.getText())) {
                    Toast.makeText(requireContext(), "El número de control es obligatorio", Toast.LENGTH_SHORT).show();
                    return false;
                }
                return true;
            case 3:
                if (TextUtils.isEmpty(etWizardPhone.getText())) {
                    Toast.makeText(requireContext(), "El teléfono es obligatorio", Toast.LENGTH_SHORT).show();
                    return false;
                }
                return true;
            default:
                return true;
        }
    }

    private void updateWizardStep() {
        tvWizardStep.setText("Paso " + currentWizardStep + " de 3");
        progressWizard.setProgress((currentWizardStep * 100) / 3);

        // Mostrar/ocultar cards
        cardStep1.setVisibility(currentWizardStep == 1 ? View.VISIBLE : View.GONE);
        cardStep2.setVisibility(currentWizardStep == 2 ? View.VISIBLE : View.GONE);
        cardStep3.setVisibility(currentWizardStep == 3 ? View.VISIBLE : View.GONE);

        // Mostrar/ocultar botones
        btnWizardBack.setVisibility(currentWizardStep > 1 ? View.VISIBLE : View.GONE);
        btnWizardNext.setVisibility(currentWizardStep < 3 ? View.VISIBLE : View.GONE);
        btnWizardFinish.setVisibility(currentWizardStep == 3 ? View.VISIBLE : View.GONE);
    }

    private void saveWizardData() {
        if (currentProfile == null) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) return;
            currentProfile = new UserProfile(user.getUid(), user.getEmail(),
                    user.getDisplayName() != null ? user.getDisplayName() : "", "email");
        }

        // Actualizar datos del perfil
        currentProfile.setFullName(etWizardFullName.getText().toString().trim());
        currentProfile.setCurp(etWizardCurp.getText().toString().trim());
        currentProfile.setDateOfBirth(etWizardDateBirth.getText().toString().trim());
        currentProfile.setGender(rbWizardMasculino.isChecked() ? "Masculino" : "Femenino");
        currentProfile.setControlNumber(etWizardControlNumber.getText().toString().trim());
        currentProfile.setCareer(spWizardCareer.getSelectedItem().toString());
        currentProfile.setPhoneNumber(etWizardPhone.getText().toString().trim());
        currentProfile.setEmergencyContactName(etWizardEmergencyContact.getText().toString().trim());
        currentProfile.setEmergencyContactPhone(etWizardEmergencyPhone.getText().toString().trim());
        currentProfile.setMedicalConditions(etWizardMedicalConditions.getText().toString().trim());

        int completeness = currentProfile.getProfileCompleteness();
        currentProfile.setProfileComplete(completeness >= 70);
        currentProfile.setUpdatedAt(String.valueOf(System.currentTimeMillis()));

        profileManager.actualizarPerfilActual(currentProfile,
                () -> {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Datos guardados correctamente", Toast.LENGTH_SHORT).show();
                        // Recargar vista apropiada
                        loadProfileAndShowView();
                    }
                },
                error -> {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Error al guardar: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ========== PROFILE VIEW METHODS ==========

    private void initProfileViews() {
        btnEditProfile = profileView.findViewById(R.id.btn_edit_profile);
        tvFullName = profileView.findViewById(R.id.tv_full_name);
        tvCurp = profileView.findViewById(R.id.tv_curp);
        tvDateOfBirth = profileView.findViewById(R.id.tv_date_of_birth);
        tvGender = profileView.findViewById(R.id.tv_gender);
        tvControlNumber = profileView.findViewById(R.id.tv_control_number);
        tvCareer = profileView.findViewById(R.id.tv_career);
        tvSemester = profileView.findViewById(R.id.tv_semester);
        tvMedicalConditions = profileView.findViewById(R.id.tv_medical_conditions);
        tvPhone = profileView.findViewById(R.id.tv_phone);
        tvEmail = profileView.findViewById(R.id.tv_email);
        tvAddress = profileView.findViewById(R.id.tv_address);
        tvEmergencyContact = profileView.findViewById(R.id.tv_emergency_contact);
        tvEmergencyPhone = profileView.findViewById(R.id.tv_emergency_phone);
    }

    private void loadProfileData() {
        if (currentProfile != null) {
            tvFullName.setText(currentProfile.getFullName().isEmpty() ? "No especificado" : currentProfile.getFullName());
            tvCurp.setText(currentProfile.getCurp().isEmpty() ? "No especificado" : currentProfile.getCurp());
            tvDateOfBirth.setText(currentProfile.getDateOfBirth().isEmpty() ? "No especificado" : currentProfile.getDateOfBirth());
            tvGender.setText(currentProfile.getGender().isEmpty() ? "No especificado" : currentProfile.getGender());
            tvControlNumber.setText(currentProfile.getControlNumber().isEmpty() ? "No especificado" : currentProfile.getControlNumber());
            tvCareer.setText(currentProfile.getCareer().isEmpty() ? "No especificado" : currentProfile.getCareer());
            tvSemester.setText("No especificado"); // No hay campo semestre en UserProfile
            tvMedicalConditions.setText(currentProfile.getMedicalConditions().isEmpty() ? "Ninguna" : currentProfile.getMedicalConditions());
            tvPhone.setText(currentProfile.getPhoneNumber().isEmpty() ? "No especificado" : currentProfile.getPhoneNumber());
            tvEmail.setText(currentProfile.getEmail().isEmpty() ? "No especificado" : currentProfile.getEmail());
            tvAddress.setText("No especificado"); // No hay campo dirección en UserProfile
            tvEmergencyContact.setText(currentProfile.getEmergencyContactName().isEmpty() ? "No especificado" : currentProfile.getEmergencyContactName());
            tvEmergencyPhone.setText(currentProfile.getEmergencyContactPhone().isEmpty() ? "No especificado" : currentProfile.getEmergencyContactPhone());
        }
    }

    private void setupEditButton() {
        btnEditProfile.setOnClickListener(v -> showEditFormView());
    }

    // ========== EDIT FORM METHODS ==========

    private void initEditFormViews() {
        etNombreCompleto = editFormView.findViewById(R.id.et_nombre_completo);
        etCurp = editFormView.findViewById(R.id.et_curp);
        etFechaNacimiento = editFormView.findViewById(R.id.et_fecha_nacimiento);
        rgSexo = editFormView.findViewById(R.id.rg_sexo);
        rbMasculino = editFormView.findViewById(R.id.rb_masculino);
        rbFemenino = editFormView.findViewById(R.id.rb_femenino);
        etNumeroControl = editFormView.findViewById(R.id.et_numero_control);
        spSemestre = editFormView.findViewById(R.id.sp_semestre);
        spCarrera = editFormView.findViewById(R.id.sp_carrera);
        etCondicionesMedicas = editFormView.findViewById(R.id.et_condiciones_medicas);
        etTelefono = editFormView.findViewById(R.id.et_telefono);
        etEmail = editFormView.findViewById(R.id.et_email);
        etDireccion = editFormView.findViewById(R.id.et_direccion);
        etEmergencyContact = editFormView.findViewById(R.id.et_emergency_contact);
        etEmergencyPhone = editFormView.findViewById(R.id.et_emergency_phone);
        btnGuardar = editFormView.findViewById(R.id.btn_guardar);
    }

    private void setupSpinners() {
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
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                        etFechaNacimiento.setText(sdf.format(calendar.getTime()));
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
            datePickerDialog.show();
        });
    }

    private void loadEditFormData() {
        if (currentProfile != null) {
            etNombreCompleto.setText(currentProfile.getFullName());
            etCurp.setText(currentProfile.getCurp());
            etFechaNacimiento.setText(currentProfile.getDateOfBirth());
            if (currentProfile.getGender() != null) {
                if (currentProfile.getGender().equalsIgnoreCase("Masculino")) {
                    rbMasculino.setChecked(true);
                } else if (currentProfile.getGender().equalsIgnoreCase("Femenino")) {
                    rbFemenino.setChecked(true);
                }
            }
            etNumeroControl.setText(currentProfile.getControlNumber());
            etCondicionesMedicas.setText(currentProfile.getMedicalConditions());
            if (currentProfile.getCareer() != null && !currentProfile.getCareer().isEmpty()) {
                ArrayAdapter<String> adapter = (ArrayAdapter<String>) spCarrera.getAdapter();
                int position = adapter.getPosition(currentProfile.getCareer());
                if (position >= 0) {
                    spCarrera.setSelection(position);
                }
            }
            etTelefono.setText(currentProfile.getPhoneNumber());
            etEmail.setText(currentProfile.getEmail());
            etEmergencyContact.setText(currentProfile.getEmergencyContactName());
            etEmergencyPhone.setText(currentProfile.getEmergencyContactPhone());

            if (!TextUtils.isEmpty(currentProfile.getControlNumber())) {
                etNumeroControl.setEnabled(false);
            }
            etEmail.setEnabled(false);
        }
    }

    private void setupSaveButton() {
        btnGuardar.setOnClickListener(v -> saveEditFormData());
    }

    private void saveEditFormData() {
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

        if (currentProfile == null) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) return;
            currentProfile = new UserProfile(user.getUid(), user.getEmail(),
                    user.getDisplayName() != null ? user.getDisplayName() : "", "email");
        }

        currentProfile.setFullName(etNombreCompleto.getText().toString().trim());
        currentProfile.setCurp(etCurp.getText().toString().trim());
        currentProfile.setDateOfBirth(etFechaNacimiento.getText().toString().trim());
        currentProfile.setGender(rbMasculino.isChecked() ? "Masculino" : "Femenino");
        currentProfile.setControlNumber(etNumeroControl.getText().toString().trim());
        currentProfile.setCareer(spCarrera.getSelectedItem().toString());
        currentProfile.setPhoneNumber(etTelefono.getText().toString().trim());
        currentProfile.setEmergencyContactName(etEmergencyContact.getText().toString().trim());
        currentProfile.setEmergencyContactPhone(etEmergencyPhone.getText().toString().trim());
        currentProfile.setMedicalConditions(etCondicionesMedicas.getText().toString().trim());

        int completeness = currentProfile.getProfileCompleteness();
        currentProfile.setProfileComplete(completeness >= 70);
        currentProfile.setUpdatedAt(String.valueOf(System.currentTimeMillis()));

        profileManager.actualizarPerfilActual(currentProfile,
                () -> {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Datos guardados correctamente", Toast.LENGTH_SHORT).show();
                        loadProfileAndShowView();
                    }
                },
                error -> {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Error al guardar: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
