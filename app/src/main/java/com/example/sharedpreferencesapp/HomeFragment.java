package com.example.sharedpreferencesapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class HomeFragment extends Fragment {

    private TextView tvWelcome, tvUserName;
    private ImageView ivSchoolIcon;
    private ProfileManager profileManager;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Inicializar vistas
        tvWelcome = view.findViewById(R.id.tv_welcome);
        tvUserName = view.findViewById(R.id.tv_user_name);
        ivSchoolIcon = view.findViewById(R.id.iv_school_icon);

        profileManager = new ProfileManager(requireContext());

        // Cargar nombre de usuario desde preferencias como respaldo
        SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String username = prefs.getString("username", "Estudiante");
        tvUserName.setText(username);

        // Intentar cargar el perfil completo desde Firestore
        loadUserProfile();
    }

    private void loadUserProfile() {
        profileManager.obtenerPerfilActual(
                profile -> {
                    if (profile != null && isAdded()) { // Verificar que el fragment sigue asociado a una actividad
                        String displayName = profile.getFullName().isEmpty() ?
                                profile.getDisplayName() : profile.getFullName();
                        tvUserName.setText(displayName);
                    }
                },
                error -> {
                    if (isAdded()) { // Verificar que el fragment sigue asociado a una actividad
                        Toast.makeText(requireContext(),
                                "Error al cargar el perfil: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }
}