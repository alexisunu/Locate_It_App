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
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

public class incidentes_mapa extends AppCompatActivity {

    private static final String TAG = "INCIDENTE_MAPA_DEBUG";

    private MapView map = null;
    private FusedLocationProviderClient fusedLocationClient;
    private MyLocationNewOverlay myLocationOverlay;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 2;
    private GeoPoint currentLocation;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().load(getApplicationContext(), PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        setContentView(R.layout.incidentes_mapa);

        map = findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getController().setZoom(15.0);

        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        setupButtonClickListeners();
        checkLocationPermission(); // Llamar a esto inicializará la carga de datos y ubicación
    }

    private void setupButtonClickListeners() {
        FloatingActionButton fabCenterLocation = findViewById(R.id.fab_center_location);
        fabCenterLocation.setOnClickListener(view -> {
            if (currentLocation != null) {
                map.getController().setZoom(18.0);
                map.getController().animateTo(currentLocation);
            } else {
                getDeviceLocation();
            }
        });

        FloatingActionButton fabAddIncident = findViewById(R.id.fab_add_incident);
        fabAddIncident.setOnClickListener(view -> {
            startActivity(new Intent(this, ReportarIncidente.class));
        });
    }

    private void loadAllIncidents() {
        Log.d(TAG, "Iniciando carga de todos los incidentes.");
        db.collection("incidents").get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d(TAG, "Consulta de incidentes EXITOSA. Documentos encontrados: " + task.getResult().size());
                    if (task.getResult().isEmpty()) {
                        Log.w(TAG, "No se encontró ningún incidente en la base de datos.");
                        return;
                    }
                    map.getOverlays().clear(); // Limpiar marcadores antiguos antes de añadir nuevos
                    setupLocationOverlay(); // Volver a añadir el overlay de la ubicación del usuario
                    
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        addIncidentMarkerToMap(document);
                    }
                    map.invalidate();
                } else {
                    Log.e(TAG, "FALLO la consulta de incidentes: ", task.getException());
                }
            });
    }

    private void addIncidentMarkerToMap(QueryDocumentSnapshot document) {
        try {
            String tipoIncidente = document.getString("tipo"); 
            Double latitud = document.getDouble("latitud");
            Double longitud = document.getDouble("longitud");
            String incidenteId = document.getId();

            if (tipoIncidente != null && latitud != null && longitud != null) {
                GeoPoint incidentLocation = new GeoPoint(latitud, longitud);
                Marker incidentMarker = new Marker(map);
                incidentMarker.setPosition(incidentLocation);
                incidentMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                incidentMarker.setTitle(tipoIncidente);
                
                Drawable icon = ContextCompat.getDrawable(this, org.osmdroid.library.R.drawable.marker_default).mutate();
                icon.setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN);
                incidentMarker.setIcon(icon);

                incidentMarker.setRelatedObject(incidenteId);
                incidentMarker.setOnMarkerClickListener((marker, mapView) -> {
                    String clickedIncidenteId = (String) marker.getRelatedObject();
                    Intent intent = new Intent(incidentes_mapa.this, IncidenteDetalleActivity.class);
                    intent.putExtra(IncidenteDetalleActivity.EXTRA_INCIDENTE_ID, clickedIncidenteId);
                    startActivity(intent);
                    return true;
                });

                map.getOverlays().add(incidentMarker);
            } else {
                Log.w(TAG, "Incidente " + incidenteId + " OMITIDO por tener datos nulos.");
            }
        } catch (Exception e) {
            Log.e(TAG, "EXCEPCIÓN al procesar el incidente: " + document.getId(), e);
        }
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Permiso de ubicación ya concedido.");
            setupMapAndData();
        } else {
            Log.d(TAG, "Solicitando permiso de ubicación.");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    // Nuevo método para organizar la inicialización
    private void setupMapAndData(){
        setupLocationOverlay();
        getDeviceLocation();
        loadAllIncidents();
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
                    Log.d(TAG, "Ubicación obtenida y mapa centrado.");
                }
            });
        } catch (SecurityException e) {
            Log.e(TAG, "Error de seguridad al obtener ubicación", e);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permiso de ubicación concedido por el usuario.");
                setupMapAndData();
            } else {
                Log.w(TAG, "Permiso de ubicación denegado.");
                Toast.makeText(this, "El permiso de ubicación es necesario para el mapa.", Toast.LENGTH_LONG).show();
                // Aún si deniega, cargamos los incidentes, que no dependen de la ubicación del usuario.
                loadAllIncidents(); 
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume - recargando incidentes.");
        loadAllIncidents(); // Recargar los incidentes por si se añadió uno nuevo
        if (map != null) map.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (map != null) map.onPause();
    }
}
