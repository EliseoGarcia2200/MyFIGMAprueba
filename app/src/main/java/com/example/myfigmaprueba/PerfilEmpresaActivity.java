package com.example.myfigmaprueba;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

public class PerfilEmpresaActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mostrarPantalla(R.layout.activity_main10);

        TextView nombre = findViewById(R.id.tvNombreEmpresa);
        nombre.setText(sesion.getNombre());

        alTocar(R.id.tvVerTodas, MisPublicacionesActivity.class);

        // TEMPORAL: el diseño aún no tiene botón de "Cerrar sesión".
        // Mientras tanto, mantén presionado el nombre para cerrar sesión.
        // TODO: pedir a quien hace el diseño un botón y conectarlo a cerrarSesion().
        nombre.setOnLongClickListener(v -> {
            Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show();
            cerrarSesion();
            return true;
        });
    }
}
