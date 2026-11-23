package com.example.myfigmaprueba;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/** Lista de panaderías con excedentes que ve el cliente en la pestaña "Local". */
public class LocalAdapter extends RecyclerView.Adapter<LocalAdapter.Holder> {

    public interface OnLocalClick {
        void alTocar(Local local);
    }

    private final List<Local> items = new ArrayList<>();
    private final OnLocalClick listener;

    public LocalAdapter(OnLocalClick listener) {
        this.listener = listener;
    }

    public void actualizar(List<Local> nuevos) {
        items.clear();
        items.addAll(nuevos);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View vista = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_local, parent, false);
        return new Holder(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Local local = items.get(position);
        holder.nombre.setText(local.nombre);
        holder.excedentes.setText("• Con excedentes (" + local.productosDisponibles
                + (local.productosDisponibles == 1 ? " producto)" : " productos)"));

        View.OnClickListener abrir = v -> listener.alTocar(local);
        holder.itemView.setOnClickListener(abrir);
        holder.verMas.setOnClickListener(abrir);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView nombre;
        final TextView excedentes;
        final View verMas;

        Holder(@NonNull View itemView) {
            super(itemView);
            nombre = itemView.findViewById(R.id.tvNombreLocal);
            excedentes = itemView.findViewById(R.id.tvExcedentesLocal);
            verMas = itemView.findViewById(R.id.btnVerMasLocal);
        }
    }
}
