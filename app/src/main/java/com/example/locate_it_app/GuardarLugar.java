package com.example.locate_it_app;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import android.util.Log;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

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

public class GuardarLugar extends AppCompatActivity {

    // Activity Result Launchers
    private ActivityResultLauncher<Uri> tomarFotoLauncher;
    private ActivityResultLauncher<String> requestCameraPermissionLauncher;
    private ActivityResultLauncher<String> requestLocationPermissionLauncher;

    // Cloudinary configuration
    private static final String CLOUDINARY_CLOUD_NAME = "dqfioqnrl";  // CÁMBIALO
    private static final String CLOUDINARY_API_KEY = "912943113326729";        // CÁMBIALO
    private static final String CLOUDINARY_API_SECRET = "n3E9k7vJ7e7XLtaIgSDZXiawxRE";  // CÁMBIALO
    private static final String TAG = "GuardarLugar";


    // Vistas
    private Spinner spinnerCategoria;
    private EditText edtNombre, edtDescripcion;
    private Button btnGuardar;
    private MapView map;
    private ImageView imgFoto, iconoCamara;
    private TextView txtAgregarFoto;

    // Variables de foto
    private Uri fotoUri;
    private File archivoFoto;
    private boolean fotoCapturada = false;

    // Variables de ubicación
    private FusedLocationProviderClient fusedLocationClient;
    private double latitud, longitud;

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Configuración de osmdroid
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_guardar_lugar);
        inicializarCloudinary();
        // ====== REGISTRAR LAUNCHERS (DEBE SER AL INICIO) ======

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

        // Launcher para tomar foto
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

        // Launcher para permiso de ubicación (MODERNIZADO)
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
        spinnerCategoria = findViewById(R.id.spinnerCategoria);
        edtNombre = findViewById(R.id.edtNombre);
        edtDescripcion = findViewById(R.id.edtDescripcion);
        btnGuardar = findViewById(R.id.btnGuardar);
        map = findViewById(R.id.map);
        imgFoto = findViewById(R.id.imgFoto);
        iconoCamara = findViewById(R.id.iconoCamara);
        txtAgregarFoto = findViewById(R.id.txtAgregarFoto);

        // ====== INICIALIZAR UBICACIÓN ======
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // ====== CONFIGURAR MAPA ======
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);

        // Solicitar permisos de ubicación
        solicitarPermisosUbicacion();

        // ====== CONFIGURAR SPINNER ======
        String[] categorias = {"parque", "centro comercial", "restaurante", "cafetería",
                "mirador", "museo", "bar", "playa", "montaña", "otro"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, categorias);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategoria.setAdapter(adapter);

        // ====== CONFIGURAR BOTÓN DE CÁMARA ======
        LinearLayout btnAgregarFotoLayout = findViewById(R.id.btnAgregarFoto);
        btnAgregarFotoLayout.setOnClickListener(view -> solicitarPermisoCamara());

        // ====== CONFIGURAR BOTÓN GUARDAR ======
        btnGuardar.setOnClickListener(view -> guardarLugar());
    }

    // ====== MÉTODOS DE UBICACIÓN (MODERNIZADOS) ======

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
                            GeoPoint startPoint = new GeoPoint(latitud, longitud);

                            // Configurar marcador
                            Marker startMarker = new Marker(map);
                            startMarker.setPosition(startPoint);
                            startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                            startMarker.setTitle("Tu ubicación");
                            map.getOverlays().add(startMarker);

                            // Centrar mapa
                            map.getController().setZoom(16.0);
                            map.getController().setCenter(startPoint);
                            map.invalidate();
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
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir("fotos_lugares");
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    private void mostrarFotoCapturada() {
        imgFoto.setVisibility(View.VISIBLE);
        imgFoto.setImageURI(null);
        imgFoto.setImageURI(fotoUri);
        iconoCamara.setVisibility(View.GONE);
        txtAgregarFoto.setText("Cambiar foto");
        Toast.makeText(this, "Foto capturada correctamente", Toast.LENGTH_SHORT).show();
    }

    // ====== GUARDAR LUGAR EN FIRESTORE ======

    private void guardarLugar() {
        String nombre = edtNombre.getText().toString().trim();
        String descripcion = edtDescripcion.getText().toString().trim();
        String categoria = spinnerCategoria.getSelectedItem().toString();

        if (nombre.isEmpty() || descripcion.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Debes iniciar sesión para guardar un lugar.", Toast.LENGTH_SHORT).show();
            return;
        }
        String userId = currentUser.getUid();

        // Deshabilitar botón mientras se guarda
        btnGuardar.setEnabled(false);
        btnGuardar.setText("Guardando...");

        // Si hay foto capturada, subirla primero
        if (fotoCapturada && archivoFoto != null) {
            subirFotoYGuardarLugar(nombre, descripcion, categoria, userId);
        } else {
            // Guardar sin foto
            guardarLugarEnFirestore(nombre, descripcion, categoria, userId, null);
        }
    }


    // ====== LIFECYCLE DEL MAPA ======

    @Override
    protected void onResume() {
        super.onResume();
        map.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        map.onPause();
    }

    private void inicializarCloudinary() {
        try {
            Map<String, Object> config = new HashMap<>();
            config.put("cloud_name", CLOUDINARY_CLOUD_NAME);
            config.put("api_key", CLOUDINARY_API_KEY);
            config.put("api_secret", CLOUDINARY_API_SECRET);
            config.put("secure", true);

            MediaManager.init(this, config);
            Log.d(TAG, "Cloudinary inicializado correctamente");
        } catch (Exception e) {
            Log.e(TAG, "Error al inicializar Cloudinary: " + e.getMessage());
        }
    }

    private void subirFotoYGuardarLugar(String nombre, String descripcion, String categoria, String userId) {
        Toast.makeText(this, "Subiendo foto...", Toast.LENGTH_SHORT).show();

        MediaManager.get()
                .upload(fotoUri)
                .option("folder", "locate_it/" + userId)  // Organizar por usuario
                .option("resource_type", "auto")
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {
                        Log.d(TAG, "Iniciando subida de foto...");
                    }

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {
                        double progress = (double) bytes / totalBytes * 100;
                        Log.d(TAG, "Progreso: " + (int)progress + "%");
                    }

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        // Obtener URL de la foto subida
                        String urlFoto = (String) resultData.get("secure_url");
                        Log.d(TAG, "Foto subida correctamente: " + urlFoto);

                        // Guardar lugar con la URL de la foto
                        guardarLugarEnFirestore(nombre, descripcion, categoria, userId, urlFoto);

                        // Eliminar archivo temporal
                        if (archivoFoto != null && archivoFoto.exists()) {
                            archivoFoto.delete();
                        }
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        Log.e(TAG, "Error al subir foto: " + error.getDescription());
                        runOnUiThread(() -> {
                            Toast.makeText(GuardarLugar.this,
                                    "Error al subir la foto. Guardando sin imagen...",
                                    Toast.LENGTH_SHORT).show();
                            // Guardar sin foto si falla la subida
                            guardarLugarEnFirestore(nombre, descripcion, categoria, userId, null);
                        });
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {
                        Log.d(TAG, "Reintentando subida...");
                    }
                })
                .dispatch();
    }

    private void guardarLugarEnFirestore(String nombre, String descripcion, String categoria,
                                         String userId, String urlFoto) {
        Map<String, Object> lugar = new HashMap<>();
        lugar.put("nombre", nombre);
        lugar.put("descripcion", descripcion);
        lugar.put("categoria", categoria);
        lugar.put("latitud", latitud);
        lugar.put("longitud", longitud);
        lugar.put("userId", userId);
        lugar.put("fechaDeCreacion", Timestamp.now());

        // Solo agregar URL si la foto se subió correctamente
        if (urlFoto != null && !urlFoto.isEmpty()) {
            lugar.put("imagenUrl", urlFoto);
        }

        db.collection("places")
                .add(lugar)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Lugar guardado con ID: " + documentReference.getId());
                    Toast.makeText(this, "Lugar guardado con éxito", Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al guardar lugar: " + e.getMessage());
                    Toast.makeText(this, "Error al guardar el lugar", Toast.LENGTH_SHORT).show();
                    // Rehabilitar botón si falla
                    btnGuardar.setEnabled(true);
                    btnGuardar.setText("Guardar Lugar");
                });
    }

}
