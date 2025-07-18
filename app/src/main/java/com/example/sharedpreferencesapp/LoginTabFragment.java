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
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginTabFragment extends Fragment {

    private static final String TAG = "LoginTabFragment";

    private EditText loginEmail, loginPassword;
    private Button loginButton;
    private GoogleSignInButton googleBtn;

    private GoogleSignInClient gClient;
    private FirebaseAuth auth;
    private ActivityResultLauncher<Intent> activityResultLauncher;
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
        loginButton.setOnClickListener(v -> {
            String email = loginEmail.getText().toString().trim();
            String password = loginPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(getActivity(), "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show();
            } else {
                loginUsuarioConEmail(email, password);
            }
        });

        if (googleBtn != null) {
            googleBtn.setOnClickListener(v -> {
                Intent signInIntent = gClient.getSignInIntent();
                activityResultLauncher.launch(signInIntent);
            });
        }
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
                                    guardarSesionLocalYRedirigir(email, email, "email");
                                },
                                e -> { // onFailure
                                    Log.e(TAG, "Error verificando perfil en Firestore", e);
                                    guardarSesionLocalYRedirigir(email, email, "email");
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
                            // Al hacer login con Google, también verificamos que su perfil exista en Firestore.
                            profileManager.crearOVerificarPerfil(user, user.getDisplayName(), "google",
                                    () -> { // onSuccess
                                        guardarSesionLocalYRedirigir(user.getDisplayName(), user.getEmail(), "google");
                                    },
                                    e -> { // onFailure
                                        Log.e(TAG, "Error verificando perfil de Google en Firestore", e);
                                        guardarSesionLocalYRedirigir(user.getDisplayName(), user.getEmail(), "google");
                                    });
                        }
                    } else {
                        Log.e(TAG, "Firebase auth falló", task.getException());
                        Toast.makeText(getActivity(), "Error en la autenticación.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void guardarSesionLocalYRedirigir(String username, String email, String method) {
        if (getActivity() == null) return;
        SharedPreferences prefs = getActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("isLoggedIn", true);
        editor.putString("username", username);
        editor.putString("email", email);
        editor.putString("loginMethod", method);
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