package com.example.myfigmaprueba;

/** Datos de un usuario ya validado (nunca incluye la contraseña). */
public class Usuario {

    public static final String ROL_CLIENTE = "cliente";
    public static final String ROL_PANADERIA = "panaderia";

    public final long id;
    public final String nombre;
    public final String correo;
    public final String rol;

    public Usuario(long id, String nombre, String correo, String rol) {
        this.id = id;
        this.nombre = nombre;
        this.correo = correo;
        this.rol = rol;
    }
}
