package com.example.myfigmaprueba;

/** Un producto que una panadería publica para vender. */
public class Publicacion {

    public final long id;
    public final long panaderiaId;
    public final String nombre;
    public final double precio;
    public final String detalles;
    public final int cantidad;
    public final boolean cajaSorpresa;
    /** Ruta del archivo de la foto dentro de la app, o null si no tiene foto. */
    public final String imagen;
    /** Nombre de la panadería. Solo viene lleno cuando el cliente consulta productos. */
    public final String panaderiaNombre;

    public Publicacion(long id, long panaderiaId, String nombre, double precio,
                       String detalles, int cantidad, boolean cajaSorpresa, String imagen) {
        this(id, panaderiaId, nombre, precio, detalles, cantidad, cajaSorpresa, imagen, null);
    }

    public Publicacion(long id, long panaderiaId, String nombre, double precio,
                       String detalles, int cantidad, boolean cajaSorpresa, String imagen,
                       String panaderiaNombre) {
        this.id = id;
        this.panaderiaId = panaderiaId;
        this.nombre = nombre;
        this.precio = precio;
        this.detalles = detalles;
        this.cantidad = cantidad;
        this.cajaSorpresa = cajaSorpresa;
        this.imagen = imagen;
        this.panaderiaNombre = panaderiaNombre;
    }
}
