package com.example.sharedpreferencesapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

public class PerfilAdminFragment extends Fragment {

    private static final String TAG = "PerfilAdminFragment";
    private TextView tvEmail;
    private MaterialButton btnCerrarSesion;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_perfil_admin, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvEmail = view.findViewById(R.id.tv_email);
        btnCerrarSesion = view.findViewById(R.id.btn_cerrar_sesion);

        // Cargar información del usuario
        cargarInformacionUsuario();

        // Configurar botón de cerrar sesión
        btnCerrarSesion.setOnClickListener(v -> cerrarSesion());
    }

    private void cargarInformacionUsuario() {
        SharedPreferences prefs = getActivity().getSharedPreferences("UserPrefs", getActivity().MODE_PRIVATE);
        String email = prefs.getString("email", "");

        if (!email.isEmpty()) {
            tvEmail.setText(email);
        } else {
            tvEmail.setText("No disponible");
        }
    }

    private void cerrarSesion() {
        Log.d(TAG, "Cerrando sesión de administrador");

        // Limpiar SharedPreferences
        SharedPreferences prefs = getActivity().getSharedPreferences("UserPrefs", getActivity().MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("isLoggedIn", false);
        editor.remove("username");
        editor.remove("email");
        editor.remove("userType");
        editor.apply();

        // Cerrar sesión en Firebase
        FirebaseAuth.getInstance().signOut();

        // Redirigir al login
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        getActivity().finish();
    }
}

