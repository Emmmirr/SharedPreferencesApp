package com.example.sharedpreferencesapp;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions; // <-- AÑADIDO: Import necesario
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Map;
import java.util.function.Consumer;

public class FirebaseManager {

    private static final String TAG = "FirebaseManager";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseStorage storage = FirebaseStorage.getInstance();

    // Nombres de las colecciones
    private static final String COLLECTION_ALUMNOS = "alumnos";
    private static final String COLLECTION_PROTOCOLOS = "protocolos";
    private static final String COLLECTION_CALENDARIOS = "calendarios";


    // --- Métodos para Alumnos (Sin cambios) ---

    public CollectionReference getAlumnosCollection() {
        return db.collection(COLLECTION_ALUMNOS);
    }

    public void cargarAlumnos(Consumer<Task<QuerySnapshot>> onCompleteListener) {
        getAlumnosCollection().get().addOnCompleteListener(onCompleteListener::accept);
    }

    public void buscarAlumnoPorId(String alumnoId, Consumer<Task<DocumentSnapshot>> onCompleteListener) {
        getAlumnosCollection().document(alumnoId).get().addOnCompleteListener(onCompleteListener::accept);
    }

    public void guardarAlumno(String alumnoId, Map<String, Object> alumnoData, Runnable onSuccess, Consumer<Exception> onFailure) {
        DocumentReference docRef;
        if (alumnoId == null || alumnoId.isEmpty()) {
            docRef = getAlumnosCollection().document(); // Firestore genera un ID único
        } else {
            docRef = getAlumnosCollection().document(alumnoId);
        }
        alumnoData.put("id", docRef.getId());
        docRef.set(alumnoData)
                .addOnSuccessListener(aVoid -> onSuccess.run())
                .addOnFailureListener(onFailure::accept);
    }

    public void eliminarAlumno(String alumnoId, Runnable onSuccess, Consumer<Exception> onFailure) {
        getAlumnosCollection().document(alumnoId).delete()
                .addOnSuccessListener(aVoid -> onSuccess.run())
                .addOnFailureListener(onFailure::accept);
    }


    // --- Métodos para Protocolos (Sin cambios) ---

    public CollectionReference getProtocolosCollection() {
        return db.collection(COLLECTION_PROTOCOLOS);
    }

    public void cargarProtocolos(Consumer<Task<QuerySnapshot>> onCompleteListener) {
        getProtocolosCollection().get().addOnCompleteListener(onCompleteListener::accept);
    }

    public void buscarProtocoloPorId(String protocoloId, Consumer<Task<DocumentSnapshot>> onCompleteListener) {
        getProtocolosCollection().document(protocoloId).get().addOnCompleteListener(onCompleteListener::accept);
    }

    public void guardarProtocolo(String protocoloId, Map<String, Object> protocoloData, Runnable onSuccess, Consumer<Exception> onFailure) {
        DocumentReference docRef;
        if (protocoloId == null || protocoloId.isEmpty()) {
            docRef = getProtocolosCollection().document(); // Firestore genera un ID
        } else {
            docRef = getProtocolosCollection().document(protocoloId);
        }
        protocoloData.put("id", docRef.getId());
        docRef.set(protocoloData)
                .addOnSuccessListener(aVoid -> onSuccess.run())
                .addOnFailureListener(onFailure::accept);
    }

    public void eliminarProtocolo(String protocoloId, Runnable onSuccess, Consumer<Exception> onFailure) {
        getProtocolosCollection().document(protocoloId).delete()
                .addOnSuccessListener(aVoid -> onSuccess.run())
                .addOnFailureListener(onFailure::accept);
    }


    // --- Métodos para Calendarios ---

    public void cargarCalendarios(Consumer<Task<QuerySnapshot>> onCompleteListener) {
        db.collection(COLLECTION_CALENDARIOS).get().addOnCompleteListener(onCompleteListener::accept);
    }

    public void buscarCalendarioPorId(String calendarioId, Consumer<Task<DocumentSnapshot>> onCompleteListener) {
        db.collection(COLLECTION_CALENDARIOS).document(calendarioId).get().addOnCompleteListener(onCompleteListener::accept);
    }

    public void guardarCalendarioCompleto(String calendarioId, Map<String, Object> calendarioData, Runnable onSuccess) {
        db.collection(COLLECTION_CALENDARIOS).document(calendarioId)
                .set(calendarioData)
                .addOnSuccessListener(aVoid -> onSuccess.run())
                .addOnFailureListener(e -> Log.w(TAG, "Error guardando calendario", e));
    }

    public void actualizarCalendario(String calendarioId, Map<String, Object> updates, Runnable onSuccess) {
        db.collection(COLLECTION_CALENDARIOS).document(calendarioId)
                .update(updates)
                .addOnSuccessListener(aVoid -> onSuccess.run())
                .addOnFailureListener(e -> Log.w(TAG, "Error actualizando calendario", e));
    }

    // AÑADIDO: Método para guardar o actualizar campos de un calendario.
    // Utiliza SetOptions.merge() para no borrar los campos existentes.
    public void guardarOActualizarCalendario(String calendarioId, Map<String, Object> calendarioData, Runnable onSuccess, Consumer<Exception> onFailure) {
        db.collection(COLLECTION_CALENDARIOS).document(calendarioId)
                .set(calendarioData, SetOptions.merge()) // La clave es "merge"
                .addOnSuccessListener(aVoid -> onSuccess.run())
                .addOnFailureListener(onFailure::accept);
    }

    // AÑADIDO: Método para eliminar un calendario con manejo de errores.
    public void eliminarCalendario(String calendarioId, Runnable onSuccess, Consumer<Exception> onFailure) {
        db.collection(COLLECTION_CALENDARIOS).document(calendarioId).delete()
                .addOnSuccessListener(aVoid -> onSuccess.run())
                .addOnFailureListener(onFailure::accept);
    }


    // --- Métodos de Storage (Sin cambios) ---

    public void subirPdfYObtenerUrl(Uri fileUri, String rutaStorage, Consumer<String> onResult, Consumer<Exception> onFailure) {
        StorageReference fileRef = storage.getReference().child(rutaStorage);
        fileRef.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    onResult.accept(uri.toString());
                }))
                .addOnFailureListener(onFailure::accept);
    }
}