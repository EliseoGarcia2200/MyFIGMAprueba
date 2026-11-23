package com.example.myfigmaprueba;

/** Una panadería que tiene productos disponibles (lo que ve el cliente en "Local"). */
public class Local {

    public final long id;
    public final String nombre;
    public final int productosDisponibles;

    public Local(long id, String nombre, int productosDisponibles) {
        this.id = id;
        this.nombre = nombre;
        this.productosDisponibles = productosDisponibles;
    }
}
