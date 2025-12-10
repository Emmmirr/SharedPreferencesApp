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
import com.google.firebase.auth.FirebaseAuth;

public class AdminMainActivity extends AppCompatActivity {

    private static final String TAG = "AdminMainActivity";
    private ProfileManager profileManager;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Verificar que el usuario sea administrador
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean("isLoggedIn", false);
        String userType = prefs.getString("userType", "");

        if (!isLoggedIn) {
            // No está logueado, redirigir al login
            Log.d(TAG, "Usuario no logueado, redirigiendo al login");
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        } else if (!"administrator".equals(userType)) {
            // Está logueado pero no es administrador, redirigir según su tipo
            Log.d(TAG, "Usuario no es administrador, redirigiendo según tipo: " + userType);
            Intent intent;
            if ("student".equals(userType)) {
                intent = new Intent(this, StudentMainActivity.class);
            } else {
                intent = new Intent(this, MainActivity.class);
            }
            startActivity(intent);
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setupNavegacion();

        // Inicializar ProfileManager
        profileManager = new ProfileManager(this);
    }

    private void setupNavegacion() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
            NavigationUI.setupWithNavController(bottomNavigationView, navController);
        } else {
            Log.e(TAG, "NavHostFragment no encontrado");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Verificar en onResume también
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean("isLoggedIn", false);
        String userType = prefs.getString("userType", "");

        if (!isLoggedIn) {
            // No está logueado, redirigir al login
            Log.d(TAG, "Usuario no logueado (onResume), redirigiendo al login");
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        } else if (!"administrator".equals(userType)) {
            // Está logueado pero no es administrador, redirigir según su tipo
            Log.d(TAG, "Usuario no es administrador (onResume), redirigiendo según tipo: " + userType);
            Intent intent;
            if ("student".equals(userType)) {
                intent = new Intent(this, StudentMainActivity.class);
            } else {
                intent = new Intent(this, MainActivity.class);
            }
            startActivity(intent);
            finish();
        }
    }
}

