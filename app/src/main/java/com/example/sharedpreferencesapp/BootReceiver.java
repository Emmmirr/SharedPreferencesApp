package com.example.sharedpreferencesapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        // Nos aseguramos de que la acción que activó el receptor sea la de arranque completado.
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, "Dispositivo reiniciado. Intentando reprogramar alarmas...");

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                // Si hay un usuario, procedemos a reprogramar sus alarmas.
                reprogramarTodasLasAlarmas(context, user.getUid());
            } else {
                Log.w(TAG, "No hay usuario logueado. No se pueden reprogramar alarmas.");
            }
        }
    }

    private void reprogramarTodasLasAlarmas(Context context, String userId) {
        FirebaseManager firebaseManager = new FirebaseManager();
        AlarmScheduler scheduler = new AlarmScheduler(context);

        // Obtenemos todos los documentos de la colección de calendarios del usuario.
        firebaseManager.cargarCalendarios(userId, task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                task.getResult().forEach(calendarioDoc -> {
                    String calendarioId = calendarioDoc.getId();
                    Log.d(TAG, "Reprogramando para calendario: " + calendarioId);

                    // Definimos los campos de Firebase que contienen las fechas, etiquetas y nombres de archivo.
                    String[] fechas = {
                            calendarioDoc.getString("fechaPrimeraEntrega"),
                            calendarioDoc.getString("fechaSegundaEntrega"),
                            calendarioDoc.getString("fechaResultado")
                    };
                    String[] labels = {
                            calendarioDoc.getString("labelPrimeraEntrega"),
                            calendarioDoc.getString("labelSegundaEntrega"),
                            calendarioDoc.getString("labelResultado")
                    };
                    String[] pdfFields = {
                            "pdfUriPrimeraEntrega",
                            "pdfUriSegundaEntrega",
                            "pdfUriResultado"
                    };
                    String[] defaultLabels = {"1ª Entrega", "2ª Entrega", "Resultado"};

                    // Iteramos sobre las tres posibles fechas de cada calendario.
                    for (int i = 0; i < fechas.length; i++) {
                        String fecha = fechas[i];
                        // Usamos la etiqueta guardada, o una por defecto si no existe.
                        String label = labels[i] != null && !labels[i].isEmpty() ? labels[i] : defaultLabels[i];
                        String pdfField = pdfFields[i];

                        // Solo programamos la alarma si la fecha existe y no está vacía.
                        if (fecha != null && !fecha.isEmpty()) {
                            // Llamamos al scheduler pasando el índice 'i' para que genere los
                            // mismos requestCodes únicos que se generaron originalmente.
                            scheduler.scheduleAlarmsForDate(userId, calendarioId, fecha, label, pdfField, i);
                        }
                    }
                });
            } else {
                Log.e(TAG, "Error al cargar calendarios en el reinicio.", task.getException());
            }
        });
    }
}