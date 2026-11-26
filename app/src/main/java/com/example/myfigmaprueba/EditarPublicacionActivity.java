package com.example.myfigmaprueba;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;

/** Sirve para crear una publicación nueva o editar una existente (según el id recibido). */
public class EditarPublicacionActivity extends BaseActivity {

    /** Id de la publicación a editar. Si no se envía, la pantalla crea una nueva. */
    public static final String EXTRA_ID = "publicacion_id";

    private static final String ESTADO_IMAGEN_NUEVA = "imagen_nueva";

    private DBHelper db;
    private EditText etNombre;
    private EditText etPrecio;
    private EditText etDetalles;
    private EditText etCantidad;
    private CheckBox cbCajaSorpresa;
    private TextView btnEliminar;
    private ImageView imgProducto;
    private TextView tvAnadirImagen;

    private long idEditando = -1;

    /** Foto que ya tenía guardada la publicación (al editar). */
    private String imagenGuardada;
    /** Foto elegida en esta pantalla que todavía no se ha guardado. */
    private String imagenNueva;
    private boolean guardado = false;

    /** Selector de fotos del sistema (no pide permisos). Debe crearse antes de onCreate. */
    private final ActivityResultLauncher<PickVisualMediaRequest> selectorImagen =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(),
                    this::alElegirImagen);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!verificarAcceso()) return;
        mostrarPantalla(R.layout.activity_main13);

        db = new DBHelper(this);
        etNombre = findViewById(R.id.etNombreProducto);
        etPrecio = findViewById(R.id.etPrecioProducto);
        etDetalles = findViewById(R.id.etDetallesProducto);
        etCantidad = findViewById(R.id.etCantidad);
        cbCajaSorpresa = findViewById(R.id.cbCajaSorpresa);
        btnEliminar = findViewById(R.id.btnEliminar);
        imgProducto = findViewById(R.id.imgSubirProducto);
        tvAnadirImagen = findViewById(R.id.tvAnadirImagen);

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

        // Si la pantalla se recreó (por ejemplo, al girar el teléfono), se recupera la foto nueva.
        if (savedInstanceState != null) {
            imagenNueva = savedInstanceState.getString(ESTADO_IMAGEN_NUEVA);
            if (imagenNueva != null) mostrarImagen(imagenNueva);
        }

        View.OnClickListener abrirSelector = v -> selectorImagen.launch(
                new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build());
        imgProducto.setOnClickListener(abrirSelector);
        tvAnadirImagen.setOnClickListener(abrirSelector);

        findViewById(R.id.btnPublicar).setOnClickListener(v -> guardar());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ESTADO_IMAGEN_NUEVA, imagenNueva);
    }

    /** Si se sale sin guardar, la foto nueva que se había copiado se borra para no dejar basura. */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isFinishing() && !guardado) {
            ImagenUtil.borrar(imagenNueva);
        }
        if (db != null) db.close();
    }

    // ------------------------------------------------------------------ fotos

    private void alElegirImagen(Uri uri) {
        if (uri == null) return; // cerró el selector sin elegir

        Context app = getApplicationContext();
        // Reducir y copiar la foto puede tardar, así que se hace fuera del hilo principal.
        new Thread(() -> {
            String ruta = ImagenUtil.guardarDesdeUri(app, uri);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    ImagenUtil.borrar(ruta);
                    return;
                }
                if (ruta == null) {
                    Toast.makeText(this, "No se pudo cargar la imagen", Toast.LENGTH_SHORT).show();
                    return;
                }
                ImagenUtil.borrar(imagenNueva); // reemplaza una elección anterior sin guardar
                imagenNueva = ruta;
                mostrarImagen(ruta);
            });
        }).start();
    }

    private void mostrarImagen(String ruta) {
        Bitmap bitmap = ImagenUtil.cargar(ruta, 600);
        if (bitmap == null) return;
        imgProducto.setPadding(0, 0, 0, 0);
        imgProducto.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imgProducto.setImageBitmap(bitmap);
        tvAnadirImagen.setText("Cambiar imagen");
    }

    // --------------------------------------------------------------- guardado

    private void llenarCampos(Publicacion p) {
        etNombre.setText(p.nombre);
        etPrecio.setText(p.precio == Math.floor(p.precio)
                ? String.valueOf((long) p.precio) : String.valueOf(p.precio));
        etDetalles.setText(p.detalles);
        etCantidad.setText(String.valueOf(p.cantidad));
        cbCajaSorpresa.setChecked(p.cajaSorpresa);

        imagenGuardada = p.imagen;
        if (imagenGuardada != null) mostrarImagen(imagenGuardada);
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
            precio = Dinero.centavos(etPrecio.getText().toString()) / 100.0;
        } catch (IllegalArgumentException e) {
            etPrecio.setError(e.getMessage());
            return;
        }

        int cantidad;
        try {
            cantidad = Integer.parseInt(etCantidad.getText().toString().trim());
        } catch (NumberFormatException e) {
            etCantidad.setError("Cantidad no válida");
            return;
        }
        if (cantidad < 0 || cantidad > 10000) {
            etCantidad.setError("Cantidad entre 0 y 10,000. Usa 0 si está agotado.");
            return;
        }

        long panaderiaId = sesion.getId();
        boolean cajaSorpresa = cbCajaSorpresa.isChecked();
        String imagenFinal = imagenNueva != null ? imagenNueva : imagenGuardada;

        boolean guardada;
        if (idEditando == -1) {
            guardada = db.insertarPublicacion(new Publicacion(
                    -1, panaderiaId, nombre, precio, detalles, cantidad, cajaSorpresa,
                    imagenFinal)) != -1;
        } else {
            guardada = db.actualizarPublicacion(new Publicacion(
                    idEditando, panaderiaId, nombre, precio, detalles, cantidad, cajaSorpresa,
                    imagenFinal));
        }

        if (!guardada) {
            Toast.makeText(this, "No se pudo guardar, intenta de nuevo", Toast.LENGTH_SHORT).show();
            return;
        }

        guardado = true;
        // Si se cambió la foto, la anterior ya no sirve.
        if (imagenNueva != null && imagenGuardada != null && !db.imagenEnPedidos(imagenGuardada)) {
            ImagenUtil.borrar(imagenGuardada);
        }
        Toast.makeText(this, "Publicación guardada", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void confirmarEliminar() {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar publicación")
                .setMessage("¿Seguro que quieres eliminarla? Esta acción no se puede deshacer.")
                .setPositiveButton("Eliminar", (dialogo, cual) -> {
                    if (db.eliminarPublicacion(idEditando, sesion.getId())) {
                        if (!db.imagenEnPedidos(imagenGuardada)) ImagenUtil.borrar(imagenGuardada);
                        Toast.makeText(this, "Publicación eliminada", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(this, "No se pudo eliminar la publicación", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
