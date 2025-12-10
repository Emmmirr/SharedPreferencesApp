package com.example.sharedpreferencesapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AlumnoAdminAdapter extends RecyclerView.Adapter<AlumnoAdminAdapter.AlumnoViewHolder> {

    private final List<UserProfile> alumnosList;
    private final Context context;

    public AlumnoAdminAdapter(Context context, List<UserProfile> alumnosList) {
        this.context = context;
        this.alumnosList = alumnosList;
    }

    @NonNull
    @Override
    public AlumnoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_alumno_admin, parent, false);
        return new AlumnoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlumnoViewHolder holder, int position) {
        UserProfile alumno = alumnosList.get(position);

        // Nombre del alumno - intentar fullName, luego displayName, luego email, luego "Sin nombre"
        String nombre = "";
        if (alumno.getFullName() != null && !alumno.getFullName().isEmpty()) {
            nombre = alumno.getFullName();
        } else if (alumno.getDisplayName() != null && !alumno.getDisplayName().isEmpty()) {
            nombre = alumno.getDisplayName();
        } else if (alumno.getEmail() != null && !alumno.getEmail().isEmpty()) {
            nombre = alumno.getEmail();
        } else {
            nombre = "Sin nombre";
        }
        holder.tvNombre.setText(nombre);

        // Información: Control | Carrera
        String controlNumber = alumno.getControlNumber() != null && !alumno.getControlNumber().isEmpty() ?
                alumno.getControlNumber() : "Sin número";
        String career = alumno.getCareer() != null && !alumno.getCareer().isEmpty() ?
                alumno.getCareer() : "Sin carrera";
        holder.tvInfo.setText(controlNumber + " | " + career);

        // Email
        String email = alumno.getEmail() != null && !alumno.getEmail().isEmpty() ?
                alumno.getEmail() : "Sin email";
        holder.tvEmail.setText(email);
    }

    @Override
    public int getItemCount() {
        return alumnosList.size();
    }

    static class AlumnoViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView tvNombre;
        TextView tvInfo;
        TextView tvEmail;

        AlumnoViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.card_alumno);
            tvNombre = itemView.findViewById(R.id.tv_nombre);
            tvInfo = itemView.findViewById(R.id.tv_info);
            tvEmail = itemView.findViewById(R.id.tv_email);
        }
    }
}

