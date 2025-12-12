package com.example.locate_it_app;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class IncidenteAdapter extends RecyclerView.Adapter<IncidenteAdapter.IncidenteViewHolder> {

    private Context context;
    private List<Incidente> incidentes;

    public IncidenteAdapter(Context context, List<Incidente> incidentes) {
        this.context = context;
        this.incidentes = incidentes;
    }

    @NonNull
    @Override
    public IncidenteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_incidente_card, parent, false);
        return new IncidenteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull IncidenteViewHolder holder, int position) {
        Incidente incidente = incidentes.get(position);

        holder.tvTipo.setText(incidente.getTipo());

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        String fechaFormateada = sdf.format(incidente.getFechaDeCreacion().toDate());
        holder.tvFecha.setText(fechaFormateada);

        if (incidente.getImagenUrl() != null && !incidente.getImagenUrl().isEmpty()) {
            Picasso.get().load(incidente.getImagenUrl()).into(holder.ivFoto);
        }

        // Cambiar color del borde según el tipo de incidente
        int colorResId;
        switch (incidente.getTipo().toLowerCase()) {
            case "accidente":
                colorResId = R.color.red;
                break;
            case "congestión":
                colorResId = R.color.login_button_color;
                break;
            case "construcción":
                colorResId = R.color.text_hint_color;
                break;
            default:
                colorResId = R.color.black;
                break;
        }
        holder.cardView.setStrokeColor(ContextCompat.getColor(context, colorResId));

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, IncidenteDetalleActivity.class);
            intent.putExtra(IncidenteDetalleActivity.EXTRA_INCIDENTE_ID, incidente.getId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return incidentes.size();
    }

    static class IncidenteViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        ImageView ivFoto;
        TextView tvTipo, tvFecha;

        public IncidenteViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.card_incidente);
            ivFoto = itemView.findViewById(R.id.iv_incidente_foto);
            tvTipo = itemView.findViewById(R.id.tv_incidente_tipo);
            tvFecha = itemView.findViewById(R.id.tv_incidente_fecha);
        }
    }
}
