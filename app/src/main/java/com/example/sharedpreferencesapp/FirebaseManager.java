package com.example.sharedpreferencesapp;

import android.net.Uri;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class FirebaseManager {

    private static final String TAG = "FirebaseManager";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseStorage storage = FirebaseStorage.getInstance();

    private static final String COLLECTION_CALENDARIOS = "calendarios";

    /**
     * Obtiene todos los calendarios de Firestore.
     * @param onCompleteListener Callback que se ejecuta con la lista de documentos.
     */
    public void cargarCalendarios(Consumer<Task<QuerySnapshot>> onCompleteListener) {
        db.collection(COLLECTION_CALENDARIOS).get().addOnCompleteListener(onCompleteListener::accept);
    }

    /**
     * Obtiene un solo calendario por su ID.
     * @param calendarioId El ID del documento del calendario.
     * @param onCompleteListener Callback que se ejecuta con el documento.
     */
    public void buscarCalendarioPorId(String calendarioId, Consumer<Task<DocumentSnapshot>> onCompleteListener) {
        db.collection(COLLECTION_CALENDARIOS).document(calendarioId).get().addOnCompleteListener(onCompleteListener::accept);
    }

    /**
     * Guarda (crea o sobreescribe) un calendario completo en Firestore.
     * @param calendarioId El ID del documento a guardar.
     * @param calendarioData Los datos del calendario en un mapa.
     * @param onSuccess Callback en caso de éxito.
     */
    public void guardarCalendarioCompleto(String calendarioId, Map<String, Object> calendarioData, Runnable onSuccess) {
        db.collection(COLLECTION_CALENDARIOS).document(calendarioId)
                .set(calendarioData)
                .addOnSuccessListener(aVoid -> onSuccess.run())
                .addOnFailureListener(e -> Log.w(TAG, "Error guardando calendario", e));
    }

    /**
     * Actualiza campos específicos de un calendario sin sobreescribir el documento entero.
     * @param calendarioId El ID del documento a actualizar.
     * @param updates Los campos a actualizar en un mapa.
     * @param onSuccess Callback en caso de éxito.
     */
    public void actualizarCalendario(String calendarioId, Map<String, Object> updates, Runnable onSuccess) {
        db.collection(COLLECTION_CALENDARIOS).document(calendarioId)
                .update(updates)
                .addOnSuccessListener(aVoid -> onSuccess.run())
                .addOnFailureListener(e -> Log.w(TAG, "Error actualizando calendario", e));
    }

    /**
     * Sube un archivo PDF a Cloud Storage y devuelve la URL de descarga.
     * @param fileUri El URI local del archivo a subir.
     * @param rutaStorage La ruta donde se guardará en Cloud Storage.
     * @param onResult Callback que se ejecuta con la URL de descarga del archivo.
     * @param onFailure Callback que se ejecuta si hay un error.
     */
    public void subirPdfYObtenerUrl(Uri fileUri, String rutaStorage, Consumer<String> onResult, Consumer<Exception> onFailure) {
        StorageReference fileRef = storage.getReference().child(rutaStorage);
        fileRef.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    onResult.accept(uri.toString());
                }))
                .addOnFailureListener(onFailure::accept);
    }
}