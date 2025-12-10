package com.example.sharedpreferencesapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Map;

public class BloqueAutorizadoAdapter extends RecyclerView.Adapter<BloqueAutorizadoAdapter.BloqueViewHolder> {

    private final List<Map<String, Object>> bloquesList;
    private final Context context;
    private final OnBloqueClickListener onBloqueClickListener;
    private final OnEliminarClickListener onEliminarClickListener;

    public interface OnBloqueClickListener {
        void onBloqueClick(String bloqueId, String nombreBloque);
    }

    public interface OnEliminarClickListener {
        void onEliminarClick(String bloqueId, String nombreBloque);
    }

    public BloqueAutorizadoAdapter(Context context, List<Map<String, Object>> bloquesList,
                                   OnBloqueClickListener onBloqueClickListener,
                                   OnEliminarClickListener onEliminarClickListener) {
        this.context = context;
        this.bloquesList = bloquesList;
        this.onBloqueClickListener = onBloqueClickListener;
        this.onEliminarClickListener = onEliminarClickListener;
    }

    @NonNull
    @Override
    public BloqueViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_bloque_autorizado, parent, false);
        return new BloqueViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BloqueViewHolder holder, int position) {
        Map<String, Object> bloque = bloquesList.get(position);
        String bloqueId = (String) bloque.get("id");
        String nombreBloque = (String) bloque.get("nombre");
        List<String> numeros = (List<String>) bloque.get("numeros");

        holder.tvNombreBloque.setText(nombreBloque);
        int cantidad = numeros != null ? numeros.size() : 0;
        holder.tvCantidadNumeros.setText(cantidad + " número" + (cantidad != 1 ? "s" : "") + " de control");

        holder.itemView.setOnClickListener(v -> {
            if (onBloqueClickListener != null && bloqueId != null) {
                onBloqueClickListener.onBloqueClick(bloqueId, nombreBloque);
            }
        });

        holder.btnEliminar.setOnClickListener(v -> {
            if (onEliminarClickListener != null && bloqueId != null) {
                onEliminarClickListener.onEliminarClick(bloqueId, nombreBloque);
            }
        });
    }

    @Override
    public int getItemCount() {
        return bloquesList.size();
    }

    static class BloqueViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombreBloque;
        TextView tvCantidadNumeros;
        android.widget.LinearLayout btnEliminar;

        BloqueViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombreBloque = itemView.findViewById(R.id.tv_nombre_bloque);
            tvCantidadNumeros = itemView.findViewById(R.id.tv_cantidad_numeros);
            btnEliminar = itemView.findViewById(R.id.btn_eliminar_bloque);
        }
    }
}

