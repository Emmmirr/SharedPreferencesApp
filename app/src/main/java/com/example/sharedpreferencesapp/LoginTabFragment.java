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

    private EditText loginEmail, loginPassword;
    private Button loginButton;
    private GoogleSignInButton googleBtn;

    // Variables para Google Sign-In
    private GoogleSignInOptions gOptions;
    private GoogleSignInClient gClient;
    private FirebaseAuth auth;
    private ActivityResultLauncher<Intent> activityResultLauncher;

    public LoginTabFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login_tab, container, false);

        // Inicializar vistas
        loginEmail = view.findViewById(R.id.login_email);
        loginPassword = view.findViewById(R.id.login_password);
        loginButton = view.findViewById(R.id.login_button);
        googleBtn = view.findViewById(R.id.googleBtn);

        // Verificar si el botón de Google existe
        if (googleBtn == null) {
            Log.e("LoginTabFragment", "GoogleSignInButton no encontrado en el layout");
            Toast.makeText(getContext(), "Error: Botón de Google no encontrado", Toast.LENGTH_SHORT).show();
            return view;
        }

        // Inicializar Firebase Auth
        auth = FirebaseAuth.getInstance();

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
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            Intent data = result.getData();
                            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                            try {
                                GoogleSignInAccount account = task.getResult(ApiException.class);
                                Log.d("LoginTabFragment", "Google Sign-In exitoso: " + account.getEmail());
                                firebaseAuthWithGoogle(account.getIdToken());
                            } catch (ApiException e) {
                                Log.e("LoginTabFragment", "Error en Google Sign-In", e);
                                Toast.makeText(getActivity(), "Error en Google Sign-In: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Log.e("LoginTabFragment", "Google Sign-In cancelado o falló");
                            Toast.makeText(getActivity(), "Google Sign-In cancelado", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

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
        googleBtn.setOnClickListener(v -> {
            Log.d("LoginTabFragment", "Botón de Google clickeado");
            Toast.makeText(getActivity(), "Iniciando Google Sign-In...", Toast.LENGTH_SHORT).show();
            Intent signInIntent = gClient.getSignInIntent();
            activityResultLauncher.launch(signInIntent);
        });

        return view;
    }

    private void loginUser(String email, String password) {
        SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String usuarioCorrecto = prefs.getString("usuarioCorrecto", null);
        String passwordCorrecto = prefs.getString("passwordCorrecto", null);

        if (usuarioCorrecto != null && passwordCorrecto != null &&
                email.equals(usuarioCorrecto) && password.equals(passwordCorrecto)) {

            // Login exitoso
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("isLoggedIn", true);
            editor.putString("username", email);
            editor.putString("loginMethod", "normal");
            editor.apply();

            Intent intent = new Intent(getActivity(), MainActivity.class);
            startActivity(intent);
            requireActivity().finish();
        } else {
            Toast.makeText(getActivity(), "Credenciales incorrectas", Toast.LENGTH_SHORT).show();
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(requireActivity(), new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d("LoginTabFragment", "Firebase auth exitoso");
                            // Guardar información en SharedPreferences
                            GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(requireActivity());
                            if (account != null) {
                                SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = prefs.edit();
                                editor.putBoolean("isLoggedIn", true);
                                editor.putString("username", account.getDisplayName());
                                editor.putString("email", account.getEmail());
                                editor.putString("loginMethod", "google");
                                editor.apply();

                                Toast.makeText(getActivity(), "Inicio de sesión exitoso con Google", Toast.LENGTH_SHORT).show();

                                Intent intent = new Intent(getActivity(), MainActivity.class);
                                startActivity(intent);
                                requireActivity().finish();
                            }
                        } else {
                            Log.e("LoginTabFragment", "Firebase auth falló", task.getException());
                            Toast.makeText(getActivity(), "Error en la autenticación: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}