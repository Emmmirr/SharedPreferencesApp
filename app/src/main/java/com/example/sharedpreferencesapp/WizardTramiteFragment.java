package com.example.sharedpreferencesapp;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class WizardTramiteFragment extends Fragment {

    private static final String TAG = "WizardTramiteFragment";
    private static final String ARG_DOCUMENTO_ID = "documento_id";
    private static final String ARG_DOCUMENTO_NOMBRE = "documento_nombre";

    private String documentoId;
    private String documentoNombre;
    private String currentUserId;
    private FirebaseManager firebaseManager;

    // Vistas
    private TextView tvTituloWizard;
    private ProgressBar progressBar;
    private TextView tvProgreso;
    private FrameLayout containerPasos;
    private LinearLayout btnAnterior, btnSiguiente, btnFinalizar, btnGuardarBorrador;

    // Estado del wizard
    private int pasoActual = 1;
    private int totalPasos = 3;
    private Map<String, String> datosFormulario = new HashMap<>();

    // Campos del paso actual (se inicializarán dinámicamente según el documento)
    private TextInputEditText[] camposActuales;

    public static WizardTramiteFragment newInstance(String documentoId, String documentoNombre) {
        WizardTramiteFragment fragment = new WizardTramiteFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DOCUMENTO_ID, documentoId);
        args.putString(ARG_DOCUMENTO_NOMBRE, documentoNombre);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            documentoId = getArguments().getString(ARG_DOCUMENTO_ID);
            documentoNombre = getArguments().getString(ARG_DOCUMENTO_NOMBRE);
        }
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        firebaseManager = new FirebaseManager();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_wizard_tramite, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvTituloWizard = view.findViewById(R.id.tvTituloWizard);
        progressBar = view.findViewById(R.id.progressBar);
        tvProgreso = view.findViewById(R.id.tvProgreso);
        containerPasos = view.findViewById(R.id.containerPasos);
        btnAnterior = view.findViewById(R.id.btnAnterior);
        btnSiguiente = view.findViewById(R.id.btnSiguiente);
        btnFinalizar = view.findViewById(R.id.btnFinalizar);
        btnGuardarBorrador = view.findViewById(R.id.btnGuardarBorrador);

        tvTituloWizard.setText("Llenar: " + documentoNombre);

        // Configurar listeners
        btnAnterior.setOnClickListener(v -> pasoAnterior());
        btnSiguiente.setOnClickListener(v -> pasoSiguiente());
        btnFinalizar.setOnClickListener(v -> finalizarWizard());
        btnGuardarBorrador.setOnClickListener(v -> guardarBorrador());

        // Inicializar totalPasos según el tipo de documento
        if (documentoId != null && documentoId.equals("reportes_parciales")) {
            totalPasos = 1;
        } else if (documentoId != null && (documentoId.equals("solicitud_residencias") || documentoId.contains("carta"))) {
            totalPasos = 1;
        } else {
            totalPasos = 3;
        }

        // Cargar borrador si existe
        cargarBorrador();

        // Mostrar primer paso
        mostrarPaso(1);
    }

    private void mostrarPaso(int paso) {
        pasoActual = paso;
        actualizarUI();

        // Limpiar contenedor
        containerPasos.removeAllViews();

        // Crear vista del paso según el tipo de documento
        View pasoView = crearVistaPaso(paso);
        if (pasoView != null) {
            containerPasos.addView(pasoView);
        }
    }

    private View crearVistaPaso(int paso) {
        LayoutInflater inflater = LayoutInflater.from(getContext());

        // Personalizar según el tipo de documento
        if (documentoId.equals("solicitud_residencias")) {
            return crearPasoSolicitud(inflater, paso);
        } else if (documentoId.contains("carta")) {
            return crearPasoCarta(inflater, paso);
        } else if (documentoId.equals("reportes_parciales")) {
            // Reportes parciales usa Word, solo un paso
            return crearPasoReportesParciales(inflater);
        } else {
            // Pasos genéricos para otros documentos
            return crearPasoGenerico(inflater, paso);
        }
    }

    private View crearPasoReportesParciales(LayoutInflater inflater) {
        View view = inflater.inflate(R.layout.wizard_paso_reportes_parciales, containerPasos, false);
        return view;
    }

    private View crearPasoSolicitud(LayoutInflater inflater, int paso) {
        if (paso == 1) {
            View view = inflater.inflate(R.layout.wizard_paso_solicitud, containerPasos, false);
            // Los campos se guardarán automáticamente al avanzar
            return view;
        } else {
            return crearPasoGenerico(inflater, paso);
        }
    }

    private View crearPasoCarta(LayoutInflater inflater, int paso) {
        if (paso == 1) {
            View view = inflater.inflate(R.layout.wizard_paso_carta, containerPasos, false);
            return view;
        } else {
            return crearPasoGenerico(inflater, paso);
        }
    }

    private View crearPasoGenerico(LayoutInflater inflater, int paso) {
        View view = inflater.inflate(R.layout.wizard_paso_generico, containerPasos, false);
        return view;
    }

    private void actualizarUI() {
        int progreso = (pasoActual * 100) / totalPasos;
        progressBar.setProgress(progreso);
        tvProgreso.setText("Paso " + pasoActual + " de " + totalPasos);

        // Mostrar/ocultar botones
        btnAnterior.setVisibility(pasoActual > 1 ? View.VISIBLE : View.GONE);
        btnSiguiente.setVisibility(pasoActual < totalPasos ? View.VISIBLE : View.GONE);
        btnFinalizar.setVisibility(pasoActual == totalPasos ? View.VISIBLE : View.GONE);
    }

    private void pasoAnterior() {
        if (pasoActual > 1) {
            guardarDatosPasoActual();
            mostrarPaso(pasoActual - 1);
        }
    }

    private void pasoSiguiente() {
        if (validarPasoActual()) {
            guardarDatosPasoActual();
            if (pasoActual < totalPasos) {
                mostrarPaso(pasoActual + 1);
            }
        } else {
            Toast.makeText(getContext(), "Por favor completa todos los campos obligatorios", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean validarPasoActual() {
        View pasoView = containerPasos.getChildAt(0);
        if (pasoView == null) return true;

        // Validar campos requeridos según el tipo de documento
        if (documentoId.equals("solicitud_residencias")) {
            return validarPasoSolicitud(pasoView);
        } else if (documentoId.contains("carta")) {
            return validarPasoCarta(pasoView);
        } else if (documentoId.equals("reportes_parciales")) {
            return validarPasoReportesParciales(pasoView);
        }
        return true;
    }

    private boolean validarPasoReportesParciales(View view) {
        boolean valido = true;

        com.google.android.material.textfield.TextInputEditText etDia = view.findViewById(R.id.etDia);
        com.google.android.material.textfield.TextInputEditText etMes = view.findViewById(R.id.etMes);
        com.google.android.material.textfield.TextInputEditText etDepartamento = view.findViewById(R.id.etDepartamentoAcademico);
        com.google.android.material.textfield.TextInputEditText etNombreResidente = view.findViewById(R.id.etNombreResidente);
        com.google.android.material.textfield.TextInputEditText etNumeroControl = view.findViewById(R.id.etNumeroControl);
        com.google.android.material.textfield.TextInputEditText etCarrera = view.findViewById(R.id.etCarrera);
        com.google.android.material.textfield.TextInputEditText etNombreProyecto = view.findViewById(R.id.etNombreProyecto);
        com.google.android.material.textfield.TextInputEditText etEmpresa = view.findViewById(R.id.etEmpresa);
        com.google.android.material.textfield.TextInputEditText etNoAsesoria = view.findViewById(R.id.etNoAsesoria);
        com.google.android.material.textfield.TextInputEditText etTipoAsesoria = view.findViewById(R.id.etTipoAsesoria);
        com.google.android.material.textfield.TextInputEditText etTemas = view.findViewById(R.id.etTemasAsesorar);
        com.google.android.material.textfield.TextInputEditText etSolucion = view.findViewById(R.id.etSolucionRecomendada);
        com.google.android.material.textfield.TextInputEditText etAsesor = view.findViewById(R.id.etNombreAsesorInterno);
        com.google.android.material.textfield.TextInputEditText etResidenteFirma = view.findViewById(R.id.etNombreResidenteFirma);

        // Validar campos obligatorios
        if (etDia != null && TextUtils.isEmpty(etDia.getText())) {
            etDia.setError("Campo obligatorio");
            valido = false;
        }
        if (etMes != null && TextUtils.isEmpty(etMes.getText())) {
            etMes.setError("Campo obligatorio");
            valido = false;
        }
        if (etDepartamento != null && TextUtils.isEmpty(etDepartamento.getText())) {
            etDepartamento.setError("Campo obligatorio");
            valido = false;
        }
        if (etNombreResidente != null && TextUtils.isEmpty(etNombreResidente.getText())) {
            etNombreResidente.setError("Campo obligatorio");
            valido = false;
        }
        if (etNumeroControl != null && TextUtils.isEmpty(etNumeroControl.getText())) {
            etNumeroControl.setError("Campo obligatorio");
            valido = false;
        }
        if (etCarrera != null && TextUtils.isEmpty(etCarrera.getText())) {
            etCarrera.setError("Campo obligatorio");
            valido = false;
        }
        if (etNombreProyecto != null && TextUtils.isEmpty(etNombreProyecto.getText())) {
            etNombreProyecto.setError("Campo obligatorio");
            valido = false;
        }
        if (etEmpresa != null && TextUtils.isEmpty(etEmpresa.getText())) {
            etEmpresa.setError("Campo obligatorio");
            valido = false;
        }
        if (etNoAsesoria != null && TextUtils.isEmpty(etNoAsesoria.getText())) {
            etNoAsesoria.setError("Campo obligatorio");
            valido = false;
        }
        if (etTipoAsesoria != null && TextUtils.isEmpty(etTipoAsesoria.getText())) {
            etTipoAsesoria.setError("Campo obligatorio");
            valido = false;
        }
        // etTemas y etSolucion son opcionales, no se validan
        if (etAsesor != null && TextUtils.isEmpty(etAsesor.getText())) {
            etAsesor.setError("Campo obligatorio");
            valido = false;
        }
        if (etResidenteFirma != null && TextUtils.isEmpty(etResidenteFirma.getText())) {
            etResidenteFirma.setError("Campo obligatorio");
            valido = false;
        }

        return valido;
    }

    private boolean validarPasoSolicitud(View view) {
        com.google.android.material.textfield.TextInputEditText etNombreEmpresa = view.findViewById(R.id.etNombreEmpresa);
        com.google.android.material.textfield.TextInputEditText etMotivo = view.findViewById(R.id.etMotivo);

        if (etNombreEmpresa != null && TextUtils.isEmpty(etNombreEmpresa.getText())) {
            etNombreEmpresa.setError("Este campo es obligatorio");
            return false;
        }
        if (etMotivo != null && TextUtils.isEmpty(etMotivo.getText())) {
            etMotivo.setError("Este campo es obligatorio");
            return false;
        }
        return true;
    }

    private boolean validarPasoCarta(View view) {
        com.google.android.material.textfield.TextInputEditText etDestinatario = view.findViewById(R.id.etDestinatario);
        com.google.android.material.textfield.TextInputEditText etCuerpo = view.findViewById(R.id.etCuerpo);

        if (etDestinatario != null && TextUtils.isEmpty(etDestinatario.getText())) {
            etDestinatario.setError("Este campo es obligatorio");
            return false;
        }
        if (etCuerpo != null && TextUtils.isEmpty(etCuerpo.getText())) {
            etCuerpo.setError("Este campo es obligatorio");
            return false;
        }
        return true;
    }

    private void guardarDatosPasoActual() {
        // Guardar datos de los campos visibles en el paso actual
        View pasoView = containerPasos.getChildAt(0);
        if (pasoView == null) return;

        // Buscar todos los TextInputEditText en el paso actual
        buscarYGuardarCampos(pasoView);
    }

    private void buscarYGuardarCampos(View view) {
        if (view instanceof com.google.android.material.textfield.TextInputEditText) {
            com.google.android.material.textfield.TextInputEditText editText = (com.google.android.material.textfield.TextInputEditText) view;
            String key = obtenerClaveCampo(editText);
            String value = editText.getText() != null ? editText.getText().toString() : "";
            if (!value.isEmpty()) {
                datosFormulario.put(key, value);
            }
        } else if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                buscarYGuardarCampos(group.getChildAt(i));
            }
        }
    }

    private String obtenerClaveCampo(com.google.android.material.textfield.TextInputEditText editText) {
        // Mapear IDs de campos a claves para el PDF/Word
        int id = editText.getId();
        if (id == R.id.etNombreEmpresa) return "nombreEmpresa";
        if (id == R.id.etMotivo) return "motivo";
        if (id == R.id.etDestinatario) return "destinatario";
        if (id == R.id.etCuerpo) return "cuerpo";

        // Campos de reportes parciales
        if (id == R.id.etDia) return "dia";
        if (id == R.id.etMes) return "mes";
        if (id == R.id.etDepartamentoAcademico) return "departamento_academico";
        if (id == R.id.etNombreResidente) return "nombre_del_residente";
        if (id == R.id.etNumeroControl) return "numero_control";
        if (id == R.id.etCarrera) return "carrera";
        if (id == R.id.etNombreProyecto) return "nombre_del_proyecto";
        if (id == R.id.etPeriodoRealizacion) return "periodo_realizacion";
        if (id == R.id.etEmpresa) return "empresa";
        if (id == R.id.etNoAsesoria) return "no_asesoria";
        if (id == R.id.etTipoAsesoria) return "tipo_asesoria";
        if (id == R.id.etTemasAsesorar) return "temas_asesorar";
        if (id == R.id.etSolucionRecomendada) return "solucion_recomendada";
        if (id == R.id.etNombreAsesorInterno) return "nombre_asesor_interno";
        if (id == R.id.etNombreResidenteFirma) return "nombre_residente";

        // Por defecto usar el hint
        return editText.getHint() != null ? editText.getHint().toString() : "campo_" + id;
    }

    private Map<String, String> convertirDatosParaPDF() {
        Map<String, String> datosMap = new HashMap<>();
        for (Map.Entry<String, String> entry : datosFormulario.entrySet()) {
            // Convertir claves a formato esperado por el generador
            String key = entry.getKey();
            if (key.equals("nombreEmpresa") || key.equals("Nombre de la empresa")) {
                datosMap.put("nombreEmpresa", entry.getValue());
            } else if (key.equals("motivo") || key.equals("Motivo de la solicitud")) {
                datosMap.put("motivo", entry.getValue());
            } else if (key.equals("destinatario") || key.equals("Destinatario")) {
                datosMap.put("destinatario", entry.getValue());
            } else if (key.equals("cuerpo") || key.equals("Cuerpo de la carta")) {
                datosMap.put("cuerpo", entry.getValue());
            } else {
                datosMap.put(key, entry.getValue());
            }
        }
        return datosMap;
    }

    private void guardarBorrador() {
        if (currentUserId == null) {
            Toast.makeText(getContext(), "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show();
            return;
        }

        guardarDatosPasoActual();
        Map<String, Object> borradorData = new HashMap<>(datosFormulario);

        firebaseManager.guardarBorradorWizard(
                currentUserId,
                documentoId,
                borradorData,
                () -> {
                    Toast.makeText(getContext(), "Borrador guardado", Toast.LENGTH_SHORT).show();
                },
                error -> {
                    Log.e(TAG, "Error guardando borrador", error);
                    Toast.makeText(getContext(), "Error al guardar borrador", Toast.LENGTH_SHORT).show();
                }
        );
    }

    private void finalizarWizard() {
        if (!validarPasoActual()) {
            Toast.makeText(getContext(), "Por favor completa todos los campos obligatorios", Toast.LENGTH_SHORT).show();
            return;
        }

        guardarDatosPasoActual();
        generarPDF();
    }

    private void generarPDF() {
        if (getContext() == null || getActivity() == null) return;

        // Todos los documentos usan Word con placeholders desde Firebase Storage
        // El método generarDesdeWord() descarga el template, lo llena con {{placeholders}} y convierte a PDF
        generarDesdeWord();
    }


    private void cargarBorrador() {
        if (currentUserId == null) return;

        FirebaseFirestore.getInstance()
                .collection("user_profiles")
                .document(currentUserId)
                .collection("tramites_formatos")
                .document("documentos")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        Map<String, Object> documentosData = documentSnapshot.getData();
                        if (documentosData != null && documentosData.containsKey(documentoId)) {
                            Object docObj = documentosData.get(documentoId);
                            if (docObj instanceof Map) {
                                Map<String, Object> docData = (Map<String, Object>) docObj;
                                if (docData.containsKey("borrador")) {
                                    Object borradorObj = docData.get("borrador");
                                    if (borradorObj instanceof Map) {
                                        Map<String, Object> borrador = (Map<String, Object>) borradorObj;
                                        // Cargar datos del borrador en el formulario
                                        for (Map.Entry<String, Object> entry : borrador.entrySet()) {
                                            datosFormulario.put(entry.getKey(), entry.getValue().toString());
                                        }
                                        // Cargar datos en los campos del primer paso
                                        getActivity().runOnUiThread(() -> {
                                            cargarDatosEnCampos();
                                            Toast.makeText(getContext(), "Borrador cargado", Toast.LENGTH_SHORT).show();
                                        });
                                    }
                                }
                            }
                        }
                    }
                })
                .addOnFailureListener(error -> {
                    Log.d(TAG, "No se encontró borrador previo");
                });
    }

    private void cargarDatosEnCampos() {
        View pasoView = containerPasos.getChildAt(0);
        if (pasoView == null) return;

        // Cargar datos en los campos según el tipo de documento
        if (documentoId.equals("solicitud_residencias")) {
            com.google.android.material.textfield.TextInputEditText etNombreEmpresa = pasoView.findViewById(R.id.etNombreEmpresa);
            com.google.android.material.textfield.TextInputEditText etMotivo = pasoView.findViewById(R.id.etMotivo);
            if (etNombreEmpresa != null && datosFormulario.containsKey("Nombre de la empresa")) {
                etNombreEmpresa.setText(datosFormulario.get("Nombre de la empresa"));
            }
            if (etMotivo != null && datosFormulario.containsKey("Motivo de la solicitud")) {
                etMotivo.setText(datosFormulario.get("Motivo de la solicitud"));
            }
        } else if (documentoId.contains("carta")) {
            com.google.android.material.textfield.TextInputEditText etDestinatario = pasoView.findViewById(R.id.etDestinatario);
            com.google.android.material.textfield.TextInputEditText etCuerpo = pasoView.findViewById(R.id.etCuerpo);
            if (etDestinatario != null && datosFormulario.containsKey("Destinatario")) {
                etDestinatario.setText(datosFormulario.get("Destinatario"));
            }
            if (etCuerpo != null && datosFormulario.containsKey("Cuerpo de la carta")) {
                etCuerpo.setText(datosFormulario.get("Cuerpo de la carta"));
            }
        } else if (documentoId.equals("reportes_parciales")) {
            // Cargar datos de reportes parciales
            cargarDatosReportesParciales(pasoView);
        }
    }

    private void cargarDatosReportesParciales(View view) {
        // Cargar todos los campos de reportes parciales
        com.google.android.material.textfield.TextInputEditText etDia = view.findViewById(R.id.etDia);
        com.google.android.material.textfield.TextInputEditText etMes = view.findViewById(R.id.etMes);
        com.google.android.material.textfield.TextInputEditText etDepartamento = view.findViewById(R.id.etDepartamentoAcademico);
        com.google.android.material.textfield.TextInputEditText etNombreResidente = view.findViewById(R.id.etNombreResidente);
        com.google.android.material.textfield.TextInputEditText etNumeroControl = view.findViewById(R.id.etNumeroControl);
        com.google.android.material.textfield.TextInputEditText etCarrera = view.findViewById(R.id.etCarrera);
        com.google.android.material.textfield.TextInputEditText etNombreProyecto = view.findViewById(R.id.etNombreProyecto);
        com.google.android.material.textfield.TextInputEditText etPeriodo = view.findViewById(R.id.etPeriodoRealizacion);
        com.google.android.material.textfield.TextInputEditText etEmpresa = view.findViewById(R.id.etEmpresa);
        com.google.android.material.textfield.TextInputEditText etNoAsesoria = view.findViewById(R.id.etNoAsesoria);
        com.google.android.material.textfield.TextInputEditText etTipoAsesoria = view.findViewById(R.id.etTipoAsesoria);
        com.google.android.material.textfield.TextInputEditText etTemas = view.findViewById(R.id.etTemasAsesorar);
        com.google.android.material.textfield.TextInputEditText etSolucion = view.findViewById(R.id.etSolucionRecomendada);
        com.google.android.material.textfield.TextInputEditText etAsesor = view.findViewById(R.id.etNombreAsesorInterno);
        com.google.android.material.textfield.TextInputEditText etResidenteFirma = view.findViewById(R.id.etNombreResidenteFirma);

        if (etDia != null && datosFormulario.containsKey("dia")) etDia.setText(datosFormulario.get("dia"));
        if (etMes != null && datosFormulario.containsKey("mes")) etMes.setText(datosFormulario.get("mes"));
        if (etDepartamento != null && datosFormulario.containsKey("departamento_academico")) etDepartamento.setText(datosFormulario.get("departamento_academico"));
        if (etNombreResidente != null && datosFormulario.containsKey("nombre_del_residente")) etNombreResidente.setText(datosFormulario.get("nombre_del_residente"));
        if (etNumeroControl != null && datosFormulario.containsKey("numero_control")) etNumeroControl.setText(datosFormulario.get("numero_control"));
        if (etCarrera != null && datosFormulario.containsKey("carrera")) etCarrera.setText(datosFormulario.get("carrera"));
        if (etNombreProyecto != null && datosFormulario.containsKey("nombre_del_proyecto")) etNombreProyecto.setText(datosFormulario.get("nombre_del_proyecto"));
        if (etPeriodo != null && datosFormulario.containsKey("periodo_realizacion")) etPeriodo.setText(datosFormulario.get("periodo_realizacion"));
        if (etEmpresa != null && datosFormulario.containsKey("empresa")) etEmpresa.setText(datosFormulario.get("empresa"));
        if (etNoAsesoria != null && datosFormulario.containsKey("no_asesoria")) etNoAsesoria.setText(datosFormulario.get("no_asesoria"));
        if (etTipoAsesoria != null && datosFormulario.containsKey("tipo_asesoria")) etTipoAsesoria.setText(datosFormulario.get("tipo_asesoria"));
        if (etTemas != null && datosFormulario.containsKey("temas_asesorar")) etTemas.setText(datosFormulario.get("temas_asesorar"));
        if (etSolucion != null && datosFormulario.containsKey("solucion_recomendada")) etSolucion.setText(datosFormulario.get("solucion_recomendada"));
        if (etAsesor != null && datosFormulario.containsKey("nombre_asesor_interno")) etAsesor.setText(datosFormulario.get("nombre_asesor_interno"));
        if (etResidenteFirma != null && datosFormulario.containsKey("nombre_residente")) etResidenteFirma.setText(datosFormulario.get("nombre_residente"));
    }

    private void generarDesdeWord() {
        if (getContext() == null || getActivity() == null) return;
        Toast.makeText(getContext(), "Descargando y llenando formato...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            try {
                // Descargar Word desde Firebase Storage
                com.google.firebase.storage.FirebaseStorage storage = com.google.firebase.storage.FirebaseStorage.getInstance();
                String path = "formatos_plantillas/" + documentoId + ".docx";
                com.google.firebase.storage.StorageReference storageRef = storage.getReference(path);

                java.io.File tempWordFile = new java.io.File(getContext().getCacheDir(), "temp_" + documentoId + ".docx");
                java.io.File filledWordFile = new java.io.File(getContext().getCacheDir(), "filled_" + documentoId + ".docx");

                // Descargar Word
                storageRef.getFile(tempWordFile)
                        .addOnSuccessListener(taskSnapshot -> {
                            new Thread(() -> {
                                try {
                                    // Llenar el Word con los datos
                                    WordDocumentFiller filler = new WordDocumentFiller();
                                    java.io.FileInputStream inputStream = new java.io.FileInputStream(tempWordFile);
                                    java.io.FileOutputStream outputStream = new java.io.FileOutputStream(filledWordFile);

                                    // Preparar datos para el Word (usar las claves exactas del documento)
                                    Map<String, String> datosWord = new HashMap<>();
                                    for (Map.Entry<String, String> entry : datosFormulario.entrySet()) {
                                        datosWord.put(entry.getKey(), entry.getValue());
                                    }

                                    boolean exito = filler.llenarDocumentoWord(inputStream, outputStream, datosWord);
                                    inputStream.close();
                                    outputStream.close();

                                    if (exito) {
                                        // Subir el Word llenado directamente (preserva el formato original)
                                        // Luego lo convertiremos a PDF usando un servicio mejor o mantendremos el Word
                                        subirWordLlenado(filledWordFile);
                                    } else {
                                        getActivity().runOnUiThread(() -> {
                                            Toast.makeText(getContext(), "Error al llenar el documento", Toast.LENGTH_SHORT).show();
                                        });
                                    }

                                } catch (Exception e) {
                                    Log.e(TAG, "Error llenando Word", e);
                                    getActivity().runOnUiThread(() -> {
                                        Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    });
                                }
                            }).start();
                        })
                        .addOnFailureListener(exception -> {
                            Log.e(TAG, "Error descargando Word", exception);
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(getContext(), "Error al descargar el formato Word", Toast.LENGTH_LONG).show();
                            });
                        });

            } catch (Exception e) {
                Log.e(TAG, "Error en generarDesdeWord", e);
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void subirWordLlenado(java.io.File wordFile) {
        if (currentUserId == null || wordFile == null) {
            getActivity().runOnUiThread(() -> {
                Toast.makeText(getContext(), "Error: No se pudo subir el documento", Toast.LENGTH_SHORT).show();
            });
            return;
        }

        android.net.Uri fileUri = androidx.core.content.FileProvider.getUriForFile(
                getContext(),
                getContext().getApplicationContext().getPackageName() + ".provider",
                wordFile
        );

        FirebaseManager firebaseManager = new FirebaseManager();
        // Subir el Word llenado directamente (preserva el formato original)
        firebaseManager.subirWordTramite(
                currentUserId,
                documentoId,
                fileUri,
                urlWord -> {
                    // Guardar URL del Word en Firestore
                    Map<String, Object> documentoData = new HashMap<>();
                    documentoData.put("urlDocumento", urlWord); // Guardamos la URL del Word
                    documentoData.put("urlWord", urlWord); // También guardamos una referencia específica
                    documentoData.put("estado", "completado");
                    documentoData.put("fechaSubida", System.currentTimeMillis());
                    documentoData.put("datos", datosFormulario);

                    firebaseManager.guardarDocumentoTramite(
                            currentUserId,
                            documentoId,
                            documentoData,
                            () -> {
                                getActivity().runOnUiThread(() -> {
                                    Toast.makeText(getContext(), "Documento generado y guardado exitosamente", Toast.LENGTH_SHORT).show();
                                    if (getActivity() != null) {
                                        getActivity().getSupportFragmentManager().popBackStack();
                                    }
                                });
                            },
                            error -> {
                                Log.e(TAG, "Error guardando documento", error);
                                getActivity().runOnUiThread(() -> {
                                    Toast.makeText(getContext(), "Error al guardar documento", Toast.LENGTH_SHORT).show();
                                });
                            }
                    );
                },
                error -> {
                    Log.e(TAG, "Error subiendo Word", error);
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Error al subir documento", Toast.LENGTH_SHORT).show();
                    });
                }
        );
    }
}

