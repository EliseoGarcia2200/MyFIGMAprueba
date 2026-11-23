package com.example.myfigmaprueba;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class DetalleProductoActivity extends BaseActivity {

    /** Id del producto (publicación) que se muestra. */
    public static final String EXTRA_ID = "producto_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mostrarPantalla(R.layout.activity_main5);

        long id = getIntent().getLongExtra(EXTRA_ID, -1);
        Publicacion producto = new DBHelper(this).obtenerProducto(id);
        if (producto == null || producto.cantidad < 1) {
            Toast.makeText(this, "Este producto ya no está disponible", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ((TextView) findViewById(R.id.tvTituloTop)).setText(producto.nombre);
        ((TextView) findViewById(R.id.tvNombreProducto)).setText(producto.nombre);
        ((TextView) findViewById(R.id.tvSucursal)).setText("Sucursal: " + producto.panaderiaNombre);

        // Solo hay un precio por ahora, así que se oculta el precio anterior tachado.
        findViewById(R.id.tvPrecioAnterior).setVisibility(View.GONE);
        findViewById(R.id.tvFlecha).setVisibility(View.GONE);
        ((TextView) findViewById(R.id.tvPrecioActual))
                .setText(PublicacionAdapter.formatoPrecio(producto.precio));

        ((TextView) findViewById(R.id.tvDescripcionProducto)).setText(
                producto.detalles.isEmpty() ? "Sin descripción." : producto.detalles);
        ((TextView) findViewById(R.id.tvDescripcionProveedor)).setText(
                producto.panaderiaNombre + "\nDisponibles: " + producto.cantidad
                        + (producto.cajaSorpresa ? "\nIncluye caja sorpresa." : ""));

        Bitmap foto = ImagenUtil.cargar(producto.imagen, 600);
        if (foto != null) {
            ImageView imagen = findViewById(R.id.imgProducto);
            imagen.setPadding(0, 0, 0, 0);
            imagen.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imagen.setImageBitmap(foto);
        }

        // TODO (paso 4B): al comprar, guardar el pedido y descontar la cantidad.
        findViewById(R.id.btnComprar).setOnClickListener(v -> Toast.makeText(this,
                "La compra se activa en el siguiente paso", Toast.LENGTH_SHORT).show());
    }
}
