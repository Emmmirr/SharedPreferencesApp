package com.example.sharedpreferencesapp;

import android.app.AlertDialog;
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

import java.util.ArrayList;
import java.util.List;

public class GestionProtocoloFragment extends Fragment {

    private LinearLayout layoutProtocolos;
    private TextView tvNoProtocolos;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_gestion_protocolo, container, false);

        layoutProtocolos = view.findViewById(R.id.layoutProtocolos);
        tvNoProtocolos = view.findViewById(R.id.tvNoProtocolos);
        Button btnAgregar = view.findViewById(R.id.btnAgregarProtocolo);

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

        // Configurar spinner alumnos - no students available since FileManager is removed
        ArrayList<String> alumnos = new ArrayList<>();
        ArrayList<String> alumnosIds = new ArrayList<>();
        alumnos.add("No hay alumnos disponibles");
        alumnosIds.add("");

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

            if (!proyecto.isEmpty() && !empresa.isEmpty()) {

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

                    // FileManager functionality removed - no actual saving to file
                    cargarProtocolos();
                    dialog.dismiss();
                    Toast.makeText(getContext(), "Funcionalidad de guardado deshabilitada", Toast.LENGTH_SHORT).show();

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

        // FileManager functionality removed - no data loading from file
        // All fields remain empty when editing
    }

    private void cargarProtocolos() {
        layoutProtocolos.removeAllViews();

        // Show no protocols message since we're removing FileManager functionality
        tvNoProtocolos.setVisibility(View.VISIBLE);
    }

    private void crearCardProtocolo(JSONObject protocolo) {
        View cardView = LayoutInflater.from(getContext()).inflate(R.layout.card_protocolo, layoutProtocolos, false);

        TextView tvNombreProyecto = cardView.findViewById(R.id.tvNombreProyecto);
        TextView tvAlumno = cardView.findViewById(R.id.tvAlumno);
        TextView tvEmpresa = cardView.findViewById(R.id.tvEmpresa);
        TextView tvBanco = cardView.findViewById(R.id.tvBanco);
        TextView tvAsesor = cardView.findViewById(R.id.tvAsesor);
        TextView tvCiudad = cardView.findViewById(R.id.tvCiudad);
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

            // FileManager functionality removed - cannot get alumno data
            String nombreAlumno = "Sin alumno";
            String numControl = "";

            tvNombreProyecto.setText(nombreProyecto);
            tvAlumno.setText("Alumno: " + nombreAlumno + " (" + numControl + ")");
            tvEmpresa.setText("Empresa: " + empresa);
            tvBanco.setText("Banco: " + banco);
            tvAsesor.setText("Asesor: " + asesor);
            tvCiudad.setText("Ciudad: " + ciudad);

            btnEditar.setOnClickListener(v -> mostrarDialog(protocoloId));

            btnEliminar.setOnClickListener(v -> {
                new AlertDialog.Builder(getContext())
                        .setTitle("Eliminar Protocolo")
                        .setMessage("¿Eliminar este protocolo?")
                        .setPositiveButton("Eliminar", (dialog, which) -> {
                            // FileManager functionality removed - no actual deletion from file
                            cargarProtocolos();
                            Toast.makeText(getContext(), "Funcionalidad de eliminación deshabilitada", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Cancelar", null)
                        .show();
            });

        } catch (JSONException e) {
            e.printStackTrace();
        }

        layoutProtocolos.addView(cardView);
    }
}