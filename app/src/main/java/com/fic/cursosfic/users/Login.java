package com.fic.cursosfic.users;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.fic.cursosfic.R;

public class Login extends AppCompatActivity {
    private EditText etEmail, etPassword;
    private TextView btnregister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);

        btnregister = findViewById(R.id.btnregister);
        btnregister.setOnClickListener(v -> {
            Intent intent = new Intent(Login.this, Register.class);
            startActivity(intent);
        });
    }

    public void OnLogin(View view) {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // 1. Validar campos requeridos
        if (email.isEmpty()) {
            etEmail.setError("El correo electrónico es requerido");
            etEmail.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            etPassword.setError("La contraseña es requerida");
            etPassword.requestFocus();
            return;
        }

        // 2. Validar formato de email
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("El formato del correo electrónico no es válido");
            etEmail.requestFocus();
            return;
        }

        BackgroundWorker backgroundWorker = new BackgroundWorker(this);
        backgroundWorker.execute("login", email, password);
    }
}
