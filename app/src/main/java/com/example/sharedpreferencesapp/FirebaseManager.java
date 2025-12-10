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
import com.google.firebase.firestore.QueryDocumentSnapshot;
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

    // Colección para números de control autorizados para registro (organizados en bloques)
    private static final String COLLECTION_BLOQUES_AUTORIZADOS = "bloques_numeros_autorizados";

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

    // --- MÉTODOS PARA BLOQUES DE NÚMEROS DE CONTROL AUTORIZADOS ---

    /**
     * Crea un nuevo bloque de números autorizados
     */
    public void crearBloqueAutorizado(String nombreBloque, Runnable onSuccess, Consumer<Exception> onFailure) {
        Map<String, Object> bloqueData = new HashMap<>();
        bloqueData.put("nombre", nombreBloque);
        bloqueData.put("fechaCreacion", System.currentTimeMillis());
        bloqueData.put("activo", true);
        bloqueData.put("numeros", new ArrayList<String>());

        db.collection(COLLECTION_BLOQUES_AUTORIZADOS)
                .add(bloqueData)
                .addOnSuccessListener(documentReference -> onSuccess.run())
                .addOnFailureListener(e -> onFailure.accept(e));
    }

    /**
     * Obtiene todos los bloques activos
     * Nota: No usamos orderBy aquí para evitar requerir un índice compuesto en Firestore.
     * El ordenamiento se hace manualmente en el fragmento.
     */
    public void obtenerBloquesAutorizados(OnCompleteListener<QuerySnapshot> onCompleteListener) {
        db.collection(COLLECTION_BLOQUES_AUTORIZADOS)
                .whereEqualTo("activo", true)
                .get()
                .addOnCompleteListener(onCompleteListener);
    }

    /**
     * Obtiene un bloque específico por su ID
     */
    public void obtenerBloquePorId(String bloqueId, OnCompleteListener<DocumentSnapshot> onCompleteListener) {
        db.collection(COLLECTION_BLOQUES_AUTORIZADOS)
                .document(bloqueId)
                .get()
                .addOnCompleteListener(onCompleteListener);
    }

    /**
     * Agrega un número de control a un bloque específico
     */
    public void agregarNumeroABloque(String bloqueId, String numeroControl, Runnable onSuccess, Consumer<Exception> onFailure) {
        db.collection(COLLECTION_BLOQUES_AUTORIZADOS)
                .document(bloqueId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> data = documentSnapshot.getData();
                        List<String> numeros = (List<String>) data.getOrDefault("numeros", new ArrayList<>());

                        // Verificar si ya existe
                        if (numeros.contains(numeroControl)) {
                            onFailure.accept(new Exception("Este número de control ya está en el bloque"));
                            return;
                        }

                        numeros.add(numeroControl);
                        Map<String, Object> updateData = new HashMap<>();
                        updateData.put("numeros", numeros);
                        updateData.put("fechaActualizacion", System.currentTimeMillis());

                        db.collection(COLLECTION_BLOQUES_AUTORIZADOS)
                                .document(bloqueId)
                                .update(updateData)
                                .addOnSuccessListener(aVoid -> onSuccess.run())
                                .addOnFailureListener(e -> onFailure.accept(e));
                    } else {
                        onFailure.accept(new Exception("Bloque no encontrado"));
                    }
                })
                .addOnFailureListener(e -> onFailure.accept(e));
    }

    /**
     * Elimina un número de control de un bloque
     */
    public void eliminarNumeroDeBloque(String bloqueId, String numeroControl, Runnable onSuccess, Consumer<Exception> onFailure) {
        db.collection(COLLECTION_BLOQUES_AUTORIZADOS)
                .document(bloqueId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> data = documentSnapshot.getData();
                        List<String> numeros = (List<String>) data.getOrDefault("numeros", new ArrayList<>());
                        numeros.remove(numeroControl);

                        Map<String, Object> updateData = new HashMap<>();
                        updateData.put("numeros", numeros);
                        updateData.put("fechaActualizacion", System.currentTimeMillis());

                        db.collection(COLLECTION_BLOQUES_AUTORIZADOS)
                                .document(bloqueId)
                                .update(updateData)
                                .addOnSuccessListener(aVoid -> onSuccess.run())
                                .addOnFailureListener(e -> onFailure.accept(e));
                    } else {
                        onFailure.accept(new Exception("Bloque no encontrado"));
                    }
                })
                .addOnFailureListener(e -> onFailure.accept(e));
    }

    /**
     * Elimina o desactiva un bloque completo
     */
    public void eliminarBloque(String bloqueId, Runnable onSuccess, Consumer<Exception> onFailure) {
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("activo", false);
        updateData.put("fechaEliminacion", System.currentTimeMillis());

        db.collection(COLLECTION_BLOQUES_AUTORIZADOS)
                .document(bloqueId)
                .update(updateData)
                .addOnSuccessListener(aVoid -> onSuccess.run())
                .addOnFailureListener(e -> onFailure.accept(e));
    }

    /**
     * Verifica si un número de control está en cualquier bloque activo
     */
    public void verificarNumeroAutorizado(String numeroControl, Consumer<Boolean> onComplete) {
        db.collection(COLLECTION_BLOQUES_AUTORIZADOS)
                .whereEqualTo("activo", true)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Map<String, Object> data = document.getData();
                            List<String> numeros = (List<String>) data.getOrDefault("numeros", new ArrayList<>());
                            if (numeros.contains(numeroControl)) {
                                onComplete.accept(true);
                                return;
                            }
                        }
                        onComplete.accept(false);
                    } else {
                        onComplete.accept(false);
                    }
                });
    }

    // --- MÉTODOS PARA ESTADO DE RESIDENCIA ---

    private static final String SUBCOLLECTION_ESTADO_RESIDENCIA = "estado_residencia";

    /**
     * Guarda o actualiza el estado de residencia de un estudiante
     */
    public void guardarEstadoResidencia(String studentId, Map<String, Object> estadoData, Runnable onSuccess, Consumer<Exception> onFailure) {
        db.collection(COLLECTION_USER_PROFILES)
                .document(studentId)
                .collection(SUBCOLLECTION_ESTADO_RESIDENCIA)
                .document("datos")
                .set(estadoData)
                .addOnSuccessListener(aVoid -> onSuccess.run())
                .addOnFailureListener(onFailure::accept);
    }

    /**
     * Carga el estado de residencia de un estudiante
     */
    public void cargarEstadoResidencia(String studentId, Consumer<Map<String, Object>> onSuccess, Consumer<Exception> onFailure) {
        db.collection(COLLECTION_USER_PROFILES)
                .document(studentId)
                .collection(SUBCOLLECTION_ESTADO_RESIDENCIA)
                .document("datos")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                        Map<String, Object> data = task.getResult().getData();
                        if (data != null) {
                            onSuccess.accept(data);
                        } else {
                            onSuccess.accept(new HashMap<>());
                        }
                    } else {
                        // Si no existe, retornar mapa vacío
                        onSuccess.accept(new HashMap<>());
                    }
                })
                .addOnFailureListener(e -> onFailure.accept(e));
    }

}