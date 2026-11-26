package com.example.myfigmaprueba;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

public class InicioPanaderiaActivity extends BaseActivity {

    private LinearLayout contenedorPublicaciones;
    private TextView tvSinPublicaciones;
    private DBHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!verificarAcceso()) return;
        mostrarPantalla(R.layout.activity_main2);

        db = new DBHelper(this);

        TextView saludo = findViewById(R.id.tvSaludo);
        saludo.setText("¡HOLA, " + sesion.getNombre().toUpperCase(Locale.getDefault()) + "!");

        contenedorPublicaciones = findViewById(R.id.contenedorPublicaciones);
        tvSinPublicaciones = findViewById(R.id.tvSinPublicaciones);

        alTocar(R.id.btnVerMasPublicaciones, MisPublicacionesActivity.class);
        alTocar(R.id.btnVerMasPedidos, MisPedidosActivity.class);
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarPublicacionesRecientes();
    }

    private void cargarPublicacionesRecientes() {
        if (db == null || isFinishing()) return;
        cargarPedidosRecientes();
        contenedorPublicaciones.removeAllViews();

        List<Publicacion> lista = db.listarPublicaciones(sesion.getId(), "");

        if (lista == null || lista.isEmpty()) {
            db.insertarPublicacion(new Publicacion(
                    0, sesion.getId(), "Caja Sorpresa Dulce", 45.0,
                    "Surtido de 5 piezas de pan dulce del día.", 5, true, null));
            db.insertarPublicacion(new Publicacion(
                    0, sesion.getId(), "Pan de Oreja (3 pzs)", 25.0,
                    "Horneado hoy por la mañana, crujiente.", 8, false, null));
            lista = db.listarPublicaciones(sesion.getId(), "");
        }

        if (lista == null || lista.isEmpty()) {
            tvSinPublicaciones.setVisibility(View.VISIBLE);
            return;
        }

        tvSinPublicaciones.setVisibility(View.GONE);

        int limite = Math.min(lista.size(), 5);
        for (int i = 0; i < limite; i++) {
            contenedorPublicaciones.addView(crearTarjetaPublicacion(lista.get(i)));
        }
    }

    private View crearTarjetaPublicacion(Publicacion pub) {
        float density = getResources().getDisplayMetrics().density;
        int dp110 = (int) (110 * density);
        int dp14 = (int) (14 * density);
        int dp6 = (int) (6 * density);

        LinearLayout card = new LinearLayout(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                (int) (115 * density),
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMarginEnd(dp14);
        card.setLayoutParams(cardParams);
        card.setOrientation(LinearLayout.VERTICAL);

        ImageView img = new ImageView(this);
        img.setLayoutParams(new LinearLayout.LayoutParams(dp110, dp110));
        img.setBackgroundColor(getResources().getColor(R.color.gris_placeholder));
        img.setScaleType(ImageView.ScaleType.CENTER_CROP);

        if (pub.imagen != null && !pub.imagen.isEmpty()) {
            Bitmap bmp = ImagenUtil.cargar(pub.imagen, 160);
            if (bmp != null) {
                img.setImageBitmap(bmp);
            } else {
                img.setImageResource(android.R.drawable.ic_menu_gallery);
            }
        } else {
            img.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        TextView tvNombre = new TextView(this);
        LinearLayout.LayoutParams txtParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        txtParams.topMargin = dp6;
        tvNombre.setLayoutParams(txtParams);
        tvNombre.setText(pub.nombre);
        tvNombre.setTextColor(getResources().getColor(R.color.texto_oscuro));

        TextView tvPrecio = new TextView(this);
        tvPrecio.setText(String.format(Locale.getDefault(), "$%.2f", pub.precio));
        tvPrecio.setTextColor(getResources().getColor(R.color.texto_oscuro));
        tvPrecio.setTypeface(null, Typeface.BOLD);

        card.addView(img);
        card.addView(tvNombre);
        card.addView(tvPrecio);

        card.setOnClickListener(v -> {
            Intent intent = new Intent(this, EditarPublicacionActivity.class);
            intent.putExtra(EditarPublicacionActivity.EXTRA_ID, pub.id);
            startActivity(intent);
        });

        return card;
    }

    @Override
    protected void alActualizarTiempo() {
        cargarPublicacionesRecientes();
    }

    private void cargarPedidosRecientes() {
        LinearLayout panel = findViewById(R.id.contenedorPedidos);
        panel.removeAllViews();
        List<Pedido> pedidos = db.listarPedidos(sesion.getId(), "");
        findViewById(R.id.tvSinPedidos).setVisibility(pedidos.isEmpty() ? View.VISIBLE : View.GONE);

        float density = getResources().getDisplayMetrics().density;
        int dp110 = (int) (110 * density);
        int dp14 = (int) (14 * density);
        int dp6 = (int) (6 * density);

        for (int i = 0; i < Math.min(5, pedidos.size()); i++) {
            Pedido pedido = pedidos.get(i);

            LinearLayout card = new LinearLayout(this);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    (int) (115 * density),
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            cardParams.setMarginEnd(dp14);
            card.setLayoutParams(cardParams);
            card.setOrientation(LinearLayout.VERTICAL);

            ImageView img = new ImageView(this);
            img.setLayoutParams(new LinearLayout.LayoutParams(dp110, dp110));
            img.setBackgroundColor(getResources().getColor(R.color.gris_placeholder));
            img.setScaleType(ImageView.ScaleType.CENTER_CROP);
            img.setImageResource(android.R.drawable.ic_menu_gallery);

            TextView tvTitulo = new TextView(this);
            LinearLayout.LayoutParams txtParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            txtParams.topMargin = dp6;
            tvTitulo.setLayoutParams(txtParams);
            tvTitulo.setText(pedido.producto);
            tvTitulo.setMaxLines(1);
            tvTitulo.setTextColor(getResources().getColor(R.color.texto_oscuro));

            TextView tvEstrellas = new TextView(this);
            tvEstrellas.setText("☆☆☆☆☆");
            tvEstrellas.setTextColor(getResources().getColor(R.color.texto_oscuro));
            tvEstrellas.setTextSize(13);

            card.addView(img);
            card.addView(tvTitulo);
            card.addView(tvEstrellas);

            card.setOnClickListener(v -> startActivity(
                    new Intent(this, DetallePedidoActivity.class).putExtra(Pedido.EXTRA_ID, pedido.id)));

            panel.addView(card);
        }
    }

    @Override
    protected void onDestroy() {
        if (db != null) db.close();
        super.onDestroy();
    }
}