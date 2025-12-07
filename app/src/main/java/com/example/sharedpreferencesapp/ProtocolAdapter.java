package com.example.sharedpreferencesapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ProtocolAdapter extends RecyclerView.Adapter<ProtocolAdapter.ProtocolViewHolder> {

    private final List<DocumentSnapshot> protocolList;
    private final Context context;
    private final ProtocolActionListener listener;

    public interface ProtocolActionListener {
        void onViewPdfClicked(DocumentSnapshot protocol);
        void onProtocolClicked(DocumentSnapshot protocol);
        void onMenuClicked(DocumentSnapshot protocol, View view);
    }

    public ProtocolAdapter(Context context, List<DocumentSnapshot> protocolList, ProtocolActionListener listener) {
        this.context = context;
        this.protocolList = new ArrayList<>(protocolList != null ? protocolList : new ArrayList<>());
        this.listener = listener;
    }

    @NonNull
    @Override
    public ProtocolViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_protocolo, parent, false);
        return new ProtocolViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProtocolViewHolder holder, int position) {
        DocumentSnapshot protocol = protocolList.get(position);

        // Nombre del estudiante
        String nombreEstudiante = protocol.getString("nombreEstudiante");
        if (nombreEstudiante == null || nombreEstudiante.isEmpty()) {
            nombreEstudiante = protocol.getString("nombreAlumno");
        }
        if (nombreEstudiante == null || nombreEstudiante.isEmpty()) {
            nombreEstudiante = "Sin nombre";
        }
        holder.tvNombreEstudiante.setText(nombreEstudiante);

        // Número de control
        String numeroControl = protocol.getString("numeroControl");
        if (numeroControl == null || numeroControl.isEmpty()) {
            numeroControl = protocol.getString("numControl");
        }
        if (numeroControl == null || numeroControl.isEmpty()) {
            numeroControl = "Sin número";
        }
        holder.tvNumControl.setText("No. Control: " + numeroControl);

        // Información del proyecto
        String nombreProyecto = protocol.getString("nombreProyecto");
        if (nombreProyecto == null || nombreProyecto.isEmpty()) {
            nombreProyecto = "Sin proyecto";
        }
        holder.tvInfo.setText("Proyecto: " + nombreProyecto);

        // Empresa
        String empresa = protocol.getString("nombreEmpresa");
        if (empresa == null || empresa.isEmpty()) {
            empresa = "Sin empresa";
        }
        holder.tvEmpresa.setText("Empresa: " + empresa);

        // Tipo de proyecto
        String tipoProyecto = protocol.getString("tipoProyecto");
        if (tipoProyecto == null || tipoProyecto.isEmpty()) {
            tipoProyecto = "Sin tipo";
        }
        holder.tvTipoProyecto.setText("Tipo: " + tipoProyecto);

        // Fecha
        String dateText = "Fecha no disponible";
        if (protocol.contains("fechaActualizacion")) {
            try {
                Object fechaObj = protocol.get("fechaActualizacion");
                long timestamp = 0;
                if (fechaObj instanceof Long) {
                    timestamp = (Long) fechaObj;
                } else if (fechaObj instanceof Number) {
                    timestamp = ((Number) fechaObj).longValue();
                }
                if (timestamp > 0) {
                    Date date = new Date(timestamp);
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
                    dateText = "Fecha: " + sdf.format(date);
                }
            } catch (Exception e) {
                dateText = "Fecha: No disponible";
            }
        }
        holder.tvFecha.setText(dateText);

        // Configurar el botón Ver PDF
        holder.btnVerPdf.setOnClickListener(v -> {
            if (listener != null) {
                listener.onViewPdfClicked(protocol);
            }
        });

        // Configurar el texto del botón
        TextView tvButtonText = holder.btnVerPdf.findViewById(R.id.tv_button_text);
        if (tvButtonText != null) {
            tvButtonText.setText("Ver PDF");
        }

        // Configurar el icono del botón (icono de documento)
        ImageView ivButtonIcon = holder.btnVerPdf.findViewById(R.id.iv_button_icon);
        if (ivButtonIcon != null) {
            ivButtonIcon.setImageResource(R.drawable.ic_document);
            ivButtonIcon.setVisibility(View.VISIBLE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onProtocolClicked(protocol);
            }
        });
    }

    @Override
    public int getItemCount() {
        return protocolList.size();
    }

    public void updateList(List<DocumentSnapshot> newList) {
        protocolList.clear();
        if (newList != null && !newList.isEmpty()) {
            protocolList.addAll(new ArrayList<>(newList));
        }
        notifyDataSetChanged();
    }

    static class ProtocolViewHolder extends RecyclerView.ViewHolder {
        LinearLayout btnVerPdf;
        TextView tvNombreEstudiante, tvNumControl, tvInfo, tvEmpresa, tvTipoProyecto, tvFecha;

        public ProtocolViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombreEstudiante = itemView.findViewById(R.id.tv_nombre_estudiante);
            tvNumControl = itemView.findViewById(R.id.tv_num_control);
            tvInfo = itemView.findViewById(R.id.tv_info_protocolo);
            tvEmpresa = itemView.findViewById(R.id.tv_empresa);
            tvTipoProyecto = itemView.findViewById(R.id.tv_tipo_proyecto);
            tvFecha = itemView.findViewById(R.id.tv_fecha);
            btnVerPdf = itemView.findViewById(R.id.btnVerPdf);
        }
    }
}

