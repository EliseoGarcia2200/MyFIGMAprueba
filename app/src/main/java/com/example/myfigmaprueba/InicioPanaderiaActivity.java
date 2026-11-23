package com.example.myfigmaprueba;

import android.os.Bundle;
import android.widget.TextView;

import java.util.Locale;

public class InicioPanaderiaActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mostrarPantalla(R.layout.activity_main2);

        TextView saludo = findViewById(R.id.tvSaludo);
        saludo.setText("¡HOLA, " + sesion.getNombre().toUpperCase(Locale.getDefault()) + "!");

        alTocar(R.id.btnVerMasPublicaciones, MisPublicacionesActivity.class);
        alTocar(R.id.btnVerMasPedidos, MisPedidosActivity.class);
    }
}
