package com.example.sharedpreferencesapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.StudentViewHolder> {

    private final List<UserProfile> studentList;
    private final Context context;
    private final StudentActionListener listener;

    public interface StudentActionListener {
        void onApproveClicked(UserProfile student, int position);
        void onViewProtocolClicked(UserProfile student);
        void onStudentClicked(UserProfile student);
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

        holder.tvFullName.setText(student.getFullName().isEmpty() ?
                student.getDisplayName() : student.getFullName());
        holder.tvControlNumber.setText(student.getControlNumber());
        holder.tvCareer.setText(student.getCareer());

        // Mostrar estado de aprobación
        if (student.isApproved()) {
            holder.btnApprove.setText("Aprobado");
            holder.btnApprove.setEnabled(false);
        } else {
            holder.btnApprove.setText("Aprobar");
            holder.btnApprove.setEnabled(true);
        }

        // Cargar imagen de perfil
        if (student.getProfileImageUrl() != null && !student.getProfileImageUrl().isEmpty()) {
            Picasso.get().load(student.getProfileImageUrl())
                    .placeholder(R.drawable.user)
                    .into(holder.ivProfile);
        } else {
            holder.ivProfile.setImageResource(R.drawable.user);
        }

        // Configurar botones y click listeners
        holder.btnApprove.setOnClickListener(v -> {
            if (listener != null) {
                listener.onApproveClicked(student, position);
            }
        });

        holder.btnViewProtocol.setOnClickListener(v -> {
            if (listener != null) {
                listener.onViewProtocolClicked(student);
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

    static class StudentViewHolder extends RecyclerView.ViewHolder {
        CircleImageView ivProfile;
        TextView tvFullName, tvControlNumber, tvCareer;
        Button btnApprove, btnViewProtocol;

        public StudentViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProfile = itemView.findViewById(R.id.iv_student_profile);
            tvFullName = itemView.findViewById(R.id.tv_student_name);
            tvControlNumber = itemView.findViewById(R.id.tv_student_control);
            tvCareer = itemView.findViewById(R.id.tv_student_career);
            btnApprove = itemView.findViewById(R.id.btn_approve_student);
            btnViewProtocol = itemView.findViewById(R.id.btn_view_protocol);
        }
    }
}