package com.example.myfigmaprueba;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Dibuja la lista de publicaciones (una tarjeta por cada una). */
public class PublicacionAdapter extends RecyclerView.Adapter<PublicacionAdapter.Holder> {

    public interface OnPublicacionClick {
        void alTocar(Publicacion publicacion);
    }

    private final List<Publicacion> items = new ArrayList<>();
    private final OnPublicacionClick listener;

    public PublicacionAdapter(OnPublicacionClick listener) {
        this.listener = listener;
    }

    public void actualizar(List<Publicacion> nuevas) {
        items.clear();
        items.addAll(nuevas);
        notifyDataSetChanged();
    }

    /** $45 si es entero, $45.50 si tiene centavos. */
    public static String formatoPrecio(double precio) {
        if (precio == Math.floor(precio)) {
            return "$" + (long) precio;
        }
        return String.format(Locale.US, "$%.2f", precio);
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View vista = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_publicacion, parent, false);
        return new Holder(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Publicacion p = items.get(position);
        holder.nombre.setText(p.nombre);
        holder.detalle.setText(formatoPrecio(p.precio) + " · " + p.cantidad
                + (p.cantidad == 1 ? " disponible" : " disponibles"));
        holder.cajaSorpresa.setVisibility(p.cajaSorpresa ? View.VISIBLE : View.GONE);
        holder.itemView.setOnClickListener(v -> listener.alTocar(p));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView nombre;
        final TextView detalle;
        final TextView cajaSorpresa;

        Holder(@NonNull View itemView) {
            super(itemView);
            nombre = itemView.findViewById(R.id.tvNombrePublicacion);
            detalle = itemView.findViewById(R.id.tvDetallePublicacion);
            cajaSorpresa = itemView.findViewById(R.id.tvCajaSorpresa);
        }
    }
}
