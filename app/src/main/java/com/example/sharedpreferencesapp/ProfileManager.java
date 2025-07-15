package com.example.sharedpreferencesapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.function.Consumer;

/**
 * Gestor de perfiles de usuario
 * Maneja la creación, actualización y gestión de perfiles
 *
 * @author anthonyllan
 * @version 1.0
 * @since 2025-07-15
 */
public class ProfileManager {

    private static final String TAG = "ProfileManager";
    private static final String PREFS_NAME = "UserProfilePrefs";
    private static final String CURRENT_USER_ID = "current_user_id";

    private final Context context;
    private final FirebaseManager firebaseManager;
    private final SharedPreferences prefs;

    public ProfileManager(Context context) {
        this.context = context;
        this.firebaseManager = new FirebaseManager();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Crea un perfil automáticamente después del login/registro
     */
    public void crearPerfilDespuesDeAuth(String email, String displayName, String authMethod,
                                         Runnable onSuccess, Consumer<Exception> onFailure) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            onFailure.accept(new Exception("Usuario no autenticado"));
            return;
        }

        String userId = currentUser.getUid();

        // Verificar si ya existe un perfil
        firebaseManager.buscarPerfilUsuarioPorId(userId, task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                if (task.getResult().exists()) {
                    // El perfil ya existe, solo actualizar datos actuales
                    Log.d(TAG, "Perfil existente encontrado para: " + email);
                    setCurrentUserId(userId);
                    onSuccess.run();
                } else {
                    // Crear nuevo perfil
                    UserProfile newProfile = new UserProfile(userId, email, displayName, authMethod);

                    // Si es Google Auth, extraer información adicional
                    if ("google".equals(authMethod) && currentUser.getDisplayName() != null) {
                        String[] nameParts = currentUser.getDisplayName().split(" ", 2);
                        if (nameParts.length > 0) {
                            newProfile.setFirstName(nameParts[0]);
                        }
                        if (nameParts.length > 1) {
                            newProfile.setLastName(nameParts[1]);
                        }
                        if (currentUser.getPhotoUrl() != null) {
                            newProfile.setProfileImageUrl(currentUser.getPhotoUrl().toString());
                        }
                    }

                    firebaseManager.crearPerfilUsuario(newProfile,
                            () -> {
                                setCurrentUserId(userId);
                                Log.d(TAG, "Perfil creado exitosamente para: " + email);
                                onSuccess.run();
                            },
                            error -> {
                                Log.e(TAG, "Error creando perfil", error);
                                onFailure.accept(error);
                            }
                    );
                }
            } else {
                Log.e(TAG, "Error verificando perfil existente", task.getException());
                onFailure.accept(task.getException());
            }
        });
    }

    /**
     * Obtiene el perfil del usuario actual
     */
    public void obtenerPerfilActual(Consumer<UserProfile> onSuccess, Consumer<Exception> onFailure) {
        String userId = getCurrentUserId();
        if (userId == null) {
            onFailure.accept(new Exception("No hay usuario autenticado"));
            return;
        }

        firebaseManager.buscarPerfilUsuarioPorId(userId, task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                if (task.getResult().exists()) {
                    UserProfile profile = UserProfile.fromMap(task.getResult().getData());
                    onSuccess.accept(profile);
                } else {
                    onFailure.accept(new Exception("Perfil no encontrado"));
                }
            } else {
                onFailure.accept(task.getException());
            }
        });
    }

    /**
     * Actualiza el perfil del usuario actual
     */
    public void actualizarPerfilActual(UserProfile profile, Runnable onSuccess, Consumer<Exception> onFailure) {
        String userId = getCurrentUserId();
        if (userId == null) {
            onFailure.accept(new Exception("No hay usuario autenticado"));
            return;
        }

        profile.setUserId(userId);
        profile.setUpdatedAt(String.valueOf(System.currentTimeMillis()));

        // Calcular si el perfil está completo
        profile.setProfileComplete(profile.getProfileCompleteness() >= 70);

        firebaseManager.guardarPerfilUsuario(userId, profile.toMap(), onSuccess, onFailure);
    }

    /**
     * Verifica si el usuario actual tiene un perfil completo
     */
    public void verificarPerfilCompleto(Consumer<Boolean> onResult, Consumer<Exception> onFailure) {
        obtenerPerfilActual(
                profile -> onResult.accept(profile.isProfileComplete()),
                onFailure
        );
    }

    /**
     * Cierra la sesión del usuario actual
     */
    public void cerrarSesion() {
        FirebaseAuth.getInstance().signOut();
        clearCurrentUserId();

        // Limpiar SharedPreferences del login
        SharedPreferences loginPrefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        loginPrefs.edit().clear().apply();

        Log.d(TAG, "Sesión cerrada exitosamente");
    }

    /**
     * Obtiene el ID del usuario actual
     */
    public String getCurrentUserId() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            return currentUser.getUid();
        }
        return prefs.getString(CURRENT_USER_ID, null);
    }

    /**
     * Establece el ID del usuario actual
     */
    private void setCurrentUserId(String userId) {
        prefs.edit().putString(CURRENT_USER_ID, userId).apply();
    }

    /**
     * Limpia el ID del usuario actual
     */
    private void clearCurrentUserId() {
        prefs.edit().remove(CURRENT_USER_ID).apply();
    }

    /**
     * Verifica si hay un usuario autenticado
     */
    public boolean isUserAuthenticated() {
        return FirebaseAuth.getInstance().getCurrentUser() != null || getCurrentUserId() != null;
    }
}