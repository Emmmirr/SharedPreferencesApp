package com.example.sharedpreferencesapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ProtocoloFragment extends Fragment {

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
    private TextInputEditText etWizardNombreProyecto, etWizardAsesorInterno;
    private Spinner spWizardTipoProyecto;

    // Campos del wizard - Paso 2
    private TextInputEditText etWizardNombreEmpresa, etWizardRfc, etWizardDomicilio, etWizardColonia;
    private TextInputEditText etWizardCodigoPostal, etWizardCiudad, etWizardTelefonoEmpresa, etWizardMisionEmpresa;
    private Spinner spWizardTipoEmpresa;

    // Campos del wizard - Paso 3
    private TextInputEditText etWizardNombreTitular, etWizardPuestoTitular;
    private TextInputEditText etWizardAsesorExterno, etWizardPuestoAsesorExterno;
    private TextInputEditText etWizardFirmanteConvenio, etWizardPuestoFirmante;

    // Vistas de solo lectura
    private LinearLayout btnEditProtocolo;
    private TextView tvNombreProyecto, tvTipoProyecto, tvAsesorInterno;
    private TextView tvNombreEmpresa, tvTipoEmpresa, tvRfc, tvDomicilio, tvColonia;
    private TextView tvCodigoPostal, tvCiudad, tvTelefonoEmpresa, tvMisionEmpresa;
    private TextView tvNombreTitular, tvPuestoTitular, tvAsesorExterno, tvPuestoAsesorExterno;
    private TextView tvFirmanteConvenio, tvPuestoFirmante;

    // Formulario de edición (reutiliza fragment_protocolo.xml)
    private TextInputEditText etNombreProyecto, etAsesorInterno;
    private Spinner spTipoProyecto;
    private TextInputEditText etNombreEmpresa, etRfc, etDomicilio, etColonia;
    private TextInputEditText etCodigoPostal, etCiudad, etTelefono, etMision;
    private Spinner spTipoEmpresa;
    private TextInputEditText etNombreTitular, etPuestoTitular;
    private TextInputEditText etNombreAsesorExterno, etPuestoAsesorExterno;
    private TextInputEditText etFirmanteConvenio, etPuestoFirmante;
    private Button btnGuardar;

    // Firebase
    private FirebaseFirestore db;
    private ProfileManager profileManager;
    private DocumentSnapshot currentProtocolo;

    public ProtocoloFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Usar un FrameLayout como contenedor principal
        View rootView = inflater.inflate(R.layout.fragment_protocolo_container, container, false);
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Inicializar Firebase
        db = FirebaseFirestore.getInstance();
        profileManager = new ProfileManager(requireContext());

        // Cargar protocolo y decidir qué vista mostrar
        loadProtocoloAndShowView();
    }

    private void loadProtocoloAndShowView() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = user.getUid();
        db.collection("protocolos")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && isAdded()) {
                        currentProtocolo = documentSnapshot;
                        // Si existe protocolo, mostrar vista de solo lectura
                        showProfileView();
                    } else if (isAdded()) {
                        // Si no existe, mostrar wizard
                        showWizardView();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        // Si hay error, mostrar wizard
                        showWizardView();
                    }
                });
    }

    private void showWizardView() {
        if (getView() == null) return;

        ViewGroup container = getView().findViewById(R.id.container);
        if (container == null) return;

        container.removeAllViews();

        wizardView = LayoutInflater.from(requireContext())
                .inflate(R.layout.fragment_protocolo_wizard, container, false);
        container.addView(wizardView);

        initWizardViews();
        setupWizardSpinners();
        loadWizardData();
        setupWizardNavigation();
    }

    private void showProfileView() {
        if (getView() == null) return;

        ViewGroup container = getView().findViewById(R.id.container);
        if (container == null) return;

        container.removeAllViews();

        profileView = LayoutInflater.from(requireContext())
                .inflate(R.layout.fragment_protocolo_view, container, false);
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
                .inflate(R.layout.fragment_protocolo, container, false);
        container.addView(editFormView);

        initEditFormViews();
        setupSpinners();
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
        etWizardNombreProyecto = wizardView.findViewById(R.id.et_wizard_nombre_proyecto);
        spWizardTipoProyecto = wizardView.findViewById(R.id.sp_wizard_tipo_proyecto);
        etWizardAsesorInterno = wizardView.findViewById(R.id.et_wizard_asesor_interno);

        // Paso 2
        etWizardNombreEmpresa = wizardView.findViewById(R.id.et_wizard_nombre_empresa);
        spWizardTipoEmpresa = wizardView.findViewById(R.id.sp_wizard_tipo_empresa);
        etWizardRfc = wizardView.findViewById(R.id.et_wizard_rfc);
        etWizardDomicilio = wizardView.findViewById(R.id.et_wizard_domicilio);
        etWizardColonia = wizardView.findViewById(R.id.et_wizard_colonia);
        etWizardCodigoPostal = wizardView.findViewById(R.id.et_wizard_codigo_postal);
        etWizardCiudad = wizardView.findViewById(R.id.et_wizard_ciudad);
        etWizardTelefonoEmpresa = wizardView.findViewById(R.id.et_wizard_telefono_empresa);
        etWizardMisionEmpresa = wizardView.findViewById(R.id.et_wizard_mision_empresa);

        // Paso 3
        etWizardNombreTitular = wizardView.findViewById(R.id.et_wizard_nombre_titular);
        etWizardPuestoTitular = wizardView.findViewById(R.id.et_wizard_puesto_titular);
        etWizardAsesorExterno = wizardView.findViewById(R.id.et_wizard_asesor_externo);
        etWizardPuestoAsesorExterno = wizardView.findViewById(R.id.et_wizard_puesto_asesor_externo);
        etWizardFirmanteConvenio = wizardView.findViewById(R.id.et_wizard_firmante_convenio);
        etWizardPuestoFirmante = wizardView.findViewById(R.id.et_wizard_puesto_firmante);
    }

    private void setupWizardSpinners() {
        // Spinner para tipo de proyecto
        String[] tiposProyecto = {
                "Interdisciplinario",
                "Integradores",
                "Educación dual"
        };
        ArrayAdapter<String> adapterTipoProyecto = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                tiposProyecto);
        adapterTipoProyecto.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spWizardTipoProyecto.setAdapter(adapterTipoProyecto);

        // Spinner para tipo de empresa
        String[] tiposEmpresa = {
                "Industrial",
                "Servicios",
                "Público",
                "Privado",
                "Otro"
        };
        ArrayAdapter<String> adapterTipoEmpresa = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                tiposEmpresa);
        adapterTipoEmpresa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spWizardTipoEmpresa.setAdapter(adapterTipoEmpresa);
    }

    private void loadWizardData() {
        if (currentProtocolo != null && currentProtocolo.exists()) {
            etWizardNombreProyecto.setText(currentProtocolo.getString("nombreProyecto"));
            String tipoProyecto = currentProtocolo.getString("tipoProyecto");
            if (tipoProyecto != null) {
                setSpinnerSelection(spWizardTipoProyecto, tipoProyecto);
            }
            etWizardAsesorInterno.setText(currentProtocolo.getString("asesorInterno"));

            etWizardNombreEmpresa.setText(currentProtocolo.getString("nombreEmpresa"));
            String tipoEmpresa = currentProtocolo.getString("tipoEmpresa");
            if (tipoEmpresa != null) {
                setSpinnerSelection(spWizardTipoEmpresa, tipoEmpresa);
            }
            etWizardRfc.setText(currentProtocolo.getString("rfc"));
            etWizardDomicilio.setText(currentProtocolo.getString("domicilio"));
            etWizardColonia.setText(currentProtocolo.getString("colonia"));
            etWizardCodigoPostal.setText(currentProtocolo.getString("codigoPostal"));
            etWizardCiudad.setText(currentProtocolo.getString("ciudad"));
            etWizardTelefonoEmpresa.setText(currentProtocolo.getString("telefonoEmpresa"));
            etWizardMisionEmpresa.setText(currentProtocolo.getString("misionEmpresa"));

            etWizardNombreTitular.setText(currentProtocolo.getString("nombreTitular"));
            etWizardPuestoTitular.setText(currentProtocolo.getString("puestoTitular"));
            etWizardAsesorExterno.setText(currentProtocolo.getString("asesorExterno"));
            etWizardPuestoAsesorExterno.setText(currentProtocolo.getString("puestoAsesorExterno"));
            etWizardFirmanteConvenio.setText(currentProtocolo.getString("firmanteConvenio"));
            etWizardPuestoFirmante.setText(currentProtocolo.getString("puestoFirmante"));
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
                if (TextUtils.isEmpty(etWizardNombreProyecto.getText())) {
                    Toast.makeText(requireContext(), "El nombre del proyecto es obligatorio", Toast.LENGTH_SHORT).show();
                    return false;
                }
                return true;
            case 2:
                if (TextUtils.isEmpty(etWizardNombreEmpresa.getText())) {
                    Toast.makeText(requireContext(), "El nombre de la empresa es obligatorio", Toast.LENGTH_SHORT).show();
                    return false;
                }
                return true;
            case 3:
                return true; // Los contactos son opcionales
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
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show();
            return;
        }

        profileManager.obtenerPerfilActual(
                profile -> {
                    if (profile != null && isAdded()) {
                        if (profile.getSupervisorId() == null || profile.getSupervisorId().isEmpty()) {
                            Toast.makeText(requireContext(), "Error: No tienes un supervisor asignado para registrar un protocolo.", Toast.LENGTH_LONG).show();
                            return;
                        }

                        String userId = user.getUid();
                        Map<String, Object> protocolo = new HashMap<>();

                        // Información del proyecto
                        protocolo.put("nombreProyecto", getText(etWizardNombreProyecto));
                        protocolo.put("tipoProyecto", spWizardTipoProyecto.getSelectedItem().toString());
                        protocolo.put("asesorInterno", getText(etWizardAsesorInterno));

                        // Información de la empresa
                        protocolo.put("nombreEmpresa", getText(etWizardNombreEmpresa));
                        protocolo.put("tipoEmpresa", spWizardTipoEmpresa.getSelectedItem().toString());
                        protocolo.put("rfc", getText(etWizardRfc));
                        protocolo.put("domicilio", getText(etWizardDomicilio));
                        protocolo.put("colonia", getText(etWizardColonia));
                        protocolo.put("codigoPostal", getText(etWizardCodigoPostal));
                        protocolo.put("ciudad", getText(etWizardCiudad));
                        protocolo.put("telefonoEmpresa", getText(etWizardTelefonoEmpresa));
                        protocolo.put("misionEmpresa", getText(etWizardMisionEmpresa));

                        // Contactos de la empresa
                        protocolo.put("nombreTitular", getText(etWizardNombreTitular));
                        protocolo.put("puestoTitular", getText(etWizardPuestoTitular));
                        protocolo.put("asesorExterno", getText(etWizardAsesorExterno));
                        protocolo.put("puestoAsesorExterno", getText(etWizardPuestoAsesorExterno));
                        protocolo.put("firmanteConvenio", getText(etWizardFirmanteConvenio));
                        protocolo.put("puestoFirmante", getText(etWizardPuestoFirmante));

                        // Timestamp
                        protocolo.put("fechaActualizacion", System.currentTimeMillis());

                        // Referencias al perfil del estudiante
                        protocolo.put("estudianteId", profile.getUserId());
                        protocolo.put("nombreEstudiante", profile.getFullName());
                        protocolo.put("numeroControl", profile.getControlNumber());
                        protocolo.put("carrera", profile.getCareer());

                        // Referencias al supervisor
                        protocolo.put("supervisorId", profile.getSupervisorId());
                        protocolo.put("supervisorName", profile.getSupervisorName());

                        // Guardar en Firestore
                        DocumentReference protocoloRef = db.collection("protocolos").document(userId);
                        protocoloRef.set(protocolo)
                                .addOnSuccessListener(aVoid -> {
                                    if (isAdded()) {
                                        Toast.makeText(requireContext(), "Protocolo guardado correctamente", Toast.LENGTH_SHORT).show();
                                        // Recargar vista apropiada
                                        loadProtocoloAndShowView();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    if (isAdded()) {
                                        Toast.makeText(requireContext(), "Error al guardar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                },
                error -> {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Error al obtener perfil: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    // ========== PROFILE VIEW METHODS ==========

    private void initProfileViews() {
        btnEditProtocolo = profileView.findViewById(R.id.btn_edit_protocolo);
        tvNombreProyecto = profileView.findViewById(R.id.tv_nombre_proyecto);
        tvTipoProyecto = profileView.findViewById(R.id.tv_tipo_proyecto);
        tvAsesorInterno = profileView.findViewById(R.id.tv_asesor_interno);
        tvNombreEmpresa = profileView.findViewById(R.id.tv_nombre_empresa);
        tvTipoEmpresa = profileView.findViewById(R.id.tv_tipo_empresa);
        tvRfc = profileView.findViewById(R.id.tv_rfc);
        tvDomicilio = profileView.findViewById(R.id.tv_domicilio);
        tvColonia = profileView.findViewById(R.id.tv_colonia);
        tvCodigoPostal = profileView.findViewById(R.id.tv_codigo_postal);
        tvCiudad = profileView.findViewById(R.id.tv_ciudad);
        tvTelefonoEmpresa = profileView.findViewById(R.id.tv_telefono_empresa);
        tvMisionEmpresa = profileView.findViewById(R.id.tv_mision_empresa);
        tvNombreTitular = profileView.findViewById(R.id.tv_nombre_titular);
        tvPuestoTitular = profileView.findViewById(R.id.tv_puesto_titular);
        tvAsesorExterno = profileView.findViewById(R.id.tv_asesor_externo);
        tvPuestoAsesorExterno = profileView.findViewById(R.id.tv_puesto_asesor_externo);
        tvFirmanteConvenio = profileView.findViewById(R.id.tv_firmante_convenio);
        tvPuestoFirmante = profileView.findViewById(R.id.tv_puesto_firmante);
    }

    private void loadProfileData() {
        if (currentProtocolo != null && currentProtocolo.exists()) {
            tvNombreProyecto.setText(getStringOrEmpty(currentProtocolo.getString("nombreProyecto")));
            tvTipoProyecto.setText(getStringOrEmpty(currentProtocolo.getString("tipoProyecto")));
            tvAsesorInterno.setText(getStringOrEmpty(currentProtocolo.getString("asesorInterno")));
            tvNombreEmpresa.setText(getStringOrEmpty(currentProtocolo.getString("nombreEmpresa")));
            tvTipoEmpresa.setText(getStringOrEmpty(currentProtocolo.getString("tipoEmpresa")));
            tvRfc.setText(getStringOrEmpty(currentProtocolo.getString("rfc")));
            tvDomicilio.setText(getStringOrEmpty(currentProtocolo.getString("domicilio")));
            tvColonia.setText(getStringOrEmpty(currentProtocolo.getString("colonia")));
            tvCodigoPostal.setText(getStringOrEmpty(currentProtocolo.getString("codigoPostal")));
            tvCiudad.setText(getStringOrEmpty(currentProtocolo.getString("ciudad")));
            tvTelefonoEmpresa.setText(getStringOrEmpty(currentProtocolo.getString("telefonoEmpresa")));
            tvMisionEmpresa.setText(getStringOrEmpty(currentProtocolo.getString("misionEmpresa")));
            tvNombreTitular.setText(getStringOrEmpty(currentProtocolo.getString("nombreTitular")));
            tvPuestoTitular.setText(getStringOrEmpty(currentProtocolo.getString("puestoTitular")));
            tvAsesorExterno.setText(getStringOrEmpty(currentProtocolo.getString("asesorExterno")));
            tvPuestoAsesorExterno.setText(getStringOrEmpty(currentProtocolo.getString("puestoAsesorExterno")));
            tvFirmanteConvenio.setText(getStringOrEmpty(currentProtocolo.getString("firmanteConvenio")));
            tvPuestoFirmante.setText(getStringOrEmpty(currentProtocolo.getString("puestoFirmante")));
        }
    }

    private String getStringOrEmpty(String value) {
        return value == null || value.isEmpty() ? "No especificado" : value;
    }

    private void setupEditButton() {
        btnEditProtocolo.setOnClickListener(v -> showEditFormView());
    }

    // ========== EDIT FORM METHODS ==========

    private void initEditFormViews() {
        etNombreProyecto = editFormView.findViewById(R.id.et_nombre_proyecto);
        spTipoProyecto = editFormView.findViewById(R.id.sp_tipo_proyecto);
        etAsesorInterno = editFormView.findViewById(R.id.et_asesor_interno);
        etNombreEmpresa = editFormView.findViewById(R.id.et_nombre_empresa);
        spTipoEmpresa = editFormView.findViewById(R.id.sp_tipo_empresa);
        etRfc = editFormView.findViewById(R.id.et_rfc);
        etDomicilio = editFormView.findViewById(R.id.et_domicilio);
        etColonia = editFormView.findViewById(R.id.et_colonia);
        etCodigoPostal = editFormView.findViewById(R.id.et_codigo_postal);
        etCiudad = editFormView.findViewById(R.id.et_ciudad);
        etTelefono = editFormView.findViewById(R.id.et_telefono_empresa);
        etMision = editFormView.findViewById(R.id.et_mision_empresa);
        etNombreTitular = editFormView.findViewById(R.id.et_nombre_titular);
        etPuestoTitular = editFormView.findViewById(R.id.et_puesto_titular);
        etNombreAsesorExterno = editFormView.findViewById(R.id.et_asesor_externo);
        etPuestoAsesorExterno = editFormView.findViewById(R.id.et_puesto_asesor_externo);
        etFirmanteConvenio = editFormView.findViewById(R.id.et_firmante_convenio);
        etPuestoFirmante = editFormView.findViewById(R.id.et_puesto_firmante);
        btnGuardar = editFormView.findViewById(R.id.btn_guardar_protocolo);
    }

    private void setupSpinners() {
        // Spinner para tipo de proyecto
        String[] tiposProyecto = {
                "Interdisciplinario",
                "Integradores",
                "Educación dual"
        };
        ArrayAdapter<String> adapterTipoProyecto = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                tiposProyecto);
        adapterTipoProyecto.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spTipoProyecto.setAdapter(adapterTipoProyecto);

        // Spinner para tipo de empresa
        String[] tiposEmpresa = {
                "Industrial",
                "Servicios",
                "Público",
                "Privado",
                "Otro"
        };
        ArrayAdapter<String> adapterTipoEmpresa = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                tiposEmpresa);
        adapterTipoEmpresa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spTipoEmpresa.setAdapter(adapterTipoEmpresa);
    }

    private void loadEditFormData() {
        if (currentProtocolo != null && currentProtocolo.exists()) {
            etNombreProyecto.setText(currentProtocolo.getString("nombreProyecto"));
            String tipoProyecto = currentProtocolo.getString("tipoProyecto");
            if (tipoProyecto != null) {
                setSpinnerSelection(spTipoProyecto, tipoProyecto);
            }
            etAsesorInterno.setText(currentProtocolo.getString("asesorInterno"));

            etNombreEmpresa.setText(currentProtocolo.getString("nombreEmpresa"));
            String tipoEmpresa = currentProtocolo.getString("tipoEmpresa");
            if (tipoEmpresa != null) {
                setSpinnerSelection(spTipoEmpresa, tipoEmpresa);
            }
            etRfc.setText(currentProtocolo.getString("rfc"));
            etDomicilio.setText(currentProtocolo.getString("domicilio"));
            etColonia.setText(currentProtocolo.getString("colonia"));
            etCodigoPostal.setText(currentProtocolo.getString("codigoPostal"));
            etCiudad.setText(currentProtocolo.getString("ciudad"));
            etTelefono.setText(currentProtocolo.getString("telefonoEmpresa"));
            etMision.setText(currentProtocolo.getString("misionEmpresa"));

            etNombreTitular.setText(currentProtocolo.getString("nombreTitular"));
            etPuestoTitular.setText(currentProtocolo.getString("puestoTitular"));
            etNombreAsesorExterno.setText(currentProtocolo.getString("asesorExterno"));
            etPuestoAsesorExterno.setText(currentProtocolo.getString("puestoAsesorExterno"));
            etFirmanteConvenio.setText(currentProtocolo.getString("firmanteConvenio"));
            etPuestoFirmante.setText(currentProtocolo.getString("puestoFirmante"));
        }
    }

    private void setupSaveButton() {
        btnGuardar.setOnClickListener(v -> saveEditFormData());
    }

    private void saveEditFormData() {
        if (TextUtils.isEmpty(etNombreProyecto.getText())) {
            Toast.makeText(requireContext(), "El nombre del proyecto es obligatorio", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(etNombreEmpresa.getText())) {
            Toast.makeText(requireContext(), "El nombre de la empresa es obligatorio", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show();
            return;
        }

        profileManager.obtenerPerfilActual(
                profile -> {
                    if (profile != null && isAdded()) {
                        if (profile.getSupervisorId() == null || profile.getSupervisorId().isEmpty()) {
                            Toast.makeText(requireContext(), "Error: No tienes un supervisor asignado para registrar un protocolo.", Toast.LENGTH_LONG).show();
                            return;
                        }

                        String userId = user.getUid();
                        Map<String, Object> protocolo = new HashMap<>();

                        // Información del proyecto
                        protocolo.put("nombreProyecto", getText(etNombreProyecto));
                        protocolo.put("tipoProyecto", spTipoProyecto.getSelectedItem().toString());
                        protocolo.put("asesorInterno", getText(etAsesorInterno));

                        // Información de la empresa
                        protocolo.put("nombreEmpresa", getText(etNombreEmpresa));
                        protocolo.put("tipoEmpresa", spTipoEmpresa.getSelectedItem().toString());
                        protocolo.put("rfc", getText(etRfc));
                        protocolo.put("domicilio", getText(etDomicilio));
                        protocolo.put("colonia", getText(etColonia));
                        protocolo.put("codigoPostal", getText(etCodigoPostal));
                        protocolo.put("ciudad", getText(etCiudad));
                        protocolo.put("telefonoEmpresa", getText(etTelefono));
                        protocolo.put("misionEmpresa", getText(etMision));

                        // Contactos de la empresa
                        protocolo.put("nombreTitular", getText(etNombreTitular));
                        protocolo.put("puestoTitular", getText(etPuestoTitular));
                        protocolo.put("asesorExterno", getText(etNombreAsesorExterno));
                        protocolo.put("puestoAsesorExterno", getText(etPuestoAsesorExterno));
                        protocolo.put("firmanteConvenio", getText(etFirmanteConvenio));
                        protocolo.put("puestoFirmante", getText(etPuestoFirmante));

                        // Timestamp
                        protocolo.put("fechaActualizacion", System.currentTimeMillis());

                        // Referencias al perfil del estudiante
                        protocolo.put("estudianteId", profile.getUserId());
                        protocolo.put("nombreEstudiante", profile.getFullName());
                        protocolo.put("numeroControl", profile.getControlNumber());
                        protocolo.put("carrera", profile.getCareer());

                        // Referencias al supervisor
                        protocolo.put("supervisorId", profile.getSupervisorId());
                        protocolo.put("supervisorName", profile.getSupervisorName());

                        // Guardar en Firestore
                        DocumentReference protocoloRef = db.collection("protocolos").document(userId);
                        protocoloRef.set(protocolo)
                                .addOnSuccessListener(aVoid -> {
                                    if (isAdded()) {
                                        Toast.makeText(requireContext(), "Protocolo guardado correctamente", Toast.LENGTH_SHORT).show();
                                        loadProtocoloAndShowView();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    if (isAdded()) {
                                        Toast.makeText(requireContext(), "Error al guardar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                },
                error -> {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Error al obtener perfil: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void setSpinnerSelection(Spinner spinner, String value) {
        for (int i = 0; i < spinner.getCount(); i++) {
            if (spinner.getItemAtPosition(i).toString().equals(value)) {
                spinner.setSelection(i);
                break;
            }
        }
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }
}
