package com.example.locate_it_app;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText etNombre, etEmail, etPassword, etConfirmPassword;
    private Button btnRegister;
    private TextView tvLogin;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        // Firebase
        mAuth = FirebaseAuth.getInstance();

        // Vincular vistas
        etNombre = findViewById(R.id.etNombre);
        etEmail = findViewById(R.id.etEmailReg);
        etPassword = findViewById(R.id.etPasswordReg);
        etConfirmPassword = findViewById(R.id.etConfirmPasswordReg);
        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLogin);

        btnRegister.setOnClickListener(v -> registerUser());

        tvLogin.setOnClickListener(v -> finish());
    }

    private void registerUser() {
        String nombre = etNombre.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();
        String confirm = etConfirmPassword.getText().toString();

        // Validaciones
        if (nombre.isEmpty()) {
            etNombre.setError("Ingrese su nombre completo");
            etNombre.requestFocus();
            return;
        }
        if (email.isEmpty()) {
            etEmail.setError("Ingrese su correo");
            etEmail.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            etPassword.setError("Ingrese una contraseña");
            etPassword.requestFocus();
            return;
        }
        if (password.length() < 6) {
            etPassword.setError("Debe tener al menos 6 caracteres");
            etPassword.requestFocus();
            return;
        }
        if (!password.equals(confirm)) {
            etConfirmPassword.setError("Las contraseñas no coinciden");
            etConfirmPassword.requestFocus();
            return;
        }

        // Verificar si el correo ya existe
        mAuth.fetchSignInMethodsForEmail(email)
                .addOnCompleteListener(fetchTask -> {
                    if (!fetchTask.isSuccessful()) {
                        Toast.makeText(this,
                                "Error al verificar correo: " + fetchTask.getException(),
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    List<String> methods = fetchTask.getResult().getSignInMethods();

                    if (methods != null && !methods.isEmpty()) {
                        Toast.makeText(this,
                                "Este correo ya está registrado.",
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    // Crear usuario
                    mAuth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener(task -> {
                                if (!task.isSuccessful()) {
                                    Exception e = task.getException();
                                    if (e instanceof FirebaseAuthUserCollisionException) {
                                        Toast.makeText(this,
                                                "Este correo ya está registrado.",
                                                Toast.LENGTH_LONG).show();
                                    } else {
                                        Toast.makeText(this,
                                                "Error: " + e.getMessage(),
                                                Toast.LENGTH_LONG).show();
                                    }
                                    return;
                                }

                                FirebaseUser user = mAuth.getCurrentUser();
                                if (user == null) {
                                    Toast.makeText(this,
                                            "Error: usuario es null.",
                                            Toast.LENGTH_LONG).show();
                                    return;
                                }

                                // Actualizar nombre en Auth
                                UserProfileChangeRequest profileUpdates =
                                        new UserProfileChangeRequest.Builder()
                                                .setDisplayName(nombre)
                                                .build();

                                user.updateProfile(profileUpdates);

                                // Guardar en Firestore
                                FirebaseFirestore db = FirebaseFirestore.getInstance();

                                Map<String, Object> userMap = new HashMap<>();
                                userMap.put("nombre", nombre);
                                userMap.put("email", email);
                                userMap.put("fotoPerfil", null);
                                userMap.put("fechaRegistro", Timestamp.now()); // 🔥 Timestamp real

                                db.collection("users")
                                        .document(user.getUid())
                                        .set(userMap)
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(this,
                                                    "Usuario registrado correctamente",
                                                    Toast.LENGTH_SHORT).show();
                                            finish();
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(this,
                                                    "Error al guardar usuario: " + e.getMessage(),
                                                    Toast.LENGTH_LONG).show();
                                        });
                            });
                });
    }
}
