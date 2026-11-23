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

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Base de datos SQLite de la app. Por ahora solo tiene la tabla de usuarios;
 * en los siguientes pasos se agregan publicaciones, pedidos y compras aquí mismo.
 */
public class DBHelper extends SQLiteOpenHelper {

    private static final String DB_NOMBRE = "cero_desperdicio.db";
    private static final int DB_VERSION = 1;
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
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int versionVieja, int versionNueva) {
        // Solo para desarrollo: al subir DB_VERSION se borra todo y se recrea.
        // TODO antes de entregar: cambiar por migraciones que conserven los datos.
        db.execSQL("DROP TABLE IF EXISTS usuarios");
        onCreate(db);
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
