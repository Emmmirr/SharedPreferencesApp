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
    private NavController navController; // <-- CAMBIO: Hacemos el NavController una variable de instancia

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

        // --- INICIO DE CÓDIGO MODIFICADO ---
        // Después de configurar la navegación, procesamos el intent de la notificación.
        handleIntent(getIntent());
        // --- FIN DE CÓDIGO MODIFICADO ---
    }

    private void setupNavegacion() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            // --- CAMBIO: Guardamos la referencia al NavController ---
            navController = navHostFragment.getNavController();
            NavigationUI.setupWithNavController(bottomNavigationView, navController);
        } else {
            Log.e(TAG, "NavHostFragment no encontrado");
        }
    }

    // --- INICIO DE CÓDIGO AÑADIDO ---
    /**
     * Este método se llama cuando la actividad se inicia con un nuevo Intent (como el de una notificación).
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Procesamos el nuevo intent si la actividad ya estaba abierta.
        handleIntent(intent);
    }

    /**
     * Procesa el Intent para ver si contiene la instrucción de navegar a una pestaña.
     * @param intent El Intent que inició o trajo al frente la actividad.
     */
    private void handleIntent(Intent intent) {
        if (intent != null) {
            int tabToOpen = intent.getIntExtra("NAVIGATE_TO_TAB", -1);
            if (tabToOpen != -1 && navController != null) {
                // Creamos un Bundle para pasar los argumentos al fragmento de destino.
                Bundle args = new Bundle();
                args.putInt("TAB_TO_OPEN", tabToOpen);

                // Navegamos al destino que contiene el ViewPager (GestionCalendarioFragment).
                // Asegúrate de que el ID en tu nav_graph.xml sea 'gestionCalendarioFragment'.
                navController.navigate(R.id.gestionCalendarioFragment, args);
            }
        }
    }
    // --- FIN DE CÓDIGO AÑADIDO ---

    private void verificarYCrearPerfil() {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean("isLoggedIn", false);
        String loginMethod = prefs.getString("loginMethod", "");
        String email = prefs.getString("email", "");
        String username = prefs.getString("username", "");

        Log.d(TAG, "Verificando perfil - LoggedIn: " + isLoggedIn + ", Method: " + loginMethod + ", Email: " + email);

        if (isLoggedIn && !email.isEmpty()) {
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
        SharedPreferences prefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean("isLoggedIn", false);
        if (!isLoggedIn) {
            Log.d(TAG, "Usuario no logueado, redirigiendo al login");
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
    }
}