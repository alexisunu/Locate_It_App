package com.example.locate_it_app;

import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class LugarDetalleActivity extends AppCompatActivity {

    public static final String EXTRA_LUGAR_ID = "lugar_id";
    private static final String TAG = "LugarDetalleActivity";

    private FirebaseFirestore db;

    private ImageView ivPlaceImage;
    private TextView tvUserName, tvCategory, tvTemperature;
    private TextView tvPlaceName, tvPlaceAddress, tvPlaceDescription;
    private CircleImageView ivUserAvatar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lugar_detalle);

        // --- Inicialización de Vistas ---
        Toolbar toolbar = findViewById(R.id.toolbar);
        ivPlaceImage = findViewById(R.id.ivPlaceImage);
        tvUserName = findViewById(R.id.tvUserName);
        tvCategory = findViewById(R.id.tvCategory);
        tvTemperature = findViewById(R.id.tvTemperature);
        tvPlaceName = findViewById(R.id.tvPlaceName);
        tvPlaceAddress = findViewById(R.id.tvPlaceAddress);
        tvPlaceDescription = findViewById(R.id.tvPlaceDescription);
        ivUserAvatar = findViewById(R.id.ivUserAvatar);

        // --- Configuración de la Barra de Navegación ---
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // --- Inicialización de la Base de Datos ---
        db = FirebaseFirestore.getInstance();

        // --- Carga de Datos ---
        String lugarId = getIntent().getStringExtra(EXTRA_LUGAR_ID);
        if (lugarId == null || lugarId.isEmpty()) {
            Toast.makeText(this, "Error: No se proporcionó un ID de lugar.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        loadLugarData(lugarId);
    }

    private void loadLugarData(String lugarId) {
        db.collection("places").document(lugarId).get()
            .addOnSuccessListener(documentSnapshot -> {
                if (!documentSnapshot.exists()) {
                    Toast.makeText(LugarDetalleActivity.this, "El lugar no existe.", Toast.LENGTH_LONG).show();
                    return;
                }

                String nombreLugar = documentSnapshot.getString("nombre");
                String descripcion = documentSnapshot.getString("descripcion");
                String categoria = documentSnapshot.getString("categoria");
                String imagenUrl = documentSnapshot.getString("imagenUrl");
                String userId = documentSnapshot.getString("userId");
                Double latitud = documentSnapshot.getDouble("latitud");
                Double longitud = documentSnapshot.getDouble("longitud");

                tvPlaceName.setText(nombreLugar);
                tvPlaceDescription.setText(descripcion);
                tvCategory.setText(categoria);

                if (latitud != null && longitud != null) {
                    getAddressFromCoordinates(latitud, longitud);
                    getWeatherData(latitud, longitud); // <-- LLAMADA AL NUEVO MÉTODO DEL CLIMA
                } else {
                    tvPlaceAddress.setText("Ubicación no disponible");
                    tvTemperature.setText("--");
                }

                if (imagenUrl != null && !imagenUrl.isEmpty()) {
                    Picasso.get().load(imagenUrl).into(ivPlaceImage);
                }

                if (userId != null && !userId.isEmpty()) {
                    loadUserData(userId);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error al cargar el lugar", e);
                Toast.makeText(LugarDetalleActivity.this, "Error al cargar el lugar.", Toast.LENGTH_LONG).show();
            });
    }

    private void loadUserData(String userId) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String userName = documentSnapshot.getString("nombre");
                    String userAvatarUrl = documentSnapshot.getString("fotoPerfil");

                    tvUserName.setText(userName);

                    if (userAvatarUrl != null && !userAvatarUrl.isEmpty()) {
                        Picasso.get().load(userAvatarUrl).into(ivUserAvatar);
                    } else {
                        ivUserAvatar.setImageResource(R.drawable.ic_profile_user);
                    }
                }
            });
    }

    private void getAddressFromCoordinates(double latitude, double longitude) {
        new Thread(() -> {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    final String addressLine = addresses.get(0).getAddressLine(0);
                    runOnUiThread(() -> tvPlaceAddress.setText(addressLine));
                } else {
                    runOnUiThread(() -> tvPlaceAddress.setText("Dirección no encontrada"));
                }
            } catch (IOException e) {
                Log.e(TAG, "Servicio de Geocoder no disponible", e);
                runOnUiThread(() -> tvPlaceAddress.setText("Error al buscar dirección"));
            }
        }).start();
    }

    /**
     * Obtiene los datos del clima desde la API de OpenWeatherMap usando Volley.
     */
    private void getWeatherData(double latitude, double longitude) {
        // ¡IMPORTANTE! Reemplaza "TU_API_KEY" con tu propia clave de OpenWeatherMap
        String apiKey = "d97e5bdd21696cd2002a470f3ab9241e";
        String url = String.format("https://api.openweathermap.org/data/2.5/weather?lat=%f&lon=%f&appid=%s&units=metric", latitude, longitude, apiKey);

        RequestQueue queue = Volley.newRequestQueue(this);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, 
            response -> {
                try {
                    JSONObject main = response.getJSONObject("main");
                    double temp = main.getDouble("temp");
                    tvTemperature.setText(String.format(Locale.getDefault(), "%.0f°C", temp));
                } catch (JSONException e) {
                    Log.e(TAG, "Error al parsear el JSON del clima", e);
                    tvTemperature.setText("--");
                }
            },
            error -> {
                Log.e(TAG, "Error en la petición del clima: " + error.toString());
                tvTemperature.setText("--");
            }
        );

        queue.add(jsonObjectRequest);
    }
}
