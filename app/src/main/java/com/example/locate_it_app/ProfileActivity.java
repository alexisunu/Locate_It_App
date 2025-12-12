package com.example.locate_it_app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";

    private CircleImageView ivProfilePicture;
    private TextView tvUserNameHeader, tvUserEmailHeader;
    private TextView tvPlacesCount, tvIncidentsCount;
    private TextView tvInfoName, tvInfoEmail, tvCreationDate;
    private Button btnLogout;
    private Toolbar toolbar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        toolbar = findViewById(R.id.toolbar);
        ivProfilePicture = findViewById(R.id.iv_profile_picture);
        tvUserNameHeader = findViewById(R.id.tv_user_name_header);
        tvUserEmailHeader = findViewById(R.id.tv_user_email_header);
        tvPlacesCount = findViewById(R.id.tv_places_count);
        tvIncidentsCount = findViewById(R.id.tv_incidents_count);
        tvInfoName = findViewById(R.id.tv_info_name);
        tvInfoEmail = findViewById(R.id.tv_info_email);
        tvCreationDate = findViewById(R.id.tv_creation_date);
        btnLogout = findViewById(R.id.btn_logout);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        loadUserProfile();

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Toast.makeText(ProfileActivity.this, "Has cerrado sesión", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void loadUserProfile() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            // Si no hay usuario, no hay nada que cargar
            return;
        }

        String uid = user.getUid();
        String email = user.getEmail();
        long creationTimestamp = user.getMetadata().getCreationTimestamp();
        String creationDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date(creationTimestamp));

        // Poblar datos básicos
        tvUserEmailHeader.setText(email);
        tvInfoEmail.setText(email);
        tvCreationDate.setText(creationDate);

        // Cargar datos desde el documento del usuario en Firestore
        db.collection("users").document(uid).get().addOnSuccessListener(document -> {
            if (document.exists()) {
                String name = document.getString("nombre");
                String photoUrlString = document.getString("fotoPerfil");

                tvUserNameHeader.setText(name);
                tvInfoName.setText(name);

                if (photoUrlString != null && !photoUrlString.isEmpty()) {
                    Picasso.get().load(photoUrlString).into(ivProfilePicture);
                } else {
                    ivProfilePicture.setImageResource(R.drawable.ic_profile_user);
                }
            }
        });

        // --- ¡NUEVA LÓGICA PARA LOS CONTADORES! ---
        // Contar lugares guardados
        db.collection("places").whereEqualTo("userId", uid).get()
            .addOnSuccessListener(queryDocumentSnapshots -> 
                tvPlacesCount.setText(String.valueOf(queryDocumentSnapshots.size()))
            )
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error al contar lugares", e);
                tvPlacesCount.setText("0");
            });

        // Contar incidentes reportados
        db.collection("incidents").whereEqualTo("userId", uid).get()
            .addOnSuccessListener(queryDocumentSnapshots -> 
                tvIncidentsCount.setText(String.valueOf(queryDocumentSnapshots.size()))
            )
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error al contar incidentes", e);
                tvIncidentsCount.setText("0");
            });
    }
}
