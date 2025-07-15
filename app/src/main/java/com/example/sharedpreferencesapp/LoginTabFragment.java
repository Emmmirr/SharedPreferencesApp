package com.example.sharedpreferencesapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.developer.gbuttons.GoogleSignInButton;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginTabFragment extends Fragment {

    private static final String TAG = "LoginTabFragment";

    private EditText loginEmail, loginPassword;
    private Button loginButton;
    private GoogleSignInButton googleBtn;

    // Variables para Google Sign-In
    private GoogleSignInOptions gOptions;
    private GoogleSignInClient gClient;
    private FirebaseAuth auth;
    private ActivityResultLauncher<Intent> activityResultLauncher;

    // Profile Manager
    private ProfileManager profileManager;

    public LoginTabFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login_tab, container, false);

        initializeViews(view);
        initializeAuth();
        setupEventListeners();

        return view;
    }

    private void initializeViews(View view) {
        loginEmail = view.findViewById(R.id.login_email);
        loginPassword = view.findViewById(R.id.login_password);
        loginButton = view.findViewById(R.id.login_button);
        googleBtn = view.findViewById(R.id.googleBtn);

        // Verificar si el botón de Google existe
        if (googleBtn == null) {
            Log.e(TAG, "GoogleSignInButton no encontrado en el layout");
            Toast.makeText(getContext(), "Error: Botón de Google no encontrado", Toast.LENGTH_SHORT).show();
        }
    }

    private void initializeAuth() {
        // Inicializar Firebase Auth y ProfileManager
        auth = FirebaseAuth.getInstance();
        profileManager = new ProfileManager(requireContext());

        // Configuración de Google Sign-In
        gOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        gClient = GoogleSignIn.getClient(requireActivity(), gOptions);

        // Registrar el launcher para el resultado de la actividad de Google
        activityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        handleGoogleSignInResult(result);
                    }
                });
    }

    private void setupEventListeners() {
        // Listener para login normal
        loginButton.setOnClickListener(v -> {
            String email = loginEmail.getText().toString().trim();
            String password = loginPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(getActivity(), "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show();
            } else {
                loginUser(email, password);
            }
        });

        // Listener para Google Sign-In
        if (googleBtn != null) {
            googleBtn.setOnClickListener(v -> {
                Log.d(TAG, "Botón de Google clickeado");
                Toast.makeText(getActivity(), "Iniciando Google Sign-In...", Toast.LENGTH_SHORT).show();
                Intent signInIntent = gClient.getSignInIntent();
                activityResultLauncher.launch(signInIntent);
            });
        }
    }

    private void loginUser(String email, String password) {
        SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String usuarioCorrecto = prefs.getString("usuarioCorrecto", null);
        String passwordCorrecto = prefs.getString("passwordCorrecto", null);

        if (usuarioCorrecto != null && passwordCorrecto != null &&
                email.equals(usuarioCorrecto) && password.equals(passwordCorrecto)) {

            // Login exitoso con registro normal
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("isLoggedIn", true);
            editor.putString("username", email);
            editor.putString("email", email);
            editor.putString("loginMethod", "normal");
            editor.apply();

            Log.d(TAG, "Login exitoso para usuario normal: " + email);
            Toast.makeText(getActivity(), "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show();
            redirigirAMainActivity();

        } else {
            Toast.makeText(getActivity(), "Credenciales incorrectas", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Credenciales incorrectas para: " + email);
        }
    }

    private void handleGoogleSignInResult(ActivityResult result) {
        if (result.getResultCode() == Activity.RESULT_OK) {
            Intent data = result.getData();
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.d(TAG, "Google Sign-In exitoso: " + account.getEmail());
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Log.e(TAG, "Error en Google Sign-In", e);
                Toast.makeText(getActivity(), "Error en Google Sign-In: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.e(TAG, "Google Sign-In cancelado o falló");
            Toast.makeText(getActivity(), "Google Sign-In cancelado", Toast.LENGTH_SHORT).show();
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(requireActivity(), new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Firebase auth exitoso");

                            // Obtener información de la cuenta de Google
                            GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(requireActivity());
                            if (account != null) {
                                // Guardar información en SharedPreferences
                                SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = prefs.edit();
                                editor.putBoolean("isLoggedIn", true);
                                editor.putString("username", account.getDisplayName());
                                editor.putString("email", account.getEmail());
                                editor.putString("loginMethod", "google");
                                editor.apply();

                                // Crear perfil de usuario en Firebase
                                profileManager.crearPerfilDespuesDeAuth(
                                        account.getEmail(),
                                        account.getDisplayName(),
                                        "google",
                                        () -> {
                                            // Éxito
                                            Toast.makeText(getActivity(), "Inicio de sesión exitoso con Google",
                                                    Toast.LENGTH_SHORT).show();
                                            Log.d(TAG, "Perfil de Google creado exitosamente");
                                            redirigirAMainActivity();
                                        },
                                        error -> {
                                            // Error creando perfil, pero continuar
                                            Log.e(TAG, "Error creando perfil de Google, continuar", error);
                                            Toast.makeText(getActivity(), "Inicio de sesión exitoso con Google",
                                                    Toast.LENGTH_SHORT).show();
                                            redirigirAMainActivity();
                                        }
                                );
                            }
                        } else {
                            Log.e(TAG, "Firebase auth falló", task.getException());
                            Toast.makeText(getActivity(), "Error en la autenticación: " +
                                            (task.getException() != null ? task.getException().getMessage() : "Desconocido"),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void redirigirAMainActivity() {
        Intent intent = new Intent(getActivity(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}