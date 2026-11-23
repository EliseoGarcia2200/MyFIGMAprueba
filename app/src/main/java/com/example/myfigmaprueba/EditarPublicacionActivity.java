package com.example.myfigmaprueba;

import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

/** Sirve para crear una publicación nueva o editar una existente (según el id recibido). */
public class EditarPublicacionActivity extends BaseActivity {

    /** Id de la publicación a editar. Si no se envía, la pantalla crea una nueva. */
    public static final String EXTRA_ID = "publicacion_id";

    private DBHelper db;
    private EditText etNombre;
    private EditText etPrecio;
    private EditText etDetalles;
    private EditText etCantidad;
    private CheckBox cbCajaSorpresa;
    private TextView btnEliminar;

    private long idEditando = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mostrarPantalla(R.layout.activity_main13);

        db = new DBHelper(this);
        etNombre = findViewById(R.id.etNombreProducto);
        etPrecio = findViewById(R.id.etPrecioProducto);
        etDetalles = findViewById(R.id.etDetallesProducto);
        etCantidad = findViewById(R.id.etCantidad);
        cbCajaSorpresa = findViewById(R.id.cbCajaSorpresa);
        btnEliminar = findViewById(R.id.btnEliminar);

        idEditando = getIntent().getLongExtra(EXTRA_ID, -1);
        if (idEditando != -1) {
            Publicacion p = db.obtenerPublicacion(idEditando, sesion.getId());
            if (p == null) {
                Toast.makeText(this, "No se encontró la publicación", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            llenarCampos(p);
            btnEliminar.setOnClickListener(v -> confirmarEliminar());
        } else {
            // Publicación nueva: no hay nada que eliminar, el botón solo cancela.
            btnEliminar.setText("Cancelar");
            btnEliminar.setOnClickListener(v -> finish());
        }

        findViewById(R.id.btnPublicar).setOnClickListener(v -> guardar());
        // TODO (paso 3B): imgSubirProducto / tvAnadirImagen para elegir la foto.
    }

    private void llenarCampos(Publicacion p) {
        etNombre.setText(p.nombre);
        etPrecio.setText(p.precio == Math.floor(p.precio)
                ? String.valueOf((long) p.precio) : String.valueOf(p.precio));
        etDetalles.setText(p.detalles);
        etCantidad.setText(String.valueOf(p.cantidad));
        cbCajaSorpresa.setChecked(p.cajaSorpresa);
    }

    private void guardar() {
        String nombre = etNombre.getText().toString().trim();
        String detalles = etDetalles.getText().toString().trim();

        if (nombre.isEmpty()) {
            etNombre.setError("Escribe el nombre del producto");
            return;
        }

        double precio;
        try {
            precio = Double.parseDouble(etPrecio.getText().toString().trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            etPrecio.setError("Precio no válido");
            return;
        }
        if (precio <= 0) {
            etPrecio.setError("Debe ser mayor a 0");
            return;
        }

        int cantidad;
        try {
            cantidad = Integer.parseInt(etCantidad.getText().toString().trim());
        } catch (NumberFormatException e) {
            etCantidad.setError("Cantidad no válida");
            return;
        }
        if (cantidad < 1) {
            etCantidad.setError("Mínimo 1");
            return;
        }

        long panaderiaId = sesion.getId();
        boolean cajaSorpresa = cbCajaSorpresa.isChecked();

        boolean guardada;
        if (idEditando == -1) {
            guardada = db.insertarPublicacion(new Publicacion(
                    -1, panaderiaId, nombre, precio, detalles, cantidad, cajaSorpresa)) != -1;
        } else {
            guardada = db.actualizarPublicacion(new Publicacion(
                    idEditando, panaderiaId, nombre, precio, detalles, cantidad, cajaSorpresa));
        }

        if (!guardada) {
            Toast.makeText(this, "No se pudo guardar, intenta de nuevo", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, "Publicación guardada", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void confirmarEliminar() {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar publicación")
                .setMessage("¿Seguro que quieres eliminarla? Esta acción no se puede deshacer.")
                .setPositiveButton("Eliminar", (dialogo, cual) -> {
                    db.eliminarPublicacion(idEditando, sesion.getId());
                    Toast.makeText(this, "Publicación eliminada", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
