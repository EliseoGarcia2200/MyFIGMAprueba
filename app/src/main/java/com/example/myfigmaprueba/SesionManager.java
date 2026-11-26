    package com.example.myfigmaprueba;

    import android.content.Context;
    import android.content.SharedPreferences;

    /** Recuerda quién tiene la sesión iniciada, aunque se cierre la app. */
    public class SesionManager {

        private static final String PREFS = "sesion";
        private static final String K_ID = "usuario_id";
        private static final String K_NOMBRE = "usuario_nombre";
        private static final String K_ROL = "usuario_rol";

        private final SharedPreferences prefs;

        public SesionManager(Context context) {
            prefs = context.getApplicationContext()
                    .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        }

        public void iniciar(Usuario usuario) {
            prefs.edit()
                    .putLong(K_ID, usuario.id)
                    .putString(K_NOMBRE, usuario.nombre)
                    .putString(K_ROL, usuario.rol)
                    .apply();
        }

        public boolean haySesion() {
            return prefs.getLong(K_ID, -1) != -1;
        }

        public long getId() {
            return prefs.getLong(K_ID, -1);
        }

        public String getNombre() {
            return prefs.getString(K_NOMBRE, "");
        }

        public String getRol() {
            return prefs.getString(K_ROL, Usuario.ROL_CLIENTE);
        }

        public boolean esPanaderia() {
            return Usuario.ROL_PANADERIA.equals(getRol());
        }

        public void cerrar() {
            prefs.edit().clear().apply();
        }
    }
