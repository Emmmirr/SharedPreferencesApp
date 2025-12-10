package com.example.sharedpreferencesapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class AlumnoAdminAdapter extends RecyclerView.Adapter<AlumnoAdminAdapter.AlumnoViewHolder> {

    private final List<UserProfile> alumnosList;
    private final Context context;
    private final FirebaseManager firebaseManager;
    private OnAlumnoClickListener onAlumnoClickListener;
    private OnEstadoClickListener onEstadoClickListener;

    public interface OnAlumnoClickListener {
        void onAlumnoClick(UserProfile alumno);
    }

    public interface OnEstadoClickListener {
        void onEstadoClick(String studentId);
    }

    public AlumnoAdminAdapter(Context context, List<UserProfile> alumnosList) {
        this.context = context;
        this.alumnosList = alumnosList;
        this.firebaseManager = new FirebaseManager();
    }

    public void setOnAlumnoClickListener(OnAlumnoClickListener listener) {
        this.onAlumnoClickListener = listener;
    }

    public void setOnEstadoClickListener(OnEstadoClickListener listener) {
        this.onEstadoClickListener = listener;
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
        String studentId = alumno.getUserId();

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

        // Cargar y mostrar estado de residencia
        cargarYMostrarEstado(holder, studentId);

        // Click en la card completa
        holder.cardView.setOnClickListener(v -> {
            if (onAlumnoClickListener != null) {
                onAlumnoClickListener.onAlumnoClick(alumno);
            }
        });

        // Click en botón de estado
        holder.btnEditarEstado.setOnClickListener(v -> {
            if (onEstadoClickListener != null && studentId != null) {
                onEstadoClickListener.onEstadoClick(studentId);
            }
        });
    }

    private void cargarYMostrarEstado(AlumnoViewHolder holder, String studentId) {
        if (studentId == null || studentId.isEmpty()) {
            holder.badgeEstado.setText("Sin estado");
            holder.badgeEstado.setBackgroundResource(R.drawable.badge_estado);
            return;
        }

        firebaseManager.cargarEstadoResidencia(studentId, data -> {
            String estatus = (String) data.getOrDefault("estatus", "Candidato");
            holder.badgeEstado.setText(estatus);

            // Cambiar color del badge según el estatus
            int colorResId = R.color.status_pending; // Por defecto
            switch (estatus) {
                case "No elegible":
                    colorResId = R.color.status_cancelled;
                    break;
                case "Candidato":
                    colorResId = R.color.status_pending;
                    break;
                case "En trámite":
                    colorResId = R.color.status_pending;
                    break;
                case "En curso":
                    colorResId = R.color.status_ongoing;
                    break;
                case "Concluida":
                    colorResId = R.color.status_approved;
                    break;
                case "Liberada":
                    colorResId = R.color.status_approved;
                    break;
            }

            // Crear drawable con el color correspondiente
            android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable();
            drawable.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            drawable.setCornerRadius(16 * context.getResources().getDisplayMetrics().density);
            drawable.setColor(context.getColor(colorResId));
            holder.badgeEstado.setBackground(drawable);
        }, error -> {
            holder.badgeEstado.setText("Candidato");
            holder.badgeEstado.setBackgroundResource(R.drawable.badge_estado);
        });
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
        TextView badgeEstado;
        LinearLayout btnEditarEstado;

        AlumnoViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.card_alumno);
            tvNombre = itemView.findViewById(R.id.tv_nombre);
            tvInfo = itemView.findViewById(R.id.tv_info);
            tvEmail = itemView.findViewById(R.id.tv_email);
            badgeEstado = itemView.findViewById(R.id.badge_estado);
            btnEditarEstado = itemView.findViewById(R.id.btn_editar_estado);
        }
    }
}

