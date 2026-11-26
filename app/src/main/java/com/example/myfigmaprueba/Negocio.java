package com.example.myfigmaprueba;

public final class Negocio {
    public final long id;
    public final String nombre, correo, direccion, horario, telefono, descripcion;
    public final Double latitud, longitud;

    public Negocio(long id, String nombre, String correo, String direccion, String horario,
                   String telefono, String descripcion, Double latitud, Double longitud) {
        this.id = id;
        this.nombre = nombre;
        this.correo = correo;
        this.direccion = direccion;
        this.horario = horario;
        this.telefono = telefono;
        this.descripcion = descripcion;
        this.latitud = latitud;
        this.longitud = longitud;
    }

    public boolean tieneUbicacion() { return latitud != null && longitud != null; }
    public boolean permiteReservas() { return !direccion.trim().isEmpty() && !horario.trim().isEmpty(); }
}
