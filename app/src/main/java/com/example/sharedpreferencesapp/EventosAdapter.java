package com.example.sharedpreferencesapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class EventosAdapter extends RecyclerView.Adapter<EventosAdapter.EventoViewHolder> {

    private List<EventoCalendario> eventos;

    public EventosAdapter(List<EventoCalendario> eventos) {
        this.eventos = eventos;
    }

    @NonNull
    @Override
    public EventoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_evento, parent, false);
        return new EventoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventoViewHolder holder, int position) {
        EventoCalendario evento = eventos.get(position);
        holder.tvTitulo.setText(evento.getTitulo());
        holder.tvDescripcion.setText(evento.getDescripcion());

        String horaTexto = evento.getHora() != null && !evento.getHora().isEmpty()
                ? "Hora: " + evento.getHora()
                : "Todo el día";
        holder.tvHora.setText(horaTexto);
    }

    @Override
    public int getItemCount() {
        return eventos.size();
    }

    static class EventoViewHolder extends RecyclerView.ViewHolder {
        // Usa los nombres exactos de los IDs que tienes en tu XML
        TextView tvTitulo, tvDescripcion, tvHora;

        public EventoViewHolder(@NonNull View itemView) {
            super(itemView);
            // Asegúrate de que estos IDs existan en tu archivo item_evento.xml
            tvTitulo = itemView.findViewById(R.id.tvTitulo);
            tvDescripcion = itemView.findViewById(R.id.tvDescripcion);
            tvHora = itemView.findViewById(R.id.tvHora);
        }
    }
}