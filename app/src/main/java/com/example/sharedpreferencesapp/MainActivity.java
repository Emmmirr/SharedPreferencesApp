package com.example.sharedpreferencesapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private ProfileManager profileManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setupNavegacion();

        // Inicializar ProfileManager
        profileManager = new ProfileManager(this);

        // Verificar y crear perfil si es necesario
        verificarYCrearPerfil();
    }

    private void setupNavegacion() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            NavigationUI.setupWithNavController(
                    bottomNavigationView,
                    navHostFragment.getNavController()
            );
        } else {
            Log.e(TAG, "NavHostFragment no encontrado");
        }
    }

    private void verificarYCrearPerfil() {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean("isLoggedIn", false);
        String loginMethod = prefs.getString("loginMethod", "");
        String email = prefs.getString("email", "");
        String username = prefs.getString("username", "");

        Log.d(TAG, "Verificando perfil - LoggedIn: " + isLoggedIn + ", Method: " + loginMethod + ", Email: " + email);

        if (isLoggedIn && !email.isEmpty()) {
            // Solo crear perfil para usuarios normales (no Google, porque ya se crea en el login)
            if ("normal".equals(loginMethod)) {
                crearPerfilUsuarioNormal(email, username);
            } else {
                Log.d(TAG, "Usuario de Google detectado, perfil ya gestionado en login");
            }
        } else {
            Log.d(TAG, "No hay usuario logueado o email vacío");
        }
    }

    private void crearPerfilUsuarioNormal(String email, String displayName) {
        Log.d(TAG, "Creando perfil para usuario normal: " + email);

        // Para usuarios normales, crear un perfil básico
        // Como no tienen Firebase Auth, usamos un perfil simplificado
        SharedPreferences prefs = getSharedPreferences("UserProfilePrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("normal_user_email", email);
        editor.putString("normal_user_display_name", displayName);
        editor.putString("normal_user_auth_method", "normal");
        editor.putLong("normal_user_created_at", System.currentTimeMillis());
        editor.apply();

        Log.d(TAG, "Perfil básico creado para usuario normal: " + displayName);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Verificar si el usuario sigue logueado
        SharedPreferences prefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean("isLoggedIn", false);

        if (!isLoggedIn) {
            // Si no está logueado, redirigir al login
            Log.d(TAG, "Usuario no logueado, redirigiendo al login");
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
    }
}