package com.example.sharedpreferencesapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

public class HomeFragment extends Fragment {

    private TextView tvWelcome;
    private Button btnLogout;
    private GoogleSignInClient gClient;
    private FirebaseAuth auth;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        tvWelcome = view.findViewById(R.id.tvWelcome);
        btnLogout = view.findViewById(R.id.btnLogOut);

        // Inicializar Firebase Auth y Google Sign-In Client
        auth = FirebaseAuth.getInstance();
        GoogleSignInOptions gOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        gClient = GoogleSignIn.getClient(requireActivity(), gOptions);

        SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String username = prefs.getString("username", "Usuario");
        String loginMethod = prefs.getString("loginMethod", "normal");

        if (loginMethod.equals("google")) {
            tvWelcome.setText("Bienvenido (Google): " + username + "!");
        } else {
            tvWelcome.setText("Bienvenido: " + username + "!");
        }

        btnLogout.setOnClickListener(v -> {
            String method = prefs.getString("loginMethod", "normal");

            if (method.equals("google")) {
                // Cerrar sesión de Google y Firebase
                gClient.signOut().addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        // También cerrar sesión de Firebase
                        auth.signOut();
                        // Revocar acceso para forzar selector de cuenta
                        gClient.revokeAccess().addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                logoutUser(prefs);
                            }
                        });
                    }
                });
            } else {
                // Cerrar sesión normal
                logoutUser(prefs);
            }
        });

        return view;
    }

    private void logoutUser(SharedPreferences prefs) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear(); // Limpiar todas las preferencias
        editor.apply();

        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}