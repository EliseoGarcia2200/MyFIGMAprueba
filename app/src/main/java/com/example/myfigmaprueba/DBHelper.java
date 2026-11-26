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
import java.util.Locale;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Persistencia local. Las reservas y cambios de inventario son transacciones indivisibles.
 */
public class DBHelper extends SQLiteOpenHelper {

    private static final String DB_NOMBRE = "cero_desperdicio.db";
    private static final int DB_VERSION = 5;
    private static final int ITERACIONES = 10000;

    public DBHelper(Context context) {
        this(context, DB_NOMBRE);
    }

    DBHelper(Context context, String nombre) {
        super(context.getApplicationContext(), nombre, null, DB_VERSION);
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
        crearReservasYPerfiles(db);
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
                + "eliminado INTEGER NOT NULL DEFAULT 0, "
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
        // v5 también repara bases marcadas como v4 cuyo esquema estaba incompleto.
        // La versión por sí sola no garantiza que publicaciones.eliminado exista.
        if (versionVieja < 5) crearReservasYPerfiles(db);
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
        if (!Usuario.ROL_CLIENTE.equals(rol) && !Usuario.ROL_PANADERIA.equals(rol)) return -1;
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
        if (!esRol(p.panaderiaId, Usuario.ROL_PANADERIA) || !publicacionValida(p)) return -1;
        ContentValues valores = valoresDe(p);
        valores.put("creado", System.currentTimeMillis());
        return getWritableDatabase().insert("publicaciones", null, valores);
    }

    /** Actualiza una publicación (solo si pertenece a esa panadería). */
    public boolean actualizarPublicacion(Publicacion p) {
        if (!esRol(p.panaderiaId, Usuario.ROL_PANADERIA) || !publicacionValida(p)) return false;
        int filas = getWritableDatabase().update("publicaciones", valoresDe(p),
                "id = ? AND panaderia_id = ? AND eliminado = 0",
                new String[]{String.valueOf(p.id), String.valueOf(p.panaderiaId)});
        return filas > 0;
    }

    /** Elimina una publicación (solo si pertenece a esa panadería). */
    public boolean eliminarPublicacion(long id, long panaderiaId) {
        if (!esRol(panaderiaId, Usuario.ROL_PANADERIA)) return false;
        ContentValues valores = new ContentValues();
        valores.put("eliminado", 1);
        int filas = getWritableDatabase().update("publicaciones", valores,
                "id = ? AND panaderia_id = ? AND eliminado = 0",
                new String[]{String.valueOf(id), String.valueOf(panaderiaId)});
        return filas > 0;
    }

    public Publicacion obtenerPublicacion(long id, long panaderiaId) {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.query("publicaciones", COLUMNAS_PUBLICACION,
                "id = ? AND panaderia_id = ? AND eliminado = 0",
                new String[]{String.valueOf(id), String.valueOf(panaderiaId)},
                null, null, null)) {
            return c.moveToFirst() ? publicacionDe(c) : null;
        }
    }

    /** Publicaciones de una panadería, las más nuevas primero. filtro puede ir vacío. */
    public List<Publicacion> listarPublicaciones(long panaderiaId, String filtro) {
        expirarPedidos();
        String where = "panaderia_id = ? AND eliminado = 0";
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
                    + "FROM publicaciones p JOIN usuarios u ON u.id = p.panaderia_id AND u.rol = 'panaderia' ";

    /**
     * Productos con existencias de todas las panaderías (las más nuevas primero).
     * panaderiaId > 0 limita a una sola panadería; filtro busca en el nombre del
     * producto o de la panadería y puede ir vacío.
     */
    public List<Publicacion> listarProductosDisponibles(long panaderiaId, String filtro) {
        expirarPedidos();
        StringBuilder sql = new StringBuilder(SELECT_PRODUCTO).append("WHERE p.cantidad > 0 AND p.eliminado = 0");
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
        expirarPedidos();
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.rawQuery(SELECT_PRODUCTO + "WHERE p.id = ? AND p.eliminado = 0",
                new String[]{String.valueOf(id)})) {
            return c.moveToFirst() ? productoDe(c) : null;
        }
    }

    /** Panaderías que tienen al menos un producto con existencias. filtro busca por nombre. */
    public List<Local> listarLocales(String filtro) {
        expirarPedidos();
        StringBuilder sql = new StringBuilder(
                "SELECT u.id, u.nombre, COUNT(p.id) FROM usuarios u "
                        + "JOIN publicaciones p ON p.panaderia_id = u.id "
                        + "WHERE u.rol = ? AND p.cantidad > 0 AND p.eliminado = 0");
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

    // ------------------------------------------------------------- perfiles y pedidos

    private static void crearReservasYPerfiles(SQLiteDatabase db) {
        agregarColumnaSiFalta(db, "publicaciones", "eliminado", "INTEGER NOT NULL DEFAULT 0");
        for (String columna : new String[]{"direccion", "horario", "telefono", "descripcion"}) {
            agregarColumnaSiFalta(db, "usuarios", columna, "TEXT NOT NULL DEFAULT ''");
        }
        agregarColumnaSiFalta(db, "usuarios", "latitud", "REAL");
        agregarColumnaSiFalta(db, "usuarios", "longitud", "REAL");
        db.execSQL("CREATE TABLE IF NOT EXISTS pedidos (id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "cliente_id INTEGER NOT NULL, panaderia_id INTEGER NOT NULL, publicacion_id INTEGER NOT NULL, "
                + "producto TEXT NOT NULL, detalles TEXT NOT NULL, imagen TEXT, cliente TEXT NOT NULL, "
                + "correo_cliente TEXT NOT NULL, panaderia TEXT NOT NULL, direccion TEXT NOT NULL, "
                + "horario TEXT NOT NULL, latitud REAL, longitud REAL, "
                + "precio_centavos INTEGER NOT NULL CHECK(precio_centavos > 0), "
                + "cantidad INTEGER NOT NULL CHECK(cantidad > 0 AND cantidad <= 10000), "
                + "estado TEXT NOT NULL CHECK(estado IN ('reservado','entregado','cancelado','vencido')), "
                + "token TEXT NOT NULL UNIQUE, solicitud TEXT NOT NULL UNIQUE, "
                + "creado INTEGER NOT NULL, vence INTEGER NOT NULL, actualizado INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_pedidos_cliente ON pedidos(cliente_id, creado)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_pedidos_panaderia ON pedidos(panaderia_id, creado)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_pedidos_vencimiento ON pedidos(estado, vence)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_publicaciones_panaderia ON publicaciones(panaderia_id, eliminado, cantidad)");
    }

    public Usuario obtenerUsuario(long id) {
        try (Cursor c = getReadableDatabase().query("usuarios", new String[]{"id", "nombre", "correo", "rol"},
                "id = ?", new String[]{String.valueOf(id)}, null, null, null)) {
            return c.moveToFirst() ? new Usuario(c.getLong(0), c.getString(1), c.getString(2), c.getString(3)) : null;
        }
    }

    public boolean esRol(long id, String rol) {
        Usuario u = obtenerUsuario(id);
        return u != null && rol.equals(u.rol);
    }

    private static boolean publicacionValida(Publicacion p) {
        try {
            Dinero.centavos(p.precio);
            return p.nombre != null && !p.nombre.trim().isEmpty() && p.cantidad >= 0 && p.cantidad <= 10000;
        } catch (IllegalArgumentException e) { return false; }
    }

    public Negocio obtenerNegocio(long id) {
        try (Cursor c = getReadableDatabase().query("usuarios",
                new String[]{"id", "nombre", "correo", "direccion", "horario", "telefono", "descripcion", "latitud", "longitud"},
                "id = ? AND rol = ?", new String[]{String.valueOf(id), Usuario.ROL_PANADERIA}, null, null, null)) {
            if (!c.moveToFirst()) return null;
            return new Negocio(c.getLong(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4),
                    c.getString(5), c.getString(6), c.isNull(7) ? null : c.getDouble(7), c.isNull(8) ? null : c.getDouble(8));
        }
    }

    public boolean guardarNegocio(long id, String direccion, String horario, String telefono,
                                   String descripcion, Double latitud, Double longitud) {
        if ((latitud == null) != (longitud == null)) return false;
        if (latitud != null && (!Double.isFinite(latitud) || !Double.isFinite(longitud)
                || Math.abs(latitud) > 90 || Math.abs(longitud) > 180)) return false;
        if (direccion.trim().isEmpty() || horario.trim().isEmpty()) return false;
        ContentValues v = new ContentValues();
        v.put("direccion", direccion.trim()); v.put("horario", horario.trim());
        v.put("telefono", telefono.trim()); v.put("descripcion", descripcion.trim());
        v.put("latitud", latitud); v.put("longitud", longitud);
        return getWritableDatabase().update("usuarios", v, "id = ? AND rol = ?",
                new String[]{String.valueOf(id), Usuario.ROL_PANADERIA}) == 1;
    }

    /** Un reintento con la misma solicitud devuelve el pedido existente sin descontar otra vez. */
    public long reservar(long clienteId, long productoId, int cantidad, String solicitud) {
        if (cantidad < 1 || cantidad > 10000 || solicitud == null || solicitud.isEmpty())
            throw new IllegalArgumentException("Cantidad no válida");
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            expirarDentroDe(db, System.currentTimeMillis());
            Usuario cliente = obtenerUsuario(clienteId);
            if (cliente == null || !Usuario.ROL_CLIENTE.equals(cliente.rol))
                throw new IllegalArgumentException("Solo los clientes pueden reservar");
            try (Cursor existente = db.query("pedidos", new String[]{"id"}, "solicitud = ? AND cliente_id = ?",
                    new String[]{solicitud, String.valueOf(clienteId)}, null, null, null)) {
                if (existente.moveToFirst()) {
                    long id = existente.getLong(0); db.setTransactionSuccessful(); return id;
                }
            }
            Publicacion producto;
            try (Cursor c = db.rawQuery(SELECT_PRODUCTO + "WHERE p.id = ? AND p.eliminado = 0",
                    new String[]{String.valueOf(productoId)})) {
                if (!c.moveToFirst()) throw new IllegalArgumentException("El producto ya no está disponible");
                producto = productoDe(c);
            }
            if (cantidad > producto.cantidad) throw new IllegalArgumentException("No hay suficientes existencias");
            Negocio negocio = obtenerNegocio(producto.panaderiaId);
            if (negocio == null || !negocio.permiteReservas())
                throw new IllegalArgumentException("La panadería debe completar su dirección y horario antes de recibir reservas");
            long precio = Dinero.centavos(producto.precio);
            ContentValues stock = new ContentValues(); stock.put("cantidad", producto.cantidad - cantidad);
            if (db.update("publicaciones", stock, "id = ? AND eliminado = 0 AND cantidad >= ?",
                    new String[]{String.valueOf(productoId), String.valueOf(cantidad)}) != 1)
                throw new IllegalArgumentException("Las existencias cambiaron; intenta de nuevo");
            long ahora = System.currentTimeMillis();
            ContentValues v = new ContentValues();
            v.put("cliente_id", clienteId); v.put("panaderia_id", producto.panaderiaId);
            v.put("publicacion_id", productoId); v.put("producto", producto.nombre);
            v.put("detalles", producto.detalles); v.put("imagen", producto.imagen);
            v.put("cliente", cliente.nombre); v.put("correo_cliente", cliente.correo);
            v.put("panaderia", negocio.nombre); v.put("direccion", negocio.direccion);
            v.put("horario", negocio.horario); v.put("latitud", negocio.latitud); v.put("longitud", negocio.longitud);
            v.put("precio_centavos", precio); v.put("cantidad", cantidad); v.put("estado", Pedido.RESERVADO);
            byte[] bytes = new byte[8]; new SecureRandom().nextBytes(bytes);
            StringBuilder token = new StringBuilder();
            for (byte b : bytes) token.append(String.format(Locale.ROOT, "%02X", b & 0xff));
            v.put("token", token.toString()); v.put("solicitud", solicitud);
            v.put("creado", ahora); v.put("vence", ahora + 2 * 60 * 60 * 1000L); v.put("actualizado", ahora);
            long id = db.insertOrThrow("pedidos", null, v);
            db.setTransactionSuccessful();
            return id;
        } finally { db.endTransaction(); }
    }

    public List<Pedido> listarPedidos(long usuarioId, String filtro) {
        expirarPedidos();
        boolean vendedor = esRol(usuarioId, Usuario.ROL_PANADERIA);
        String where = (vendedor ? "panaderia_id" : "cliente_id") + " = ?";
        List<String> args = new ArrayList<>(); args.add(String.valueOf(usuarioId));
        if (filtro != null && !filtro.trim().isEmpty()) {
            where += " AND (producto LIKE ? OR cliente LIKE ? OR panaderia LIKE ? OR CAST(id AS TEXT) LIKE ? OR estado LIKE ?)";
            for (int i = 0; i < 5; i++) args.add("%" + filtro.trim() + "%");
        }
        List<Pedido> pedidos = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query("pedidos", null, where, args.toArray(new String[0]),
                null, null, "creado DESC, id DESC")) {
            while (c.moveToNext()) pedidos.add(new Pedido(c));
        }
        return pedidos;
    }

    public Pedido obtenerPedido(long id, long usuarioId) {
        expirarPedidos();
        return consultarPedido(getReadableDatabase(), id, usuarioId);
    }

    private static Pedido consultarPedido(SQLiteDatabase db, long id, long usuarioId) {
        try (Cursor c = db.query("pedidos", null, "id = ? AND (cliente_id = ? OR panaderia_id = ?)",
                new String[]{String.valueOf(id), String.valueOf(usuarioId), String.valueOf(usuarioId)}, null, null, null)) {
            return c.moveToFirst() ? new Pedido(c) : null;
        }
    }

    public boolean cancelarPedido(long id, long usuarioId) {
        SQLiteDatabase db = getWritableDatabase(); db.beginTransaction();
        try {
            long ahora = System.currentTimeMillis(); expirarDentroDe(db, ahora);
            Pedido p = consultarPedido(db, id, usuarioId);
            if (p == null || !Pedido.RESERVADO.equals(p.estado)) { db.setTransactionSuccessful(); return false; }
            cambiarEstado(db, p, Pedido.CANCELADO, ahora, true);
            db.setTransactionSuccessful(); return true;
        } finally { db.endTransaction(); }
    }

    public boolean entregarPedido(long id, long panaderiaId, String codigo) {
        if (!esRol(panaderiaId, Usuario.ROL_PANADERIA) || codigo == null) return false;
        String token = codigo.trim().replace("cerodesperdicio:pedido:", "").replace("-", "").toUpperCase(Locale.ROOT);
        SQLiteDatabase db = getWritableDatabase(); db.beginTransaction();
        try {
            long ahora = System.currentTimeMillis(); expirarDentroDe(db, ahora);
            Pedido p = consultarPedido(db, id, panaderiaId);
            if (p == null || p.panaderiaId != panaderiaId || !Pedido.RESERVADO.equals(p.estado)
                    || !MessageDigest.isEqual(p.token.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8))) {
                db.setTransactionSuccessful(); return false;
            }
            cambiarEstado(db, p, Pedido.ENTREGADO, ahora, false);
            db.setTransactionSuccessful(); return true;
        } finally { db.endTransaction(); }
    }

    public void expirarPedidos() {
        SQLiteDatabase db = getWritableDatabase(); db.beginTransaction();
        try {
            expirarDentroDe(db, System.currentTimeMillis()); db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }

    private static void expirarDentroDe(SQLiteDatabase db, long ahora) {
        List<Pedido> vencidos = new ArrayList<>();
        try (Cursor c = db.query("pedidos", null, "estado = ? AND vence <= ?",
                new String[]{Pedido.RESERVADO, String.valueOf(ahora)}, null, null, null)) {
            while (c.moveToNext()) vencidos.add(new Pedido(c));
        }
        for (Pedido p : vencidos) cambiarEstado(db, p, Pedido.VENCIDO, ahora, true);
    }

    private static void cambiarEstado(SQLiteDatabase db, Pedido p, String estado, long ahora, boolean devolverStock) {
        ContentValues v = new ContentValues(); v.put("estado", estado); v.put("actualizado", ahora);
        if (db.update("pedidos", v, "id = ? AND estado = ?",
                new String[]{String.valueOf(p.id), Pedido.RESERVADO}) == 1 && devolverStock) {
            db.execSQL("UPDATE publicaciones SET cantidad = cantidad + ? WHERE id = ?",
                    new Object[]{p.cantidad, p.publicacionId});
        }
    }

    public boolean imagenEnPedidos(String ruta) {
        if (ruta == null) return false;
        try (Cursor c = getReadableDatabase().query("pedidos", new String[]{"id"}, "imagen = ?",
                new String[]{ruta}, null, null, null, "1")) { return c.moveToFirst(); }
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
