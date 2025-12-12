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

import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class IncidenteDetalleActivity extends AppCompatActivity {

    public static final String EXTRA_INCIDENTE_ID = "incidente_id";
    private static final String TAG = "IncidenteDetalle";

    private FirebaseFirestore db;

    private ImageView ivPlaceImage;
    private TextView tvUserName, tvCategory;
    private TextView tvPlaceName, tvPlaceAddress, tvPlaceDescription;
    private CircleImageView ivUserAvatar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incidente_detalle);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        ivPlaceImage = findViewById(R.id.ivPlaceImage);
        tvUserName = findViewById(R.id.tvUserName);
        tvCategory = findViewById(R.id.tvCategory);
        tvPlaceName = findViewById(R.id.tvPlaceName);
        tvPlaceAddress = findViewById(R.id.tvPlaceAddress);
        tvPlaceDescription = findViewById(R.id.tvPlaceDescription);
        ivUserAvatar = findViewById(R.id.ivUserAvatar);

        db = FirebaseFirestore.getInstance();

        String incidenteId = getIntent().getStringExtra(EXTRA_INCIDENTE_ID);
        if (incidenteId == null || incidenteId.isEmpty()) {
            Toast.makeText(this, "Error: No se proporcionó un ID de incidente.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        loadIncidenteData(incidenteId);
    }

    private void loadIncidenteData(String incidenteId) {
        db.collection("incidents").document(incidenteId).get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String nombreIncidente = documentSnapshot.getString("titulo"); // Asumiendo que el campo es "titulo"
                    String descripcion = documentSnapshot.getString("descripcion");
                    String categoria = documentSnapshot.getString("tipo"); // Asumiendo que el campo es "tipo"
                    String imagenUrl = documentSnapshot.getString("imagenUrl");
                    String userId = documentSnapshot.getString("userId");
                    Double latitud = documentSnapshot.getDouble("latitud");
                    Double longitud = documentSnapshot.getDouble("longitud");

                    tvPlaceName.setText(nombreIncidente);
                    tvPlaceDescription.setText(descripcion);
                    tvCategory.setText(categoria);

                    if (latitud != null && longitud != null) {
                        getAddressFromCoordinates(latitud, longitud);
                    } else {
                        tvPlaceAddress.setText("Ubicación no disponible");
                    }

                    if (imagenUrl != null && !imagenUrl.isEmpty()) {
                        Picasso.get().load(imagenUrl).into(ivPlaceImage);
                    }

                    if (userId != null && !userId.isEmpty()) {
                        loadUserData(userId);
                    }

                } else {
                    Toast.makeText(IncidenteDetalleActivity.this, "El incidente no existe.", Toast.LENGTH_LONG).show();
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(IncidenteDetalleActivity.this, "Error al cargar el incidente: " + e.getMessage(), Toast.LENGTH_LONG).show();
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
}
