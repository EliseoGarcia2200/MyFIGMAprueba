package com.example.myfigmaprueba;

import android.os.Bundle;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import java.util.Locale;

public class RegistroActivity extends BaseActivity {

    private EditText etNombre;
    private EditText etCorreo;
    private EditText etPassword;
    private RadioGroup rgTipoUsuario;
    private DBHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mostrarPantalla(R.layout.activity_main12);

        db = new DBHelper(this);
        etNombre = findViewById(R.id.etRegNombre);
        etCorreo = findViewById(R.id.etRegCorreo);
        etPassword = findViewById(R.id.etRegPassword);
        rgTipoUsuario = findViewById(R.id.rgTipoUsuario);

        // "Inicia sesión" solo regresa al login que ya estaba abajo.
        findViewById(R.id.tvIrLogin).setOnClickListener(v -> finish());
        findViewById(R.id.btnRegistrar).setOnClickListener(v -> intentarRegistro());
    }

    private void intentarRegistro() {
        String nombre = etNombre.getText().toString().trim();
        String correo = etCorreo.getText().toString().trim().toLowerCase(Locale.ROOT);
        String password = etPassword.getText().toString();

        if (nombre.isEmpty()) {
            etNombre.setError("Escribe tu nombre");
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            etCorreo.setError("Correo no válido");
            return;
        }
        if (password.length() < 6) {
            etPassword.setError("Mínimo 6 caracteres");
            return;
        }
        if (db.correoExiste(correo)) {
            etCorreo.setError("Este correo ya está registrado");
            return;
        }

        String rol = rgTipoUsuario.getCheckedRadioButtonId() == R.id.rbPanaderia
                ? Usuario.ROL_PANADERIA : Usuario.ROL_CLIENTE;

        long id = db.registrarUsuario(nombre, correo, password, rol);
        if (id == -1) {
            Toast.makeText(this, "No se pudo crear la cuenta, intenta de nuevo", Toast.LENGTH_SHORT).show();
            return;
        }

        // Cuenta creada: se inicia sesión directo y se manda a su pantalla principal.
        sesion.iniciar(new Usuario(id, nombre, correo, rol));
        Toast.makeText(this, "¡Cuenta creada!", Toast.LENGTH_SHORT).show();
        irAInicioSegunRol();
    }
}
