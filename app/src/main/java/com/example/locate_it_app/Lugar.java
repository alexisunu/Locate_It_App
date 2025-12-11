package com.example.locate_it_app;

public class Lugar {
    private String documentId;
    private String nombre;
    private String descripcion;
    private String categoria;
    private String imagenUrl;
    private double latitud;
    private double longitud;

    // Constructor vacío requerido para Firestore
    public Lugar() {}

    // Constructor principal
    public Lugar(String documentId, String nombre, String descripcion, String categoria, String imagenUrl, double latitud, double longitud) {
        this.documentId = documentId;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.categoria = categoria;
        this.imagenUrl = imagenUrl;
        this.latitud = latitud;
        this.longitud = longitud;
    }

    // Getters y Setters
    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public String getImagenUrl() { return imagenUrl; }
    public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; }

    public double getLatitud() { return latitud; }
    public void setLatitud(double latitud) { this.latitud = latitud; }

    public double getLongitud() { return longitud; }
    public void setLongitud(double longitud) { this.longitud = longitud; }
}
