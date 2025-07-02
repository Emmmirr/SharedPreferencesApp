package com.example.sharedpreferencesapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

public class LoginTabFragment extends Fragment {

    private EditText loginEmail, loginPassword;  // Cambié los nombres
    private Button loginButton;

    public LoginTabFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login_tab, container, false);

        // Usar los IDs correctos del layout original
        loginEmail = view.findViewById(R.id.login_email);        // Este es el ID correcto
        loginPassword = view.findViewById(R.id.login_password);  // Este es el ID correcto
        loginButton = view.findViewById(R.id.login_button);      // Este es el ID correcto

        loginButton.setOnClickListener(v -> {
            String email = loginEmail.getText().toString().trim();      // Cambié a email
            String password = loginPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(getContext(), "Complete todos los campos", Toast.LENGTH_SHORT).show();
                return;
            }

            // Verificar credenciales
            SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
            String usuarioCorrecto = prefs.getString("usuarioCorrecto", null);
            String passwordCorrecto = prefs.getString("passwordCorrecto", null);

            if (usuarioCorrecto != null && passwordCorrecto != null &&
                    email.equals(usuarioCorrecto) && password.equals(passwordCorrecto)) {

                // Login exitoso
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("isLoggedIn", true);
                editor.putString("username", email);  // Cambié a email
                editor.apply();

                Intent intent = new Intent(getActivity(), MainActivity.class);
                startActivity(intent);
                requireActivity().finish();
            } else {
                Toast.makeText(getContext(), "Credenciales incorrectas", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }
}