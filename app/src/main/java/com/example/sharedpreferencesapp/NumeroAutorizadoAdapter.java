package com.example.sharedpreferencesapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class NumeroAutorizadoAdapter extends RecyclerView.Adapter<NumeroAutorizadoAdapter.NumeroAutorizadoViewHolder> {

    private final List<String> numerosAutorizadosList;
    private final Context context;
    private final OnEliminarClickListener onEliminarClickListener;

    public interface OnEliminarClickListener {
        void onEliminarClick(String numeroControl);
    }

    public NumeroAutorizadoAdapter(Context context, List<String> numerosAutorizadosList, OnEliminarClickListener listener) {
        this.context = context;
        this.numerosAutorizadosList = numerosAutorizadosList;
        this.onEliminarClickListener = listener;
    }

    @NonNull
    @Override
    public NumeroAutorizadoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_numero_autorizado, parent, false);
        return new NumeroAutorizadoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NumeroAutorizadoViewHolder holder, int position) {
        String numeroControl = numerosAutorizadosList.get(position);
        holder.tvNumeroControl.setText(numeroControl);

        holder.btnEliminar.setOnClickListener(v -> {
            if (onEliminarClickListener != null) {
                onEliminarClickListener.onEliminarClick(numeroControl);
            }
        });
    }

    @Override
    public int getItemCount() {
        return numerosAutorizadosList.size();
    }

    static class NumeroAutorizadoViewHolder extends RecyclerView.ViewHolder {
        TextView tvNumeroControl;
        android.widget.LinearLayout btnEliminar;

        NumeroAutorizadoViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNumeroControl = itemView.findViewById(R.id.tv_numero_control);
            btnEliminar = itemView.findViewById(R.id.btn_eliminar_autorizado);
        }
    }
}

