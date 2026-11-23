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
    private static final int DB_VERSION = 3;
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

    /** Agrega una columna solo si todavía no existe (así la migración nunca falla por repetirse). */
    private static void agregarColumnaSiFalta(SQLiteDatabase db, String tabla,
                                              String columna, String definicion) {
        try (Cursor c = db.rawQuery("PRAGMA table_info(" + tabla + ")", null)) {
            int indiceNombre = c.getColumnIndex("name");
            while (c.moveToNext()) {
                if (columna.equals(c.getString(indiceNombre))) return;
            }
        }
        db.execSQL("ALTER TABLE " + tabla + " ADD COLUMN " + columna + " " + definicion);
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
                + "imagen TEXT, "
                + "creado INTEGER NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int versionVieja, int versionNueva) {
        // Cada versión nueva agrega lo que le falta SIN borrar los datos existentes.
        if (versionVieja < 2) {
            crearTablaPublicaciones(db); // ya trae todas las columnas actuales
        }
        if (versionVieja < 3) {
            // Versión 3: foto de la publicación (ruta del archivo).
            agregarColumnaSiFalta(db, "publicaciones", "imagen", "TEXT");
        }
        // Versión 4 en adelante: agregar aquí abajo con agregarColumnaSiFalta(...).
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
            "id", "panaderia_id", "nombre", "precio", "detalles", "cantidad", "caja_sorpresa", "imagen"
    };

    private static Publicacion publicacionDe(Cursor c) {
        return new Publicacion(c.getLong(0), c.getLong(1), c.getString(2), c.getDouble(3),
                c.getString(4), c.getInt(5), c.getInt(6) == 1, c.getString(7));
    }

    private static ContentValues valoresDe(Publicacion p) {
        ContentValues valores = new ContentValues();
        valores.put("panaderia_id", p.panaderiaId);
        valores.put("nombre", p.nombre);
        valores.put("precio", p.precio);
        valores.put("detalles", p.detalles);
        valores.put("cantidad", p.cantidad);
        valores.put("caja_sorpresa", p.cajaSorpresa ? 1 : 0);
        valores.put("imagen", p.imagen);
        return valores;
    }

    // ------------------------------------------------- lo que ve el cliente

    private static final String SELECT_PRODUCTO =
            "SELECT p.id, p.panaderia_id, p.nombre, p.precio, p.detalles, p.cantidad, "
                    + "p.caja_sorpresa, p.imagen, u.nombre "
                    + "FROM publicaciones p JOIN usuarios u ON u.id = p.panaderia_id ";

    /**
     * Productos con existencias de todas las panaderías (las más nuevas primero).
     * panaderiaId > 0 limita a una sola panadería; filtro busca en el nombre del
     * producto o de la panadería y puede ir vacío.
     */
    public List<Publicacion> listarProductosDisponibles(long panaderiaId, String filtro) {
        StringBuilder sql = new StringBuilder(SELECT_PRODUCTO).append("WHERE p.cantidad > 0");
        List<String> args = new ArrayList<>();
        if (panaderiaId > 0) {
            sql.append(" AND p.panaderia_id = ?");
            args.add(String.valueOf(panaderiaId));
        }
        if (filtro != null && !filtro.isEmpty()) {
            sql.append(" AND (p.nombre LIKE ? OR u.nombre LIKE ?)");
            args.add("%" + filtro + "%");
            args.add("%" + filtro + "%");
        }
        sql.append(" ORDER BY p.creado DESC");

        List<Publicacion> lista = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.rawQuery(sql.toString(), args.toArray(new String[0]))) {
            while (c.moveToNext()) {
                lista.add(productoDe(c));
            }
        }
        return lista;
    }

    /** Un producto con el nombre de su panadería, o null si ya no existe. */
    public Publicacion obtenerProducto(long id) {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.rawQuery(SELECT_PRODUCTO + "WHERE p.id = ?",
                new String[]{String.valueOf(id)})) {
            return c.moveToFirst() ? productoDe(c) : null;
        }
    }

    /** Panaderías que tienen al menos un producto con existencias. filtro busca por nombre. */
    public List<Local> listarLocales(String filtro) {
        StringBuilder sql = new StringBuilder(
                "SELECT u.id, u.nombre, COUNT(p.id) FROM usuarios u "
                        + "JOIN publicaciones p ON p.panaderia_id = u.id "
                        + "WHERE u.rol = ? AND p.cantidad > 0");
        List<String> args = new ArrayList<>();
        args.add(Usuario.ROL_PANADERIA);
        if (filtro != null && !filtro.isEmpty()) {
            sql.append(" AND u.nombre LIKE ?");
            args.add("%" + filtro + "%");
        }
        sql.append(" GROUP BY u.id, u.nombre ORDER BY u.nombre COLLATE NOCASE");

        List<Local> lista = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.rawQuery(sql.toString(), args.toArray(new String[0]))) {
            while (c.moveToNext()) {
                lista.add(new Local(c.getLong(0), c.getString(1), c.getInt(2)));
            }
        }
        return lista;
    }

    /** Nombre de un usuario (la panadería), o null si no existe. */
    public String obtenerNombreUsuario(long id) {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.query("usuarios", new String[]{"nombre"}, "id = ?",
                new String[]{String.valueOf(id)}, null, null, null)) {
            return c.moveToFirst() ? c.getString(0) : null;
        }
    }

    private static Publicacion productoDe(Cursor c) {
        return new Publicacion(c.getLong(0), c.getLong(1), c.getString(2), c.getDouble(3),
                c.getString(4), c.getInt(5), c.getInt(6) == 1, c.getString(7), c.getString(8));
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
