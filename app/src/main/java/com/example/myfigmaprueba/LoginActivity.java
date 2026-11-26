package com.example.myfigmaprueba;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import java.util.Locale;

public class LoginActivity extends BaseActivity {

    private EditText etCorreo;
    private EditText etPassword;
    private DBHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getApplication() instanceof DiagnosticoInicio
                && ((DiagnosticoInicio) getApplication()).mostrarInformePendiente(this)) return;

        // Si ya había una sesión guardada, se salta el login.
        if (sesion.haySesion()) {
            irAInicioSegunRol();
            return;
        }

        mostrarPantalla(R.layout.activity_main1);
        db = new DBHelper(this);
        etCorreo = findViewById(R.id.etCorreo);
        etPassword = findViewById(R.id.etPassword);

        alTocar(R.id.tvRegistrate, RegistroActivity.class);
        findViewById(R.id.btnIniciarSesion).setOnClickListener(v -> intentarLogin());
    }

    private void intentarLogin() {
        String correo = etCorreo.getText().toString().trim().toLowerCase(Locale.ROOT);
        String password = etPassword.getText().toString();

        if (correo.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Escribe tu correo y contraseña", Toast.LENGTH_SHORT).show();
            return;
        }

        Usuario usuario = db.iniciarSesion(correo, password);
        if (usuario == null) {
            Toast.makeText(this, "Correo o contraseña incorrectos", Toast.LENGTH_SHORT).show();
            return;
        }

        sesion.iniciar(usuario);
        irAInicioSegunRol();
    }
    @Override protected void onDestroy() { if (db != null) db.close(); super.onDestroy(); }
}
