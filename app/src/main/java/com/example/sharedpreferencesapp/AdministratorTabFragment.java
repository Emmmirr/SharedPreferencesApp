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
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import com.developer.gbuttons.GoogleSignInButton;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class AdministratorTabFragment extends Fragment {

    private static final String TAG = "AdministratorTabFragment";

    // Vista para login
    private View loginView;
    private EditText loginEmail, loginPassword;
    private Button loginButton;
    private GoogleSignInButton googleBtn;
    private TextView tvGoToAdminSignup;

    // Vista para registro
    private View signupView;
    private EditText signupEmail, signupPassword, signupConfirm;
    private Button signupButton;
    private GoogleSignInButton googleSignupBtn;
    private TextView tvGoToAdminLogin;

    private boolean isLoginView = true; // Por defecto mostramos login

    private GoogleSignInClient gClient;
    private FirebaseAuth auth;
    private ActivityResultLauncher<Intent> activityResultLauncher;
    private ProfileManager profileManager;

    public AdministratorTabFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_administrator_tab, container, false);

        initializeViews(view);
        initializeAuth();
        setupEventListeners();

        // Por defecto mostramos login
        showLoginView();

        return view;
    }

    private void initializeViews(View view) {
        // Inicializar las vistas de login y registro
        loginView = view.findViewById(R.id.admin_login_view);
        signupView = view.findViewById(R.id.admin_signup_view);

        // Inicializar campos de login
        loginEmail = view.findViewById(R.id.login_email);
        loginPassword = view.findViewById(R.id.login_password);
        loginButton = view.findViewById(R.id.login_button);
        googleBtn = view.findViewById(R.id.googleBtn);
        tvGoToAdminSignup = view.findViewById(R.id.tv_go_to_admin_signup);

        // Inicializar campos de registro
        signupEmail = view.findViewById(R.id.signup_email);
        signupPassword = view.findViewById(R.id.signup_password);
        signupConfirm = view.findViewById(R.id.signup_confirm);
        signupButton = view.findViewById(R.id.signup_button);
        googleSignupBtn = view.findViewById(R.id.googleSignupBtn);
        tvGoToAdminLogin = view.findViewById(R.id.tv_go_to_admin_login);
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
                this::handleGoogleSignInResult);
    }

    private void setupEventListeners() {
        // Evento de login con email/password
        loginButton.setOnClickListener(v -> {
            String email = loginEmail.getText().toString().trim();
            String password = loginPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(getActivity(), "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show();
            } else {
                loginUsuarioConEmail(email, password);
            }
        });

        // Evento de login con Google
        if (googleBtn != null) {
            googleBtn.setOnClickListener(v -> {
                Intent signInIntent = gClient.getSignInIntent();
                activityResultLauncher.launch(signInIntent);
            });
        }

        // Cambiar a vista de registro
        tvGoToAdminSignup.setOnClickListener(v -> showSignupView());

        // Evento de registro con email/password
        signupButton.setOnClickListener(v -> {
            String email = signupEmail.getText().toString().trim();
            String password = signupPassword.getText().toString().trim();
            String confirmPassword = signupConfirm.getText().toString().trim();

            if (validateAdminForm(email, password, confirmPassword)) {
                registrarUsuarioConEmail(email, password);
            }
        });

        // Evento de registro con Google
        if (googleSignupBtn != null) {
            googleSignupBtn.setOnClickListener(v -> {
                Intent signInIntent = gClient.getSignInIntent();
                activityResultLauncher.launch(signInIntent);
            });
        }

        // Cambiar a vista de login
        tvGoToAdminLogin.setOnClickListener(v -> showLoginView());
    }

    private void showLoginView() {
        isLoginView = true;
        loginView.setVisibility(View.VISIBLE);
        signupView.setVisibility(View.GONE);
    }

    private void showSignupView() {
        isLoginView = false;
        loginView.setVisibility(View.GONE);
        signupView.setVisibility(View.VISIBLE);
    }

    private boolean validateAdminForm(String email, String password, String confirmPassword) {
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

                        // Crear un objeto UserProfile con userType="administrator"
                        UserProfile newProfile = new UserProfile(
                                user.getUid(),
                                email,
                                email,
                                "email"
                        );
                        newProfile.setUserType("administrator");

                        // Después de crear el usuario en Auth, crea su perfil en Firestore
                        profileManager.crearOVerificarPerfil(user, email, "email",
                                () -> { // onSuccess
                                    // Actualizar con el userType específico
                                    profileManager.actualizarPerfilActual(newProfile,
                                            () -> {
                                                Toast.makeText(getContext(), "Registro exitoso como Administrador", Toast.LENGTH_SHORT).show();
                                                guardarSesionLocalYRedirigir(email, email, "email", "administrator");
                                            },
                                            e -> {
                                                Log.e(TAG, "Error actualizando perfil", e);
                                                Toast.makeText(getContext(), "Error creando perfil: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                guardarSesionLocalYRedirigir(email, email, "email", "administrator");
                                            });
                                },
                                e -> { // onFailure
                                    Log.e(TAG, "Error creando perfil en Firestore", e);
                                    Toast.makeText(getContext(), "Registro exitoso (error al crear perfil).", Toast.LENGTH_SHORT).show();
                                    guardarSesionLocalYRedirigir(email, email, "email", "administrator");
                                });
                    } else {
                        Log.w(TAG, "createUserWithEmail:failure", task.getException());
                        Toast.makeText(getContext(), "Error en el registro: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void loginUsuarioConEmail(String email, String password) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity(), task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithEmail:success");
                        FirebaseUser user = auth.getCurrentUser();
                        // Al hacer login, también verificamos que su perfil exista en Firestore.
                        profileManager.crearOVerificarPerfil(user, email, "email",
                                () -> { // onSuccess
                                    guardarSesionLocalYRedirigir(email, email, "email", "administrator");
                                },
                                e -> { // onFailure
                                    Log.e(TAG, "Error verificando perfil en Firestore", e);
                                    guardarSesionLocalYRedirigir(email, email, "email", "administrator");
                                });
                    } else {
                        Log.w(TAG, "signInWithEmail:failure", task.getException());
                        Toast.makeText(getContext(), "Credenciales incorrectas.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void handleGoogleSignInResult(ActivityResult result) {
        if (result.getResultCode() == Activity.RESULT_OK) {
            Intent data = result.getData();
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Log.e(TAG, "Error en Google Sign-In", e);
                Toast.makeText(getActivity(), "Error en Google Sign-In: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(requireActivity(), task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            if (!isLoginView) {
                                // Es registro, crear perfil como administrator
                                UserProfile newProfile = new UserProfile(
                                        user.getUid(),
                                        user.getEmail(),
                                        user.getDisplayName() != null ? user.getDisplayName() : user.getEmail(),
                                        "google"
                                );
                                newProfile.setUserType("administrator");

                                profileManager.crearOVerificarPerfil(user, user.getDisplayName(), "google",
                                        () -> { // onSuccess
                                            profileManager.actualizarPerfilActual(newProfile,
                                                    () -> {
                                                        Toast.makeText(getContext(), "Registro exitoso como Administrador", Toast.LENGTH_SHORT).show();
                                                        guardarSesionLocalYRedirigir(user.getDisplayName(), user.getEmail(), "google", "administrator");
                                                    },
                                                    e -> {
                                                        Log.e(TAG, "Error actualizando perfil", e);
                                                        guardarSesionLocalYRedirigir(user.getDisplayName(), user.getEmail(), "google", "administrator");
                                                    });
                                        },
                                        e -> { // onFailure
                                            Log.e(TAG, "Error creando/verificando perfil de Google", e);
                                            guardarSesionLocalYRedirigir(user.getDisplayName(), user.getEmail(), "google", "administrator");
                                        });
                            } else {
                                // Es login, verificar perfil existente
                                profileManager.crearOVerificarPerfil(user, user.getDisplayName(), "google",
                                        () -> { // onSuccess
                                            guardarSesionLocalYRedirigir(user.getDisplayName(), user.getEmail(), "google", "administrator");
                                        },
                                        e -> { // onFailure
                                            Log.e(TAG, "Error verificando perfil de Google en Firestore", e);
                                            guardarSesionLocalYRedirigir(user.getDisplayName(), user.getEmail(), "google", "administrator");
                                        });
                            }
                        }
                    } else {
                        Log.e(TAG, "Firebase auth falló", task.getException());
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
        editor.putString("userType", userType);
        editor.apply();

        redirigirAAdminMainActivity();
    }

    private void redirigirAAdminMainActivity() {
        if (getActivity() == null) return;
        Intent intent = new Intent(getActivity(), AdminMainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        getActivity().finish();
    }
}

