package com.example.sharedpreferencesapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ProtocoloFragment extends Fragment {

    // Información del proyecto
    private TextInputEditText etNombreProyecto, etAsesorInterno;
    private Spinner spTipoProyecto;

    // Información de la empresa
    private TextInputEditText etNombreEmpresa, etRfc, etDomicilio, etColonia;
    private TextInputEditText etCodigoPostal, etCiudad, etTelefono, etMision;
    private Spinner spTipoEmpresa;

    // Contactos de la empresa
    private TextInputEditText etNombreTitular, etPuestoTitular;
    private TextInputEditText etNombreAsesorExterno, etPuestoAsesorExterno;
    private TextInputEditText etFirmanteConvenio, etPuestoFirmante;

    // Botones
    private Button btnGuardar;

    // Firebase
    private FirebaseFirestore db;
    private ProfileManager profileManager;

    public ProtocoloFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_protocolo, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Inicializar Firebase
        db = FirebaseFirestore.getInstance();
        profileManager = new ProfileManager(requireContext());

        // Inicializar vistas
        initViews(view);

        // Configurar spinners
        setupSpinners();

        // Cargar datos si ya existe un protocolo
        loadProtocoloData();

        // Configurar botón guardar
        btnGuardar.setOnClickListener(v -> saveProtocoloData());
    }

    private void initViews(View view) {
        // Información del proyecto
        etNombreProyecto = view.findViewById(R.id.et_nombre_proyecto);
        spTipoProyecto = view.findViewById(R.id.sp_tipo_proyecto);
        etAsesorInterno = view.findViewById(R.id.et_asesor_interno);

        // Información de la empresa
        etNombreEmpresa = view.findViewById(R.id.et_nombre_empresa);
        spTipoEmpresa = view.findViewById(R.id.sp_tipo_empresa);
        etRfc = view.findViewById(R.id.et_rfc);
        etDomicilio = view.findViewById(R.id.et_domicilio);
        etColonia = view.findViewById(R.id.et_colonia);
        etCodigoPostal = view.findViewById(R.id.et_codigo_postal);
        etCiudad = view.findViewById(R.id.et_ciudad);
        etTelefono = view.findViewById(R.id.et_telefono_empresa);
        etMision = view.findViewById(R.id.et_mision_empresa);

        // Contactos de la empresa
        etNombreTitular = view.findViewById(R.id.et_nombre_titular);
        etPuestoTitular = view.findViewById(R.id.et_puesto_titular);
        etNombreAsesorExterno = view.findViewById(R.id.et_asesor_externo);
        etPuestoAsesorExterno = view.findViewById(R.id.et_puesto_asesor_externo);
        etFirmanteConvenio = view.findViewById(R.id.et_firmante_convenio);
        etPuestoFirmante = view.findViewById(R.id.et_puesto_firmante);

        // Botón guardar
        btnGuardar = view.findViewById(R.id.btn_guardar_protocolo);
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

    private void loadProtocoloData() {
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
                        // Información del proyecto
                        etNombreProyecto.setText(documentSnapshot.getString("nombreProyecto"));
                        String tipoProyecto = documentSnapshot.getString("tipoProyecto");
                        if (tipoProyecto != null) {
                            setSpinnerSelection(spTipoProyecto, tipoProyecto);
                        }
                        etAsesorInterno.setText(documentSnapshot.getString("asesorInterno"));

                        // Información de la empresa
                        etNombreEmpresa.setText(documentSnapshot.getString("nombreEmpresa"));
                        String tipoEmpresa = documentSnapshot.getString("tipoEmpresa");
                        if (tipoEmpresa != null) {
                            setSpinnerSelection(spTipoEmpresa, tipoEmpresa);
                        }
                        etRfc.setText(documentSnapshot.getString("rfc"));
                        etDomicilio.setText(documentSnapshot.getString("domicilio"));
                        etColonia.setText(documentSnapshot.getString("colonia"));
                        etCodigoPostal.setText(documentSnapshot.getString("codigoPostal"));
                        etCiudad.setText(documentSnapshot.getString("ciudad"));
                        etTelefono.setText(documentSnapshot.getString("telefonoEmpresa"));
                        etMision.setText(documentSnapshot.getString("misionEmpresa"));

                        // Contactos de la empresa
                        etNombreTitular.setText(documentSnapshot.getString("nombreTitular"));
                        etPuestoTitular.setText(documentSnapshot.getString("puestoTitular"));
                        etNombreAsesorExterno.setText(documentSnapshot.getString("asesorExterno"));
                        etPuestoAsesorExterno.setText(documentSnapshot.getString("puestoAsesorExterno"));
                        etFirmanteConvenio.setText(documentSnapshot.getString("firmanteConvenio"));
                        etPuestoFirmante.setText(documentSnapshot.getString("puestoFirmante"));

                        Toast.makeText(requireContext(), "Protocolo cargado correctamente", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Error al cargar datos: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setSpinnerSelection(Spinner spinner, String value) {
        for (int i = 0; i < spinner.getCount(); i++) {
            if (spinner.getItemAtPosition(i).toString().equals(value)) {
                spinner.setSelection(i);
                break;
            }
        }
    }

    private void saveProtocoloData() {
        // Validar campos obligatorios
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

        String userId = user.getUid();

        // Obtener valores del formulario
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
        profileManager.obtenerPerfilActual(
                profile -> {
                    if (profile != null && isAdded()) {
                        protocolo.put("estudiante", profile.getUserId());
                        protocolo.put("nombreEstudiante", profile.getFullName());
                        protocolo.put("numeroControl", profile.getControlNumber());
                        protocolo.put("carrera", profile.getCareer());

                        // Guardar en Firestore
                        DocumentReference protocoloRef = db.collection("protocolos").document(userId);
                        protocoloRef.set(protocolo)
                                .addOnSuccessListener(aVoid -> {
                                    if (isAdded()) {
                                        Toast.makeText(requireContext(), "Protocolo guardado correctamente", Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(requireContext(),
                                "Error al obtener perfil: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }
}