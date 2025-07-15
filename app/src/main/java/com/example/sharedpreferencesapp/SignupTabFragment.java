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
import androidx.viewpager2.widget.ViewPager2;

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

public class SignupTabFragment extends Fragment {

    private static final String TAG = "SignupTabFragment";

    private EditText signupEmail, signupPassword, signupConfirm;
    private Button signupButton;
    private GoogleSignInButton googleSignupBtn;

    // Variables para Google Sign-In
    private GoogleSignInOptions gOptions;
    private GoogleSignInClient gClient;
    private FirebaseAuth auth;
    private ActivityResultLauncher<Intent> activityResultLauncher;

    // Profile Manager
    private ProfileManager profileManager;

    public SignupTabFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_signup_tab, container, false);

        initializeViews(view);
        initializeAuth();
        setupEventListeners();

        return view;
    }

    private void initializeViews(View view) {
        signupEmail = view.findViewById(R.id.signup_email);
        signupPassword = view.findViewById(R.id.signup_password);
        signupConfirm = view.findViewById(R.id.signup_confirm);
        signupButton = view.findViewById(R.id.signup_button);
        googleSignupBtn = view.findViewById(R.id.googleSignupBtn);
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
                        handleGoogleSignUpResult(result);
                    }
                });
    }

    private void setupEventListeners() {
        // Listener para registro normal
        signupButton.setOnClickListener(v -> {
            String email = signupEmail.getText().toString().trim();
            String password = signupPassword.getText().toString().trim();
            String confirmPassword = signupConfirm.getText().toString().trim();

            if (validateForm(email, password, confirmPassword)) {
                registrarUsuarioNormal(email, password);
            }
        });

        // Listener para Google Sign-In (registro)
        if (googleSignupBtn != null) {
            googleSignupBtn.setOnClickListener(v -> {
                Log.d(TAG, "Botón de Google registro clickeado");
                Toast.makeText(getActivity(), "Registrándose con Google...", Toast.LENGTH_SHORT).show();
                Intent signInIntent = gClient.getSignInIntent();
                activityResultLauncher.launch(signInIntent);
            });
        }
    }

    private boolean validateForm(String email, String password, String confirmPassword) {
        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(getContext(), "Complete todos los campos", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(getContext(), "Ingrese un email válido", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(getContext(), "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (password.length() < 4) {
            Toast.makeText(getContext(), "La contraseña debe tener al menos 4 caracteres", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void registrarUsuarioNormal(String email, String password) {
        // Verificar si el usuario ya existe
        SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String existingUser = prefs.getString("usuarioCorrecto", null);

        if (existingUser != null && existingUser.equals(email)) {
            Toast.makeText(getContext(), "El usuario ya existe. Use el login.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Guardar usuario en SharedPreferences
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("usuarioCorrecto", email);
        editor.putString("passwordCorrecto", password);
        editor.apply();

        Log.d(TAG, "Usuario normal registrado: " + email);
        Toast.makeText(getContext(), "Usuario registrado exitosamente", Toast.LENGTH_SHORT).show();

        // Cambiar al tab de login
        ViewPager2 viewPager = requireActivity().findViewById(R.id.view_pager);
        if (viewPager != null) {
            viewPager.setCurrentItem(0);
        }

        // Limpiar campos
        clearForm();
    }

    private void handleGoogleSignUpResult(ActivityResult result) {
        if (result.getResultCode() == Activity.RESULT_OK) {
            Intent data = result.getData();
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.d(TAG, "Google Sign-Up exitoso: " + account.getEmail());
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Log.e(TAG, "Error en Google Sign-Up", e);
                Toast.makeText(getActivity(), "Error en Google Sign-Up: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.e(TAG, "Google Sign-Up cancelado o falló");
            Toast.makeText(getActivity(), "Google Sign-Up cancelado", Toast.LENGTH_SHORT).show();
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(requireActivity(), new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Firebase auth exitoso para registro");

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
                                            Toast.makeText(getActivity(), "Registro exitoso con Google",
                                                    Toast.LENGTH_SHORT).show();
                                            Log.d(TAG, "Perfil de Google creado exitosamente en registro");
                                            redirigirAMainActivity();
                                        },
                                        error -> {
                                            // Error creando perfil, pero continuar
                                            Log.e(TAG, "Error creando perfil de Google en registro, continuar", error);
                                            Toast.makeText(getActivity(), "Registro exitoso con Google",
                                                    Toast.LENGTH_SHORT).show();
                                            redirigirAMainActivity();
                                        }
                                );
                            }
                        } else {
                            Log.e(TAG, "Firebase auth falló en registro", task.getException());
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

    private void clearForm() {
        signupEmail.setText("");
        signupPassword.setText("");
        signupConfirm.setText("");
    }
}