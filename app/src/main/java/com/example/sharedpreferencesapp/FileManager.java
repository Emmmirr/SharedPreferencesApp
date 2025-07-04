package com.example.sharedpreferencesapp;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class FileManager {
    private static final String TAG = "FileManager";
    private static final String ALUMNOS_FILE = "alumnos.json";
    private static final String PROTOCOLOS_FILE = "protocolos.json";

    private Context context;

    public FileManager(Context context) {
        this.context = context;
    }

    // ==================== MÉTODOS PARA ALUMNOS ====================

    public List<JSONObject> cargarAlumnos() {
        return cargarDatosDesdeArchivo(ALUMNOS_FILE);
    }

    public boolean guardarAlumnos(List<JSONObject> alumnos) {
        return guardarDatosEnArchivo(ALUMNOS_FILE, alumnos);
    }

    public boolean agregarAlumno(JSONObject alumno) {
        List<JSONObject> alumnos = cargarAlumnos();
        alumnos.add(alumno);
        return guardarAlumnos(alumnos);
    }

    public boolean actualizarAlumno(String alumnoId, JSONObject alumnoActualizado) {
        List<JSONObject> alumnos = cargarAlumnos();
        for (int i = 0; i < alumnos.size(); i++) {
            try {
                if (alumnos.get(i).getString("id").equals(alumnoId)) {
                    alumnos.set(i, alumnoActualizado);
                    return guardarAlumnos(alumnos);
                }
            } catch (JSONException e) {
                Log.e(TAG, "Error actualizando alumno", e);
            }
        }
        return false;
    }

    public boolean eliminarAlumno(String alumnoId) {
        List<JSONObject> alumnos = cargarAlumnos();
        for (int i = 0; i < alumnos.size(); i++) {
            try {
                if (alumnos.get(i).getString("id").equals(alumnoId)) {
                    alumnos.remove(i);
                    return guardarAlumnos(alumnos);
                }
            } catch (JSONException e) {
                Log.e(TAG, "Error eliminando alumno", e);
            }
        }
        return false;
    }

    public JSONObject buscarAlumnoPorId(String alumnoId) {
        List<JSONObject> alumnos = cargarAlumnos();
        for (JSONObject alumno : alumnos) {
            try {
                if (alumno.getString("id").equals(alumnoId)) {
                    return alumno;
                }
            } catch (JSONException e) {
                Log.e(TAG, "Error buscando alumno", e);
            }
        }
        return null;
    }

    // ==================== MÉTODOS PARA PROTOCOLOS ====================

    public List<JSONObject> cargarProtocolos() {
        return cargarDatosDesdeArchivo(PROTOCOLOS_FILE);
    }

    public boolean guardarProtocolos(List<JSONObject> protocolos) {
        return guardarDatosEnArchivo(PROTOCOLOS_FILE, protocolos);
    }

    public boolean agregarProtocolo(JSONObject protocolo) {
        List<JSONObject> protocolos = cargarProtocolos();
        protocolos.add(protocolo);
        return guardarProtocolos(protocolos);
    }

    public boolean actualizarProtocolo(String protocoloId, JSONObject protocoloActualizado) {
        List<JSONObject> protocolos = cargarProtocolos();
        for (int i = 0; i < protocolos.size(); i++) {
            try {
                if (protocolos.get(i).getString("id").equals(protocoloId)) {
                    protocolos.set(i, protocoloActualizado);
                    return guardarProtocolos(protocolos);
                }
            } catch (JSONException e) {
                Log.e(TAG, "Error actualizando protocolo", e);
            }
        }
        return false;
    }

    public boolean eliminarProtocolo(String protocoloId) {
        List<JSONObject> protocolos = cargarProtocolos();
        for (int i = 0; i < protocolos.size(); i++) {
            try {
                if (protocolos.get(i).getString("id").equals(protocoloId)) {
                    protocolos.remove(i);
                    return guardarProtocolos(protocolos);
                }
            } catch (JSONException e) {
                Log.e(TAG, "Error eliminando protocolo", e);
            }
        }
        return false;
    }

    public JSONObject buscarProtocoloPorId(String protocoloId) {
        List<JSONObject> protocolos = cargarProtocolos();
        for (JSONObject protocolo : protocolos) {
            try {
                if (protocolo.getString("id").equals(protocoloId)) {
                    return protocolo;
                }
            } catch (JSONException e) {
                Log.e(TAG, "Error buscando protocolo", e);
            }
        }
        return null;
    }

    // ==================== MÉTODOS PRIVADOS GENERALES ====================

    private List<JSONObject> cargarDatosDesdeArchivo(String nombreArchivo) {
        List<JSONObject> datos = new ArrayList<>();

        try {
            File file = new File(context.getFilesDir(), nombreArchivo);
            if (!file.exists()) {
                Log.d(TAG, "Archivo " + nombreArchivo + " no existe, retornando lista vacía");
                return datos;
            }

            FileInputStream fis = new FileInputStream(file);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader bufferedReader = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();
            String line;

            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }

            bufferedReader.close();

            String jsonString = sb.toString();
            if (!jsonString.isEmpty()) {
                JSONArray jsonArray = new JSONArray(jsonString);
                for (int i = 0; i < jsonArray.length(); i++) {
                    datos.add(jsonArray.getJSONObject(i));
                }
            }

            Log.d(TAG, "Cargados " + datos.size() + " elementos desde " + nombreArchivo);

        } catch (IOException e) {
            Log.e(TAG, "Error de IO cargando datos desde " + nombreArchivo, e);
        } catch (JSONException e) {
            Log.e(TAG, "Error de JSON cargando datos desde " + nombreArchivo, e);
        }

        return datos;
    }

    private boolean guardarDatosEnArchivo(String nombreArchivo, List<JSONObject> datos) {
        try {
            JSONArray jsonArray = new JSONArray();
            for (JSONObject dato : datos) {
                jsonArray.put(dato);
            }

            File file = new File(context.getFilesDir(), nombreArchivo);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(jsonArray.toString().getBytes());
            fos.close();

            Log.d(TAG, "Guardados " + datos.size() + " elementos en " + nombreArchivo);
            return true;

        } catch (IOException e) {
            Log.e(TAG, "Error de IO guardando datos en " + nombreArchivo, e);
            return false;
        }
    }

    // ==================== MÉTODO DE UTILIDAD ====================

    public void limpiarTodosLosArchivos() {
        try {
            File alumnosFile = new File(context.getFilesDir(), ALUMNOS_FILE);
            File protocolosFile = new File(context.getFilesDir(), PROTOCOLOS_FILE);

            if (alumnosFile.exists()) alumnosFile.delete();
            if (protocolosFile.exists()) protocolosFile.delete();

            Log.d(TAG, "Archivos limpiados correctamente");
        } catch (Exception e) {
            Log.e(TAG, "Error limpiando archivos", e);
        }
    }

    // ==================== MÉTODOS DE VALIDACIÓN ====================

    public boolean validarIntegridadDatos() {
        try {
            // Validar alumnos
            List<JSONObject> alumnos = cargarAlumnos();
            for (JSONObject alumno : alumnos) {
                if (!alumno.has("id") || !alumno.has("nombre")) {
                    Log.e(TAG, "Alumno con datos incompletos encontrado");
                    return false;
                }
            }

            // Validar protocolos
            List<JSONObject> protocolos = cargarProtocolos();
            for (JSONObject protocolo : protocolos) {
                if (!protocolo.has("id") || !protocolo.has("nombreProyecto")) {
                    Log.e(TAG, "Protocolo con datos incompletos encontrado");
                    return false;
                }
            }

            Log.d(TAG, "Validación de integridad exitosa");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Error en validación de integridad", e);
            return false;
        }
    }

    public int contarAlumnos() {
        return cargarAlumnos().size();
    }

    public int contarProtocolos() {
        return cargarProtocolos().size();
    }
}