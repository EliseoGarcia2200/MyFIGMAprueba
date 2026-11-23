package com.example.myfigmaprueba;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/** Lista de productos que ve el cliente (tarjeta con foto, sucursal y disponibilidad). */
public class ProductoAdapter extends RecyclerView.Adapter<ProductoAdapter.Holder> {

    public interface OnProductoClick {
        void alTocar(Publicacion producto);
    }

    /** Con esta cantidad o menos, el producto se marca como "Por agotarse". */
    private static final int LIMITE_POR_AGOTARSE = 3;
    private static final int LADO_MINIATURA = 160;

    private final List<Publicacion> items = new ArrayList<>();
    private final OnProductoClick listener;

    public ProductoAdapter(OnProductoClick listener) {
        this.listener = listener;
    }

    public void actualizar(List<Publicacion> nuevos) {
        items.clear();
        items.addAll(nuevos);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View vista = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_producto, parent, false);
        return new Holder(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Publicacion p = items.get(position);
        holder.nombre.setText(p.nombre);
        holder.sucursal.setText("Sucursal: " + p.panaderiaNombre);

        boolean porAgotarse = p.cantidad <= LIMITE_POR_AGOTARSE;
        holder.estado.setText(porAgotarse ? "• Por agotarse" : "• Disponible");
        int colorFondo = porAgotarse ? R.color.amarillo_fondo : R.color.verde_fondo;
        int colorTexto = porAgotarse ? R.color.amarillo_texto : R.color.verde_texto;
        holder.estado.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(holder.itemView.getContext(), colorFondo)));
        holder.estado.setTextColor(
                ContextCompat.getColor(holder.itemView.getContext(), colorTexto));

        Miniaturas.mostrar(holder.imagen, p.imagen, LADO_MINIATURA);

        View.OnClickListener abrir = v -> listener.alTocar(p);
        holder.itemView.setOnClickListener(abrir);
        holder.verDetalles.setOnClickListener(abrir);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ImageView imagen;
        final TextView nombre;
        final TextView sucursal;
        final TextView estado;
        final View verDetalles;

        Holder(@NonNull View itemView) {
            super(itemView);
            imagen = itemView.findViewById(R.id.imgProductoItem);
            nombre = itemView.findViewById(R.id.tvNombreItem);
            sucursal = itemView.findViewById(R.id.tvSucursalItem);
            estado = itemView.findViewById(R.id.tvEstadoItem);
            verDetalles = itemView.findViewById(R.id.btnVerDetallesItem);
        }
    }
}
