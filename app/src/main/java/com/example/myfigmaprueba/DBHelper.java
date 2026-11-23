package com.example.myfigmaprueba;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Base de datos SQLite de la app. Tablas: usuarios y publicaciones;
 * en los siguientes pasos se agregan pedidos y compras aquí mismo.
 */
public class DBHelper extends SQLiteOpenHelper {

    private static final String DB_NOMBRE = "cero_desperdicio.db";
    private static final int DB_VERSION = 2;
    private static final int ITERACIONES = 10000;

    public DBHelper(Context context) {
        super(context.getApplicationContext(), DB_NOMBRE, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE usuarios ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "nombre TEXT NOT NULL, "
                + "correo TEXT NOT NULL UNIQUE, "
                + "password_hash TEXT NOT NULL, "
                + "salt TEXT NOT NULL, "
                + "rol TEXT NOT NULL, "
                + "creado INTEGER NOT NULL)");
        crearTablaPublicaciones(db);
    }

    private void crearTablaPublicaciones(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE publicaciones ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "panaderia_id INTEGER NOT NULL, "
                + "nombre TEXT NOT NULL, "
                + "precio REAL NOT NULL, "
                + "detalles TEXT NOT NULL DEFAULT '', "
                + "cantidad INTEGER NOT NULL, "
                + "caja_sorpresa INTEGER NOT NULL DEFAULT 0, "
                + "creado INTEGER NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int versionVieja, int versionNueva) {
        // Cada versión nueva agrega lo que le falta SIN borrar los datos existentes.
        if (versionVieja < 2) {
            crearTablaPublicaciones(db);
        }
        // Cuando exista la versión 3 (por ejemplo, imágenes), se agrega aquí abajo:
        // if (versionVieja < 3) { db.execSQL("ALTER TABLE ..."); }
    }

    // ---------------------------------------------------------------- usuarios

    public boolean correoExiste(String correo) {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.query("usuarios", new String[]{"id"}, "correo = ?",
                new String[]{correo}, null, null, null, "1")) {
            return c.moveToFirst();
        }
    }

    /** Guarda un usuario nuevo. Devuelve su id, o -1 si no se pudo guardar. */
    public long registrarUsuario(String nombre, String correo, String password, String rol) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);

        ContentValues valores = new ContentValues();
        valores.put("nombre", nombre);
        valores.put("correo", correo);
        valores.put("password_hash", hashear(password, salt));
        valores.put("salt", Base64.encodeToString(salt, Base64.NO_WRAP));
        valores.put("rol", rol);
        valores.put("creado", System.currentTimeMillis());
        return getWritableDatabase().insert("usuarios", null, valores);
    }

    /** Devuelve el usuario si el correo y la contraseña son correctos; si no, null. */
    public Usuario iniciarSesion(String correo, String password) {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.query("usuarios",
                new String[]{"id", "nombre", "correo", "password_hash", "salt", "rol"},
                "correo = ?", new String[]{correo}, null, null, null)) {
            if (!c.moveToFirst()) return null;

            byte[] salt = Base64.decode(c.getString(4), Base64.NO_WRAP);
            String hashCalculado = hashear(password, salt);
            String hashGuardado = c.getString(3);
            boolean coincide = MessageDigest.isEqual(
                    hashCalculado.getBytes(StandardCharsets.UTF_8),
                    hashGuardado.getBytes(StandardCharsets.UTF_8));
            if (!coincide) return null;

            return new Usuario(c.getLong(0), c.getString(1), c.getString(2), c.getString(5));
        }
    }

    // ---------------------------------------------------------- publicaciones

    /** Guarda una publicación nueva. Devuelve su id, o -1 si no se pudo guardar. */
    public long insertarPublicacion(Publicacion p) {
        ContentValues valores = valoresDe(p);
        valores.put("creado", System.currentTimeMillis());
        return getWritableDatabase().insert("publicaciones", null, valores);
    }

    /** Actualiza una publicación (solo si pertenece a esa panadería). */
    public boolean actualizarPublicacion(Publicacion p) {
        int filas = getWritableDatabase().update("publicaciones", valoresDe(p),
                "id = ? AND panaderia_id = ?",
                new String[]{String.valueOf(p.id), String.valueOf(p.panaderiaId)});
        return filas > 0;
    }

    /** Elimina una publicación (solo si pertenece a esa panadería). */
    public boolean eliminarPublicacion(long id, long panaderiaId) {
        int filas = getWritableDatabase().delete("publicaciones",
                "id = ? AND panaderia_id = ?",
                new String[]{String.valueOf(id), String.valueOf(panaderiaId)});
        return filas > 0;
    }

    public Publicacion obtenerPublicacion(long id, long panaderiaId) {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.query("publicaciones", COLUMNAS_PUBLICACION,
                "id = ? AND panaderia_id = ?",
                new String[]{String.valueOf(id), String.valueOf(panaderiaId)},
                null, null, null)) {
            return c.moveToFirst() ? publicacionDe(c) : null;
        }
    }

    /** Publicaciones de una panadería, las más nuevas primero. filtro puede ir vacío. */
    public List<Publicacion> listarPublicaciones(long panaderiaId, String filtro) {
        String where = "panaderia_id = ?";
        String[] args;
        if (filtro == null || filtro.isEmpty()) {
            args = new String[]{String.valueOf(panaderiaId)};
        } else {
            where += " AND nombre LIKE ?";
            args = new String[]{String.valueOf(panaderiaId), "%" + filtro + "%"};
        }

        List<Publicacion> lista = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.query("publicaciones", COLUMNAS_PUBLICACION, where, args,
                null, null, "creado DESC")) {
            while (c.moveToNext()) {
                lista.add(publicacionDe(c));
            }
        }
        return lista;
    }

    private static final String[] COLUMNAS_PUBLICACION = {
            "id", "panaderia_id", "nombre", "precio", "detalles", "cantidad", "caja_sorpresa"
    };

    private static Publicacion publicacionDe(Cursor c) {
        return new Publicacion(c.getLong(0), c.getLong(1), c.getString(2), c.getDouble(3),
                c.getString(4), c.getInt(5), c.getInt(6) == 1);
    }

    private static ContentValues valoresDe(Publicacion p) {
        ContentValues valores = new ContentValues();
        valores.put("panaderia_id", p.panaderiaId);
        valores.put("nombre", p.nombre);
        valores.put("precio", p.precio);
        valores.put("detalles", p.detalles);
        valores.put("cantidad", p.cantidad);
        valores.put("caja_sorpresa", p.cajaSorpresa ? 1 : 0);
        return valores;
    }

    // ------------------------------------------------------------- contraseñas

    /** Convierte la contraseña en un hash con sal; la contraseña real nunca se guarda. */
    private static String hashear(String password, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERACIONES, 256);
            SecretKeyFactory fabrica = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            byte[] hash = fabrica.generateSecret(spec).getEncoded();
            return Base64.encodeToString(hash, Base64.NO_WRAP);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("No se pudo calcular el hash", e);
        }
    }
}
