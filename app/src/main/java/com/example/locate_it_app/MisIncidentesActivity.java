package com.example.locate_it_app;

import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class MisIncidentesActivity extends AppCompatActivity {

    private static final String TAG = "MisIncidentesActivity";

    private RecyclerView rvIncidentes;
    private IncidenteAdapter incidenteAdapter;
    private List<Incidente> incidenteList;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mis_incidentes);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        rvIncidentes = findViewById(R.id.rv_incidentes);
        rvIncidentes.setLayoutManager(new LinearLayoutManager(this));

        incidenteList = new ArrayList<>();
        incidenteAdapter = new IncidenteAdapter(this, incidenteList);
        rvIncidentes.setAdapter(incidenteAdapter);

        cargarIncidentes();
    }

    private void cargarIncidentes() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "Usuario no autenticado.");
            return;
        }

        String userId = currentUser.getUid();

        db.collection("incidents")
                .whereEqualTo("userId", userId)
                .orderBy("fechaDeCreacion", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        incidenteList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Incidente incidente = document.toObject(Incidente.class);
                            incidente.setId(document.getId());
                            incidenteList.add(incidente);
                        }
                        incidenteAdapter.notifyDataSetChanged();
                    } else {
                        Log.e(TAG, "Error al cargar los incidentes: ", task.getException());
                    }
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
