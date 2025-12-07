package com.example.sharedpreferencesapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.StudentViewHolder> {

    private final List<UserProfile> studentList;
    private final Context context;
    private final StudentActionListener listener;

    public interface StudentActionListener {
        void onApproveClicked(UserProfile student, int position);
        void onViewProtocolClicked(UserProfile student);
        void onStudentClicked(UserProfile student);
        void onMenuClicked(UserProfile student, View view);
    }

    public StudentAdapter(Context context, List<UserProfile> studentList, StudentActionListener listener) {
        this.context = context;
        this.studentList = studentList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public StudentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_student, parent, false);
        return new StudentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StudentViewHolder holder, int position) {
        UserProfile student = studentList.get(position);

        // Nombre del estudiante
        String fullName = student.getFullName().isEmpty() ?
                student.getDisplayName() : student.getFullName();
        holder.tvFullName.setText(fullName);

        // Información: Control | Carrera
        String controlNumber = student.getControlNumber().isEmpty() ? "Sin número" : student.getControlNumber();
        String career = student.getCareer().isEmpty() ? "Sin carrera" : student.getCareer();
        holder.tvInfo.setText(controlNumber + " | " + career);

        // Estado de aprobación
        boolean isApproved = student.isApproved();
        if (isApproved) {
            holder.tvStatus.setText("Aprobado");
            holder.tvStatus.setTextColor(context.getResources().getColor(R.color.status_approved));
            holder.tvApproveAction.setVisibility(View.GONE);
        } else {
            holder.tvStatus.setText("Pendiente");
            holder.tvStatus.setTextColor(context.getResources().getColor(R.color.status_pending));
            holder.tvApproveAction.setVisibility(View.VISIBLE);
        }

        // Fecha - usar createdAt o updatedAt
        String dateText = "";
        if (student.getCreatedAt() != null && !student.getCreatedAt().isEmpty()) {
            try {
                long timestamp = Long.parseLong(student.getCreatedAt());
                Date date = new Date(timestamp);
                SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
                dateText = sdf.format(date);
            } catch (NumberFormatException e) {
                dateText = "Fecha no disponible";
            }
        } else if (student.getUpdatedAt() != null && !student.getUpdatedAt().isEmpty()) {
            try {
                long timestamp = Long.parseLong(student.getUpdatedAt());
                Date date = new Date(timestamp);
                SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
                dateText = sdf.format(date);
            } catch (NumberFormatException e) {
                dateText = "Fecha no disponible";
            }
        } else {
            dateText = "Fecha no disponible";
        }
        holder.tvDate.setText(dateText);

        // Cargar imagen de perfil
        if (student.getProfileImageUrl() != null && !student.getProfileImageUrl().isEmpty()) {
            Picasso.get().load(student.getProfileImageUrl())
                    .placeholder(R.drawable.user)
                    .error(R.drawable.user)
                    .into(holder.ivProfile);
        } else {
            holder.ivProfile.setImageResource(R.drawable.user);
        }

        // Configurar click listeners
        holder.tvApproveAction.setOnClickListener(v -> {
            if (listener != null) {
                listener.onApproveClicked(student, position);
            }
        });

        holder.ivMenu.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMenuClicked(student, v);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onStudentClicked(student);
            }
        });
    }

    @Override
    public int getItemCount() {
        return studentList.size();
    }

    public void updateStudent(UserProfile updatedStudent, int position) {
        studentList.set(position, updatedStudent);
        notifyItemChanged(position);
    }

    public void updateList(List<UserProfile> newList) {
        // Eliminar duplicados basados en userId
        java.util.Set<String> seenIds = new java.util.HashSet<>();
        java.util.List<UserProfile> uniqueList = new java.util.ArrayList<>();
        for (UserProfile student : newList) {
            String userId = student.getUserId();
            if (userId != null && !userId.isEmpty() && !seenIds.contains(userId)) {
                seenIds.add(userId);
                uniqueList.add(student);
            }
        }

        studentList.clear();
        studentList.addAll(uniqueList);
        notifyDataSetChanged();
    }

    static class StudentViewHolder extends RecyclerView.ViewHolder {
        CircleImageView ivProfile;
        TextView tvFullName, tvInfo, tvStatus, tvDate, tvApproveAction;
        ImageView ivMenu;

        public StudentViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProfile = itemView.findViewById(R.id.iv_student_profile);
            tvFullName = itemView.findViewById(R.id.tv_student_name);
            tvInfo = itemView.findViewById(R.id.tv_student_info);
            tvStatus = itemView.findViewById(R.id.tv_student_status);
            tvDate = itemView.findViewById(R.id.tv_student_date);
            tvApproveAction = itemView.findViewById(R.id.tvApproveAction);
            ivMenu = itemView.findViewById(R.id.ivMenu);
        }
    }
}
