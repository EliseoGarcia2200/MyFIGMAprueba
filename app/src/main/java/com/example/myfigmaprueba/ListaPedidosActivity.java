package com.example.myfigmaprueba;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public abstract class ListaPedidosActivity extends BaseActivity {
    private PedidoAdapter adapter;
    private EditText filtro;
    private DBHelper db;
    @Override protected void onCreate(Bundle estado) {
        super.onCreate(estado); if (!verificarAcceso()) return;
        mostrarPantalla(R.layout.activity_lista_pedidos);
        ((TextView) findViewById(R.id.tvTituloPedidos)).setText(sesion.esPanaderia() ? "Mis pedidos" : "Mis compras");
        db = new DBHelper(this); filtro = findViewById(R.id.etFiltroPedidos);
        adapter = new PedidoAdapter(sesion.esPanaderia(), p -> startActivity(new Intent(this,
                sesion.esPanaderia() ? DetallePedidoActivity.class : DetalleCompraActivity.class).putExtra(Pedido.EXTRA_ID, p.id)));
        RecyclerView lista = findViewById(R.id.rvPedidos); lista.setLayoutManager(new LinearLayoutManager(this));
        lista.setAdapter(adapter); alCambiarTexto(filtro, this::cargar);
    }
    @Override protected void onResume() { super.onResume(); cargar(); }
    @Override protected void alActualizarTiempo() { cargar(); }
    private void cargar() {
        if (db == null || isFinishing()) return;
        List<Pedido> pedidos = db.listarPedidos(sesion.getId(), filtro.getText().toString()); adapter.actualizar(pedidos);
        TextView vacio = findViewById(R.id.tvListaVacia);
        vacio.setVisibility(pedidos.isEmpty() ? View.VISIBLE : View.GONE);
        vacio.setText(filtro.getText().toString().trim().isEmpty()
                ? (sesion.esPanaderia() ? "Aún no has recibido pedidos." : "Aún no tienes compras. Busca un producto para reservar.")
                : "No hay pedidos que coincidan con la búsqueda.");
    }
    @Override protected void onDestroy() { if (db != null) db.close(); super.onDestroy(); }
}
