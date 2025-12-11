package com.example.locate_it_app;

import android.Manifest;
import android.content.pm.PackageManager;
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
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

public class lugares_mapa extends AppCompatActivity {

    private MapView map = null;
    private FusedLocationProviderClient fusedLocationClient;
    private MyLocationNewOverlay myLocationOverlay;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private GeoPoint currentLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- IMPORTANTE: Configuración de OSMDroid ---
        // Esto es necesario para evitar problemas de "User-Agent" y de almacenamiento en caché.
        Configuration.getInstance().load(getApplicationContext(), PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));

        setContentView(R.layout.lugares_mapa);

        // Inicializa el cliente de ubicación
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // --- Inicialización del Mapa ---
        map = findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK); // Establece el proveedor de losetas del mapa
        map.setMultiTouchControls(true); // Permite hacer zoom con los dedos
        map.getController().setZoom(15.0); // Establece un zoom inicial

        checkLocationPermission();
        setupButtonClickListeners();
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
        myLocationOverlay.enableMyLocation(); // Habilita el seguimiento de la ubicación
        myLocationOverlay.setDrawAccuracyEnabled(true); // Dibuja el círculo de precisión
        map.getOverlays().add(myLocationOverlay);
    }

    private void getDeviceLocation() {
        try {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    currentLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
                    map.getController().animateTo(currentLocation);
                } else {
                    Log.d("MapActivity", "La ubicación actual es nula.");
                }
            });
        } catch (SecurityException e) {
            Log.e("MapActivity", "Error de seguridad", e);
        }
    }

    private void setupButtonClickListeners() {
        FloatingActionButton fabCenterLocation = findViewById(R.id.fab_center_location);
        fabCenterLocation.setOnClickListener(view -> {
            if (currentLocation != null) {
                map.getController().animateTo(currentLocation);
                Toast.makeText(this, "Centrando ubicación", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Ubicación no disponible aún.", Toast.LENGTH_SHORT).show();
            }
        });

        FloatingActionButton fabAddPlace = findViewById(R.id.fab_add_place);
        fabAddPlace.setOnClickListener(view -> {
            Toast.makeText(this, "Registrar Lugar (próximamente)", Toast.LENGTH_SHORT).show();
        });

        com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        // Hacemos que el item del medio no sea seleccionable

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_share_place) {
                Toast.makeText(this, "Compartir Lugar (próximamente)", Toast.LENGTH_SHORT).show();
                return true;
            } else if (itemId == R.id.nav_my_places) {
                Toast.makeText(this, "Mis Lugares (próximamente)", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });
    }




    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupLocationOverlay();
                getDeviceLocation();
            } else {
                Toast.makeText(this, "El permiso de ubicación es necesario para mostrar el mapa.", Toast.LENGTH_LONG).show();
            }
        }
    }

    // --- Ciclo de vida para OSMDroid ---
    @Override
    protected void onResume() {
        super.onResume();
        if (map != null) {
            map.onResume(); // Necesario para el mapa
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (map != null) {
            map.onPause(); // Necesario para el mapa
        }
    }
}
