package com.fic.cursosfic.users;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.fic.cursosfic.R;

import org.json.JSONArray;
import org.json.JSONObject;

public class Register extends AppCompatActivity {

    private EditText etName, etEmail2, etPassword2, etPasswordConfirm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        etName = findViewById(R.id.etName);
        etEmail2 = findViewById(R.id.etEmail2);
        etPassword2 = findViewById(R.id.etPassword2);
        etPasswordConfirm = findViewById(R.id.etPasswordConfirm);
        TextView tvIrLogin = findViewById(R.id.tvIrLogin);
        tvIrLogin.setOnClickListener(v -> finish());
    }

    public void OnRegister(View view) {
        String name = etName.getText().toString().trim();
        String email = etEmail2.getText().toString().trim();
        String password = etPassword2.getText().toString().trim();
        String passwordConfirm = etPasswordConfirm.getText().toString().trim();
        String type = "register";

        // 1. Validaciones de campos requeridos
        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || passwordConfirm.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos requeridos", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. Validación de nombre
        if (name.length() > 255) {
            etName.setError("El nombre es demasiado largo");
            etName.requestFocus();
            return;
        }

        // 3. Validación de email
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail2.setError("El formato del correo electrónico no es válido");
            etEmail2.requestFocus();
            return;
        }
        if (email.length() > 255) {
            etEmail2.setError("El correo electrónico es demasiado largo");
            etEmail2.requestFocus();
            return;
        }

        // 4. Validación de contraseña (mínimo 8 caracteres)
        if (password.length() < 8) {
            etPassword2.setError("La contraseña debe tener al menos 8 caracteres");
            etPassword2.requestFocus();
            return;
        }

        // 5. Validación de confirmación de contraseña (debe coincidir con la contraseña)
        if (!password.equals(passwordConfirm)) {
            etPasswordConfirm.setError("Las contraseñas no coinciden");
            etPasswordConfirm.requestFocus();
            return;
        }

        BackgroundWorker backgroundWorker = new BackgroundWorker(this, (isSuccess, action, registeredEmail, result) -> {
            if (isSuccess) {
                Toast.makeText(Register.this, "¡Registro exitoso!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(Register.this, Login.class);
                intent.putExtra("email", registeredEmail);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            } else {
                handleRegisterErrors(result.body);
            }
        });
        backgroundWorker.execute(type, name, email, password, passwordConfirm);
    }

    private void handleRegisterErrors(String body) {
        if (body == null || body.isEmpty()) {
            Toast.makeText(this, "Error de conexión con el servidor", Toast.LENGTH_LONG).show();
            return;
        }
        try {
            JSONObject json = new JSONObject(body);
            JSONObject errors = json.optJSONObject("errors");
            boolean fieldErrorSet = false;
            boolean isEmailTaken = false;

            if (errors != null) {
                if (errors.has("name")) {
                    etName.setError(getFirstErrorMessage(errors, "name"));
                    if (!fieldErrorSet) { etName.requestFocus(); fieldErrorSet = true; }
                }
                if (errors.has("email")) {
                    String emailMsg = getFirstErrorMessage(errors, "email");
                    etEmail2.setError(emailMsg);
                    isEmailTaken = true;
                    if (!fieldErrorSet) { etEmail2.requestFocus(); fieldErrorSet = true; }
                }
                if (errors.has("password")) {
                    etPassword2.setError(getFirstErrorMessage(errors, "password"));
                    if (!fieldErrorSet) { etPassword2.requestFocus(); fieldErrorSet = true; }
                }
                if (errors.has("password_confirmation") || errors.has("passwordConfirm")) {
                    String key = errors.has("password_confirmation") ? "password_confirmation" : "passwordConfirm";
                    etPasswordConfirm.setError(getFirstErrorMessage(errors, key));
                    if (!fieldErrorSet) { etPasswordConfirm.requestFocus(); fieldErrorSet = true; }
                }
            }

            if (isEmailTaken) {
                String currentEmail = etEmail2.getText().toString().trim();
                new AlertDialog.Builder(this)
                        .setTitle("Correo ya registrado")
                        .setMessage("El correo '" + currentEmail + "' ya se encuentra registrado. ¿Deseas iniciar sesión con este correo?")
                        .setPositiveButton("Sí, iniciar sesión", (dialog, which) -> {
                            Intent intent = new Intent(Register.this, Login.class);
                            intent.putExtra("email", currentEmail);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            startActivity(intent);
                            finish();
                        })
                        .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss())
                        .show();
            } else if (!fieldErrorSet) {
                String msg = json.optString("message", "Error al registrar el usuario");
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, BackgroundWorker.humanMessage(body), Toast.LENGTH_LONG).show();
        }
    }

    private String getFirstErrorMessage(JSONObject errorsObj, String key) {
        Object val = errorsObj.opt(key);
        if (val instanceof JSONArray) {
            JSONArray arr = (JSONArray) val;
            return arr.optString(0, "Campo inválido");
        } else if (val != null) {
            return val.toString();
        }
        return "Campo inválido";
    }
}
