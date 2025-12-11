package com.example.locate_it_app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

public class dashboard extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private ImageView ivProfilePhoto;
    private TextView tvUserName, tvUserEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dashboard);

        // Inicializar Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Encontrar los componentes de la UI
        ivProfilePhoto = findViewById(R.id.ivProfilePhoto);
        tvUserName = findViewById(R.id.tvUserName);
        tvUserEmail = findViewById(R.id.tvUserEmail);

        Button btnGoToLugares = findViewById(R.id.btnGoToLugares);
        Button btnGoToIncidentes = findViewById(R.id.btnGoToIncidentes);
        Button btnGoToProfile = findViewById(R.id.btnGoToProfile);
        // La línea para encontrar btnLogout ha sido eliminada.

        // Cargar datos del usuario
        loadUserProfile();

        // --- Configurar Listeners para los botones ---

        btnGoToLugares.setOnClickListener(v -> {
            Intent intent = new Intent(dashboard.this, lugares_mapa.class);
            startActivity(intent);
        });

        btnGoToIncidentes.setOnClickListener(v -> {
            Intent intent = new Intent(dashboard.this, incidentes_mapa.class);
            startActivity(intent);
        });

        btnGoToProfile.setOnClickListener(v -> {
            // Aquí irá la lógica para abrir la futura actividad de perfiles
            Toast.makeText(dashboard.this, "Actividad de Perfil próximamente", Toast.LENGTH_SHORT).show();
        });

        // El OnClickListener para btnLogout ha sido eliminado.
    }

    private void loadUserProfile() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String uid = user.getUid(); // El ID único del usuario
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            // Accedemos al documento del usuario en la colección "users"
            db.collection("users").document(uid).get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document != null && document.exists()) {
                        // --- El documento existe, obtenemos todos los datos de aquí ---

                        // 1. Obtener y mostrar el nombre
                        String name = document.getString("nombre");
                        if (name != null && !name.isEmpty()) {
                            tvUserName.setText(name);
                        } else {
                            tvUserName.setText("Sin nombre registrado");
                        }

                        // 2. Obtener la URL de la foto y cargarla con Picasso
                        String photoUrlString = document.getString("fotoPerfil");
                        if (photoUrlString != null && !photoUrlString.isEmpty()) {
                            Picasso.get()
                                    .load(photoUrlString)
                                    .placeholder(R.drawable.ic_default_profile)
                                    .error(R.drawable.ic_default_profile)
                                    .into(ivProfilePhoto);
                        } else {
                            ivProfilePhoto.setImageResource(R.drawable.ic_default_profile);
                        }

                    } else {
                        tvUserName.setText("Usuario no encontrado");
                        ivProfilePhoto.setImageResource(R.drawable.ic_default_profile);
                        Log.d("Dashboard", "No such document for UID: " + uid); // <-- Corregido
                    }
                } else {
                    tvUserName.setText("Error al cargar perfil");
                    ivProfilePhoto.setImageResource(R.drawable.ic_default_profile);
                    Log.d("Dashboard", "get failed with ", task.getException()); // <-- Corregido
                }
            });

            // --- OBTENER EL CORREO ---
            String email = user.getEmail();
            if (email != null && !email.isEmpty()) {
                tvUserEmail.setText(email);
            } else {
                tvUserEmail.setText("Email no disponible");
            }
        }
    }
}
