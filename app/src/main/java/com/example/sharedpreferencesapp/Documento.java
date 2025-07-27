package com.example.sharedpreferencesapp;

public class Documento {
    private String id;
    private String nombre;
    private String url;
    private String usuarioId;
    private String nombreUsuario;
    private long fechaSubida;
    private boolean esPublico;
    private String tipo;
    private String carrera;
    private String carreraDestino;

    // Constructor vacío necesario para Firestore
    public Documento() {}

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getUsuarioId() { return usuarioId; }
    public void setUsuarioId(String usuarioId) { this.usuarioId = usuarioId; }

    public String getNombreUsuario() { return nombreUsuario; }
    public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }

    public long getFechaSubida() { return fechaSubida; }
    public void setFechaSubida(long fechaSubida) { this.fechaSubida = fechaSubida; }

    public boolean isEsPublico() { return esPublico; }
    public void setEsPublico(boolean esPublico) { this.esPublico = esPublico; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getCarrera() { return carrera; }
    public void setCarrera(String carrera) { this.carrera = carrera; }

    public String getCarreraDestino() { return carreraDestino; }
    public void setCarreraDestino(String carreraDestino) { this.carreraDestino = carreraDestino; }
}