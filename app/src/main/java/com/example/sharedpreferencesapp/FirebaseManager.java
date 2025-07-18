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
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class FirebaseManager {

    private static final String TAG = "FirebaseManager";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseStorage storage = FirebaseStorage.getInstance();

    // Colección principal para los perfiles de usuario
    private static final String COLLECTION_USER_PROFILES = "user_profiles";

    // Nombres de las SUB-COLECCIONES que existirán dentro de cada documento de usuario
    private static final String SUBCOLLECTION_ALUMNOS = "alumnos";
    private static final String SUBCOLLECTION_PROTOCOLOS = "protocolos";
    private static final String SUBCOLLECTION_CALENDARIOS = "calendarios";


    // --- MÉTODOS PARA PERFILES DE USUARIO ---

    // CORREGIDO: Se cambia Consumer<Task<...>> por OnCompleteListener<...>
    public void buscarPerfilUsuarioPorId(String userId, OnCompleteListener<DocumentSnapshot> onCompleteListener) {
        db.collection(COLLECTION_USER_PROFILES).document(userId).get().addOnCompleteListener(onCompleteListener);
    }

    public void guardarPerfilUsuario(String userId, Map<String, Object> perfilData, Runnable onSuccess, Consumer<Exception> onFailure) {
        perfilData.put("updatedAt", String.valueOf(System.currentTimeMillis()));
        db.collection(COLLECTION_USER_PROFILES)
                .document(userId)
                .set(perfilData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> onSuccess.run())
                .addOnFailureListener(onFailure::accept);
    }

    public void crearPerfilUsuario(UserProfile profile, Runnable onSuccess, Consumer<Exception> onFailure) {
        Map<String, Object> perfilData = profile.toMap();
        perfilData.put("createdAt", String.valueOf(System.currentTimeMillis()));
        perfilData.put("updatedAt", String.valueOf(System.currentTimeMillis()));
        db.collection(COLLECTION_USER_PROFILES)
                .document(profile.getUserId())
                .set(perfilData)
                .addOnSuccessListener(aVoid -> onSuccess.run())
                .addOnFailureListener(onFailure::accept);
    }


    // --- MÉTODOS PARA ALUMNOS (adaptados a sub-colecciones) ---

    // CORREGIDO: Se cambia Consumer<Task<...>> por OnCompleteListener<...>
    public void cargarAlumnos(String userId, OnCompleteListener<QuerySnapshot> onCompleteListener) {
        db.collection(COLLECTION_USER_PROFILES).document(userId).collection(SUBCOLLECTION_ALUMNOS).get().addOnCompleteListener(onCompleteListener);
    }

    // CORREGIDO: Se cambia Consumer<Task<...>> por OnCompleteListener<...>
    public void buscarAlumnoPorId(String userId, String alumnoId, OnCompleteListener<DocumentSnapshot> onCompleteListener) {
        db.collection(COLLECTION_USER_PROFILES).document(userId).collection(SUBCOLLECTION_ALUMNOS).document(alumnoId).get().addOnCompleteListener(onCompleteListener);
    }

    public void guardarAlumno(String userId, String alumnoId, Map<String, Object> alumnoData, Runnable onSuccess, Consumer<Exception> onFailure) {
        DocumentReference docRef;
        if (alumnoId == null || alumnoId.isEmpty()) {
            docRef = db.collection(COLLECTION_USER_PROFILES).document(userId).collection(SUBCOLLECTION_ALUMNOS).document();
        } else {
            docRef = db.collection(COLLECTION_USER_PROFILES).document(userId).collection(SUBCOLLECTION_ALUMNOS).document(alumnoId);
        }
        alumnoData.put("id", docRef.getId());
        docRef.set(alumnoData)
                .addOnSuccessListener(aVoid -> onSuccess.run())
                .addOnFailureListener(onFailure::accept);
    }

    public void eliminarAlumno(String userId, String alumnoId, Runnable onSuccess, Consumer<Exception> onFailure) {
        db.collection(COLLECTION_USER_PROFILES).document(userId).collection(SUBCOLLECTION_ALUMNOS).document(alumnoId).delete()
                .addOnSuccessListener(aVoid -> onSuccess.run())
                .addOnFailureListener(onFailure::accept);
    }


    // --- MÉTODOS PARA PROTOCOLOS (adaptados a sub-colecciones) ---

    // CORREGIDO: Se cambia Consumer<Task<...>> por OnCompleteListener<...>
    public void cargarProtocolos(String userId, OnCompleteListener<QuerySnapshot> onCompleteListener) {
        db.collection(COLLECTION_USER_PROFILES).document(userId).collection(SUBCOLLECTION_PROTOCOLOS).get().addOnCompleteListener(onCompleteListener);
    }

    // CORREGIDO: Se cambia Consumer<Task<...>> por OnCompleteListener<...>
    public void buscarProtocoloPorId(String userId, String protocoloId, OnCompleteListener<DocumentSnapshot> onCompleteListener) {
        db.collection(COLLECTION_USER_PROFILES).document(userId).collection(SUBCOLLECTION_PROTOCOLOS).document(protocoloId).get().addOnCompleteListener(onCompleteListener);
    }

    public void guardarProtocolo(String userId, String protocoloId, Map<String, Object> protocoloData, Runnable onSuccess, Consumer<Exception> onFailure) {
        DocumentReference docRef;
        if (protocoloId == null || protocoloId.isEmpty()) {
            docRef = db.collection(COLLECTION_USER_PROFILES).document(userId).collection(SUBCOLLECTION_PROTOCOLOS).document();
        } else {
            docRef = db.collection(COLLECTION_USER_PROFILES).document(userId).collection(SUBCOLLECTION_PROTOCOLOS).document(protocoloId);
        }
        protocoloData.put("id", docRef.getId());
        docRef.set(protocoloData)
                .addOnSuccessListener(aVoid -> onSuccess.run())
                .addOnFailureListener(onFailure::accept);
    }

    public void eliminarProtocolo(String userId, String protocoloId, Runnable onSuccess, Consumer<Exception> onFailure) {
        db.collection(COLLECTION_USER_PROFILES).document(userId).collection(SUBCOLLECTION_PROTOCOLOS).document(protocoloId).delete()
                .addOnSuccessListener(aVoid -> onSuccess.run())
                .addOnFailureListener(onFailure::accept);
    }


    // --- MÉTODOS PARA CALENDARIOS (adaptados a sub-colecciones) ---

    // CORREGIDO: Se cambia Consumer<Task<...>> por OnCompleteListener<...>
    public void cargarCalendarios(String userId, OnCompleteListener<QuerySnapshot> onCompleteListener) {
        db.collection(COLLECTION_USER_PROFILES).document(userId).collection(SUBCOLLECTION_CALENDARIOS).get().addOnCompleteListener(onCompleteListener);
    }

    // CORREGIDO: Se cambia Consumer<Task<...>> por OnCompleteListener<...>
    public void buscarCalendarioPorId(String userId, String calendarioId, OnCompleteListener<DocumentSnapshot> onCompleteListener) {
        db.collection(COLLECTION_USER_PROFILES).document(userId).collection(SUBCOLLECTION_CALENDARIOS).document(calendarioId).get().addOnCompleteListener(onCompleteListener);
    }

    public void guardarOActualizarCalendario(String userId, String calendarioId, Map<String, Object> calendarioData, Runnable onSuccess, Consumer<Exception> onFailure) {
        db.collection(COLLECTION_USER_PROFILES).document(userId).collection(SUBCOLLECTION_CALENDARIOS).document(calendarioId)
                .set(calendarioData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> onSuccess.run())
                .addOnFailureListener(onFailure::accept);
    }

    public void eliminarCalendario(String userId, String calendarioId, Runnable onSuccess, Consumer<Exception> onFailure) {
        db.collection(COLLECTION_USER_PROFILES).document(userId).collection(SUBCOLLECTION_CALENDARIOS).document(calendarioId).delete()
                .addOnSuccessListener(aVoid -> onSuccess.run())
                .addOnFailureListener(onFailure::accept);
    }


    // --- MÉTODOS DE STORAGE ---
    // (Estos no necesitan cambios)

    public void subirArchivo(String userId, Uri fileUri, String folderName, Consumer<String> onResult, Consumer<Exception> onFailure) {
        // Crea una ruta única para el archivo para evitar sobreescrituras
        String fileName = System.currentTimeMillis() + "_" + fileUri.getLastPathSegment();
        String rutaStorage = "user_files/" + userId + "/" + folderName + "/" + fileName;
        StorageReference fileRef = storage.getReference().child(rutaStorage);

        fileRef.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot ->
                        fileRef.getDownloadUrl().addOnSuccessListener(uri ->
                                onResult.accept(uri.toString())
                        )
                )
                .addOnFailureListener(onFailure::accept);
    }

    public void subirPdfYObtenerUrl(String userId, Uri fileUri, String nombreCarpeta, Consumer<String> onResult, Consumer<Exception> onFailure) {
        String rutaStorage = userId + "/" + nombreCarpeta + "/" + System.currentTimeMillis() + "_" + fileUri.getLastPathSegment();
        StorageReference fileRef = storage.getReference().child(rutaStorage);
        fileRef.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    onResult.accept(uri.toString());
                }))
                .addOnFailureListener(onFailure::accept);
    }
}