package com.example.sharedpreferencesapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class GestionProtocoloFragment extends Fragment {

    private static final String TAG = "GestionProtocoloFrag";
    private LinearLayout layoutProtocolos;
    private TextView tvNoProtocolos;
    private Button btnAgregar;

    private FirebaseManager firebaseManager;
    private FileManager fileManager; // Mantenido para el generador de PDF

    // AÑADIDO: Variable para guardar el ID del usuario actual
    private String currentUserId;

    private JSONObject protocoloPendiente;

    private final ActivityResultLauncher<Intent> selectorCarpeta = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null && protocoloPendiente != null) {
                        generarPDFEnUbicacion(protocoloPendiente, uri);
                    }
                }
            }
    );

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_gestion_protocolo, container, false);

        layoutProtocolos = view.findViewById(R.id.layoutProtocolos);
        tvNoProtocolos = view.findViewById(R.id.tvNoProtocolos);
        btnAgregar = view.findViewById(R.id.btnAgregarProtocolo);

        firebaseManager = new FirebaseManager();
        fileManager = new FileManager(requireContext());

        btnAgregar.setOnClickListener(v -> mostrarDialog(null));

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            this.currentUserId = user.getUid();
            cargarProtocolos();
        } else {
            tvNoProtocolos.setText("Error de sesión. Por favor, inicie sesión de nuevo.");
            tvNoProtocolos.setVisibility(View.VISIBLE);
            layoutProtocolos.setVisibility(View.GONE);
            btnAgregar.setEnabled(false);
        }
    }

    private void mostrarDialog(String protocoloId) {
        if (currentUserId == null) {
            Toast.makeText(getContext(), "Error de sesión.", Toast.LENGTH_SHORT).show();
            return;
        }

        firebaseManager.cargarAlumnos(currentUserId, task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                ArrayList<String> alumnosDisplay = new ArrayList<>();
                ArrayList<String> alumnosIds = new ArrayList<>();

                for (QueryDocumentSnapshot alumno : task.getResult()) {
                    String nombre = alumno.getString("nombre");
                    String numControl = alumno.getString("numControl");
                    alumnosDisplay.add(nombre + " (" + numControl + ")");
                    alumnosIds.add(alumno.getId());
                }

                if (alumnosDisplay.isEmpty()) {
                    Toast.makeText(getContext(), "Debe registrar al menos un alumno primero.", Toast.LENGTH_LONG).show();
                    return;
                }

                construirYMostrarDialogo(protocoloId, alumnosDisplay, alumnosIds);

            } else {
                Toast.makeText(getContext(), "Error al cargar la lista de alumnos.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void construirYMostrarDialogo(String protocoloId, ArrayList<String> alumnosDisplay, ArrayList<String> alumnosIds) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_protocolo, null);
        builder.setView(dialogView);

        TextView tvTitulo = dialogView.findViewById(R.id.tvTituloFormulario);
        Spinner spinnerAlumno = dialogView.findViewById(R.id.spinnerAlumno);
        EditText etNombreProyecto = dialogView.findViewById(R.id.etNombreProyecto);
        Spinner spinnerBanco = dialogView.findViewById(R.id.spinnerBancoProyecto);
        EditText etAsesor = dialogView.findViewById(R.id.etAsesor);
        EditText etNombreEmpresa = dialogView.findViewById(R.id.etNombreEmpresa);
        Spinner spinnerGiro = dialogView.findViewById(R.id.spinnerGiro);
        EditText etRFC = dialogView.findViewById(R.id.etRFC);
        EditText etDomicilio = dialogView.findViewById(R.id.etDomicilio);
        EditText etColonia = dialogView.findViewById(R.id.etColonia);
        EditText etCodigoPostal = dialogView.findViewById(R.id.etCodigoPostal);
        EditText etCiudad = dialogView.findViewById(R.id.etCiudad);
        EditText etCelular = dialogView.findViewById(R.id.etCelular);
        EditText etMision = dialogView.findViewById(R.id.etMision);
        EditText etTitular = dialogView.findViewById(R.id.etTitular);
        EditText etFirmante = dialogView.findViewById(R.id.etFirmante);
        EditText etPuestoTitular = dialogView.findViewById(R.id.etPuestoTitular);
        EditText etAsesorExterno = dialogView.findViewById(R.id.etAsesorExterno);
        EditText etPuestoAsesor = dialogView.findViewById(R.id.etPuestoAsesor);
        EditText etPuestoFirmante = dialogView.findViewById(R.id.etPuestoFirmante);
        Button btnGuardar = dialogView.findViewById(R.id.btnGuardar);
        Button btnCancelar = dialogView.findViewById(R.id.btnCancelar);

        ArrayAdapter<String> alumnosAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, alumnosDisplay);
        spinnerAlumno.setAdapter(alumnosAdapter);

        String[] bancos = {"Interdisciplinario", "Integradores", "Educacion dual"};
        ArrayAdapter<String> bancosAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, bancos);
        spinnerBanco.setAdapter(bancosAdapter);

        String[] giros = {"Industrial", "Servicios", "Publico", "Privado", "Otro"};
        ArrayAdapter<String> girosAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, giros);
        spinnerGiro.setAdapter(girosAdapter);

        AlertDialog dialog = builder.create();

        if (protocoloId != null) {
            tvTitulo.setText("Editar Protocolo");
            cargarDatosProtocolo(protocoloId, spinnerAlumno, etNombreProyecto, spinnerBanco,
                    etAsesor, etNombreEmpresa, spinnerGiro, etRFC, etDomicilio,
                    etColonia, etCodigoPostal, etCiudad, etCelular, etMision,
                    etTitular, etFirmante, etPuestoTitular, etAsesorExterno,
                    etPuestoAsesor, etPuestoFirmante, alumnosIds, bancos, giros);
        }

        btnGuardar.setOnClickListener(v -> {
            String proyecto = etNombreProyecto.getText().toString();
            String empresa = etNombreEmpresa.getText().toString();

            if (!proyecto.isEmpty() && !empresa.isEmpty() && spinnerAlumno.getSelectedItemPosition() != -1) {
                guardarProtocolo(protocoloId, alumnosIds.get(spinnerAlumno.getSelectedItemPosition()), etNombreProyecto, spinnerBanco,
                        etAsesor, etNombreEmpresa, spinnerGiro, etRFC, etDomicilio,
                        etColonia, etCodigoPostal, etCiudad, etCelular, etMision,
                        etTitular, etFirmante, etPuestoTitular, etAsesorExterno,
                        etPuestoAsesor, etPuestoFirmante, dialog);
            } else {
                Toast.makeText(getContext(), "Complete los campos obligatorios", Toast.LENGTH_SHORT).show();
            }
        });

        btnCancelar.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void cargarDatosProtocolo(String protocoloId, Spinner spinnerAlumno, EditText etNombreProyecto,
                                      Spinner spinnerBanco, EditText etAsesor, EditText etNombreEmpresa,
                                      Spinner spinnerGiro, EditText etRFC, EditText etDomicilio,
                                      EditText etColonia, EditText etCodigoPostal, EditText etCiudad,
                                      EditText etCelular, EditText etMision, EditText etTitular,
                                      EditText etFirmante, EditText etPuestoTitular, EditText etAsesorExterno,
                                      EditText etPuestoAsesor, EditText etPuestoFirmante,
                                      ArrayList<String> alumnosIds, String[] bancos, String[] giros) {
        if (currentUserId == null) return;

        firebaseManager.buscarProtocoloPorId(currentUserId, protocoloId, task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                DocumentSnapshot protocolo = task.getResult();
                if (protocolo.exists()) {
                    String alumnoId = protocolo.getString("alumnoId");
                    int alumnoPos = alumnosIds.indexOf(alumnoId);
                    if (alumnoPos != -1) spinnerAlumno.setSelection(alumnoPos);

                    etNombreProyecto.setText(protocolo.getString("nombreProyecto"));
                    String banco = protocolo.getString("banco");
                    for (int i = 0; i < bancos.length; i++) {
                        if (bancos[i].equals(banco)) {
                            spinnerBanco.setSelection(i);
                            break;
                        }
                    }
                    etAsesor.setText(protocolo.getString("asesor"));
                    etNombreEmpresa.setText(protocolo.getString("nombreEmpresa"));
                    String giro = protocolo.getString("giro");
                    for (int i = 0; i < giros.length; i++) {
                        if (giros[i].equals(giro)) {
                            spinnerGiro.setSelection(i);
                            break;
                        }
                    }
                    etRFC.setText(protocolo.getString("rfc"));
                    etDomicilio.setText(protocolo.getString("domicilio"));
                    etColonia.setText(protocolo.getString("colonia"));
                    etCodigoPostal.setText(protocolo.getString("codigoPostal"));
                    etCiudad.setText(protocolo.getString("ciudad"));
                    etCelular.setText(protocolo.getString("celular"));
                    etMision.setText(protocolo.getString("mision"));
                    etTitular.setText(protocolo.getString("titular"));
                    etFirmante.setText(protocolo.getString("firmante"));
                    etPuestoTitular.setText(protocolo.getString("puestoTitular"));
                    etAsesorExterno.setText(protocolo.getString("asesorExterno"));
                    etPuestoAsesor.setText(protocolo.getString("puestoAsesor"));
                    etPuestoFirmante.setText(protocolo.getString("puestoFirmante"));
                }
            }
        });
    }

    private void guardarProtocolo(String protocoloId, String alumnoId, EditText etNombreProyecto, Spinner spinnerBanco,
                                  EditText etAsesor, EditText etNombreEmpresa, Spinner spinnerGiro, EditText etRFC,
                                  EditText etDomicilio, EditText etColonia, EditText etCodigoPostal, EditText etCiudad,
                                  EditText etCelular, EditText etMision, EditText etTitular, EditText etFirmante,
                                  EditText etPuestoTitular, EditText etAsesorExterno, EditText etPuestoAsesor,
                                  EditText etPuestoFirmante, AlertDialog dialog) {
        if (currentUserId == null) return;

        Map<String, Object> protocolo = new HashMap<>();
        protocolo.put("alumnoId", alumnoId);
        protocolo.put("nombreProyecto", convertirAMayusculasSinAcentos(etNombreProyecto.getText().toString()));
        protocolo.put("banco", spinnerBanco.getSelectedItem().toString());
        protocolo.put("asesor", convertirAMayusculasSinAcentos(etAsesor.getText().toString()));
        protocolo.put("nombreEmpresa", convertirAMayusculasSinAcentos(etNombreEmpresa.getText().toString()));
        protocolo.put("giro", spinnerGiro.getSelectedItem().toString());
        protocolo.put("rfc", etRFC.getText().toString().toUpperCase());
        protocolo.put("domicilio", convertirAMayusculasSinAcentos(etDomicilio.getText().toString()));
        protocolo.put("colonia", convertirAMayusculasSinAcentos(etColonia.getText().toString()));
        protocolo.put("codigoPostal", etCodigoPostal.getText().toString());
        protocolo.put("ciudad", convertirAMayusculasSinAcentos(etCiudad.getText().toString()));
        protocolo.put("celular", etCelular.getText().toString());
        protocolo.put("mision", convertirAMayusculasSinAcentos(etMision.getText().toString()));
        protocolo.put("titular", convertirAMayusculasSinAcentos(etTitular.getText().toString()));
        protocolo.put("firmante", convertirAMayusculasSinAcentos(etFirmante.getText().toString()));
        protocolo.put("puestoTitular", convertirAMayusculasSinAcentos(etPuestoTitular.getText().toString()));
        protocolo.put("asesorExterno", convertirAMayusculasSinAcentos(etAsesorExterno.getText().toString()));
        protocolo.put("puestoAsesor", convertirAMayusculasSinAcentos(etPuestoAsesor.getText().toString()));
        protocolo.put("puestoFirmante", convertirAMayusculasSinAcentos(etPuestoFirmante.getText().toString()));

        firebaseManager.guardarProtocolo(currentUserId, protocoloId, protocolo,
                () -> {
                    dialog.dismiss();
                    cargarProtocolos();
                    String mensaje = (protocoloId == null) ? "Protocolo agregado" : "Protocolo actualizado";
                    Toast.makeText(getContext(), mensaje, Toast.LENGTH_SHORT).show();
                },
                e -> Toast.makeText(getContext(), "Error al guardar el protocolo", Toast.LENGTH_SHORT).show()
        );
    }

    private void cargarProtocolos() {
        if (currentUserId == null) return;

        layoutProtocolos.removeAllViews();
        tvNoProtocolos.setText("No hay protocolos registrados.");
        tvNoProtocolos.setVisibility(View.VISIBLE);

        firebaseManager.cargarProtocolos(currentUserId, task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                if (task.getResult().isEmpty()) {
                    tvNoProtocolos.setVisibility(View.VISIBLE);
                } else {
                    tvNoProtocolos.setVisibility(View.GONE);
                    for (QueryDocumentSnapshot documento : task.getResult()) {
                        crearCardProtocolo(documento);
                    }
                }
            } else {
                tvNoProtocolos.setVisibility(View.VISIBLE);
                Toast.makeText(getContext(), "Error al cargar protocolos.", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error cargando protocolos", task.getException());
            }
        });
    }

    private void crearCardProtocolo(DocumentSnapshot protocolo) {
        if (currentUserId == null) return;

        View cardView = LayoutInflater.from(getContext()).inflate(R.layout.card_protocolo, layoutProtocolos, false);

        TextView tvNombreProyecto = cardView.findViewById(R.id.tvNombreProyecto);
        TextView tvAlumno = cardView.findViewById(R.id.tvAlumno);
        TextView tvEmpresa = cardView.findViewById(R.id.tvEmpresa);
        TextView tvBanco = cardView.findViewById(R.id.tvBanco);
        TextView tvAsesor = cardView.findViewById(R.id.tvAsesor);
        TextView tvCiudad = cardView.findViewById(R.id.tvCiudad);
        Button btnPDF = cardView.findViewById(R.id.btnPDF);
        Button btnEditar = cardView.findViewById(R.id.btnEditar);
        Button btnEliminar = cardView.findViewById(R.id.btnEliminar);

        String protocoloId = protocolo.getId();
        String alumnoId = protocolo.getString("alumnoId");

        tvNombreProyecto.setText(protocolo.getString("nombreProyecto"));
        tvEmpresa.setText("Empresa: " + protocolo.getString("nombreEmpresa"));
        tvBanco.setText("Banco: " + protocolo.getString("banco"));
        tvAsesor.setText("Asesor: " + protocolo.getString("asesor"));
        tvCiudad.setText("Ciudad: " + protocolo.getString("ciudad"));

        if (alumnoId != null && !alumnoId.isEmpty()) {
            firebaseManager.buscarAlumnoPorId(currentUserId, alumnoId, task -> {
                if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                    DocumentSnapshot alumno = task.getResult();
                    String nombreAlumno = alumno.getString("nombre");
                    String numControl = alumno.getString("numControl");
                    tvAlumno.setText("Alumno: " + nombreAlumno + " (" + numControl + ")");
                } else {
                    tvAlumno.setText("Alumno: No encontrado");
                }
            });
        } else {
            tvAlumno.setText("Alumno: No asignado");
        }

        btnPDF.setOnClickListener(v -> {
            JSONObject protocoloJson = new JSONObject(protocolo.getData());
            try {
                protocoloJson.put("id", protocolo.getId());
                seleccionarUbicacionPDF(protocoloJson);
            } catch (JSONException e) {
                Toast.makeText(getContext(), "Error al preparar datos para PDF", Toast.LENGTH_SHORT).show();
            }
        });

        btnEditar.setOnClickListener(v -> mostrarDialog(protocoloId));

        btnEliminar.setOnClickListener(v -> {
            new AlertDialog.Builder(getContext())
                    .setTitle("Eliminar Protocolo")
                    .setMessage("¿Eliminar este protocolo?")
                    .setPositiveButton("Eliminar", (dialog, which) -> {
                        firebaseManager.eliminarProtocolo(currentUserId, protocoloId,
                                () -> {
                                    cargarProtocolos();
                                    Toast.makeText(getContext(), "Protocolo eliminado", Toast.LENGTH_SHORT).show();
                                },
                                e -> Toast.makeText(getContext(), "Error al eliminar el protocolo", Toast.LENGTH_SHORT).show()
                        );
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });

        layoutProtocolos.addView(cardView);
    }

    private void seleccionarUbicacionPDF(JSONObject protocolo) {
        protocoloPendiente = protocolo;

        String nombreProyecto = protocolo.optString("nombreProyecto", "Protocolo");
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String nombreArchivo = "Protocolo_" + nombreProyecto.replaceAll("[^a-zA-Z0-9]", "_") + "_" + timestamp + ".pdf";

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_TITLE, nombreArchivo);

        try {
            selectorCarpeta.launch(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "❌ Error al abrir selector de archivos", Toast.LENGTH_SHORT).show();
        }
    }

    private void generarPDFEnUbicacion(JSONObject protocolo, Uri uri) {
        Toast.makeText(getContext(), "📄 Generando PDF...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            if (getActivity() == null) return;
            try {
                PDFGeneratorExterno pdfGenerator = new PDFGeneratorExterno(requireContext());
                boolean exito = pdfGenerator.generarPDFProtocoloEnUri(protocolo, uri);

                getActivity().runOnUiThread(() -> {
                    if (exito) {
                        String mensaje = "✅ PDF guardado exitosamente";
                        Toast.makeText(getContext(), mensaje, Toast.LENGTH_LONG).show();

                        new AlertDialog.Builder(getContext())
                                .setTitle("📄 PDF Creado")
                                .setMessage("El archivo PDF se ha guardado en la ubicación seleccionada.")
                                .setPositiveButton("OK", null)
                                .show();
                    } else {
                        Toast.makeText(getContext(), "❌ Error al generar PDF", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "❌ Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private String convertirAMayusculasSinAcentos(String texto) {
        if (texto == null || texto.isEmpty()) {
            return "";
        }
        String textoMayus = texto.toUpperCase();
        textoMayus = textoMayus.replace("Á", "A")
                .replace("É", "E")
                .replace("Í", "I")
                .replace("Ó", "O")
                .replace("Ú", "U")
                .replace("Ñ", "N")
                .replace("Ü", "U")
                .replace("À", "A")
                .replace("È", "E")
                .replace("Ì", "I")
                .replace("Ò", "O")
                .replace("Ù", "U")
                .replace("Ã¡", "A")
                .replace("Ã©", "E")
                .replace("Ã­", "I")
                .replace("Ã³", "O")
                .replace("Ãº", "U")
                .replace("Ã±", "N")
                .replace("Ã¼", "U")
                .replace("Â", "")
                .replace("\u00A0", " ")
                .replace("\u2018", "'")
                .replace("\u2019", "'")
                .replace("\u201C", "\"")
                .replace("\u201D", "\"")
                .replace("\u2013", "-")
                .replace("\u2014", "-");

        return textoMayus.trim();
    }
}