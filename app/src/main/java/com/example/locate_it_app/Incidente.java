package com.example.locate_it_app;

import com.google.firebase.Timestamp;

public class Incidente {
    private String id;
    private String tipo;
    private String imagenUrl;
    private double latitud;
    private double longitud;
    private String userId;
    private Timestamp fechaDeCreacion;

    // Constructor vacío requerido para Firestore
    public Incidente() {}

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getImagenUrl() { return imagenUrl; }
    public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; }

    public double getLatitud() { return latitud; }
    public void setLatitud(double latitud) { this.latitud = latitud; }

    public double getLongitud() { return longitud; }
    public void setLongitud(double longitud) { this.longitud = longitud; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public Timestamp getFechaDeCreacion() { return fechaDeCreacion; }
    public void setFechaDeCreacion(Timestamp fechaDeCreacion) { this.fechaDeCreacion = fechaDeCreacion; }
}
