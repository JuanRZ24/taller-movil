package com.fic.cursosfic.users;

import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.fic.cursosfic.R;

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
            etName.setError("El nombre demasiado largo");
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
            etEmail2.setError("El correo electrónico demasiado largo");
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

        BackgroundWorker backgroundWorker = new BackgroundWorker(this);
        backgroundWorker.execute(type, name, email, password, passwordConfirm);
    }
}
