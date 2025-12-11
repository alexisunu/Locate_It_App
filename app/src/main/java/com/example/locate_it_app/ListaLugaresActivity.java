package com.example.locate_it_app;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ListaLugaresActivity extends AppCompatActivity {

    private RecyclerView recyclerViewLugares;
    private EditText etSearch;
    private Spinner spinnerCategory;
    private LugaresAdapter adapter;
    private List<Lugar> allLugares = new ArrayList<>();

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_lugares);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // --- Configuración de la Interfaz ---
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        etSearch = findViewById(R.id.et_search);
        spinnerCategory = findViewById(R.id.spinner_category);
        recyclerViewLugares = findViewById(R.id.rv_lugares);

        // --- Configuración del RecyclerView ---
        recyclerViewLugares.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LugaresAdapter(this, new ArrayList<>()); // Empezar con una lista vacía
        recyclerViewLugares.setAdapter(adapter);

        // --- Carga de Datos y Filtros ---
        loadLugaresFromFirestore();
        setupFilterListeners();
    }

    private void loadLugaresFromFirestore() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();

        db.collection("places").whereEqualTo("userId", userId).get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                allLugares.clear();
                Set<String> categories = new HashSet<>();

                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    Lugar lugar = document.toObject(Lugar.class);
                    lugar.setDocumentId(document.getId());
                    allLugares.add(lugar);
                    categories.add(lugar.getCategoria());
                }
                
                // Poblar el spinner con las categorías encontradas
                setupSpinner(new ArrayList<>(categories));
                
                // Mostrar todos los lugares al principio
                filter(); 
            });
    }

    private void setupSpinner(List<String> categories) {
        categories.add(0, "Todas"); // Añadir la opción "Todas" al principio
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(spinnerAdapter);
    }

    private void setupFilterListeners() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter();
            }
            @Override public void afterTextChanged(Editable s) { }
        });

        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filter();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    private void filter() {
        String searchText = etSearch.getText().toString().toLowerCase().trim();
        String selectedCategory = spinnerCategory.getSelectedItem() != null ? spinnerCategory.getSelectedItem().toString() : "Todas";

        List<Lugar> filteredList = new ArrayList<>();
        for (Lugar lugar : allLugares) {
            boolean nameMatches = lugar.getNombre().toLowerCase().contains(searchText);
            boolean categoryMatches = selectedCategory.equals("Todas") || lugar.getCategoria().equalsIgnoreCase(selectedCategory);

            if (nameMatches && categoryMatches) {
                filteredList.add(lugar);
            }
        }
        adapter.filterList(filteredList);
    }
}
