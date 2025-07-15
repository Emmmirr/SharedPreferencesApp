package com.example.sharedpreferencesapp;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
        return cargarDatosDesdeMemoriaInterna(ALUMNOS_FILE);
    }

    public boolean guardarAlumnos(List<JSONObject> alumnos) {
        return guardarDatosEnMemoriaInterna(ALUMNOS_FILE, alumnos);
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
        return cargarDatosDesdeMemoriaInterna(PROTOCOLOS_FILE);
    }

    public boolean guardarProtocolos(List<JSONObject> protocolos) {
        return guardarDatosEnMemoriaInterna(PROTOCOLOS_FILE, protocolos);
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

    // ==================== MÉTODOS PRIVADOS DE MEMORIA INTERNA ====================

    private List<JSONObject> cargarDatosDesdeMemoriaInterna(String nombreArchivo) {
        List<JSONObject> datos = new ArrayList<>();
        FileInputStream fis = null;

        try {
            // Usar openFileInput para leer desde memoria interna
            fis = context.openFileInput(nombreArchivo);

            StringBuilder contenido = new StringBuilder();
            int caracter;
            while ((caracter = fis.read()) != -1) {
                contenido.append((char) caracter);
            }

            String jsonString = contenido.toString();
            if (!jsonString.isEmpty()) {
                JSONArray jsonArray = new JSONArray(jsonString);
                for (int i = 0; i < jsonArray.length(); i++) {
                    datos.add(jsonArray.getJSONObject(i));
                }
            }

            Log.d(TAG, "Cargados " + datos.size() + " elementos desde memoria interna: " + nombreArchivo);

        } catch (IOException e) {
            Log.d(TAG, "Archivo " + nombreArchivo + " no existe o error de lectura, retornando lista vacía");
        } catch (JSONException e) {
            Log.e(TAG, "Error de JSON cargando datos desde " + nombreArchivo, e);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error cerrando FileInputStream", e);
                }
            }
        }

        return datos;
    }

    // ⬅️ MÉTODO CORREGIDO - Solo captura IOException
    private boolean guardarDatosEnMemoriaInterna(String nombreArchivo, List<JSONObject> datos) {
        FileOutputStream fos = null;

        try {
            JSONArray jsonArray = new JSONArray();
            for (JSONObject dato : datos) {
                jsonArray.put(dato);
            }

            // Usar openFileOutput para escribir en memoria interna
            fos = context.openFileOutput(nombreArchivo, Context.MODE_PRIVATE);
            fos.write(jsonArray.toString().getBytes());

            Log.d(TAG, "Guardados " + datos.size() + " elementos en memoria interna: " + nombreArchivo);
            return true;

        } catch (IOException e) {
            Log.e(TAG, "Error de IO guardando datos en memoria interna: " + nombreArchivo, e);
            return false;
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error cerrando FileOutputStream", e);
                }
            }
        }
    }

    // ==================== MÉTODOS DE UTILIDAD ====================

    public boolean eliminarArchivo(String nombreArchivo) {
        try {
            return context.deleteFile(nombreArchivo);
        } catch (Exception e) {
            Log.e(TAG, "Error eliminando archivo: " + nombreArchivo, e);
            return false;
        }
    }

    public void limpiarTodosLosArchivos() {
        boolean alumnosEliminados = eliminarArchivo(ALUMNOS_FILE);
        boolean protocolosEliminados = eliminarArchivo(PROTOCOLOS_FILE);

        Log.d(TAG, "Limpieza de archivos - Alumnos: " + alumnosEliminados + ", Protocolos: " + protocolosEliminados);
    }

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

    public String obtenerRutaMemoriaInterna() {
        return context.getFilesDir().getAbsolutePath();
    }

    public void mostrarContenidoArchivos() {
        Log.d(TAG, "=== RUTA DE MEMORIA INTERNA ===");
        Log.d(TAG, "Ubicación: " + obtenerRutaMemoriaInterna());

        Log.d(TAG, "=== CONTENIDO DE ALUMNOS (MEMORIA INTERNA) ===");
        List<JSONObject> alumnos = cargarAlumnos();
        for (int i = 0; i < alumnos.size(); i++) {
            Log.d(TAG, "Alumno " + (i+1) + ": " + alumnos.get(i).toString());
        }

        Log.d(TAG, "=== CONTENIDO DE PROTOCOLOS (MEMORIA INTERNA) ===");
        List<JSONObject> protocolos = cargarProtocolos();
        for (int i = 0; i < protocolos.size(); i++) {
            Log.d(TAG, "Protocolo " + (i+1) + ": " + protocolos.get(i).toString());
        }

        Log.d(TAG, "=== ESTADÍSTICAS ===");
        Log.d(TAG, "Total alumnos: " + alumnos.size());
        Log.d(TAG, "Total protocolos: " + protocolos.size());
    }

    public String exportarAlumnosATexto() {
        List<JSONObject> alumnos = cargarAlumnos();
        if (alumnos.isEmpty()) {
            return "No hay alumnos registrados";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== REPORTE DE ALUMNOS ===\n");
        sb.append("Fecha: ").append(new java.util.Date().toString()).append("\n\n");

        for (int i = 0; i < alumnos.size(); i++) {
            JSONObject alumno = alumnos.get(i);
            sb.append("ALUMNO #").append(i + 1).append("\n");
            sb.append("Nombre: ").append(alumno.optString("nombre", "N/A")).append("\n");
            sb.append("No. Control: ").append(alumno.optString("numControl", "N/A")).append("\n");
            sb.append("Carrera: ").append(alumno.optString("carrera", "N/A")).append("\n");
            sb.append("Semestre: ").append(alumno.optString("semestre", "N/A")).append("\n");
            sb.append("Email: ").append(alumno.optString("email", "N/A")).append("\n");
            sb.append("Teléfono: ").append(alumno.optString("telefono", "N/A")).append("\n");
            sb.append("-----------------------------------\n");
        }

        return sb.toString();
    }

    public boolean guardarReporteAlumnos() {
        String reporte = exportarAlumnosATexto();
        FileOutputStream fos = null;

        try {
            String nombreReporte = "reporte_alumnos_" + System.currentTimeMillis() + ".txt";
            fos = context.openFileOutput(nombreReporte, Context.MODE_PRIVATE);
            fos.write(reporte.getBytes());

            Log.d(TAG, "Reporte guardado: " + nombreReporte);
            return true;

        } catch (IOException e) {
            Log.e(TAG, "Error guardando reporte", e);
            return false;
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error cerrando FileOutputStream del reporte", e);
                }
            }
        }
    }
    public String exportarProtocolosATexto() {
        List<JSONObject> protocolos = cargarProtocolos();
        if (protocolos.isEmpty()) {
            return "No hay protocolos registrados";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== REPORTE DE PROTOCOLOS ===\n");
        sb.append("Fecha: ").append(new java.util.Date().toString()).append("\n\n");

        for (int i = 0; i < protocolos.size(); i++) {
            JSONObject protocolo = protocolos.get(i);
            sb.append("PROTOCOLO #").append(i + 1).append("\n");
            sb.append("Proyecto: ").append(protocolo.optString("nombreProyecto", "N/A")).append("\n");
            sb.append("Empresa: ").append(protocolo.optString("nombreEmpresa", "N/A")).append("\n");
            sb.append("Banco: ").append(protocolo.optString("banco", "N/A")).append("\n");
            sb.append("Asesor: ").append(protocolo.optString("asesor", "N/A")).append("\n");
            sb.append("Ciudad: ").append(protocolo.optString("ciudad", "N/A")).append("\n");
            sb.append("-----------------------------------\n");
        }

        return sb.toString();
    }

    public boolean guardarReporteProtocolos() {
        String reporte = exportarProtocolosATexto();
        FileOutputStream fos = null;

        try {
            String nombreReporte = "reporte_protocolos_" + System.currentTimeMillis() + ".txt";
            fos = context.openFileOutput(nombreReporte, Context.MODE_PRIVATE);
            fos.write(reporte.getBytes());

            Log.d(TAG, "Reporte de protocolos guardado: " + nombreReporte);
            return true;

        } catch (IOException e) {
            Log.e(TAG, "Error guardando reporte de protocolos", e);
            return false;
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error cerrando FileOutputStream del reporte de protocolos", e);
                }
            }
        }
    }

    // ==================== MÉTODOS PARA CALENDARIOS ====================

    private static final String CALENDARIOS_FILE = "calendarios.json";

    public List<JSONObject> cargarCalendarios() {
        return cargarDatosDesdeMemoriaInterna(CALENDARIOS_FILE);
    }

    public boolean guardarCalendarios(List<JSONObject> calendarios) {
        return guardarDatosEnMemoriaInterna(CALENDARIOS_FILE, calendarios);
    }

    public boolean guardarCalendario(JSONObject calendario) {
        List<JSONObject> calendarios = cargarCalendarios();

        // Buscar si ya existe un calendario para este alumno
        String alumnoId = calendario.optString("alumnoId", "");
        for (int i = 0; i < calendarios.size(); i++) {
            if (calendarios.get(i).optString("alumnoId", "").equals(alumnoId)) {
                calendarios.set(i, calendario); // Actualizar existente
                return guardarCalendarios(calendarios);
            }
        }

        // Si no existe, agregar nuevo
        calendarios.add(calendario);
        return guardarCalendarios(calendarios);
    }

    public JSONObject buscarCalendarioPorAlumnoId(String alumnoId) {
        List<JSONObject> calendarios = cargarCalendarios();
        for (JSONObject calendario : calendarios) {
            if (calendario.optString("alumnoId", "").equals(alumnoId)) {
                return calendario;
            }
        }
        return null;
    }

    public boolean eliminarCalendario(String alumnoId) {
        List<JSONObject> calendarios = cargarCalendarios();
        for (int i = 0; i < calendarios.size(); i++) {
            if (calendarios.get(i).optString("alumnoId", "").equals(alumnoId)) {
                calendarios.remove(i);
                return guardarCalendarios(calendarios);
            }
        }
        return false;
    }

    public int contarCalendarios() {
        return cargarCalendarios().size();
    }
    public JSONObject buscarCalendarioPorId(String calendarioId) {
        List<JSONObject> calendarios = cargarCalendarios();

        for (JSONObject calendario : calendarios) {
            try {
                if (calendario.getString("id").equals(calendarioId)) {
                    return calendario;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return null; // No encontrado
    }

    private boolean guardarListaCompleta(String nombreArchivo, List<JSONObject> lista) {
        JSONArray jsonArray = new JSONArray(lista);
        try (OutputStreamWriter writer = new OutputStreamWriter(context.openFileOutput(nombreArchivo, Context.MODE_PRIVATE))) {
            writer.write(jsonArray.toString());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean eliminarRegistroLocalCalendario(String calendarioId) {
        List<JSONObject> calendarios = cargarCalendarios();
        // removeIf devuelve true si la lista fue modificada
        boolean modificado = calendarios.removeIf(cal -> Objects.equals(cal.optString("id"), calendarioId));

        if (modificado) {
            // Guardar la lista actualizada (sin el elemento eliminado)
            return guardarListaCompleta(CALENDARIOS_FILE, calendarios);
        }
        // No se encontró nada que borrar o hubo un error
        return false;
    }
}