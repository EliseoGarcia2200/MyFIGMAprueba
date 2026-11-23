package com.example.myfigmaprueba;

import android.os.Bundle;

public class LoginActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mostrarPantalla(R.layout.activity_main1);
        alTocar(R.id.tvRegistrate, RegistroActivity.class);
        // TODO (paso 2): validar correo y contraseña con SQLite antes de entrar.
        alTocar(R.id.btnIniciarSesion, InicioPanaderiaActivity.class);
    }
}
