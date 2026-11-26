package com.example.myfigmaprueba;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Clase base de todas las pantallas. Aquí vive lo que se repite:
 * márgenes del sistema (edge-to-edge), botón de regresar, barra inferior y sesión.
 */
public abstract class BaseActivity extends AppCompatActivity {

    /** Sesión del usuario actual (disponible desde onCreate en todas las pantallas). */
    protected SesionManager sesion;
    private final Handler refresco = new Handler(Looper.getMainLooper());
    private final Runnable reloj = new Runnable() {
        @Override public void run() {
            if (!isFinishing() && sesion.haySesion()) alActualizarTiempo();
            refresco.postDelayed(this, 30000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sesion = new SesionManager(this);
    }

    /** La pantalla y la base validan el rol; ocultar botones por sí solo no concede permisos. */
    protected boolean verificarAcceso() {
        try (DBHelper db = new DBHelper(this)) {
            Usuario u = db.obtenerUsuario(sesion.getId());
            if (u == null) { cerrarSesion(); return false; }
            sesion.iniciar(u);
        }
        Class<?> pantalla = getClass();
        boolean empresa = pantalla == InicioPanaderiaActivity.class || pantalla == MisPublicacionesActivity.class
                || pantalla == EditarPublicacionActivity.class || pantalla == MisPedidosActivity.class
                || pantalla == DetallePedidoActivity.class;
        boolean cliente = pantalla == MisComprasActivity.class || pantalla == DetalleCompraActivity.class
                || pantalla == QrActivity.class || pantalla == CompraExitosaActivity.class;
        if ((empresa && !sesion.esPanaderia()) || (cliente && sesion.esPanaderia())) {
            Toast.makeText(this, "Esta sección no corresponde a tu tipo de cuenta", Toast.LENGTH_SHORT).show();
            irAInicioSegunRol(); return false;
        }
        return true;
    }

    @Override protected void onStart() {
        super.onStart(); refresco.postDelayed(reloj, 30000);
    }

    @Override protected void onStop() {
        refresco.removeCallbacks(reloj); super.onStop();
    }

    protected void alActualizarTiempo() { }

    /** Se usa en lugar de setContentView(). Llámalo desde onCreate de cada pantalla. */
    protected void mostrarPantalla(int layoutId) {
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(layoutId);

        View raiz = findViewById(android.R.id.content);
        if (raiz != null) {
            ViewCompat.setOnApplyWindowInsetsListener(raiz, (v, insets) -> {
                Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(barras.left, barras.top, barras.right, barras.bottom);
                return insets;
            });
        }

        configurarBotonAtras();
        configurarBarraInferior();
    }

    /** Si la pantalla tiene un botón con id btnBack, regresa a la pantalla anterior. */
    private void configurarBotonAtras() {
        View atras = findViewById(R.id.btnBack);
        if (atras != null) {
            atras.setOnClickListener(v -> finish());
        }
    }

    /** Si la pantalla incluye layout_bottom_nav, conecta sus 4 botones. */
    private void configurarBarraInferior() {
        View inicio = findViewById(R.id.navInicio);
        if (inicio != null) {
            // "Inicio" depende del rol: la panadería ve su panel, el cliente ve el buscador.
            inicio.setOnClickListener(v -> irASeccion(
                    sesion.esPanaderia() ? InicioPanaderiaActivity.class : BuscarLocalesActivity.class));
        }
        navegarDesdeBarra(R.id.navBuscar, BuscarLocalesActivity.class);
        navegarDesdeBarra(R.id.navCompras, sesion.esPanaderia() ? MisPedidosActivity.class : MisComprasActivity.class);
        LinearLayout compras = findViewById(R.id.navCompras);
        if (compras != null) {
            for (int i = 0; i < compras.getChildCount(); i++) {
                if (compras.getChildAt(i) instanceof TextView)
                    ((TextView) compras.getChildAt(i)).setText(sesion.esPanaderia() ? "Pedidos" : "Compras");
            }
        }
        navegarDesdeBarra(R.id.navPerfil, PerfilEmpresaActivity.class);
    }

    private void navegarDesdeBarra(int idBoton, Class<?> destino) {
        View boton = findViewById(idBoton);
        if (boton != null) {
            boton.setOnClickListener(v -> irASeccion(destino));
        }
    }

    private void irASeccion(Class<?> destino) {
        if (getClass() == destino) return; // ya estamos aquí
        Intent intent = new Intent(this, destino);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }

    /** Abre la pantalla principal según el rol y borra el historial (no se puede volver al login). */
    protected void irAInicioSegunRol() {
        Class<?> destino = sesion.esPanaderia()
                ? InicioPanaderiaActivity.class : BuscarLocalesActivity.class;
        Intent intent = new Intent(this, destino);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /** Borra la sesión y regresa al login. */
    protected void cerrarSesion() {
        sesion.cerrar();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /** Ejecuta la acción cada vez que cambia el texto del campo (por ejemplo, para filtrar una lista). */
    protected void alCambiarTexto(EditText campo, Runnable accion) {
        campo.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                accion.run();
            }
        });
    }

    /** Al tocar la vista con ese id, abre la pantalla destino. */
    protected void alTocar(int idVista, Class<?> destino) {
        View vista = findViewById(idVista);
        if (vista != null) {
            vista.setOnClickListener(v -> startActivity(new Intent(this, destino)));
        }
    }

    /** Al tocar la vista con ese id, abre la pantalla destino y cierra la actual. */
    protected void alTocarReemplazar(int idVista, Class<?> destino) {
        View vista = findViewById(idVista);
        if (vista != null) {
            vista.setOnClickListener(v -> {
                startActivity(new Intent(this, destino));
                finish();
            });
        }
    }
}
