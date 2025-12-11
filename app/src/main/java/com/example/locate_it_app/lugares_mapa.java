package com.example.locate_it_app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

public class lugares_mapa extends AppCompatActivity {

    private static final String TAG = "LUGAR_MAPA_DEBUG";

    private MapView map = null;
    private FusedLocationProviderClient fusedLocationClient;
    private MyLocationNewOverlay myLocationOverlay;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private GeoPoint currentLocation;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().load(getApplicationContext(), PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        setContentView(R.layout.lugares_mapa);

        map = findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getController().setZoom(15.0);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        checkLocationPermission();
        setupButtonClickListeners();
        loadUserPlaces();
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            setupLocationOverlay();
            getDeviceLocation();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void setupLocationOverlay() {
        myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), map);
        myLocationOverlay.enableMyLocation();
        map.getOverlays().add(myLocationOverlay);
    }

    private void getDeviceLocation() {
        try {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    currentLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
                    map.getController().animateTo(currentLocation);
                }
            });
        } catch (SecurityException e) {
            Log.e(TAG, "Error de seguridad al obtener ubicación", e);
        }
    }

    private void setupButtonClickListeners() {
        FloatingActionButton fabCenterLocation = findViewById(R.id.fab_center_location);
        fabCenterLocation.setOnClickListener(view -> {
            if (currentLocation != null) {
                map.getController().animateTo(currentLocation);
            } else {
                Toast.makeText(this, "Ubicación no disponible aún.", Toast.LENGTH_SHORT).show();
            }
        });

        FloatingActionButton fabAddPlace = findViewById(R.id.fab_add_place);
        fabAddPlace.setOnClickListener(view -> 
            startActivity(new Intent(this, GuardarLugar.class)));

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            // --- LÓGICA MODIFICADA ---
            if (itemId == R.id.nav_share_place) {
                Toast.makeText(this, "Compartir Lugar (próximamente)", Toast.LENGTH_SHORT).show();
                return true;
            } else if (itemId == R.id.nav_my_places) {
                startActivity(new Intent(this, ListaLugaresActivity.class));
                return true;
            }
            return false;
        });
    }

    private void loadUserPlaces() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "¡ERROR CRÍTICO! No hay usuario autenticado para cargar lugares.");
            return;
        }
        String userId = currentUser.getUid();

        db.collection("places").whereEqualTo("userId", userId).get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    if (task.getResult().isEmpty()) {
                        Log.w(TAG, "No se encontró ningún lugar para este usuario.");
                        return;
                    }
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        addPlaceMarkerToMap(document);
                    }
                    map.invalidate();
                } else {
                    Log.e(TAG, "FALLO la consulta a Firestore: ", task.getException());
                }
            });
    }

    private void addPlaceMarkerToMap(QueryDocumentSnapshot document) {
        try {
            String nombre = document.getString("nombre");
            String categoria = document.getString("categoria");
            Double latitud = document.getDouble("latitud");
            Double longitud = document.getDouble("longitud");
            String lugarId = document.getId();

            if (nombre != null && latitud != null && longitud != null && categoria != null) {
                GeoPoint placeLocation = new GeoPoint(latitud, longitud);
                Marker placeMarker = new Marker(map);
                placeMarker.setPosition(placeLocation);
                placeMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                placeMarker.setTitle(nombre);

                placeMarker.setIcon(getIconForCategory(categoria));

                placeMarker.setRelatedObject(lugarId);
                placeMarker.setOnMarkerClickListener((marker, mapView) -> {
                    String clickedLugarId = (String) marker.getRelatedObject();
                    Intent intent = new Intent(lugares_mapa.this, LugarDetalleActivity.class);
                    intent.putExtra(LugarDetalleActivity.EXTRA_LUGAR_ID, clickedLugarId);
                    startActivity(intent);
                    return true;
                });
                map.getOverlays().add(placeMarker);
            } else {
                Log.w(TAG, "Documento " + lugarId + " OMITIDO por tener datos nulos.");
            }
        } catch (Exception e) {
            Log.e(TAG, "EXCEPCIÓN al procesar el documento: " + document.getId(), e);
        }
    }

    private Drawable getIconForCategory(String category) {
        Drawable icon = ContextCompat.getDrawable(this, org.osmdroid.library.R.drawable.marker_default).mutate();
        int color;

        switch (category.toLowerCase()) {
            case "casa":
                color = Color.parseColor("#008F39"); // Verde
                break;
            case "trabajo":
                color = Color.parseColor("#2A7FF3"); // Azul
                break;
            case "restaurante":
                color = Color.parseColor("#F3632A"); // Naranja
                break;
            case "parque":
                color = Color.parseColor("#4CAF50"); // Verde claro
                break;
            default:
                color = Color.parseColor("#8E8E8E"); // Gris para "Otro"
                break;
        }

        icon.setColorFilter(color, PorterDuff.Mode.SRC_IN);
        return icon;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            setupLocationOverlay();
            getDeviceLocation();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (map != null) map.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (map != null) map.onPause();
    }
}
