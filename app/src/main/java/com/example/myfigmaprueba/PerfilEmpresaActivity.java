package com.example.myfigmaprueba;

import android.os.Bundle;

public class PerfilEmpresaActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mostrarPantalla(R.layout.activity_main10);
        alTocar(R.id.tvVerTodas, MisPublicacionesActivity.class);
        // TODO (paso 3): cargar datos de la panadería desde SQLite.
    }
}
