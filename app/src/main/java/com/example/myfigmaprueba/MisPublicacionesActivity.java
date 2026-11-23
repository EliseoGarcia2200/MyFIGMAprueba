package com.example.myfigmaprueba;

import android.os.Bundle;

public class MisPublicacionesActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mostrarPantalla(R.layout.activity_main11);
        alTocar(R.id.fabAgregarPublicacion, EditarPublicacionActivity.class);
        alTocar(R.id.btnEditar1, EditarPublicacionActivity.class);
        alTocar(R.id.btnEditar2, EditarPublicacionActivity.class);
        alTocar(R.id.btnEditar3, EditarPublicacionActivity.class);
        // TODO (paso 3): reemplazar las 3 publicaciones fijas por un RecyclerView.
    }
}
