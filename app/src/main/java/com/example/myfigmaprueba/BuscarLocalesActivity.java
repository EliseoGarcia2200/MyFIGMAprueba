package com.example.myfigmaprueba;

import android.os.Bundle;

public class BuscarLocalesActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mostrarPantalla(R.layout.activity_main3);
        alTocarReemplazar(R.id.tabProducto, BuscarProductosActivity.class);
        // TODO (paso 4): mapa de Google Maps (mapBusqueda) y lista de locales.
    }
}
