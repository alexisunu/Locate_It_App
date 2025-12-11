package com.example.locate_it_app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class dashboard extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private CircleImageView ivProfilePhoto;
    private TextView tvUserName, tvUserEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dashboard);

        mAuth = FirebaseAuth.getInstance();

        ivProfilePhoto = findViewById(R.id.ivProfilePhoto);
        tvUserName = findViewById(R.id.tvUserName);
        tvUserEmail = findViewById(R.id.tvUserEmail);

        // --- Solución Definitiva: Usando los NUEVOS IDs del XML ---
        CardView cardLugares = findViewById(R.id.card_lugares);
        CardView cardIncidentes = findViewById(R.id.card_incidentes);
        CardView cardPerfil = findViewById(R.id.card_perfil);

        loadUserProfile();

        cardLugares.setOnClickListener(v -> {
            Intent intent = new Intent(dashboard.this, lugares_mapa.class);
            startActivity(intent);
        });

        cardIncidentes.setOnClickListener(v -> {
            Intent intent = new Intent(dashboard.this, incidentes_mapa.class);
            startActivity(intent);
        });

        cardPerfil.setOnClickListener(v -> {
            Intent intent = new Intent(dashboard.this, ProfileActivity.class);
            startActivity(intent);
        });
    }

    private void loadUserProfile() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            db.collection("users").document(uid).get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document != null && document.exists()) {
                        String name = document.getString("nombre");
                        if (name != null && !name.isEmpty()) {
                            tvUserName.setText("Hola, " + name);
                        } else {
                            tvUserName.setText("Hola, Usuario");
                        }

                        String photoUrlString = document.getString("fotoPerfil");
                        if (photoUrlString != null && !photoUrlString.isEmpty()) {
                            Picasso.get()
                                    .load(photoUrlString)
                                    .placeholder(R.drawable.ic_user_verified)
                                    .error(R.drawable.ic_user_verified)
                                    .into(ivProfilePhoto);
                        } else {
                            ivProfilePhoto.setImageResource(R.drawable.ic_user_verified);
                        }

                    } else {
                        tvUserName.setText("Usuario no encontrado");
                        ivProfilePhoto.setImageResource(R.drawable.ic_user_verified);
                        Log.d("Dashboard", "No such document for UID: " + uid);
                    }
                } else {
                    tvUserName.setText("Error al cargar perfil");
                    ivProfilePhoto.setImageResource(R.drawable.ic_user_verified);
                    Log.d("Dashboard", "get failed with ", task.getException());
                }
            });

            String email = user.getEmail();
            if (email != null && !email.isEmpty()) {
                tvUserEmail.setText(email);
            } else {
                tvUserEmail.setText("Email no disponible");
            }
        }
    }
}
