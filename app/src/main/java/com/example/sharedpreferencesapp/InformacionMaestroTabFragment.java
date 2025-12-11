package com.example.sharedpreferencesapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class InformacionMaestroTabFragment extends Fragment {

    private static final String ARG_MAESTRO_ID = "maestroId";
    private String maestroId;
    private FirebaseFirestore db;

    private TextView tvNombre, tvEmail, tvTelefono;

    public static InformacionMaestroTabFragment newInstance(String maestroId) {
        InformacionMaestroTabFragment fragment = new InformacionMaestroTabFragment();
        Bundle args = new Bundle();
        args.putString(ARG_MAESTRO_ID, maestroId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            maestroId = getArguments().getString(ARG_MAESTRO_ID);
        }
        db = FirebaseFirestore.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_informacion_maestro_tab, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvNombre = view.findViewById(R.id.tv_nombre);
        tvEmail = view.findViewById(R.id.tv_email);
        tvTelefono = view.findViewById(R.id.tv_telefono);

        cargarInformacionMaestro();
    }

    private void cargarInformacionMaestro() {
        if (maestroId == null) return;

        db.collection("user_profiles")
                .document(maestroId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot doc = task.getResult();
                        if (doc.exists()) {
                            UserProfile profile = UserProfile.fromMap(doc.getData());
                            profile.setUserId(doc.getId());

                            tvNombre.setText(profile.getFullName() != null && !profile.getFullName().isEmpty() ?
                                    profile.getFullName() :
                                    (profile.getDisplayName() != null && !profile.getDisplayName().isEmpty() ?
                                            profile.getDisplayName() : "Sin nombre"));
                            tvEmail.setText(profile.getEmail() != null && !profile.getEmail().isEmpty() ?
                                    profile.getEmail() : "Sin email");
                            tvTelefono.setText(profile.getPhoneNumber() != null && !profile.getPhoneNumber().isEmpty() ?
                                    profile.getPhoneNumber() : "Sin teléfono");
                        }
                    }
                });
    }
}

