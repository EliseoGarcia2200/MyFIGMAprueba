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

    public Publicacion(long id, long panaderiaId, String nombre, double precio,
                       String detalles, int cantidad, boolean cajaSorpresa) {
        this.id = id;
        this.panaderiaId = panaderiaId;
        this.nombre = nombre;
        this.precio = precio;
        this.detalles = detalles;
        this.cantidad = cantidad;
        this.cajaSorpresa = cajaSorpresa;
    }
}
