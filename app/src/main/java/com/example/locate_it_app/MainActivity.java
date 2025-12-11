package com.example.locate_it_app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private EditText etEmail, etPassword;
    private Button btnLogin, btnRegister;
    private TextView tvRegistrar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Inicializar firebase
        mAuth = FirebaseAuth.getInstance();

        //Vinclar vistas
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegistrar = findViewById(R.id.tvRegistrar);

        //Configurar boton Login
        btnLogin.setOnClickListener(view -> LonginUser());

        tvRegistrar.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(intent);
        });


    }

    public void LonginUser(){
        String email = etEmail.getText().toString();
        String password = etPassword.getText().toString();

        //validaciones
        if(email.isEmpty() || password.isEmpty()){
            Toast.makeText(this, "Por favor ingrese todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }
        if(password.length() < 6){
            Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = mAuth.getCurrentUser();
                Toast.makeText(this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show();
                // Ir al Dashboard
                Intent intent = new Intent(MainActivity.this, dashboard.class);
                startActivity(intent);

            } else {
                Toast.makeText(this, "Error en el inicio de sesión", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Verificar si ya hay un usuario logueado
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // Usuario ya está logueado, ir al Dashboard
            Toast.makeText(this, "Ya tienes sesión activa", Toast.LENGTH_SHORT).show();
            // goToDashboard(); // Descomenta cuando crees el Dashboard
        }
    }
}