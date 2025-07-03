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

    private EditText signupEmail, signupPassword, signupConfirm;
    private Button signupButton;
    private GoogleSignInButton googleSignupBtn;

    // Variables para Google Sign-In
    private GoogleSignInOptions gOptions;
    private GoogleSignInClient gClient;
    private FirebaseAuth auth;
    private ActivityResultLauncher<Intent> activityResultLauncher;

    public SignupTabFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_signup_tab, container, false);

        // Inicializar vistas
        signupEmail = view.findViewById(R.id.signup_email);
        signupPassword = view.findViewById(R.id.signup_password);
        signupConfirm = view.findViewById(R.id.signup_confirm);
        signupButton = view.findViewById(R.id.signup_button);
        googleSignupBtn = view.findViewById(R.id.googleSignupBtn);

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
                                Log.d("SignupTabFragment", "Google Sign-In exitoso: " + account.getEmail());
                                firebaseAuthWithGoogle(account.getIdToken());
                            } catch (ApiException e) {
                                Log.e("SignupTabFragment", "Error en Google Sign-In", e);
                                Toast.makeText(getActivity(), "Error en Google Sign-In: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Log.e("SignupTabFragment", "Google Sign-In cancelado o falló");
                            Toast.makeText(getActivity(), "Google Sign-In cancelado", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        // Listener para registro normal
        signupButton.setOnClickListener(v -> {
            String email = signupEmail.getText().toString().trim();
            String password = signupPassword.getText().toString().trim();
            String confirmPassword = signupConfirm.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(getContext(), "Complete todos los campos", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirmPassword)) {
                Toast.makeText(getContext(), "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.length() < 4) {
                Toast.makeText(getContext(), "La contraseña debe tener al menos 4 caracteres", Toast.LENGTH_SHORT).show();
                return;
            }

            // Guardar usuario
            SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("usuarioCorrecto", email);
            editor.putString("passwordCorrecto", password);
            editor.apply();

            Toast.makeText(getContext(), "Usuario registrado exitosamente", Toast.LENGTH_SHORT).show();

            // Cambiar al tab de login
            ViewPager2 viewPager = requireActivity().findViewById(R.id.view_pager);
            viewPager.setCurrentItem(0);

            // Limpiar campos
            signupEmail.setText("");
            signupPassword.setText("");
            signupConfirm.setText("");
        });

        // Listener para Google Sign-In (registro)
        googleSignupBtn.setOnClickListener(v -> {
            Log.d("SignupTabFragment", "Botón de Google registro clickeado");
            Toast.makeText(getActivity(), "Registrándose con Google...", Toast.LENGTH_SHORT).show();
            Intent signInIntent = gClient.getSignInIntent();
            activityResultLauncher.launch(signInIntent);
        });

        return view;
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(requireActivity(), new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d("SignupTabFragment", "Firebase auth exitoso");
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

                                Toast.makeText(getActivity(), "Registro exitoso con Google", Toast.LENGTH_SHORT).show();

                                Intent intent = new Intent(getActivity(), MainActivity.class);
                                startActivity(intent);
                                requireActivity().finish();
                            }
                        } else {
                            Log.e("SignupTabFragment", "Firebase auth falló", task.getException());
                            Toast.makeText(getActivity(), "Error en la autenticación: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}