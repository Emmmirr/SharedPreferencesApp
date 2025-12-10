package com.example.sharedpreferencesapp;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class FirebaseManager {

    private static final String TAG = "FirebaseManager";
    public final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseStorage storage = FirebaseStorage.getInstance();

    // Colección principal para los perfiles de usuario
    private static final String COLLECTION_USER_PROFILES = "user_profiles";

    // Colección para números de control autorizados para registro
    private static final String COLLECTION_AUTORIZADOS = "numeros_control_autorizados";

    // Nombres de las SUB-COLECCIONES que existirán dentro de cada documento de usuario
    private static final String SUBCOLLECTION_ALUMNOS = "alumnos";
    private static final String SUBCOLLECTION_PROTOCOLOS = "protocolos";
    private static final String SUBCOLLECTION_CALENDARIOS = "calendarios";


    // --- MÉTODOS PARA PERFILES DE USUARIO ---

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

    public void cargarAlumnos(String userId, OnCompleteListener<QuerySnapshot> onCompleteListener) {
        db.collection(COLLECTION_USER_PROFILES).document(userId).collection(SUBCOLLECTION_ALUMNOS).get().addOnCompleteListener(onCompleteListener);
    }

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

    public void cargarProtocolos(String userId, OnCompleteListener<QuerySnapshot> onCompleteListener) {
        db.collection(COLLECTION_USER_PROFILES).document(userId).collection(SUBCOLLECTION_PROTOCOLOS).get().addOnCompleteListener(onCompleteListener);
    }

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

    public void cargarCalendarios(String userId, OnCompleteListener<QuerySnapshot> onCompleteListener) {
        db.collection(COLLECTION_USER_PROFILES).document(userId).collection(SUBCOLLECTION_CALENDARIOS).get().addOnCompleteListener(onCompleteListener);
    }

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

    public void subirPdfStorage(String userId, String calendarioId, String campoPdfKey, Uri fileUri, OnSuccessListener<String> onSuccess, OnFailureListener onFailure) {
        if (userId == null || calendarioId == null || fileUri == null) {
            onFailure.onFailure(new IllegalArgumentException("UserID, CalendarioID o la URI del archivo no pueden ser nulos."));
            return;
        }

        String path = COLLECTION_USER_PROFILES + "/" + userId + "/" + SUBCOLLECTION_CALENDARIOS + "/" + calendarioId + "/" + campoPdfKey + ".pdf";
        StorageReference storageRef = storage.getReference(path);

        storageRef.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> {
                    storageRef.getDownloadUrl()
                            .addOnSuccessListener(uri -> onSuccess.onSuccess(uri.toString()))
                            .addOnFailureListener(onFailure);
                })
                .addOnFailureListener(onFailure);
    }

    public void subirFotoPerfil(String userId, Uri fileUri, OnSuccessListener<String> onSuccess, OnFailureListener onFailure) {
        if (userId == null || fileUri == null) {
            onFailure.onFailure(new IllegalArgumentException("UserID o la URI del archivo no pueden ser nulos."));
            return;
        }


// CÓMO DEBE QUEDAR
        String path = COLLECTION_USER_PROFILES + "/" + userId + "/foto_perfil/profile_picture.jpg";
        StorageReference storageRef = storage.getReference(path);

        storageRef.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> {
                    storageRef.getDownloadUrl()
                            .addOnSuccessListener(uri -> onSuccess.onSuccess(uri.toString()))
                            .addOnFailureListener(onFailure);
                })
                .addOnFailureListener(onFailure);


    }

    public void subirCredencial(String userId, Uri fileUri, OnSuccessListener<String> onSuccess, OnFailureListener onFailure) {
        if (userId == null || fileUri == null) {
            onFailure.onFailure(new IllegalArgumentException("UserID o la URI del archivo no pueden ser nulos."));
            return;
        }

        // Ruta predecible para la credencial.
        // Ejemplo: user_profiles/{userId}/credential/credential_scan.jpg
        String path = COLLECTION_USER_PROFILES + "/" + userId + "/credencial/credential_scan.jpg";
        StorageReference storageRef = storage.getReference(path);

        storageRef.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> {
                    storageRef.getDownloadUrl()
                            .addOnSuccessListener(uri -> onSuccess.onSuccess(uri.toString()))
                            .addOnFailureListener(onFailure);
                })
                .addOnFailureListener(onFailure);
    }

    // Añadir estos métodos a tu clase FirebaseManager existente

    /**
     * Carga los estudiantes que han elegido al maestro actual como supervisor
     */
    public void cargarEstudiantesAsignados(String maestroId, OnCompleteListener<QuerySnapshot> listener) {
        db.collection("user_profiles") // Cambiado a user_profiles según tu estructura
                .whereEqualTo("supervisorId", maestroId)
                .whereEqualTo("userType", "student")
                .get()
                .addOnCompleteListener(listener);
    }

    /**
     * Actualiza el estado de aprobación de un estudiante
     */
    public void actualizarEstadoAprobacionEstudiante(String estudianteId, boolean aprobado,
                                                     OnSuccessListener<Void> onSuccess,
                                                     OnFailureListener onFailure) {
        db.collection("user_profiles") // Cambiado a user_profiles según tu estructura
                .document(estudianteId)
                .update("isApproved", aprobado)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    /**
     * Rechaza un estudiante eliminando su asignación a un maestro
     */
    public void rechazarEstudiante(String estudianteId, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("supervisorId", "");
        updates.put("supervisorName", "");

        db.collection("user_profiles") // Cambiado a user_profiles según tu estructura
                .document(estudianteId)
                .update(updates)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    // Añadir estos métodos a tu clase FirebaseManager existente

    /**
     * Carga los estudiantes que han sido aprobados por el maestro
     */
    public void cargarEstudiantesAprobados(String maestroId, OnCompleteListener<QuerySnapshot> listener) {
        db.collection("user_profiles")
                .whereEqualTo("supervisorId", maestroId)
                .whereEqualTo("isApproved", true)
                .get()
                .addOnCompleteListener(listener);
    }

    /**
     * Busca protocolos por ID de estudiante
     */
    public void buscarProtocolosPorEstudiante(String estudianteId, OnCompleteListener<QuerySnapshot> listener) {
        // Primero necesitamos encontrar los IDs de los alumnos del estudiante
        db.collection("alumnos")
                .whereEqualTo("estudianteId", estudianteId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        List<String> alumnoIds = new ArrayList<>();
                        for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                            alumnoIds.add(doc.getId());
                        }

                        // Ahora buscamos los protocolos que usan estos IDs de alumno
                        if (!alumnoIds.isEmpty()) {
                            db.collection("protocolos")
                                    .whereIn("alumnoId", alumnoIds)
                                    .get()
                                    .addOnCompleteListener(listener);
                        } else {
                            // Si no hay IDs de alumno, devuelve una lista vacía
                            db.collection("protocolos")
                                    .whereEqualTo("alumnoId", "no_existe")
                                    .get()
                                    .addOnCompleteListener(listener);
                        }
                    } else {
                        // Si el estudiante no tiene alumnos, intentamos buscar protocolos directamente asociados a él
                        db.collection("protocolos")
                                .whereEqualTo("estudianteId", estudianteId)
                                .get()
                                .addOnCompleteListener(listener);
                    }
                });
    }

    /**
     * Busca el perfil de CUALQUIER usuario (generalmente un estudiante) por su ID
     * en la colección principal de perfiles.
     * @param studentId El ID del estudiante a buscar.
     * @param onCompleteListener El listener para manejar el resultado.
     */
    public void buscarPerfilDeEstudiantePorId(String studentId, OnCompleteListener<DocumentSnapshot> onCompleteListener) {
        db.collection(COLLECTION_USER_PROFILES).document(studentId).get().addOnCompleteListener(onCompleteListener);
    }

    // --- MÉTODOS PARA TRÁMITES Y FORMATOS ---

    private static final String SUBCOLLECTION_TRAMITES_FORMATOS = "tramites_formatos";

    /**
     * Guarda o actualiza el estado de un documento en trámites y formatos
     */
    public void guardarDocumentoTramite(String userId, String documentoId, Map<String, Object> documentoData, Runnable onSuccess, Consumer<Exception> onFailure) {
        db.collection(COLLECTION_USER_PROFILES)
                .document(userId)
                .collection(SUBCOLLECTION_TRAMITES_FORMATOS)
                .document("documentos")
                .get()
                .addOnCompleteListener(task -> {
                    Map<String, Object> documentosData = new HashMap<>();
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                        documentosData = task.getResult().getData();
                    }
                    if (documentosData == null) {
                        documentosData = new HashMap<>();
                    }
                    documentosData.put(documentoId, documentoData);
                    documentosData.put("updatedAt", System.currentTimeMillis());

                    db.collection(COLLECTION_USER_PROFILES)
                            .document(userId)
                            .collection(SUBCOLLECTION_TRAMITES_FORMATOS)
                            .document("documentos")
                            .set(documentosData)
                            .addOnSuccessListener(aVoid -> onSuccess.run())
                            .addOnFailureListener(onFailure::accept);
                });
    }

    /**
     * Guarda un borrador del wizard
     */
    public void guardarBorradorWizard(String userId, String documentoId, Map<String, Object> borradorData, Runnable onSuccess, Consumer<Exception> onFailure) {
        Map<String, Object> documentoData = new HashMap<>();
        documentoData.put("borrador", borradorData);
        documentoData.put("estado", "en_proceso");
        documentoData.put("fechaGuardado", System.currentTimeMillis());

        guardarDocumentoTramite(userId, documentoId, documentoData, onSuccess, onFailure);
    }

    /**
     * Sube un PDF generado a Storage
     */
    public void subirPdfTramite(String userId, String documentoId, Uri fileUri, OnSuccessListener<String> onSuccess, OnFailureListener onFailure) {
        if (userId == null || documentoId == null || fileUri == null) {
            onFailure.onFailure(new IllegalArgumentException("UserID, DocumentoID o la URI del archivo no pueden ser nulos."));
            return;
        }

        String path = COLLECTION_USER_PROFILES + "/" + userId + "/" + SUBCOLLECTION_TRAMITES_FORMATOS + "/" + documentoId + ".pdf";
        StorageReference storageRef = storage.getReference(path);

        storageRef.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> {
                    storageRef.getDownloadUrl()
                            .addOnSuccessListener(uri -> onSuccess.onSuccess(uri.toString()))
                            .addOnFailureListener(onFailure);
                })
                .addOnFailureListener(onFailure);
    }

    /**
     * Sube un Word llenado a Storage
     */
    public void subirWordTramite(String userId, String documentoId, Uri fileUri, OnSuccessListener<String> onSuccess, OnFailureListener onFailure) {
        if (userId == null || documentoId == null || fileUri == null) {
            onFailure.onFailure(new IllegalArgumentException("UserID, DocumentoID o la URI del archivo no pueden ser nulos."));
            return;
        }

        String path = COLLECTION_USER_PROFILES + "/" + userId + "/" + SUBCOLLECTION_TRAMITES_FORMATOS + "/" + documentoId + ".docx";
        StorageReference storageRef = storage.getReference(path);

        storageRef.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> {
                    storageRef.getDownloadUrl()
                            .addOnSuccessListener(uri -> onSuccess.onSuccess(uri.toString()))
                            .addOnFailureListener(onFailure);
                })
                .addOnFailureListener(onFailure);
    }

    // --- MÉTODOS PARA NÚMEROS DE CONTROL AUTORIZADOS ---

    /**
     * Agrega un número de control a la lista de números autorizados para registro
     */
    public void agregarNumeroAutorizado(String numeroControl, Runnable onSuccess, Consumer<Exception> onFailure) {
        Map<String, Object> autorizadoData = new HashMap<>();
        autorizadoData.put("numeroControl", numeroControl);
        autorizadoData.put("fechaRegistro", System.currentTimeMillis());
        autorizadoData.put("activo", true);

        // Usar el número de control como ID del documento para evitar duplicados
        db.collection(COLLECTION_AUTORIZADOS)
                .document(numeroControl)
                .set(autorizadoData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> onSuccess.run())
                .addOnFailureListener(e -> onFailure.accept(e));
    }

    /**
     * Verifica si un número de control está en la lista de números autorizados
     */
    public void verificarNumeroAutorizado(String numeroControl, Consumer<Boolean> onComplete) {
        db.collection(COLLECTION_AUTORIZADOS)
                .document(numeroControl)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                        Map<String, Object> data = task.getResult().getData();
                        Boolean activo = (Boolean) data.getOrDefault("activo", true);
                        onComplete.accept(activo != null && activo);
                    } else {
                        onComplete.accept(false);
                    }
                });
    }

    /**
     * Obtiene todos los números de control autorizados
     */
    public void obtenerNumerosAutorizados(OnCompleteListener<QuerySnapshot> onCompleteListener) {
        db.collection(COLLECTION_AUTORIZADOS)
                .whereEqualTo("activo", true)
                .get()
                .addOnCompleteListener(onCompleteListener);
    }

    /**
     * Elimina o desactiva un número autorizado
     */
    public void eliminarNumeroAutorizado(String numeroControl, Runnable onSuccess, Consumer<Exception> onFailure) {
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("activo", false);
        updateData.put("fechaEliminacion", System.currentTimeMillis());

        db.collection(COLLECTION_AUTORIZADOS)
                .document(numeroControl)
                .update(updateData)
                .addOnSuccessListener(aVoid -> onSuccess.run())
                .addOnFailureListener(e -> onFailure.accept(e));
    }

}