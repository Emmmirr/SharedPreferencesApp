package com.example.sharedpreferencesapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class DocumentosAlumnoTabFragment extends Fragment {

    private static final String ARG_STUDENT_ID = "studentId";
    private String studentId;

    public static DocumentosAlumnoTabFragment newInstance(String studentId) {
        DocumentosAlumnoTabFragment fragment = new DocumentosAlumnoTabFragment();
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
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_documentos_alumno_tab, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // TODO: Implementar vista de documentos (protocolo, calendarios, trámites)
        TextView tvMensaje = view.findViewById(R.id.tv_mensaje);
        tvMensaje.setText("Vista de documentos del alumno\n(Protocolo, Calendarios, Trámites)");
    }
}

