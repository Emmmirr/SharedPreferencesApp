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

public class MaestroAdapter extends RecyclerView.Adapter<MaestroAdapter.MaestroViewHolder> {

    private final List<UserProfile> maestrosList;
    private final Context context;

    public MaestroAdapter(Context context, List<UserProfile> maestrosList) {
        this.context = context;
        this.maestrosList = maestrosList;
    }

    @NonNull
    @Override
    public MaestroViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_maestro, parent, false);
        return new MaestroViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MaestroViewHolder holder, int position) {
        UserProfile maestro = maestrosList.get(position);

        // Nombre del maestro - intentar fullName, luego displayName, luego email, luego "Sin nombre"
        String nombre = "";
        if (maestro.getFullName() != null && !maestro.getFullName().isEmpty()) {
            nombre = maestro.getFullName();
        } else if (maestro.getDisplayName() != null && !maestro.getDisplayName().isEmpty()) {
            nombre = maestro.getDisplayName();
        } else if (maestro.getEmail() != null && !maestro.getEmail().isEmpty()) {
            nombre = maestro.getEmail();
        } else {
            nombre = "Sin nombre";
        }
        holder.tvNombre.setText(nombre);

        // Email
        String email = maestro.getEmail() != null && !maestro.getEmail().isEmpty() ?
                maestro.getEmail() : "Sin email";
        holder.tvEmail.setText(email);
    }

    @Override
    public int getItemCount() {
        return maestrosList.size();
    }

    static class MaestroViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView tvNombre;
        TextView tvEmail;

        MaestroViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.card_maestro);
            tvNombre = itemView.findViewById(R.id.tv_nombre);
            tvEmail = itemView.findViewById(R.id.tv_email);
        }
    }
}

