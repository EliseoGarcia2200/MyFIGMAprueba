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
    private static final int DB_VERSION = 4;
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
        insertarDatosDePrueba(db);
    }

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
        if (versionVieja < 2) {
            crearTablaPublicaciones(db);
        }
        if (versionVieja < 3) {
            agregarColumnaSiFalta(db, "publicaciones", "imagen", "TEXT");
        }
        if (versionVieja < 4) {
            insertarDatosDePrueba(db);
        }
    }

    // --------------------------------------------------------- datos de prueba

    private void insertarDatosDePrueba(SQLiteDatabase db) {
        try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM publicaciones", null)) {
            if (c.moveToFirst() && c.getInt(0) > 0) return;
        }

        long ahora = System.currentTimeMillis();
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        String saltB64 = Base64.encodeToString(salt, Base64.NO_WRAP);
        String hash = hashear("123456", salt);

        ContentValues p1 = new ContentValues();
        p1.put("nombre", "Panadería Lupita");
        p1.put("correo", "lupita@pan.com");
        p1.put("password_hash", hash);
        p1.put("salt", saltB64);
        p1.put("rol", Usuario.ROL_PANADERIA);
        p1.put("creado", ahora);
        long idLupita = db.insertWithOnConflict("usuarios", null, p1, SQLiteDatabase.CONFLICT_IGNORE);

        ContentValues p2 = new ContentValues();
        p2.put("nombre", "Panadería La Esperanza");
        p2.put("correo", "esperanza@pan.com");
        p2.put("password_hash", hash);
        p2.put("salt", saltB64);
        p2.put("rol", Usuario.ROL_PANADERIA);
        p2.put("creado", ahora);
        long idEsperanza = db.insertWithOnConflict("usuarios", null, p2, SQLiteDatabase.CONFLICT_IGNORE);

        if (idLupita != -1) {
            insertarPubDemo(db, idLupita, "Pan de Oreja (3 pzs)", 25.0, "Horneado hoy por la mañana, crujiente.", 8, 0, ahora);
            insertarPubDemo(db, idLupita, "Caja Sorpresa Dulce", 45.0, "Surtido de 5 piezas de pan dulce del día.", 5, 1, ahora);
        }
        if (idEsperanza != -1) {
            insertarPubDemo(db, idEsperanza, "Conchas de Vainilla (4 pzs)", 30.0, "Excedente del turno vespertino.", 6, 0, ahora);
            insertarPubDemo(db, idEsperanza, "Bolillos Artesanales (6 pzs)", 18.0, "Ideales para tortas o molletes.", 10, 0, ahora);
        }
    }

    private void insertarPubDemo(SQLiteDatabase db, long panaderiaId, String nombre,
                                 double precio, String detalles, int cantidad,
                                 int cajaSorpresa, long creado) {
        ContentValues v = new ContentValues();
        v.put("panaderia_id", panaderiaId);
        v.put("nombre", nombre);
        v.put("precio", precio);
        v.put("detalles", detalles);
        v.put("cantidad", cantidad);
        v.put("caja_sorpresa", cajaSorpresa);
        v.put("imagen", (String) null);
        v.put("creado", creado);
        db.insert("publicaciones", null, v);
    }

    // ---------------------------------------------------------------- usuarios

    public boolean correoExiste(String correo) {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.query("usuarios", new String[]{"id"}, "correo = ?",
                new String[]{correo}, null, null, null, "1")) {
            return c.moveToFirst();
        }
    }

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

    public long insertarPublicacion(Publicacion p) {
        ContentValues valores = valoresDe(p);
        valores.put("creado", System.currentTimeMillis());
        return getWritableDatabase().insert("publicaciones", null, valores);
    }

    public boolean actualizarPublicacion(Publicacion p) {
        int filas = getWritableDatabase().update("publicaciones", valoresDe(p),
                "id = ? AND panaderia_id = ?",
                new String[]{String.valueOf(p.id), String.valueOf(p.panaderiaId)});
        return filas > 0;
    }

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

    public Publicacion obtenerProducto(long id) {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.rawQuery(SELECT_PRODUCTO + "WHERE p.id = ?",
                new String[]{String.valueOf(id)})) {
            return c.moveToFirst() ? productoDe(c) : null;
        }
    }

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