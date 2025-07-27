package com.example.sharedpreferencesapp;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AlarmScheduler {

    private static final String TAG = "AlarmScheduler";
    private final Context context;
    private final AlarmManager alarmManager;

    private static final int TYPE_DAY_BEFORE = 0;
    private static final int TYPE_DAY_OF = 1;
    private static final int TYPE_DAY_AFTER = 2;
    private static final int TYPE_SHORT_INTERVAL_CYCLE = 7;

    public AlarmScheduler(Context context) {
        this.context = context.getApplicationContext();
        this.alarmManager = (AlarmManager) this.context.getSystemService(Context.ALARM_SERVICE);
    }

    public void scheduleAlarmsForDate(String userId, String calendarioId, String fechaString, String label, String pdfFieldToCheck, int dateIndex) {
        if (fechaString == null || fechaString.isEmpty()) {
            return;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        try {
            Calendar fechaEntrega = Calendar.getInstance();
            fechaEntrega.setTime(sdf.parse(fechaString));

            Calendar fechaUnDiaAntes = (Calendar) fechaEntrega.clone();
            fechaUnDiaAntes.add(Calendar.DAY_OF_YEAR, -1);
            fechaUnDiaAntes.set(Calendar.HOUR_OF_DAY, 9);
            fechaUnDiaAntes.set(Calendar.MINUTE, 0);
            scheduleSingleAlarm(userId, calendarioId, label, pdfFieldToCheck, fechaUnDiaAntes.getTimeInMillis(), "day_before", dateIndex, TYPE_DAY_BEFORE);

            Calendar fechaMismoDia = (Calendar) fechaEntrega.clone();
            fechaMismoDia.set(Calendar.HOUR_OF_DAY, 11);
            fechaMismoDia.set(Calendar.MINUTE, 54);
            scheduleSingleAlarm(userId, calendarioId, label, pdfFieldToCheck, fechaMismoDia.getTimeInMillis(), "day_of", dateIndex, TYPE_DAY_OF);

            Calendar fechaUnDiaDespues = (Calendar) fechaEntrega.clone();
            fechaUnDiaDespues.add(Calendar.DAY_OF_YEAR, 1);
            fechaUnDiaDespues.set(Calendar.HOUR_OF_DAY, 9);
            fechaUnDiaDespues.set(Calendar.MINUTE, 0);
            scheduleSingleAlarm(userId, calendarioId, label, pdfFieldToCheck, fechaUnDiaDespues.getTimeInMillis(), "day_after", dateIndex, TYPE_DAY_AFTER);

        } catch (ParseException e) {
            Log.e(TAG, "Error al parsear fecha para la alarma: " + fechaString, e);
        }
    }

    public void scheduleShortIntervalReminder(String userId, String calendarioId, String label, String pdfFieldToCheck, int dateIndex) {
        long intervalMillis = 10 * 1000;
        long triggerTime = System.currentTimeMillis() + intervalMillis;

        Log.d(TAG, "Programando recordatorio persistente de corto plazo para dentro de 2 horas.");
        scheduleSingleAlarm(userId, calendarioId, label, pdfFieldToCheck, triggerTime, "day_of_repeating", dateIndex, TYPE_SHORT_INTERVAL_CYCLE);
    }

    public void stopShortIntervalCycle(String calendarioId, int dateIndex) {
        Log.d(TAG, "DETENIENDO CICLO de alarma de corto plazo para índice: " + dateIndex);
        int requestCode = (calendarioId.hashCode() * 100) + (dateIndex * 10) + TYPE_SHORT_INTERVAL_CYCLE;
        cancelIntent(requestCode);
    }

    public void cancelAlarmsForDate(String calendarioId, int dateIndex) {
        Log.d(TAG, "Cancelando todas las alarmas para el índice: " + dateIndex);
        cancelSingleAlarm(calendarioId, dateIndex, TYPE_DAY_BEFORE);
        cancelSingleAlarm(calendarioId, dateIndex, TYPE_DAY_OF);
        cancelSingleAlarm(calendarioId, dateIndex, TYPE_DAY_AFTER);
        stopShortIntervalCycle(calendarioId, dateIndex);
    }

    private void scheduleSingleAlarm(String userId, String calendarioId, String label, String pdfFieldToCheck, long triggerAtMillis, String alarmType, int dateIndex, int alarmTypeIndex) {
        int requestCode = (calendarioId.hashCode() * 100) + (dateIndex * 10) + alarmTypeIndex;
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra("USER_ID", userId);
        intent.putExtra("CALENDARIO_ID", calendarioId);
        intent.putExtra("LABEL", label);
        intent.putExtra("PDF_FIELD", pdfFieldToCheck);
        intent.putExtra("ALARM_TYPE", alarmType);
        intent.putExtra("DATE_INDEX", dateIndex);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0));
        if (alarmManager != null) {
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
                Log.i(TAG, "Alarma programada con requestCode: " + requestCode + " para " + new java.util.Date(triggerAtMillis));
            } catch (SecurityException se) {
                Log.e(TAG, "Permiso SCHEDULE_EXACT_ALARM no concedido.");
            }
        }
    }

    private void cancelSingleAlarm(String calendarioId, int dateIndex, int alarmTypeIndex) {
        int requestCode = (calendarioId.hashCode() * 100) + (dateIndex * 10) + alarmTypeIndex;
        cancelIntent(requestCode);
    }

    private void cancelIntent(int requestCode) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_NO_CREATE | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0));
        if (alarmManager != null && pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
    }
}