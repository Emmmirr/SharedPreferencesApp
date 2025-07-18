package com.example.sharedpreferencesapp;

import android.content.Context;
import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.function.Consumer;

/**
 * Gestor unificado de perfiles de usuario.
 * TODA la lógica de perfiles pasa por aquí y se guarda en Firestore.
 */
public class ProfileManager {

    private static final String TAG = "ProfileManager";
    private final FirebaseManager firebaseManager;

    public ProfileManager(Context context) {
        this.firebaseManager = new FirebaseManager();
    }

    /**
     * Crea un perfil en Firestore si no existe. Es seguro llamarlo tanto en login como en registro.
     * @param user El objeto FirebaseUser que acaba de autenticarse.
     * @param defaultName El nombre a usar si es un usuario nuevo (email o nombre de Google).
     * @param authMethod El método de registro ("email" o "google").
     */
    public void crearOVerificarPerfil(FirebaseUser user, String defaultName, String authMethod, Runnable onSuccess, Consumer<Exception> onFailure) {
        if (user == null) {
            onFailure.accept(new Exception("Usuario nulo, no se puede crear/verificar perfil."));
            return;
        }
        String userId = user.getUid();

        firebaseManager.buscarPerfilUsuarioPorId(userId, task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                if (task.getResult().exists()) {
                    Log.d(TAG, "Perfil ya existe para: " + user.getEmail());
                    onSuccess.run(); // El perfil ya existe, todo bien.
                } else {
                    // El perfil no existe, así que lo creamos con datos básicos.
                    Log.d(TAG, "Perfil no encontrado, creando uno nuevo para: " + user.getEmail());
                    UserProfile newProfile = new UserProfile(userId, user.getEmail(), defaultName, authMethod);
                    firebaseManager.guardarPerfilUsuario(userId, newProfile.toMap(), onSuccess, onFailure);
                }
            } else {
                // Hubo un error de red o permisos al buscar el perfil.
                onFailure.accept(task.getException());
            }
        });
    }

    /**
     * Obtiene el perfil del usuario actualmente logueado desde Firestore.
     */
    public void obtenerPerfilActual(Consumer<UserProfile> onSuccess, Consumer<Exception> onFailure) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            onFailure.accept(new Exception("No hay sesión de usuario activa"));
            return;
        }

        firebaseManager.buscarPerfilUsuarioPorId(currentUser.getUid(), task -> {
            if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                UserProfile profile = UserProfile.fromMap(task.getResult().getData());
                onSuccess.accept(profile);
            } else if (task.isSuccessful()) {
                onFailure.accept(new Exception("Perfil no encontrado en Firestore."));
            } else {
                onFailure.accept(task.getException());
            }
        });
    }

    /**
     * Actualiza el perfil del usuario actual en Firestore.
     */
    public void actualizarPerfilActual(UserProfile profile, Runnable onSuccess, Consumer<Exception> onFailure) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            onFailure.accept(new Exception("No hay sesión de usuario activa"));
            return;
        }

        profile.setUpdatedAt(String.valueOf(System.currentTimeMillis()));
        profile.setProfileComplete(profile.getProfileCompleteness() >= 70);

        firebaseManager.guardarPerfilUsuario(currentUser.getUid(), profile.toMap(), onSuccess, onFailure);
    }
}