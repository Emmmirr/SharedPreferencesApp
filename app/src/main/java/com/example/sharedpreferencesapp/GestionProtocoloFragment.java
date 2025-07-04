package com.example.sharedpreferencesapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class GestionProtocoloFragment extends Fragment {

    private LinearLayout layoutProtocolos;
    private TextView tvNoProtocolos;
    private FileManager fileManager;
    private JSONObject protocoloPendiente; // ⬅️ AGREGAR ESTA VARIABLE

    // ⬅️ AGREGAR ESTE LAUNCHER PARA SELECCIONAR CARPETA
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
        Button btnAgregar = view.findViewById(R.id.btnAgregarProtocolo);

        // Inicializar FileManager
        fileManager = new FileManager(requireContext());

        btnAgregar.setOnClickListener(v -> mostrarDialog(null));
        cargarProtocolos();

        return view;
    }

    private void mostrarDialog(String protocoloId) {
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

        // Configurar spinner alumnos
        ArrayList<String> alumnos = new ArrayList<>();
        ArrayList<String> alumnosIds = new ArrayList<>();
        List<JSONObject> alumnosData = fileManager.cargarAlumnos();

        for (JSONObject alumno : alumnosData) {
            try {
                String nombre = alumno.optString("nombre", "");
                String numControl = alumno.optString("numControl", "");
                String id = alumno.getString("id");
                alumnos.add(nombre + " (" + numControl + ")");
                alumnosIds.add(id);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        if (alumnos.isEmpty()) {
            alumnos.add("No hay alumnos registrados");
            alumnosIds.add("");
        }

        ArrayAdapter<String> alumnosAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, alumnos);
        spinnerAlumno.setAdapter(alumnosAdapter);

        // Configurar spinner banco de proyectos
        String[] bancos = {"Interdisciplinario", "Integradores", "Educación dual"};
        ArrayAdapter<String> bancosAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, bancos);
        spinnerBanco.setAdapter(bancosAdapter);

        // Configurar spinner giro
        String[] giros = {"Industrial", "Servicios", "Público", "Privado", "Otro"};
        ArrayAdapter<String> girosAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, giros);
        spinnerGiro.setAdapter(girosAdapter);

        // Si es edición, cargar datos
        if (protocoloId != null) {
            tvTitulo.setText("Editar Protocolo");
            cargarDatosProtocolo(protocoloId, spinnerAlumno, etNombreProyecto, spinnerBanco,
                    etAsesor, etNombreEmpresa, spinnerGiro, etRFC, etDomicilio,
                    etColonia, etCodigoPostal, etCiudad, etCelular, etMision,
                    etTitular, etFirmante, alumnosIds, bancos, giros);
        }

        AlertDialog dialog = builder.create();

        dialogView.findViewById(R.id.btnGuardar).setOnClickListener(v -> {
            String proyecto = etNombreProyecto.getText().toString();
            String empresa = etNombreEmpresa.getText().toString();

            if (!proyecto.isEmpty() && !empresa.isEmpty() && spinnerAlumno.getSelectedItemPosition() != -1 && !alumnosIds.get(spinnerAlumno.getSelectedItemPosition()).isEmpty()) {

                try {
                    JSONObject protocolo = new JSONObject();

                    String finalProtocoloId = protocoloId;
                    if (finalProtocoloId == null) {
                        finalProtocoloId = "protocolo_" + System.currentTimeMillis();
                    }

                    // Guardar todos los datos
                    String alumnoId = alumnosIds.get(spinnerAlumno.getSelectedItemPosition());
                    protocolo.put("id", finalProtocoloId);
                    protocolo.put("alumnoId", alumnoId);
                    protocolo.put("nombreProyecto", proyecto);
                    protocolo.put("banco", spinnerBanco.getSelectedItem().toString());
                    protocolo.put("asesor", etAsesor.getText().toString());
                    protocolo.put("nombreEmpresa", empresa);
                    protocolo.put("giro", spinnerGiro.getSelectedItem().toString());
                    protocolo.put("rfc", etRFC.getText().toString());
                    protocolo.put("domicilio", etDomicilio.getText().toString());
                    protocolo.put("colonia", etColonia.getText().toString());
                    protocolo.put("codigoPostal", etCodigoPostal.getText().toString());
                    protocolo.put("ciudad", etCiudad.getText().toString());
                    protocolo.put("celular", etCelular.getText().toString());
                    protocolo.put("mision", etMision.getText().toString());
                    protocolo.put("titular", etTitular.getText().toString());
                    protocolo.put("firmante", etFirmante.getText().toString());

                    // Guardar en archivo
                    boolean exito;
                    if (fileManager.buscarProtocoloPorId(finalProtocoloId) != null) {
                        // Actualizar existente
                        exito = fileManager.actualizarProtocolo(finalProtocoloId, protocolo);
                    } else {
                        // Agregar nuevo
                        exito = fileManager.agregarProtocolo(protocolo);
                    }

                    if (exito) {
                        cargarProtocolos();
                        dialog.dismiss();
                        String mensaje = (protocoloId == null) ? "Protocolo agregado" : "Protocolo actualizado";
                        Toast.makeText(getContext(), mensaje, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Error al guardar el protocolo", Toast.LENGTH_SHORT).show();
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(getContext(), "Error al procesar los datos", Toast.LENGTH_SHORT).show();
                }

            } else {
                Toast.makeText(getContext(), "Complete los campos obligatorios", Toast.LENGTH_SHORT).show();
            }
        });

        dialogView.findViewById(R.id.btnCancelar).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void cargarDatosProtocolo(String protocoloId, Spinner spinnerAlumno, EditText etNombreProyecto,
                                      Spinner spinnerBanco, EditText etAsesor, EditText etNombreEmpresa,
                                      Spinner spinnerGiro, EditText etRFC, EditText etDomicilio,
                                      EditText etColonia, EditText etCodigoPostal, EditText etCiudad,
                                      EditText etCelular, EditText etMision, EditText etTitular,
                                      EditText etFirmante, ArrayList<String> alumnosIds,
                                      String[] bancos, String[] giros) {

        JSONObject protocolo = fileManager.buscarProtocoloPorId(protocoloId);
        if (protocolo != null) {
            try {
                String alumnoId = protocolo.optString("alumnoId", "");
                for (int i = 0; i < alumnosIds.size(); i++) {
                    if (alumnosIds.get(i).equals(alumnoId)) {
                        spinnerAlumno.setSelection(i);
                        break;
                    }
                }

                etNombreProyecto.setText(protocolo.optString("nombreProyecto", ""));

                String banco = protocolo.optString("banco", "");
                for (int i = 0; i < bancos.length; i++) {
                    if (bancos[i].equals(banco)) {
                        spinnerBanco.setSelection(i);
                        break;
                    }
                }

                etAsesor.setText(protocolo.optString("asesor", ""));
                etNombreEmpresa.setText(protocolo.optString("nombreEmpresa", ""));

                String giro = protocolo.optString("giro", "");
                for (int i = 0; i < giros.length; i++) {
                    if (giros[i].equals(giro)) {
                        spinnerGiro.setSelection(i);
                        break;
                    }
                }

                etRFC.setText(protocolo.optString("rfc", ""));
                etDomicilio.setText(protocolo.optString("domicilio", ""));
                etColonia.setText(protocolo.optString("colonia", ""));
                etCodigoPostal.setText(protocolo.optString("codigoPostal", ""));
                etCiudad.setText(protocolo.optString("ciudad", ""));
                etCelular.setText(protocolo.optString("celular", ""));
                etMision.setText(protocolo.optString("mision", ""));
                etTitular.setText(protocolo.optString("titular", ""));
                etFirmante.setText(protocolo.optString("firmante", ""));

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void cargarProtocolos() {
        layoutProtocolos.removeAllViews();

        List<JSONObject> protocolos = fileManager.cargarProtocolos();

        if (protocolos.isEmpty()) {
            tvNoProtocolos.setVisibility(View.VISIBLE);
        } else {
            tvNoProtocolos.setVisibility(View.GONE);

            for (JSONObject protocolo : protocolos) {
                crearCardProtocolo(protocolo);
            }
        }
    }

    private void crearCardProtocolo(JSONObject protocolo) {
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

        try {
            // Cargar datos del protocolo
            String protocoloId = protocolo.getString("id");
            String nombreProyecto = protocolo.optString("nombreProyecto", "");
            String alumnoId = protocolo.optString("alumnoId", "");
            String empresa = protocolo.optString("nombreEmpresa", "");
            String banco = protocolo.optString("banco", "");
            String asesor = protocolo.optString("asesor", "");
            String ciudad = protocolo.optString("ciudad", "");

            // Obtener datos del alumno
            JSONObject alumno = fileManager.buscarAlumnoPorId(alumnoId);
            String nombreAlumno = "Sin alumno";
            String numControl = "";

            if (alumno != null) {
                nombreAlumno = alumno.optString("nombre", "Sin alumno");
                numControl = alumno.optString("numControl", "");
            }

            tvNombreProyecto.setText(nombreProyecto);
            tvAlumno.setText("Alumno: " + nombreAlumno + " (" + numControl + ")");
            tvEmpresa.setText("Empresa: " + empresa);
            tvBanco.setText("Banco: " + banco);
            tvAsesor.setText("Asesor: " + asesor);
            tvCiudad.setText("Ciudad: " + ciudad);

            // ⬅️ CLICK LISTENER ACTUALIZADO PARA SELECCIONAR UBICACIÓN
            btnPDF.setOnClickListener(v -> seleccionarUbicacionPDF(protocolo));

            btnEditar.setOnClickListener(v -> mostrarDialog(protocoloId));

            btnEliminar.setOnClickListener(v -> {
                new AlertDialog.Builder(getContext())
                        .setTitle("Eliminar Protocolo")
                        .setMessage("¿Eliminar este protocolo?")
                        .setPositiveButton("Eliminar", (dialog, which) -> {
                            boolean exito = fileManager.eliminarProtocolo(protocoloId);
                            if (exito) {
                                cargarProtocolos();
                                Toast.makeText(getContext(), "Protocolo eliminado", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getContext(), "Error al eliminar el protocolo", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("Cancelar", null)
                        .show();
            });

        } catch (JSONException e) {
            e.printStackTrace();
        }

        layoutProtocolos.addView(cardView);
    }

    // ⬅️ MÉTODO PARA SELECCIONAR UBICACIÓN DONDE GUARDAR
    private void seleccionarUbicacionPDF(JSONObject protocolo) {
        protocoloPendiente = protocolo; // Guardar protocolo para usar después

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

    // ⬅️ MÉTODO PARA GENERAR PDF EN LA UBICACIÓN SELECCIONADA
    private void generarPDFEnUbicacion(JSONObject protocolo, Uri uri) {
        Toast.makeText(getContext(), "📄 Generando PDF...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            try {
                PDFGeneratorExterno pdfGenerator = new PDFGeneratorExterno(requireContext());
                boolean exito = pdfGenerator.generarPDFProtocoloEnUri(protocolo, uri);

                requireActivity().runOnUiThread(() -> {
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
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "❌ Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
}