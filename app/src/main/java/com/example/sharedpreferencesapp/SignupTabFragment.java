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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class SignupTabFragment extends Fragment {

    private static final String TAG = "SignupTabFragment";

    private EditText signupEmail, signupPassword, signupConfirm;
    private Button signupButton;
    private GoogleSignInButton googleSignupBtn;

    private GoogleSignInClient gClient;
    private FirebaseAuth auth;
    private ActivityResultLauncher<Intent> activityResultLauncher;

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
        auth = FirebaseAuth.getInstance();
        profileManager = new ProfileManager(requireContext());

        GoogleSignInOptions gOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        gClient = GoogleSignIn.getClient(requireActivity(), gOptions);

        activityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                this::handleGoogleSignUpResult);
    }

    private void setupEventListeners() {
        signupButton.setOnClickListener(v -> {
            String email = signupEmail.getText().toString().trim();
            String password = signupPassword.getText().toString().trim();
            String confirmPassword = signupConfirm.getText().toString().trim();

            if (validateForm(email, password, confirmPassword)) {
                registrarUsuarioConEmail(email, password);
            }
        });

        if (googleSignupBtn != null) {
            googleSignupBtn.setOnClickListener(v -> {
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
        if (password.length() < 6) {
            Toast.makeText(getContext(), "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void registrarUsuarioConEmail(String email, String password) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity(), task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "createUserWithEmail:success");
                        FirebaseUser user = auth.getCurrentUser();

                        // Crear un objeto UserProfile con userType="admin"
                        UserProfile newProfile = new UserProfile(
                                user.getUid(),
                                email,
                                email,
                                "email"
                        );
                        newProfile.setUserType("admin");

                        // Después de crear el usuario en Auth, crea su perfil en Firestore
                        profileManager.crearOVerificarPerfil(user, email, "email",
                                () -> { // onSuccess
                                    // Actualizar con el userType específico
                                    profileManager.actualizarPerfilActual(newProfile,
                                            () -> {
                                                Toast.makeText(getContext(), "Registro exitoso como Administrador", Toast.LENGTH_SHORT).show();
                                                guardarSesionLocalYRedirigir(email, email, "email", "admin");
                                            },
                                            e -> {
                                                Log.e(TAG, "Error actualizando perfil", e);
                                                Toast.makeText(getContext(), "Error creando perfil: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                guardarSesionLocalYRedirigir(email, email, "email", "admin");
                                            });
                                },
                                e -> { // onFailure
                                    Log.e(TAG, "Error creando perfil en Firestore", e);
                                    Toast.makeText(getContext(), "Registro exitoso (error al crear perfil).", Toast.LENGTH_SHORT).show();
                                    guardarSesionLocalYRedirigir(email, email, "email", "admin");
                                });
                    } else {
                        Log.w(TAG, "createUserWithEmail:failure", task.getException());
                        Toast.makeText(getContext(), "Error en el registro: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void handleGoogleSignUpResult(ActivityResult result) {
        if (result.getResultCode() == Activity.RESULT_OK) {
            Intent data = result.getData();
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Log.e(TAG, "Error en Google Sign-Up", e);
                Toast.makeText(getActivity(), "Error en Google Sign-Up: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.e(TAG, "Google Sign-Up cancelado o falló");
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(requireActivity(), task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            // Crear un objeto UserProfile con userType="admin" para Google
                            UserProfile newProfile = new UserProfile(
                                    user.getUid(),
                                    user.getEmail(),
                                    user.getDisplayName() != null ? user.getDisplayName() : user.getEmail(),
                                    "google"
                            );
                            newProfile.setUserType("admin");

                            // Después de autenticar con Google, crea/verifica su perfil en Firestore
                            profileManager.crearOVerificarPerfil(user, user.getDisplayName(), "google",
                                    () -> { // onSuccess
                                        // Actualizar con el userType específico
                                        profileManager.actualizarPerfilActual(newProfile,
                                                () -> {
                                                    Toast.makeText(getContext(), "Registro exitoso como Administrador", Toast.LENGTH_SHORT).show();
                                                    guardarSesionLocalYRedirigir(user.getDisplayName(), user.getEmail(), "google", "admin");
                                                },
                                                e -> {
                                                    Log.e(TAG, "Error actualizando perfil", e);
                                                    guardarSesionLocalYRedirigir(user.getDisplayName(), user.getEmail(), "google", "admin");
                                                });
                                    },
                                    e -> { // onFailure
                                        Log.e(TAG, "Error creando/verificando perfil de Google", e);
                                        guardarSesionLocalYRedirigir(user.getDisplayName(), user.getEmail(), "google", "admin");
                                    });
                        }
                    } else {
                        Log.e(TAG, "Firebase auth falló en registro", task.getException());
                        Toast.makeText(getActivity(), "Error en la autenticación.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void guardarSesionLocalYRedirigir(String username, String email, String method, String userType) {
        if (getActivity() == null) return;
        SharedPreferences prefs = getActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("isLoggedIn", true);
        editor.putString("username", username);
        editor.putString("email", email);
        editor.putString("loginMethod", method);
        editor.putString("userType", userType); // Guardar el tipo de usuario (admin)
        editor.apply();

        redirigirAMainActivity();
    }

    private void redirigirAMainActivity() {
        if (getActivity() == null) return;
        Intent intent = new Intent(getActivity(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        getActivity().finish();
    }
}