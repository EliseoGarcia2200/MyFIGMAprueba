package com.example.myfigmaprueba;

import android.os.Bundle;

public class RegistroActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mostrarPantalla(R.layout.activity_main12);
        alTocar(R.id.tvIrLogin, LoginActivity.class);
        // TODO (paso 2): guardar el usuario en SQLite al tocar btnRegistrar.
    }
}
