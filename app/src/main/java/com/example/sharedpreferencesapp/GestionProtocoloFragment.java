package com.example.sharedpreferencesapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class GestionProtocoloFragment extends Fragment {

    private LinearLayout layoutProtocolos;
    private TextView tvNoProtocolos;
    private SharedPreferences preferences;
    private SharedPreferences alumnosPrefs;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_gestion_protocolo, container, false);

        layoutProtocolos = view.findViewById(R.id.layoutProtocolos);
        tvNoProtocolos = view.findViewById(R.id.tvNoProtocolos);
        Button btnAgregar = view.findViewById(R.id.btnAgregarProtocolo);

        preferences = getActivity().getSharedPreferences("ProtocolosPrefs", Context.MODE_PRIVATE);
        alumnosPrefs = getActivity().getSharedPreferences("AlumnosPrefs", Context.MODE_PRIVATE);

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
        Set<String> alumnosSet = alumnosPrefs.getStringSet("lista_alumnos", new HashSet<>());
        for (String alumnoId : alumnosSet) {
            String nombre = alumnosPrefs.getString(alumnoId + "_nombre", "");
            String numControl = alumnosPrefs.getString(alumnoId + "_numControl", "");
            alumnos.add(nombre + " (" + numControl + ")");
            alumnosIds.add(alumnoId);
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

            String alumnoId = preferences.getString(protocoloId + "_alumnoId", "");
            for (int i = 0; i < alumnosIds.size(); i++) {
                if (alumnosIds.get(i).equals(alumnoId)) {
                    spinnerAlumno.setSelection(i);
                    break;
                }
            }

            etNombreProyecto.setText(preferences.getString(protocoloId + "_nombreProyecto", ""));

            String banco = preferences.getString(protocoloId + "_banco", "");
            for (int i = 0; i < bancos.length; i++) {
                if (bancos[i].equals(banco)) {
                    spinnerBanco.setSelection(i);
                    break;
                }
            }

            etAsesor.setText(preferences.getString(protocoloId + "_asesor", ""));
            etNombreEmpresa.setText(preferences.getString(protocoloId + "_nombreEmpresa", ""));

            String giro = preferences.getString(protocoloId + "_giro", "");
            for (int i = 0; i < giros.length; i++) {
                if (giros[i].equals(giro)) {
                    spinnerGiro.setSelection(i);
                    break;
                }
            }

            etRFC.setText(preferences.getString(protocoloId + "_rfc", ""));
            etDomicilio.setText(preferences.getString(protocoloId + "_domicilio", ""));
            etColonia.setText(preferences.getString(protocoloId + "_colonia", ""));
            etCodigoPostal.setText(preferences.getString(protocoloId + "_codigoPostal", ""));
            etCiudad.setText(preferences.getString(protocoloId + "_ciudad", ""));
            etCelular.setText(preferences.getString(protocoloId + "_celular", ""));
            etMision.setText(preferences.getString(protocoloId + "_mision", ""));
            etTitular.setText(preferences.getString(protocoloId + "_titular", ""));
            etFirmante.setText(preferences.getString(protocoloId + "_firmante", ""));
        }

        AlertDialog dialog = builder.create();

        dialogView.findViewById(R.id.btnGuardar).setOnClickListener(v -> {
            String proyecto = etNombreProyecto.getText().toString();
            String empresa = etNombreEmpresa.getText().toString();

            if (!proyecto.isEmpty() && !empresa.isEmpty() && spinnerAlumno.getSelectedItemPosition() != -1 && !alumnosIds.get(spinnerAlumno.getSelectedItemPosition()).isEmpty()) {
                String finalProtocoloId = protocoloId;
                if (finalProtocoloId == null) {
                    finalProtocoloId = "protocolo_" + System.currentTimeMillis();

                    // Solo agregar a la lista si es nuevo
                    Set<String> protocolosSet = new HashSet<>(preferences.getStringSet("lista_protocolos", new HashSet<>()));
                    protocolosSet.add(finalProtocoloId);
                    preferences.edit().putStringSet("lista_protocolos", protocolosSet).apply();
                }

                SharedPreferences.Editor editor = preferences.edit();

                // Guardar todos los datos
                String alumnoId = alumnosIds.get(spinnerAlumno.getSelectedItemPosition());
                editor.putString(finalProtocoloId + "_alumnoId", alumnoId);
                editor.putString(finalProtocoloId + "_nombreProyecto", proyecto);
                editor.putString(finalProtocoloId + "_banco", spinnerBanco.getSelectedItem().toString());
                editor.putString(finalProtocoloId + "_asesor", etAsesor.getText().toString());
                editor.putString(finalProtocoloId + "_nombreEmpresa", empresa);
                editor.putString(finalProtocoloId + "_giro", spinnerGiro.getSelectedItem().toString());
                editor.putString(finalProtocoloId + "_rfc", etRFC.getText().toString());
                editor.putString(finalProtocoloId + "_domicilio", etDomicilio.getText().toString());
                editor.putString(finalProtocoloId + "_colonia", etColonia.getText().toString());
                editor.putString(finalProtocoloId + "_codigoPostal", etCodigoPostal.getText().toString());
                editor.putString(finalProtocoloId + "_ciudad", etCiudad.getText().toString());
                editor.putString(finalProtocoloId + "_celular", etCelular.getText().toString());
                editor.putString(finalProtocoloId + "_mision", etMision.getText().toString());
                editor.putString(finalProtocoloId + "_titular", etTitular.getText().toString());
                editor.putString(finalProtocoloId + "_firmante", etFirmante.getText().toString());
                editor.apply();

                cargarProtocolos();
                dialog.dismiss();

                String mensaje = (protocoloId == null) ? "Protocolo agregado" : "Protocolo actualizado";
                Toast.makeText(getContext(), mensaje, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Complete los campos obligatorios", Toast.LENGTH_SHORT).show();
            }
        });

        dialogView.findViewById(R.id.btnCancelar).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void cargarProtocolos() {
        layoutProtocolos.removeAllViews();

        Set<String> protocolosSet = preferences.getStringSet("lista_protocolos", new HashSet<>());

        if (protocolosSet.isEmpty()) {
            tvNoProtocolos.setVisibility(View.VISIBLE);
        } else {
            tvNoProtocolos.setVisibility(View.GONE);

            for (String protocoloId : protocolosSet) {
                View cardView = LayoutInflater.from(getContext()).inflate(R.layout.card_protocolo, layoutProtocolos, false);

                TextView tvNombreProyecto = cardView.findViewById(R.id.tvNombreProyecto);
                TextView tvAlumno = cardView.findViewById(R.id.tvAlumno);
                TextView tvEmpresa = cardView.findViewById(R.id.tvEmpresa);
                TextView tvBanco = cardView.findViewById(R.id.tvBanco);
                TextView tvAsesor = cardView.findViewById(R.id.tvAsesor);
                TextView tvCiudad = cardView.findViewById(R.id.tvCiudad);
                Button btnEditar = cardView.findViewById(R.id.btnEditar);
                Button btnEliminar = cardView.findViewById(R.id.btnEliminar);

                // Cargar datos
                String nombreProyecto = preferences.getString(protocoloId + "_nombreProyecto", "");
                String alumnoId = preferences.getString(protocoloId + "_alumnoId", "");
                String empresa = preferences.getString(protocoloId + "_nombreEmpresa", "");
                String banco = preferences.getString(protocoloId + "_banco", "");
                String asesor = preferences.getString(protocoloId + "_asesor", "");
                String ciudad = preferences.getString(protocoloId + "_ciudad", "");

                String nombreAlumno = alumnosPrefs.getString(alumnoId + "_nombre", "Sin alumno");
                String numControl = alumnosPrefs.getString(alumnoId + "_numControl", "");

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
                                SharedPreferences.Editor editor = preferences.edit();
                                Set<String> protocolos = new HashSet<>(preferences.getStringSet("lista_protocolos", new HashSet<>()));
                                protocolos.remove(protocoloId);
                                editor.putStringSet("lista_protocolos", protocolos);

                                // Eliminar todos los campos
                                editor.remove(protocoloId + "_alumnoId");
                                editor.remove(protocoloId + "_nombreProyecto");
                                editor.remove(protocoloId + "_banco");
                                editor.remove(protocoloId + "_asesor");
                                editor.remove(protocoloId + "_nombreEmpresa");
                                editor.remove(protocoloId + "_giro");
                                editor.remove(protocoloId + "_rfc");
                                editor.remove(protocoloId + "_domicilio");
                                editor.remove(protocoloId + "_colonia");
                                editor.remove(protocoloId + "_codigoPostal");
                                editor.remove(protocoloId + "_ciudad");
                                editor.remove(protocoloId + "_celular");
                                editor.remove(protocoloId + "_mision");
                                editor.remove(protocoloId + "_titular");
                                editor.remove(protocoloId + "_firmante");
                                editor.apply();

                                cargarProtocolos();
                                Toast.makeText(getContext(), "Protocolo eliminado", Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("Cancelar", null)
                            .show();
                });

                layoutProtocolos.addView(cardView);
            }
        }
    }
}