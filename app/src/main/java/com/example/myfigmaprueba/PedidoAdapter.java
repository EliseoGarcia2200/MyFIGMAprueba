package com.example.myfigmaprueba;

import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class PedidoAdapter extends RecyclerView.Adapter<PedidoAdapter.Holder> {
    private final List<Pedido> pedidos = new ArrayList<>();
    private final Consumer<Pedido> abrir;
    private final boolean vendedor;
    public PedidoAdapter(boolean vendedor, Consumer<Pedido> abrir) { this.vendedor = vendedor; this.abrir = abrir; }
    public void actualizar(List<Pedido> lista) { pedidos.clear(); pedidos.addAll(lista); notifyDataSetChanged(); }
    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int tipo) {
        LinearLayout root = new LinearLayout(parent.getContext()); root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutParams(new RecyclerView.LayoutParams(-1, -2));
        LinearLayout card = Pantalla.tarjeta(root);
        return new Holder(root, Pantalla.texto(card, "", true), Pantalla.texto(card, "", false));
    }
    @Override public void onBindViewHolder(@NonNull Holder h, int posicion) {
        Pedido p = pedidos.get(posicion);
        h.titulo.setText(p.producto + " · " + p.cantidad + (p.cantidad == 1 ? " unidad" : " unidades"));
        h.detalles.setText(p.folio() + "\n" + (vendedor ? p.cliente : p.panaderia)
                + "\n" + p.estadoTexto() + "\n" + Dinero.formato(p.totalCentavos())
                + "\n" + Pedido.fecha(p.creado) + "\nVer detalles");
        h.titulo.setTextIsSelectable(false); h.detalles.setTextIsSelectable(false);
        h.itemView.setOnClickListener(v -> abrir.accept(p));
        h.titulo.setOnClickListener(v -> abrir.accept(p)); h.detalles.setOnClickListener(v -> abrir.accept(p));
    }
    @Override public int getItemCount() { return pedidos.size(); }
    static final class Holder extends RecyclerView.ViewHolder {
        final TextView titulo, detalles;
        Holder(LinearLayout root, TextView titulo, TextView detalles) { super(root); this.titulo = titulo; this.detalles = detalles; }
    }
}
