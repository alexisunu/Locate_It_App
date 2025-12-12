package com.example.locate_it_app;

import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Locale;

public class LugaresAdapter extends RecyclerView.Adapter<LugaresAdapter.LugarViewHolder> {

    private List<Lugar> lugaresList;
    private Context context;
    private static final String TAG = "LugaresAdapter";

    public LugaresAdapter(Context context, List<Lugar> lugaresList) {
        this.context = context;
        this.lugaresList = lugaresList;
    }

    @NonNull
    @Override
    public LugarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_lugar_card, parent, false);
        return new LugarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LugarViewHolder holder, int position) {
        Lugar lugar = lugaresList.get(position);

        // --- Poblar Vistas ---
        holder.tvNombre.setText(lugar.getNombre());
        holder.tvDescripcion.setText(lugar.getDescripcion());
        holder.chipCategoria.setText(lugar.getCategoria());

        if (lugar.getImagenUrl() != null && !lugar.getImagenUrl().isEmpty()) {
            Picasso.get().load(lugar.getImagenUrl()).into(holder.ivBackground);
        } else {
            holder.ivBackground.setImageResource(R.drawable.cradle_background); 
        }

        // --- Cargar Dirección y Temperatura ---
        getAddressFromCoordinates(lugar.getLatitud(), lugar.getLongitud(), holder.tvDireccion);
        getWeatherData(lugar.getLatitud(), lugar.getLongitud(), holder.tvTemperatura);

        // --- Listeners ---
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, LugarDetalleActivity.class);
            intent.putExtra(LugarDetalleActivity.EXTRA_LUGAR_ID, lugar.getDocumentId());
            context.startActivity(intent);
        });

        holder.btnVerMapa.setOnClickListener(v -> {
            Intent intent = new Intent(context, lugares_mapa.class);
            intent.putExtra("lugar_latitud", lugar.getLatitud());
            intent.putExtra("lugar_longitud", lugar.getLongitud());
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        });

        holder.btnWhatsapp.setOnClickListener(v ->
                compartirPorWhatsApp(lugar, holder.tvDireccion.getText().toString()));
    }

    @Override
    public int getItemCount() {
        return lugaresList.size();
    }

    public void filterList(List<Lugar> filteredList) {
        lugaresList = filteredList;
        notifyDataSetChanged();
    }

    private void getAddressFromCoordinates(double latitude, double longitude, TextView textView) {
        new Thread(() -> {
            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    final String addressLine = addresses.get(0).getAddressLine(0);
                    ((AppCompatActivity) context).runOnUiThread(() -> textView.setText(addressLine));
                } else {
                    ((AppCompatActivity) context).runOnUiThread(() -> textView.setText("Dirección no encontrada"));
                }
            } catch (IOException e) {
                Log.e(TAG, "Servicio de Geocoder no disponible", e);
                ((AppCompatActivity) context).runOnUiThread(() -> textView.setText("Error al buscar dirección"));
            }
        }).start();
    }

    private void getWeatherData(double latitude, double longitude, TextView textView) {
        // Recuerda reemplazar con tu API Key
        String apiKey = "d45a93d0344b58133555543c7b963503"; 
        String url = String.format("https://api.openweathermap.org/data/2.5/weather?lat=%f&lon=%f&appid=%s&units=metric", latitude, longitude, apiKey);

        RequestQueue queue = Volley.newRequestQueue(context);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
            response -> {
                try {
                    JSONObject main = response.getJSONObject("main");
                    double temp = main.getDouble("temp");
                    textView.setText(String.format(Locale.getDefault(), "%.0f°C", temp));
                } catch (JSONException e) {
                    textView.setText("--");
                }
            },
            error -> textView.setText("--")
        );
        queue.add(request);
    }
    /**
     * Comparte la información del lugar por WhatsApp
     */
    private void compartirPorWhatsApp(Lugar lugar, String direccion) {
        // Construir mensaje personalizado
        StringBuilder mensaje = new StringBuilder();
        mensaje.append("Mira este lugar increíble *").append(lugar.getNombre()).append("*\n\n");

        // Agregar categoría
        mensaje.append("🏷Categoría: ").append(lugar.getCategoria()).append("\n\n");

        // Agregar descripción si existe
        if (lugar.getDescripcion() != null && !lugar.getDescripcion().isEmpty()) {
            mensaje.append("ℹ️ ").append(lugar.getDescripcion()).append("\n\n");
        }

        // Agregar dirección si está disponible
        if (direccion != null && !direccion.isEmpty() && !direccion.equals("Dirección no encontrada")) {
            mensaje.append("📌 Dirección: ").append(direccion).append("\n\n");
        }

        // Agregar enlace a Google Maps
        mensaje.append("🗺️ Ver ubicación en Google Maps:\n");
        mensaje.append("https://maps.google.com/?q=")
                .append(lugar.getLatitud())
                .append(",")
                .append(lugar.getLongitud());

        try {
            // Codificar el mensaje para URL
            String mensajeCodificado = URLEncoder.encode(mensaje.toString(), "UTF-8");

            // Crear URL de WhatsApp
            String urlWhatsApp = "https://api.whatsapp.com/send?text=" + mensajeCodificado;

            // Crear intent
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(urlWhatsApp));

            // Intentar abrir WhatsApp
            context.startActivity(intent);

            Log.d(TAG, "Compartiendo lugar: " + lugar.getNombre());

        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Error al codificar mensaje", e);
            Toast.makeText(context, "Error al compartir", Toast.LENGTH_SHORT).show();
        } catch (android.content.ActivityNotFoundException e) {
            // WhatsApp no está instalado
            Log.w(TAG, "WhatsApp no instalado, usando selector genérico");
        }
    }


    public static class LugarViewHolder extends RecyclerView.ViewHolder {
        ImageView ivBackground;
        TextView tvNombre, tvDireccion, tvDescripcion, tvTemperatura;
        Chip chipCategoria;
        MaterialButton btnVerMapa, btnWhatsapp;

        public LugarViewHolder(@NonNull View itemView) {
            super(itemView);
            ivBackground = itemView.findViewById(R.id.iv_lugar_background);
            tvNombre = itemView.findViewById(R.id.tv_lugar_nombre);
            tvDireccion = itemView.findViewById(R.id.tv_lugar_direccion);
            tvDescripcion = itemView.findViewById(R.id.tv_lugar_descripcion);
            tvTemperatura = itemView.findViewById(R.id.tv_lugar_temperatura);
            chipCategoria = itemView.findViewById(R.id.chip_categoria);
            btnVerMapa = itemView.findViewById(R.id.btn_ver_mapa);
            btnWhatsapp = itemView.findViewById(R.id.btn_whatsapp);
        }
    }
}
