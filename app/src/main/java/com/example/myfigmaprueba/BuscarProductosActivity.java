package com.example.myfigmaprueba;

import android.os.Bundle;

public class BuscarProductosActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mostrarPantalla(R.layout.activity_main4);
        alTocarReemplazar(R.id.tabLocal, BuscarLocalesActivity.class);
        // TODO (paso 4): lista de productos con RecyclerView.
    }
}
