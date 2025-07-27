package com.example.sharedpreferencesapp;

public class EventoCalendario {
    private String id;
    private String titulo;
    private String descripcion;
    private String fecha;
    private String hora;
    private String carrera;
    private boolean paraTodos;

    // Constructor vacío necesario para Firestore
    public EventoCalendario() {}

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public String getHora() { return hora; }
    public void setHora(String hora) { this.hora = hora; }

    public String getCarrera() { return carrera; }
    public void setCarrera(String carrera) { this.carrera = carrera; }

    public boolean isParaTodos() { return paraTodos; }
    public void setParaTodos(boolean paraTodos) { this.paraTodos = paraTodos; }
}