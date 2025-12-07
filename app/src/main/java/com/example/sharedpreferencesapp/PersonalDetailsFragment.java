package com.example.sharedpreferencesapp;

import android.app.DatePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class PersonalDetailsFragment extends Fragment {

    private static final String TAG = "PersonalDetailsFragment";

    private ProfileManager profileManager;
    private UserProfile currentProfile;

    // Vistas
    private TextView tvFullName, tvDateOfBirth, tvGender, tvCurp, tvCareer, tvControlNumber;
    private TextView tvPhoneNumber, tvMedicalConditions, tvEmergencyContact;
    private View optionFullName, optionDateOfBirth, optionGender, optionCurp, optionCareer;
    private View optionControlNumber, optionPhoneNumber, optionMedicalConditions, optionEmergencyContact;
    private Toolbar toolbar;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_personal_details, container, false);

        initializeViews(view);
        setupToolbar();
        setupEventListeners();

        profileManager = new ProfileManager(requireContext());
        cargarPerfilUsuario();

        return view;
    }

    private void initializeViews(View view) {
        toolbar = view.findViewById(R.id.toolbar);

        // TextViews
        tvFullName = view.findViewById(R.id.tvFullName);
        tvDateOfBirth = view.findViewById(R.id.tvDateOfBirth);
        tvGender = view.findViewById(R.id.tvGender);
        tvCurp = view.findViewById(R.id.tvCurp);
        tvCareer = view.findViewById(R.id.tvCareer);
        tvControlNumber = view.findViewById(R.id.tvControlNumber);
        tvPhoneNumber = view.findViewById(R.id.tvPhoneNumber);
        tvMedicalConditions = view.findViewById(R.id.tvMedicalConditions);
        tvEmergencyContact = view.findViewById(R.id.tvEmergencyContact);

        // Opciones clickeables
        optionFullName = view.findViewById(R.id.optionFullName);
        optionDateOfBirth = view.findViewById(R.id.optionDateOfBirth);
        optionGender = view.findViewById(R.id.optionGender);
        optionCurp = view.findViewById(R.id.optionCurp);
        optionCareer = view.findViewById(R.id.optionCareer);
        optionControlNumber = view.findViewById(R.id.optionControlNumber);
        optionPhoneNumber = view.findViewById(R.id.optionPhoneNumber);
        optionMedicalConditions = view.findViewById(R.id.optionMedicalConditions);
        optionEmergencyContact = view.findViewById(R.id.optionEmergencyContact);
    }

    private void setupToolbar() {
        toolbar.setNavigationOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });
    }

    private void setupEventListeners() {
        optionFullName.setOnClickListener(v -> {
            if (currentProfile != null) {
                mostrarDialogEditarCampo("Nombre completo", currentProfile.getFullName(),
                        value -> {
                            currentProfile.setFullName(value);
                            actualizarCampo("fullName", value);
                        });
            }
        });

        optionDateOfBirth.setOnClickListener(v -> {
            if (currentProfile != null) {
                mostrarDatePicker();
            }
        });

        optionGender.setOnClickListener(v -> {
            if (currentProfile != null) {
                mostrarDialogSeleccionarGenero();
            }
        });

        optionCurp.setOnClickListener(v -> {
            if (currentProfile != null) {
                mostrarDialogEditarCampo("CURP", currentProfile.getCurp(),
                        value -> {
                            currentProfile.setCurp(value);
                            actualizarCampo("curp", value);
                        });
            }
        });

        optionCareer.setOnClickListener(v -> {
            if (currentProfile != null) {
                mostrarDialogSeleccionarCarrera();
            }
        });

        optionControlNumber.setOnClickListener(v -> {
            if (currentProfile != null) {
                mostrarDialogEditarCampo("Número de control", currentProfile.getControlNumber(),
                        value -> {
                            currentProfile.setControlNumber(value);
                            actualizarCampo("controlNumber", value);
                        });
            }
        });

        optionPhoneNumber.setOnClickListener(v -> {
            if (currentProfile != null) {
                mostrarDialogEditarCampo("Teléfono", currentProfile.getPhoneNumber(),
                        value -> {
                            currentProfile.setPhoneNumber(value);
                            actualizarCampo("phoneNumber", value);
                        });
            }
        });

        optionMedicalConditions.setOnClickListener(v -> {
            if (currentProfile != null) {
                mostrarDialogEditarCampo("Condiciones médicas", currentProfile.getMedicalConditions(),
                        value -> {
                            currentProfile.setMedicalConditions(value);
                            actualizarCampo("medicalConditions", value);
                        });
            }
        });

        optionEmergencyContact.setOnClickListener(v -> {
            if (currentProfile != null) {
                mostrarDialogEditarContactoEmergencia();
            }
        });
    }

    private void cargarPerfilUsuario() {
        if (getActivity() == null) return;

        profileManager.obtenerPerfilActual(
                profile -> {
                    if (getActivity() != null && isAdded()) {
                        this.currentProfile = profile;
                        Log.d(TAG, "Perfil cargado - Nombre: " + (profile != null ? profile.getFullName() : "null"));
                        getActivity().runOnUiThread(() -> actualizarVistas());
                    }
                },
                error -> {
                    Log.e(TAG, "Error al cargar el perfil", error);
                    if (getActivity() != null && isAdded()) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "Error al cargar el perfil: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    }
                }
        );
    }

    private void actualizarVistas() {
        if (currentProfile == null || getView() == null) return;

        // Obtener valores de forma segura, manejando nulls
        String fullName = currentProfile.getFullName() != null ? currentProfile.getFullName() : "";
        String dateOfBirth = currentProfile.getDateOfBirth() != null ? currentProfile.getDateOfBirth() : "";
        String gender = currentProfile.getGender() != null ? currentProfile.getGender() : "";
        String curp = currentProfile.getCurp() != null ? currentProfile.getCurp() : "";
        String career = currentProfile.getCareer() != null ? currentProfile.getCareer() : "";
        String controlNumber = currentProfile.getControlNumber() != null ? currentProfile.getControlNumber() : "";
        String phoneNumber = currentProfile.getPhoneNumber() != null ? currentProfile.getPhoneNumber() : "";
        String medicalConditions = currentProfile.getMedicalConditions() != null ? currentProfile.getMedicalConditions() : "";
        String emergencyContactName = currentProfile.getEmergencyContactName() != null ? currentProfile.getEmergencyContactName() : "";
        String emergencyContactPhone = currentProfile.getEmergencyContactPhone() != null ? currentProfile.getEmergencyContactPhone() : "";

        // Actualizar TextViews con los valores
        tvFullName.setText(fullName.isEmpty() ? "Sin especificar" : fullName);
        tvDateOfBirth.setText(dateOfBirth.isEmpty() ? "Sin especificar" : dateOfBirth);
        tvGender.setText(gender.isEmpty() ? "Sin especificar" : gender);
        tvCurp.setText(curp.isEmpty() ? "Sin especificar" : curp);
        tvCareer.setText(career.isEmpty() ? "Sin especificar" : career);
        tvControlNumber.setText(controlNumber.isEmpty() ? "Sin especificar" : controlNumber);
        tvPhoneNumber.setText(phoneNumber.isEmpty() ? "Sin especificar" : phoneNumber);
        tvMedicalConditions.setText(medicalConditions.isEmpty() ? "Sin especificar" : medicalConditions);

        // Formatear contacto de emergencia
        String emergencyContact = "";
        if (!emergencyContactName.isEmpty() || !emergencyContactPhone.isEmpty()) {
            emergencyContact = emergencyContactName;
            if (!emergencyContactPhone.isEmpty()) {
                if (!emergencyContact.isEmpty()) {
                    emergencyContact += " - " + emergencyContactPhone;
                } else {
                    emergencyContact = emergencyContactPhone;
                }
            }
        }
        tvEmergencyContact.setText(emergencyContact.isEmpty() ? "Sin especificar" : emergencyContact);

        Log.d(TAG, "Vistas actualizadas - Nombre: " + fullName + ", Fecha: " + dateOfBirth);
    }

    private void mostrarDialogEditarCampo(String titulo, String valorActual, OnValueSavedListener listener) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_editar_campo_simple, null);
        TextInputEditText etValue = dialogView.findViewById(R.id.etValue);
        etValue.setText(valorActual);

        new AlertDialog.Builder(getContext())
                .setTitle("Editar " + titulo)
                .setView(dialogView)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    String nuevoValor = etValue.getText().toString().trim();
                    listener.onSaved(nuevoValor);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void mostrarDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(),
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(year, month, dayOfMonth);
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    String fecha = sdf.format(selectedDate.getTime());
                    currentProfile.setDateOfBirth(fecha);
                    actualizarCampo("dateOfBirth", fecha);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void mostrarDialogSeleccionarGenero() {
        String[] generos = {"Masculino", "Femenino", "Otro"};
        int seleccionActual = 0;
        for (int i = 0; i < generos.length; i++) {
            if (generos[i].equals(currentProfile.getGender())) {
                seleccionActual = i;
                break;
            }
        }

        new AlertDialog.Builder(getContext())
                .setTitle("Seleccionar género")
                .setSingleChoiceItems(generos, seleccionActual, (dialog, which) -> {
                    currentProfile.setGender(generos[which]);
                    actualizarCampo("gender", generos[which]);
                    dialog.dismiss();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void mostrarDialogSeleccionarCarrera() {
        String[] carreras = {"INGENIERIA EN SISTEMAS COMPUTACIONALES", "INGENIERIA CIVIL",
                "INGENIERIA EN GESTION EMPRESARIAL", "INGENIERIA EN INFORMATICA", "CONTABILIDAD"};
        int seleccionActual = 0;
        for (int i = 0; i < carreras.length; i++) {
            if (carreras[i].equals(currentProfile.getCareer())) {
                seleccionActual = i;
                break;
            }
        }

        new AlertDialog.Builder(getContext())
                .setTitle("Seleccionar carrera")
                .setSingleChoiceItems(carreras, seleccionActual, (dialog, which) -> {
                    currentProfile.setCareer(carreras[which]);
                    actualizarCampo("career", carreras[which]);
                    dialog.dismiss();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void mostrarDialogEditarContactoEmergencia() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_editar_contacto_emergencia, null);
        TextInputEditText etName = dialogView.findViewById(R.id.etEmergencyContactName);
        TextInputEditText etPhone = dialogView.findViewById(R.id.etEmergencyContactPhone);

        etName.setText(currentProfile.getEmergencyContactName());
        etPhone.setText(currentProfile.getEmergencyContactPhone());

        new AlertDialog.Builder(getContext())
                .setTitle("Editar contacto de emergencia")
                .setView(dialogView)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    String nombre = etName.getText().toString().trim();
                    String telefono = etPhone.getText().toString().trim();
                    currentProfile.setEmergencyContactName(nombre);
                    currentProfile.setEmergencyContactPhone(telefono);
                    // Actualizar ambos campos en una sola operación
                    if (getActivity() != null && isAdded()) {
                        profileManager.actualizarPerfilActual(currentProfile,
                                () -> {
                                    if (getActivity() != null && isAdded()) {
                                        getActivity().runOnUiThread(() -> {
                                            Toast.makeText(getContext(), "Contacto de emergencia actualizado", Toast.LENGTH_SHORT).show();
                                            actualizarVistas();
                                        });
                                    }
                                },
                                error -> {
                                    Log.e(TAG, "Error al actualizar contacto de emergencia", error);
                                    if (getActivity() != null && isAdded()) {
                                        getActivity().runOnUiThread(() -> {
                                            Toast.makeText(getContext(), "Error al actualizar: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                                        });
                                    }
                                }
                        );
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void actualizarCampo(String campo, String valor) {
        if (getActivity() == null || currentProfile == null) return;

        profileManager.actualizarPerfilActual(currentProfile,
                () -> {
                    if (getActivity() != null && isAdded()) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "Campo actualizado", Toast.LENGTH_SHORT).show();
                            actualizarVistas();
                        });
                    }
                },
                error -> {
                    Log.e(TAG, "Error al actualizar campo: " + campo, error);
                    if (getActivity() != null && isAdded()) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "Error al actualizar: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    }
                }
        );
    }

    interface OnValueSavedListener {
        void onSaved(String value);
    }
}

