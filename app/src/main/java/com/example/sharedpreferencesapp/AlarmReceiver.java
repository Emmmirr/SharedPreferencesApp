package com.example.sharedpreferencesapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import com.google.firebase.firestore.DocumentSnapshot;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "AlarmReceiver";
    public static final String CHANNEL_ID = "ALARM_REMINDER_CHANNEL_V2";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Alarma recibida! Tipo: " + intent.getStringExtra("ALARM_TYPE"));

        String userId = intent.getStringExtra("USER_ID");
        String calendarioId = intent.getStringExtra("CALENDARIO_ID");
        String label = intent.getStringExtra("LABEL");
        String pdfField = intent.getStringExtra("PDF_FIELD");
        String alarmType = intent.getStringExtra("ALARM_TYPE");
        int dateIndex = intent.getIntExtra("DATE_INDEX", -1);

        if (calendarioId == null || userId == null || label == null || pdfField == null || alarmType == null || dateIndex == -1) {
            Log.e(TAG, "Datos insuficientes en el intent de la alarma.");
            return;
        }

        final PendingResult pendingResult = goAsync();
        FirebaseManager firebaseManager = new FirebaseManager();
        AlarmScheduler scheduler = new AlarmScheduler(context);

        firebaseManager.buscarCalendarioPorId(userId, calendarioId, task -> {
            try {
                if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                    DocumentSnapshot calendario = task.getResult();
                    boolean docSubido = calendario.getString(pdfField) != null && !calendario.getString(pdfField).isEmpty();

                    if (docSubido) {
                        scheduler.stopShortIntervalCycle(calendarioId, dateIndex);
                        return;
                    }

                    String titulo = "Recordatorio";
                    String contenido = "";
                    boolean debeNotificar = false;

                    switch (alarmType) {
                        case "day_before":
                            contenido = "Mañana es la fecha límite para: " + label;
                            debeNotificar = true;
                            break;
                        case "day_of":
                        case "day_of_repeating":
                            titulo = "¡Recordatorio Urgente!";
                            contenido = "Hoy es la fecha límite para entregar: " + label;
                            scheduler.scheduleShortIntervalReminder(userId, calendarioId, label, pdfField, dateIndex);
                            debeNotificar = true;
                            break;
                        case "day_after":
                            scheduler.stopShortIntervalCycle(calendarioId, dateIndex);
                            titulo = "¡Fecha Vencida!";
                            contenido = "No se ha subido el documento para: " + label;
                            debeNotificar = true;
                            break;
                    }

                    if (debeNotificar) {
                        int notificationId = (calendarioId.hashCode() * 100) + (dateIndex * 10);
                        mostrarNotificacion(context, titulo, contenido, notificationId);
                    }
                }
            } finally {
                pendingResult.finish();
            }
        });
    }

    private void mostrarNotificacion(Context context, String title, String content, int notificationId) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Uri soundUri = Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.custom_sound);

        // --- INICIO DE LA LÓGICA DE NAVEGACIÓN ---
        // 1. Crear un Intent para abrir la MainActivity, que es la entrada a la app.
        Intent resultIntent = new Intent(context, MainActivity.class);

        // 2. Añadir un "extra" para indicar que queremos ir a la segunda pestaña (índice 1).
        resultIntent.putExtra("NAVIGATE_TO_TAB", 1);

        // 3. Flags para un comportamiento de navegación limpio (abre una nueva tarea o limpia la existente).
        resultIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        // 4. Crear el PendingIntent que envuelve el Intent y se ejecutará al tocar la notificación.
        PendingIntent resultPendingIntent = PendingIntent.getActivity(
                context,
                notificationId, // Reutilizamos el ID de la notificación para que el PendingIntent sea único.
                resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        // --- FIN DE LA LÓGICA DE NAVEGACIÓN ---


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Recordatorios de Calendario";
            String description = "Canal para notificaciones de fechas de entrega";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            AudioAttributes audioAttributes = new AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).setUsage(AudioAttributes.USAGE_NOTIFICATION).build();
            channel.setSound(soundUri, audioAttributes);
            notificationManager.createNotificationChannel(channel);
        }

        Bitmap largeImage = BitmapFactory.decodeResource(context.getResources(), R.drawable.notification_image);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setStyle(new NotificationCompat.BigPictureStyle().bigPicture(largeImage).bigLargeIcon((Bitmap) null))
                // Asignar el PendingIntent a la notificación. Ahora es "tocable".
                .setContentIntent(resultPendingIntent)
                .setSound(soundUri);

        notificationManager.notify(notificationId, builder.build());
    }
}