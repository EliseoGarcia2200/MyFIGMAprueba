package com.example.myfigmaprueba;

import android.os.Bundle;

public class InicioPanaderiaActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mostrarPantalla(R.layout.activity_main2);
        alTocar(R.id.btnVerMasPublicaciones, MisPublicacionesActivity.class);
        alTocar(R.id.btnVerMasPedidos, MisPedidosActivity.class);
        // TODO (paso 2): mostrar el nombre real en tvSaludo.
    }
}
