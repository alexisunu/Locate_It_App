package com.example.locate_it_app;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ReportarIncidente extends AppCompatActivity {

    private static final String TAG = "ReportarIncidente";

    // Cloudinary credentials (usa las mismas de GuardarLugar)
    private static final String CLOUDINARY_CLOUD_NAME = "dqfioqnrl";
    private static final String CLOUDINARY_API_KEY = "912943113326729";
    private static final String CLOUDINARY_API_SECRET = "n3E9k7vJ7e7XLtaIgSDZXiawxRE";

    // Activity Result Launchers
    private ActivityResultLauncher<Uri> tomarFotoLauncher;
    private ActivityResultLauncher<String> requestCameraPermissionLauncher;
    private ActivityResultLauncher<String> requestLocationPermissionLauncher;

    // Vistas
    private RadioGroup rgTipoIncidente;
    private Button btnReportarIncidente;
    private ImageView ivAgregarFotoIncidente, iconoCamara;
    private TextView txtAgregarFoto;
    private MapView mapIncidente;

    // Variables de foto
    private Uri fotoUri;
    private File archivoFoto;
    private boolean fotoCapturada = false;

    // Variables de ubicación
    private FusedLocationProviderClient fusedLocationClient;
    private double latitud = 0, longitud = 0;
    private boolean ubicacionObtenida = false;

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_reportar_incidente);

        // ====== INICIALIZAR CLOUDINARY ======
        inicializarCloudinary();

        // ====== REGISTRAR LAUNCHERS ======

        // Launcher para permiso de cámara
        requestCameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        abrirCamara();
                    } else {
                        Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // Launcher para tomar foto (foto completa, no preview)
        tomarFotoLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success) {
                        fotoCapturada = true;
                        mostrarFotoCapturada();
                    } else {
                        Toast.makeText(this, "No se capturó la foto", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // Launcher para permiso de ubicación
        requestLocationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        obtenerUbicacionActual();
                    } else {
                        Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // ====== INICIALIZAR FIREBASE ======
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // ====== VINCULAR VISTAS ======
        rgTipoIncidente = findViewById(R.id.rgTipoIncidente);
        btnReportarIncidente = findViewById(R.id.btnReportarIncidente);
        ivAgregarFotoIncidente = findViewById(R.id.ivAgregarFotoIncidente);
        iconoCamara = findViewById(R.id.iconoCamara);
        txtAgregarFoto = findViewById(R.id.txtAgregarFoto);
        mapIncidente = findViewById(R.id.mapIncidente);

        // ====== INICIALIZAR UBICACIÓN ======
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // ====== CONFIGURAR MAPA ======
        mapIncidente.setTileSource(TileSourceFactory.MAPNIK);
        mapIncidente.setMultiTouchControls(true);

        // Solicitar permisos de ubicación
        solicitarPermisosUbicacion();

        // ====== CONFIGURAR BOTONES ======
        findViewById(R.id.btnAgregarFotoIncidente).setOnClickListener(view -> solicitarPermisoCamara());
        btnReportarIncidente.setOnClickListener(view -> reportarIncidente());
    }

    private void inicializarCloudinary() {
        try {
            Map<String, Object> config = new HashMap<>();
            config.put("cloud_name", CLOUDINARY_CLOUD_NAME);
            config.put("api_key", CLOUDINARY_API_KEY);
            config.put("api_secret", CLOUDINARY_API_SECRET);
            config.put("secure", true);
            MediaManager.init(this, config);
            Log.d(TAG, "Cloudinary inicializado");
        } catch (Exception e) {
            Log.e(TAG, "Error al inicializar Cloudinary: " + e.getMessage());
        }
    }

    // ====== MÉTODOS DE UBICACIÓN ======

    private void solicitarPermisosUbicacion() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            obtenerUbicacionActual();
        } else {
            requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void obtenerUbicacionActual() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            latitud = location.getLatitude();
                            longitud = location.getLongitude();
                            ubicacionObtenida = true;

                            GeoPoint startPoint = new GeoPoint(latitud, longitud);

                            Marker startMarker = new Marker(mapIncidente);
                            startMarker.setPosition(startPoint);
                            startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                            startMarker.setTitle("Ubicación del incidente");
                            mapIncidente.getOverlays().add(startMarker);

                            mapIncidente.getController().setZoom(16.0);
                            mapIncidente.getController().setCenter(startPoint);
                            mapIncidente.invalidate();
                        } else {
                            Toast.makeText(this, "No se pudo obtener la ubicación", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    // ====== MÉTODOS DE CÁMARA ======

    private void solicitarPermisoCamara() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            abrirCamara();
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void abrirCamara() {
        try {
            archivoFoto = crearArchivoImagen();
            fotoUri = FileProvider.getUriForFile(
                    this,
                    "com.example.locate_it_app.fileprovider",
                    archivoFoto
            );
            tomarFotoLauncher.launch(fotoUri);
        } catch (IOException e) {
            Toast.makeText(this, "Error al crear archivo de imagen", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private File crearArchivoImagen() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(new Date());
        String imageFileName = "INCIDENTE_" + timeStamp + "_";
        File storageDir = getExternalFilesDir("fotos_incidentes");
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    private void mostrarFotoCapturada() {
        ivAgregarFotoIncidente.setVisibility(View.VISIBLE);
        ivAgregarFotoIncidente.setImageURI(null);
        ivAgregarFotoIncidente.setImageURI(fotoUri);
        iconoCamara.setVisibility(View.GONE);
        txtAgregarFoto.setText("Cambiar foto");
        Toast.makeText(this, "Foto capturada", Toast.LENGTH_SHORT).show();
    }

    // ====== REPORTAR INCIDENTE ======

    private void reportarIncidente() {
        // Validar tipo de incidente
        int selectedId = rgTipoIncidente.getCheckedRadioButtonId();
        if (selectedId == -1) {
            Toast.makeText(this, "Selecciona un tipo de incidente", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validar foto
        if (!fotoCapturada) {
            Toast.makeText(this, "Por favor, agrega una foto del incidente", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validar ubicación
        if (!ubicacionObtenida) {
            Toast.makeText(this, "Esperando ubicación...", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validar usuario autenticado
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Debes iniciar sesión", Toast.LENGTH_SHORT).show();
            return;
        }

        RadioButton selectedRadioButton = findViewById(selectedId);
        String tipoIncidente = selectedRadioButton.getText().toString();
        String userId = currentUser.getUid();

        // Deshabilitar botón
        btnReportarIncidente.setEnabled(false);
        btnReportarIncidente.setText("Reportando...");

        // Subir foto y guardar incidente
        subirFotoYGuardarIncidente(tipoIncidente, userId);
    }

    private void subirFotoYGuardarIncidente(String tipoIncidente, String userId) {
        Toast.makeText(this, "Subiendo evidencia...", Toast.LENGTH_SHORT).show();

        MediaManager.get()
                .upload(fotoUri)
                .option("folder", "locate_it/incidentes/" + userId)
                .option("resource_type", "auto")
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {
                        Log.d(TAG, "Iniciando subida de foto...");
                    }

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {
                        double progress = (double) bytes / totalBytes * 100;
                        Log.d(TAG, "Progreso: " + (int) progress + "%");
                    }

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String urlFoto = (String) resultData.get("secure_url");
                        Log.d(TAG, "Foto subida: " + urlFoto);
                        guardarIncidenteEnFirestore(tipoIncidente, userId, urlFoto);

                        if (archivoFoto != null && archivoFoto.exists()) {
                            archivoFoto.delete();
                        }
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        Log.e(TAG, "Error al subir foto: " + error.getDescription());
                        runOnUiThread(() -> {
                            Toast.makeText(ReportarIncidente.this,
                                    "Error al subir la foto", Toast.LENGTH_SHORT).show();
                            btnReportarIncidente.setEnabled(true);
                            btnReportarIncidente.setText("Reportar Incidente");
                        });
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {
                        Log.d(TAG, "Reintentando subida...");
                    }
                })
                .dispatch();
    }

    private void guardarIncidenteEnFirestore(String tipoIncidente, String userId, String urlFoto) {
        Map<String, Object> incidente = new HashMap<>();
        incidente.put("tipo", tipoIncidente);
        incidente.put("imagenUrl", urlFoto);
        incidente.put("latitud", latitud);
        incidente.put("longitud", longitud);
        incidente.put("userId", userId);
        incidente.put("fechaDeCreacion", Timestamp.now());

        db.collection("incidents")
                .add(incidente)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "✅ Incidente reportado con ID: " + documentReference.getId());
                    Toast.makeText(this, "Incidente reportado con éxito", Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error al reportar incidente: " + e.getMessage());
                    Toast.makeText(this, "Error al reportar el incidente", Toast.LENGTH_SHORT).show();
                    btnReportarIncidente.setEnabled(true);
                    btnReportarIncidente.setText("Reportar Incidente");
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapIncidente.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapIncidente.onPause();
    }
}
