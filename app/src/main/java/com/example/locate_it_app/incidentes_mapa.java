package com.example.locate_it_app;

import android.Manifest;
import android.content.Intent;
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

// La clase se llama como tu archivo
public class incidentes_mapa extends AppCompatActivity {

    private MapView map = null;
    private FusedLocationProviderClient fusedLocationClient;
    private MyLocationNewOverlay myLocationOverlay;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 2; // Usamos un código diferente por buena práctica
    private GeoPoint currentLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- Configuración de OSMDroid ---
        Configuration.getInstance().load(getApplicationContext(), PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));

        // --- Usamos tu layout ---
        setContentView(R.layout.incidentes_mapa);

        // Inicializa el cliente de ubicación
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // --- Inicialización del Mapa ---
        map = findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getController().setZoom(15.0);

        checkLocationPermission();
        setupButtonClickListeners(); // Llama al método para configurar los botones de esta activity
    }

    private void setupButtonClickListeners() {
        // --- Botón de centrar ubicación (misma funcionalidad) ---
        FloatingActionButton fabCenterLocation = findViewById(R.id.fab_center_location);
        fabCenterLocation.setOnClickListener(view -> {
            if (currentLocation != null) {
                map.getController().animateTo(currentLocation);
                Toast.makeText(this, "Centrando ubicación", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Ubicación no disponible aún.", Toast.LENGTH_SHORT).show();
            }
        });

        // --- Botón para REGISTRAR INCIDENTE (apunta al nuevo ID) ---
        FloatingActionButton fabAddIncident = findViewById(R.id.fab_add_incident);
        fabAddIncident.setOnClickListener(view -> {
            // Lógica futura para la activity de registrar incidente
            Intent intent = new Intent(this, ReportarIncidente.class);
            startActivity(intent);
        });

        // Ya no necesitamos un listener para la BottomNavigationView ya que no tiene items seleccionables.
    }


    // El resto de los métodos son idénticos a MapActivity y se pueden copiar directamente

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
        myLocationOverlay.setDrawAccuracyEnabled(true);
        map.getOverlays().add(myLocationOverlay);
    }

    private void getDeviceLocation() {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    currentLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
                    map.getController().animateTo(currentLocation);
                } else {
                    Log.d("incidentes_mapa", "La ubicación actual es nula.");
                }
            });
        } catch (SecurityException e) {
            Log.e("incidentes_mapa", "Error de seguridad", e);
        }
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

    @Override
    protected void onResume() {
        super.onResume();
        if (map != null) {
            map.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (map != null) {
            map.onPause();
        }
    }
}
