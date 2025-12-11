package com.example.sharedpreferencesapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;

public class PerfilAdminFragment extends Fragment {

    private static final String TAG = "PerfilAdminFragment";

    // Vistas
    private TextView tvDisplayName, tvEmail;
    private View optionLogout;
    private TextView btnLogout;
    private ProfileManager profileManager;
    private UserProfile currentProfile;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_perfil_admin, container, false);

        initializeViews(view);
        setupEventListeners();

        profileManager = new ProfileManager(requireContext());
        cargarPerfilUsuario();

        return view;
    }

    private void initializeViews(View view) {
        tvDisplayName = view.findViewById(R.id.tvDisplayName);
        tvEmail = view.findViewById(R.id.tvEmail);
        optionLogout = view.findViewById(R.id.optionLogout);
        btnLogout = view.findViewById(R.id.btnLogout);
    }

    private void setupEventListeners() {
        // Opción: Cerrar Sesión
        if (optionLogout != null) {
            optionLogout.setOnClickListener(v -> mostrarDialogCerrarSesion());
        }
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> mostrarDialogCerrarSesion());
        }
    }

    private void cargarPerfilUsuario() {
        profileManager.obtenerPerfilActual(
                profile -> {
                    this.currentProfile = profile;
                    actualizarVistasPerfil(profile);
                },
                error -> {
                    Log.e(TAG, "Error al cargar el perfil desde Firestore", error);
                    // Si no hay perfil en Firestore, usar datos de SharedPreferences
                    cargarInformacionDesdeSharedPreferences();
                }
        );
    }

    private void actualizarVistasPerfil(UserProfile profile) {
        if (profile == null || getContext() == null) return;

        // Actualizar header
        String displayName = profile.getFullName().isEmpty() ? profile.getDisplayName() : profile.getFullName();
        if (displayName.isEmpty()) {
            displayName = "Administrador";
        }
        tvDisplayName.setText(displayName);
        tvEmail.setText(profile.getEmail());
    }

    private void cargarInformacionDesdeSharedPreferences() {
        if (getActivity() == null) return;
        android.content.SharedPreferences prefs = getActivity().getSharedPreferences("UserPrefs", android.content.Context.MODE_PRIVATE);
        String email = prefs.getString("email", "");
        String username = prefs.getString("username", "");

        if (!email.isEmpty()) {
            tvEmail.setText(email);
        }
        if (!username.isEmpty()) {
            tvDisplayName.setText(username);
        } else {
            tvDisplayName.setText("Administrador");
        }
    }

    private void mostrarDialogCerrarSesion() {
        new AlertDialog.Builder(getContext())
                .setTitle("Cerrar Sesión")
                .setMessage("¿Estás seguro de que deseas cerrar sesión?")
                .setPositiveButton("Sí", (dialog, which) -> cerrarSesion())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void cerrarSesion() {
        Log.d(TAG, "Cerrando sesión de administrador");

        // Limpiar SharedPreferences
        if (getActivity() != null) {
            android.content.SharedPreferences prefs = getActivity().getSharedPreferences("UserPrefs", android.content.Context.MODE_PRIVATE);
            android.content.SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("isLoggedIn", false);
            editor.remove("username");
            editor.remove("email");
            editor.remove("userType");
            editor.apply();
        }

        // Cerrar sesión en Firebase
        FirebaseAuth.getInstance().signOut();

        // Redirigir al login
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        if (getActivity() != null) {
            getActivity().finish();
        }
    }
}
