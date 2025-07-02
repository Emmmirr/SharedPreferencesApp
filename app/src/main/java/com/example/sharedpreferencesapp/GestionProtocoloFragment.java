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
import java.util.List;
import java.util.Set;

public class GestionProtocoloFragment extends Fragment {

    private LinearLayout layoutProtocolos;
    private TextView tvNoProtocolos;
    private Button btnAgregarProtocolo;
    private SharedPreferences sharedPreferences;
    private SharedPreferences alumnosPreferences;

    public GestionProtocoloFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_gestion_protocolo, container, false);

        layoutProtocolos = view.findViewById(R.id.layoutProtocolos);
        tvNoProtocolos = view.findViewById(R.id.tvNoProtocolos);
        btnAgregarProtocolo = view.findViewById(R.id.btnAgregarProtocolo);

        sharedPreferences = requireContext().getSharedPreferences("ProtocolosPrefs", Context.MODE_PRIVATE);
        alumnosPreferences = requireContext().getSharedPreferences("AlumnosPrefs", Context.MODE_PRIVATE);

        btnAgregarProtocolo.setOnClickListener(v -> mostrarFormularioProtocolo(null));

        cargarProtocolos();

        return view;
    }

    private void cargarProtocolos() {
        layoutProtocolos.removeAllViews();

        Set<String> protocolosSet = sharedPreferences.getStringSet("lista_protocolos", new HashSet<>());

        if (protocolosSet.isEmpty()) {
            tvNoProtocolos.setVisibility(View.VISIBLE);
        } else {
            tvNoProtocolos.setVisibility(View.GONE);

            for (String protocoloId : protocolosSet) {
                crearCardProtocolo(protocoloId);
            }
        }
    }

    private void crearCardProtocolo(String protocoloId) {
        View cardView = LayoutInflater.from(getContext()).inflate(R.layout.card_protocolo, layoutProtocolos, false);

        TextView tvNombreProyecto = cardView.findViewById(R.id.tvNombreProyecto);
        TextView tvAlumno = cardView.findViewById(R.id.tvAlumno);
        TextView tvEmpresa = cardView.findViewById(R.id.tvEmpresa);
        TextView tvEstado = cardView.findViewById(R.id.tvEstado);
        TextView tvFecha = cardView.findViewById(R.id.tvFecha);
        Button btnEditar = cardView.findViewById(R.id.btnEditar);
        Button btnEliminar = cardView.findViewById(R.id.btnEliminar);

        // Cargar datos del protocolo
        String nombreProyecto = sharedPreferences.getString(protocoloId + "_nombreProyecto", "");
        String alumnoId = sharedPreferences.getString(protocoloId + "_alumnoId", "");
        String empresa = sharedPreferences.getString(protocoloId + "_empresa", "");
        String estado = sharedPreferences.getString(protocoloId + "_estado", "");
        String fechaInicio = sharedPreferences.getString(protocoloId + "_fechaInicio", "");
        String avance = sharedPreferences.getString(protocoloId + "_avance", "0");

        // Obtener nombre del alumno
        String nombreAlumno = alumnosPreferences.getString(alumnoId + "_nombre", "Alumno no encontrado");

        tvNombreProyecto.setText(nombreProyecto);
        tvAlumno.setText("Alumno: " + nombreAlumno);
        tvEmpresa.setText(empresa);
        tvEstado.setText(estado + " (" + avance + "%)");
        tvFecha.setText("Inicio: " + fechaInicio);

        btnEditar.setOnClickListener(v -> mostrarFormularioProtocolo(protocoloId));
        btnEliminar.setOnClickListener(v -> eliminarProtocolo(protocoloId));

        layoutProtocolos.addView(cardView);
    }

    private void mostrarFormularioProtocolo(String protocoloId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_protocolo, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        TextView tvTitulo = dialogView.findViewById(R.id.tvTituloFormulario);
        Spinner spinnerAlumno = dialogView.findViewById(R.id.spinnerAlumno);
        EditText etNombreProyecto = dialogView.findViewById(R.id.etNombreProyecto);
        EditText etEmpresa = dialogView.findViewById(R.id.etEmpresa);
        EditText etFechaInicio = dialogView.findViewById(R.id.etFechaInicio);
        Spinner spinnerEstado = dialogView.findViewById(R.id.spinnerEstado);
        EditText etAvance = dialogView.findViewById(R.id.etAvance);
        EditText etObservaciones = dialogView.findViewById(R.id.etObservaciones);
        Button btnCancelar = dialogView.findViewById(R.id.btnCancelar);
        Button btnGuardar = dialogView.findViewById(R.id.btnGuardar);

        // Configurar spinner de alumnos
        List<String> alumnos = cargarListaAlumnos();
        ArrayAdapter<String> alumnosAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, alumnos);
        alumnosAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAlumno.setAdapter(alumnosAdapter);

        // Configurar spinner de estados
        String[] estados = {"En proceso", "En revisión", "Completado"};
        ArrayAdapter<String> estadosAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, estados);
        estadosAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEstado.setAdapter(estadosAdapter);

        // Si es edición, cargar datos existentes
        if (protocoloId != null) {
            tvTitulo.setText("Editar Protocolo");
            etNombreProyecto.setText(sharedPreferences.getString(protocoloId + "_nombreProyecto", ""));
            etEmpresa.setText(sharedPreferences.getString(protocoloId + "_empresa", ""));
            etFechaInicio.setText(sharedPreferences.getString(protocoloId + "_fechaInicio", ""));
            etAvance.setText(sharedPreferences.getString(protocoloId + "_avance", ""));
            etObservaciones.setText(sharedPreferences.getString(protocoloId + "_observaciones", ""));

            // Seleccionar alumno y estado actuales
            String alumnoActual = sharedPreferences.getString(protocoloId + "_alumnoId", "");
            String estadoActual = sharedPreferences.getString(protocoloId + "_estado", "");

            for (int i = 0; i < alumnos.size(); i++) {
                if (alumnos.get(i).contains(alumnoActual)) {
                    spinnerAlumno.setSelection(i);
                    break;
                }
            }

            for (int i = 0; i < estados.length; i++) {
                if (estados[i].equals(estadoActual)) {
                    spinnerEstado.setSelection(i);
                    break;
                }
            }
        }

        btnCancelar.setOnClickListener(v -> dialog.dismiss());

        btnGuardar.setOnClickListener(v -> {
            if (etNombreProyecto.getText().toString().trim().isEmpty() ||
                    spinnerAlumno.getSelectedItem() == null) {
                Toast.makeText(getContext(), "Complete los campos obligatorios", Toast.LENGTH_SHORT).show();
                return;
            }

            guardarProtocolo(protocoloId, spinnerAlumno, etNombreProyecto, etEmpresa,
                    etFechaInicio, spinnerEstado, etAvance, etObservaciones);

            cargarProtocolos();
            dialog.dismiss();

            String mensaje = (protocoloId == null) ? "Protocolo agregado exitosamente" : "Protocolo actualizado exitosamente";
            Toast.makeText(getContext(), mensaje, Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    private List<String> cargarListaAlumnos() {
        List<String> alumnos = new ArrayList<>();
        Set<String> alumnosSet = alumnosPreferences.getStringSet("lista_alumnos", new HashSet<>());

        for (String alumnoId : alumnosSet) {
            String nombre = alumnosPreferences.getString(alumnoId + "_nombre", "");
            String numControl = alumnosPreferences.getString(alumnoId + "_numControl", "");
            alumnos.add(nombre + " (" + numControl + ") - " + alumnoId);
        }

        if (alumnos.isEmpty()) {
            alumnos.add("No hay alumnos registrados");
        }

        return alumnos;
    }

    private void guardarProtocolo(String protocoloId, Spinner spinnerAlumno, EditText etNombreProyecto,
                                  EditText etEmpresa, EditText etFechaInicio, Spinner spinnerEstado,
                                  EditText etAvance, EditText etObservaciones) {

        SharedPreferences.Editor editor = sharedPreferences.edit();

        // Si es nuevo protocolo, generar ID
        if (protocoloId == null) {
            protocoloId = "protocolo_" + System.currentTimeMillis();

            // Agregar a la lista de protocolos
            Set<String> protocolosSet = new HashSet<>(sharedPreferences.getStringSet("lista_protocolos", new HashSet<>()));
            protocolosSet.add(protocoloId);
            editor.putStringSet("lista_protocolos", protocolosSet);
        }

        // Extraer alumnoId del spinner
        String alumnoSeleccionado = spinnerAlumno.getSelectedItem().toString();
        String alumnoId = "";
        if (alumnoSeleccionado.contains(" - ")) {
            alumnoId = alumnoSeleccionado.substring(alumnoSeleccionado.lastIndexOf(" - ") + 3);
        }

        // Guardar datos del protocolo
        editor.putString(protocoloId + "_alumnoId", alumnoId);
        editor.putString(protocoloId + "_nombreProyecto", etNombreProyecto.getText().toString().trim());
        editor.putString(protocoloId + "_empresa", etEmpresa.getText().toString().trim());
        editor.putString(protocoloId + "_fechaInicio", etFechaInicio.getText().toString().trim());
        editor.putString(protocoloId + "_estado", spinnerEstado.getSelectedItem().toString());
        editor.putString(protocoloId + "_avance", etAvance.getText().toString().trim());
        editor.putString(protocoloId + "_observaciones", etObservaciones.getText().toString().trim());

        editor.apply();
    }

    private void eliminarProtocolo(String protocoloId) {
        new AlertDialog.Builder(getContext())
                .setTitle("Eliminar Protocolo")
                .setMessage("¿Está seguro de que desea eliminar este protocolo?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    SharedPreferences.Editor editor = sharedPreferences.edit();

                    // Remover de la lista
                    Set<String> protocolosSet = new HashSet<>(sharedPreferences.getStringSet("lista_protocolos", new HashSet<>()));
                    protocolosSet.remove(protocoloId);
                    editor.putStringSet("lista_protocolos", protocolosSet);

                    // Eliminar todos los datos del protocolo
                    editor.remove(protocoloId + "_alumnoId");
                    editor.remove(protocoloId + "_nombreProyecto");
                    editor.remove(protocoloId + "_empresa");
                    editor.remove(protocoloId + "_fechaInicio");
                    editor.remove(protocoloId + "_estado");
                    editor.remove(protocoloId + "_avance");
                    editor.remove(protocoloId + "_observaciones");

                    editor.apply();
                    cargarProtocolos();

                    Toast.makeText(getContext(), "Protocolo eliminado", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}