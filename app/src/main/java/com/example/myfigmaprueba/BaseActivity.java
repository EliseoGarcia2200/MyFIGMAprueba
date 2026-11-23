package com.example.myfigmaprueba;

import android.content.Intent;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Clase base de todas las pantallas. Aquí vive lo que se repite:
 * márgenes del sistema (edge-to-edge), botón de regresar y barra inferior.
 */
public abstract class BaseActivity extends AppCompatActivity {

    /** Se usa en lugar de setContentView(). Llámalo desde onCreate de cada pantalla. */
    protected void mostrarPantalla(int layoutId) {
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(layoutId);

        View raiz = findViewById(R.id.main);
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
        navegarDesdeBarra(R.id.navInicio, InicioPanaderiaActivity.class);
        navegarDesdeBarra(R.id.navBuscar, BuscarLocalesActivity.class);
        navegarDesdeBarra(R.id.navCompras, MisComprasActivity.class);
        navegarDesdeBarra(R.id.navPerfil, PerfilEmpresaActivity.class);
    }

    private void navegarDesdeBarra(int idBoton, Class<?> destino) {
        View boton = findViewById(idBoton);
        if (boton == null) return;
        boton.setOnClickListener(v -> {
            if (getClass() == destino) return; // ya estamos aquí
            Intent intent = new Intent(this, destino);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
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
