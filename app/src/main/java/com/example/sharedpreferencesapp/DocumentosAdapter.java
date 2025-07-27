package com.example.sharedpreferencesapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DocumentosAdapter extends RecyclerView.Adapter<DocumentosAdapter.DocumentoViewHolder> {

    public interface OnDocumentoClickListener {
        void onDocumentoClick(Documento documento);
    }

    private List<Documento> documentos;
    private OnDocumentoClickListener listener;

    public DocumentosAdapter(List<Documento> documentos, OnDocumentoClickListener listener) {
        this.documentos = documentos;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DocumentoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_documento, parent, false);
        return new DocumentoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DocumentoViewHolder holder, int position) {
        Documento documento = documentos.get(position);
        holder.tvNombreDocumento.setText(documento.getNombre());

        // Formatear fecha
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        String fechaFormateada = sdf.format(new Date(documento.getFechaSubida()));

        // Mostrar quien subió el documento y cuándo
        String infoSubida = "Subido por: " + documento.getNombreUsuario() + " - " + fechaFormateada;
        holder.tvInfoDocumento.setText(infoSubida);

        // Configurar click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDocumentoClick(documento);
            }
        });
    }

    @Override
    public int getItemCount() {
        return documentos.size();
    }

    static class DocumentoViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombreDocumento, tvInfoDocumento;

        public DocumentoViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombreDocumento = itemView.findViewById(R.id.tvNombreDocumento);
            tvInfoDocumento = itemView.findViewById(R.id.tvInfoDocumento);
        }
    }
}