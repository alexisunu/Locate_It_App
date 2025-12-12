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
import android.view.View;
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
import com.google.firebase.firestore.Query;
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

        setupButtonClickListeners();
        checkLocationPermission();
    }

    private void loadUserPlaces() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();
        Query query = db.collection("places").whereEqualTo("userId", userId);

        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                map.getOverlays().clear();
                setupLocationOverlay();

                if (task.getResult().isEmpty()) {
                    Log.w(TAG, "No se encontraron lugares");
                } else {
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        addPlaceMarkerToMap(document);
                    }
                }
                map.invalidate();
            } else {
                Log.e(TAG, "FALLO la consulta de lugares: ", task.getException());
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
                    Intent intent = new Intent(lugares_mapa.this, LugarDetalleActivity.class);
                    intent.putExtra(LugarDetalleActivity.EXTRA_LUGAR_ID, (String) marker.getRelatedObject());
                    startActivity(intent);
                    return true;
                });
                map.getOverlays().add(placeMarker);
            }
        } catch (Exception e) {
            Log.e(TAG, "EXCEPCIÓN al procesar el documento: " + document.getId(), e);
        }
    }
    
    private Drawable getIconForCategory(String category) {
        Drawable icon = ContextCompat.getDrawable(this, org.osmdroid.library.R.drawable.marker_default).mutate();
        int color;
        switch (category.toLowerCase()) {
            case "parque": color = Color.parseColor("#4CAF50"); break;
            case "centro comercial": color = Color.parseColor("#FF9800"); break;
            case "restaurante": color = Color.parseColor("#F44336"); break;
            case "cafetería": color = Color.parseColor("#795548"); break;
            case "mirador": color = Color.parseColor("#03A9F4"); break;
            case "museo": color = Color.parseColor("#9C27B0"); break;
            case "bar": color = Color.parseColor("#E91E63"); break;
            case "playa": color = Color.parseColor("#00BCD4"); break;
            case "montaña": color = Color.parseColor("#8BC34A"); break;
            case "otro":
            default: color = Color.parseColor("#8E8E8E"); break;
        }
        icon.setColorFilter(color, PorterDuff.Mode.SRC_IN);
        return icon;
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            setupLocationOverlay();
            getDeviceLocation();
            loadUserPlaces(); // Carga inicial
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
                    map.getController().setZoom(18.0);
                    map.getController().animateTo(currentLocation);
                }
            });
        } catch (SecurityException e) {
            Log.e(TAG, "Error de seguridad", e);
        }
    }

    private void setupButtonClickListeners() {
        FloatingActionButton fabAddPlace = findViewById(R.id.fab_add_place);
        fabAddPlace.setOnClickListener(view -> 
            startActivity(new Intent(this, GuardarLugar.class)));

        FloatingActionButton fabCenterLocation = findViewById(R.id.fab_center_location);
        fabCenterLocation.setOnClickListener(view -> {
            if (currentLocation != null) {
                map.getController().setZoom(18.0);
                map.getController().animateTo(currentLocation);
            } else {
                getDeviceLocation();
            }
        });

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_my_places) {
                startActivity(new Intent(this, ListaLugaresActivity.class));
                return true;
            }
            return false;
        });
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            checkLocationPermission();
        } else {
            loadUserPlaces();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (map != null) map.onResume();
        loadUserPlaces();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (map != null) map.onPause();
    }
}
