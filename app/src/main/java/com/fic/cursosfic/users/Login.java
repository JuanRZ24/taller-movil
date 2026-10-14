package com.fic.cursosfic.users;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.fic.cursosfic.R;
import com.fic.cursosfic.menu.menu;

import org.json.JSONArray;
import org.json.JSONObject;

public class Login extends AppCompatActivity {
    private EditText etEmail, etPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);

        TextView btnregister = findViewById(R.id.btnregister);
        btnregister.setOnClickListener(v -> {
            Intent intent = new Intent(Login.this, Register.class);
            startActivity(intent);
        });

        checkIntentForEmail(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        checkIntentForEmail(intent);
    }

    private void checkIntentForEmail(Intent intent) {
        if (intent != null && intent.hasExtra("email")) {
            String email = intent.getStringExtra("email");
            if (email != null && !email.isEmpty()) {
                etEmail.setText(email);
                etPassword.requestFocus();
            }
        }
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

        BackgroundWorker backgroundWorker = new BackgroundWorker(this, (isSuccess, action, loggedEmail, result) -> {
            if (isSuccess) {
                Toast.makeText(Login.this, "¡Inicio de sesión exitoso!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(Login.this, menu.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            } else {
                handleLoginErrors(result.body);
            }
        });
        backgroundWorker.execute("login", email, password);
    }

    private void handleLoginErrors(String body) {
        if (body == null || body.isEmpty()) {
            Toast.makeText(this, "Error de conexión con el servidor", Toast.LENGTH_LONG).show();
            return;
        }
        try {
            JSONObject json = new JSONObject(body);
            JSONObject errors = json.optJSONObject("errors");
            if (errors != null) {
                if (errors.has("email")) {
                    etEmail.setError(getFirstErrorMessage(errors, "email"));
                    etEmail.requestFocus();
                    return;
                }
                if (errors.has("password")) {
                    etPassword.setError(getFirstErrorMessage(errors, "password"));
                    etPassword.requestFocus();
                    return;
                }
            }
            String msg = json.optString("message", "Credenciales incorrectas");
            etPassword.setError(msg);
            etPassword.requestFocus();
        } catch (Exception e) {
            Toast.makeText(this, BackgroundWorker.humanMessage(body), Toast.LENGTH_LONG).show();
        }
    }

    private String getFirstErrorMessage(JSONObject errorsObj, String key) {
        Object val = errorsObj.opt(key);
        if (val instanceof JSONArray) {
            JSONArray arr = (JSONArray) val;
            return arr.optString(0, "Error en el campo");
        } else if (val != null) {
            return val.toString();
        }
        return "Error en el campo";
    }
}
