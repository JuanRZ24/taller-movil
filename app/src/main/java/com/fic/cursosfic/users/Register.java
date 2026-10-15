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

    // Inicializa los campos de registro y configura la opcion de regresar a la pantalla de login
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

    // Accion al presionar el boton registrar para validar todos los datos ingresados
    public void OnRegister(View view) {
        String name = etName.getText().toString().trim();
        String email = etEmail2.getText().toString().trim();
        String password = etPassword2.getText().toString().trim();
        String passwordConfirm = etPasswordConfirm.getText().toString().trim();
        String type = "register";

        // Revisa que ningun campo este vacio
        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || passwordConfirm.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos requeridos", Toast.LENGTH_SHORT).show();
            return;
        }

        // Revisa que el nombre no sea demasiado largo
        if (name.length() > 255) {
            etName.setError("El nombre es demasiado largo");
            etName.requestFocus();
            return;
        }

        // Valida que el formato del correo sea correcto
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

        // Revisa que la contraseña tenga minimo 8 caracteres
        if (password.length() < 8) {
            etPassword2.setError("La contraseña debe tener al menos 8 caracteres");
            etPassword2.requestFocus();
            return;
        }

        // Compara que la contraseña y su confirmación sean exactamente iguales
        if (!password.equals(passwordConfirm)) {
            etPasswordConfirm.setError("Las contraseñas no coinciden");
            etPasswordConfirm.requestFocus();
            return;
        }

        // Manda los datos del nuevo usuario al servidor y procesa el resultado
        BackgroundWorker backgroundWorker = new BackgroundWorker(this, (isSuccess, action, registeredEmail, result) -> {
            if (isSuccess) {
                Toast.makeText(Register.this, "¡Registro exitoso!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(Register.this, Login.class);
                intent.putExtra("email", registeredEmail);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            } else {
                handleRegisterErrors(result.body, result.code);
            }
        });
        backgroundWorker.execute(type, name, email, password, passwordConfirm);
    }

    // Procesa los errores del servidor asignandolos a cada caja de texto o mostrando el dialogo si el correo ya existe
    private void handleRegisterErrors(String body, int code) {
        if (code == -1 || body == null || body.isEmpty()) {
            Toast.makeText(this, "Servicio no disponible", Toast.LENGTH_LONG).show();
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

            // Si el correo ya existe le pregunta al usuario si prefiere iniciar sesion
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
                String msg = json.optString("message", "Servicio no disponible");
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Servicio no disponible", Toast.LENGTH_LONG).show();
        }
    }

    // Helper para sacar el primer mensaje de error de las respuestas en JSON de la API
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
