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

public class InformacionAlumnoTabFragment extends Fragment {

    private static final String ARG_STUDENT_ID = "studentId";
    private String studentId;
    private FirebaseFirestore db;

    private TextView tvNombre, tvEmail, tvControl, tvCarrera, tvTelefono, tvFechaNacimiento;

    public static InformacionAlumnoTabFragment newInstance(String studentId) {
        InformacionAlumnoTabFragment fragment = new InformacionAlumnoTabFragment();
        Bundle args = new Bundle();
        args.putString(ARG_STUDENT_ID, studentId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            studentId = getArguments().getString(ARG_STUDENT_ID);
        }
        db = FirebaseFirestore.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_informacion_alumno_tab, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvNombre = view.findViewById(R.id.tv_nombre);
        tvEmail = view.findViewById(R.id.tv_email);
        tvControl = view.findViewById(R.id.tv_control);
        tvCarrera = view.findViewById(R.id.tv_carrera);
        tvTelefono = view.findViewById(R.id.tv_telefono);
        tvFechaNacimiento = view.findViewById(R.id.tv_fecha_nacimiento);

        cargarInformacionAlumno();
    }

    private void cargarInformacionAlumno() {
        if (studentId == null) return;

        db.collection("user_profiles")
                .document(studentId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot doc = task.getResult();
                        if (doc.exists()) {
                            UserProfile profile = UserProfile.fromMap(doc.getData());
                            profile.setUserId(doc.getId());

                            tvNombre.setText(profile.getFullName() != null && !profile.getFullName().isEmpty() ?
                                    profile.getFullName() : "Sin nombre");
                            tvEmail.setText(profile.getEmail() != null && !profile.getEmail().isEmpty() ?
                                    profile.getEmail() : "Sin email");
                            tvControl.setText(profile.getControlNumber() != null && !profile.getControlNumber().isEmpty() ?
                                    profile.getControlNumber() : "Sin número");
                            tvCarrera.setText(profile.getCareer() != null && !profile.getCareer().isEmpty() ?
                                    profile.getCareer() : "Sin carrera");
                            tvTelefono.setText(profile.getPhoneNumber() != null && !profile.getPhoneNumber().isEmpty() ?
                                    profile.getPhoneNumber() : "Sin teléfono");
                            tvFechaNacimiento.setText(profile.getDateOfBirth() != null && !profile.getDateOfBirth().isEmpty() ?
                                    profile.getDateOfBirth() : "Sin fecha");
                        }
                    }
                });
    }
}

