package com.example.sharedpreferencesapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class StudentMainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "StudentMainActivity";
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private TextView navUsername, navEmail;
    private ProfileManager profileManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // AÑADIDO: Verificar que el usuario sea estudiante
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
        } else if (!"student".equals(userType)) {
            // Está logueado pero no es estudiante, redirigir a MainActivity
            Log.d(TAG, "Usuario no es estudiante, redirigiendo a MainActivity");
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_student_main);

        profileManager = new ProfileManager(this);

        // Configurar toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Configurar drawer layout
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Configurar toggle para abrir/cerrar drawer
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Configurar header del navigation drawer
        setupNavHeader();

        // Cargar fragment inicial (Home)
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
            navigationView.setCheckedItem(R.id.nav_home);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // AÑADIDO: Verificar en onResume también
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean("isLoggedIn", false);
        String userType = prefs.getString("userType", "");

        if (!isLoggedIn) {
            // No está logueado, redirigir al login
            Log.d(TAG, "Usuario no logueado (onResume), redirigiendo al login");
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        } else if (!"student".equals(userType)) {
            // Está logueado pero no es estudiante, redirigir a MainActivity
            Log.d(TAG, "Usuario no es estudiante (onResume), redirigiendo a MainActivity");
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }

    private void setupNavHeader() {
        navUsername = navigationView.getHeaderView(0).findViewById(R.id.nav_header_username);
        navEmail = navigationView.getHeaderView(0).findViewById(R.id.nav_header_email);

        // Obtener datos de las preferencias
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String username = prefs.getString("username", "Estudiante");
        String email = prefs.getString("email", "");

        navUsername.setText(username);
        navEmail.setText(email);

        // Cargar perfil completo desde Firestore
        profileManager.obtenerPerfilActual(
                profile -> {
                    if (profile != null) {
                        String displayName = profile.getFullName().isEmpty() ?
                                profile.getDisplayName() : profile.getFullName();
                        navUsername.setText(displayName);
                        navEmail.setText(profile.getEmail());
                    }
                },
                error -> Toast.makeText(StudentMainActivity.this,
                        "Error al cargar perfil: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show()
        );
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Fragment selectedFragment = null;
        int itemId = item.getItemId();

        if (itemId == R.id.nav_home) {
            selectedFragment = new HomeFragment();
        } else if (itemId == R.id.nav_perfil) {
            selectedFragment = new PerfilFragment();
        } else if (itemId == R.id.nav_carga_academica) {
            selectedFragment = new CargaAcademicaFragment();
        } else if (itemId == R.id.nav_protocolo) {
            selectedFragment = new ProtocoloFragment();
        } else if (itemId == R.id.nav_calendario) {
            selectedFragment = new CalendarioFragment();
        } else if (itemId == R.id.nav_logout) {
            cerrarSesion();
            return true;
        }

        if (selectedFragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, selectedFragment)
                    .commit();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void cerrarSesion() {
        // Limpiar sesión local
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        prefs.edit().clear().apply();

        // Cerrar sesión en Firebase
        FirebaseAuth.getInstance().signOut();

        // Redirigir a login
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}