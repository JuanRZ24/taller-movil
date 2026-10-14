package com.fic.cursosfic.users;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.fic.cursosfic.R;

import java.util.concurrent.Executors;

public class Profile extends AppCompatActivity {

    private static final String PREFS = "auth_prefs";
    public static final String KEY_PROFILE_IMAGE = "profile_image_uri";

    private ImageView ivProfilePicture, ivCameraBadge;
    private TextView tvProfileName, tvProfileEmail, tvProfileRole, tvPhotoHint;
    private Button btnBecomeInstructor;

    private final ActivityResultLauncher<String> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    try {
                        getContentResolver().takePersistableUriPermission(
                                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                        );
                    } catch (Exception ignored) {
                    }

                    uploadPhotoToApi(uri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        Toolbar toolbar = findViewById(R.id.toolbarProfile);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
            }
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        ivProfilePicture = findViewById(R.id.ivProfilePicture);
        ivCameraBadge = findViewById(R.id.ivCameraBadge);
        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfileEmail = findViewById(R.id.tvProfileEmail);
        tvProfileRole = findViewById(R.id.tvProfileRole);
        tvPhotoHint = findViewById(R.id.tvPhotoHint);
        btnBecomeInstructor = findViewById(R.id.btnBecomeInstructor);

        loadUserData();
    }

    private void loadUserData() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String name = prefs.getString(BackgroundWorker.KEY_NAME, "No especificado");
        String email = prefs.getString(BackgroundWorker.KEY_EMAIL, "No especificado");
        String role = prefs.getString(BackgroundWorker.KEY_ROLE, "Estudiante");
        String imageUriStr = prefs.getString(KEY_PROFILE_IMAGE, "");

        if (name == null || name.isEmpty()) name = "No especificado";
        if (email == null || email.isEmpty()) email = "No especificado";
        if (role == null || role.isEmpty()) role = "Estudiante";

        tvProfileName.setText(name);
        tvProfileEmail.setText(email);
        tvProfileRole.setText(role.substring(0, 1).toUpperCase() + role.substring(1));

        boolean isInstructor = "instructor".equalsIgnoreCase(role) || "profesor".equalsIgnoreCase(role);

        if (isInstructor) {
            // Usuario es Instructor -> Habilitar cambio de foto
            btnBecomeInstructor.setVisibility(View.GONE);
            ivCameraBadge.setVisibility(View.VISIBLE);
            tvPhotoHint.setVisibility(View.VISIBLE);

            View.OnClickListener pickPhotoListener = v -> imagePickerLauncher.launch("image/*");
            ivProfilePicture.setOnClickListener(pickPhotoListener);
            ivCameraBadge.setOnClickListener(pickPhotoListener);

            loadProfileImage(imageUriStr);

        } else {
            // Usuario es Estudiante -> Mostrar opción para volverse Instructor
            btnBecomeInstructor.setVisibility(View.VISIBLE);
            ivCameraBadge.setVisibility(View.GONE);
            tvPhotoHint.setVisibility(View.GONE);

            ivProfilePicture.setOnClickListener(v ->
                    Toast.makeText(Profile.this, "Solo los Instructores pueden cambiar su foto de perfil.", Toast.LENGTH_SHORT).show()
            );

            btnBecomeInstructor.setOnClickListener(v -> showBecomeInstructorDialog());
            loadProfileImage("");
        }
    }

    private void loadProfileImage(String uriString) {
        if (uriString != null && !uriString.isEmpty()) {
            if (uriString.startsWith("http://") || uriString.startsWith("https://")) {
                Executors.newSingleThreadExecutor().execute(() -> {
                    try {
                        java.io.InputStream in = new java.net.URL(uriString).openStream();
                        android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(in);
                        runOnUiThread(() -> {
                            if (bitmap != null) {
                                ivProfilePicture.setImageTintList(null);
                                ivProfilePicture.clearColorFilter();
                                ivProfilePicture.setImageBitmap(bitmap);
                            } else {
                                setDefaultProfileImage();
                            }
                        });
                    } catch (Exception e) {
                        runOnUiThread(this::setDefaultProfileImage);
                    }
                });
            } else {
                try {
                    Uri uri = Uri.parse(uriString);
                    ivProfilePicture.setImageTintList(null);
                    ivProfilePicture.clearColorFilter();
                    ivProfilePicture.setImageURI(uri);
                } catch (Exception e) {
                    setDefaultProfileImage();
                }
            }
        } else {
            setDefaultProfileImage();
        }
    }

    private void setDefaultProfileImage() {
        ivProfilePicture.setImageResource(android.R.drawable.ic_menu_myplaces);
        ivProfilePicture.setImageTintList(android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.brand_header)
        ));
    }

    private void showBecomeInstructorDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 30, 50, 10);

        final EditText etHeadline = new EditText(this);
        etHeadline.setHint("Titular (ej. Ingeniero de Software)");
        layout.addView(etHeadline);

        final EditText etSpecialty = new EditText(this);
        etSpecialty.setHint("Especialidad (ej. Desarrollo Móvil Android)");
        layout.addView(etSpecialty);

        final EditText etBio = new EditText(this);
        etBio.setHint("Biografía (ej. Apasionado por la tecnología...)");
        layout.addView(etBio);

        new AlertDialog.Builder(this)
                .setTitle("Volverse Instructor")
                .setMessage("Esta acción NO se puede deshacer. Por favor completa la siguiente información para tu perfil de instructor:")
                .setView(layout)
                .setPositiveButton("Confirmar y Guardar", (dialog, which) -> {
                    String headline = etHeadline.getText().toString().trim();
                    String specialty = etSpecialty.getText().toString().trim();
                    String bio = etBio.getText().toString().trim();

                    if (headline.isEmpty() || specialty.isEmpty() || bio.isEmpty()) {
                        Toast.makeText(this, "Todos los campos (Titular, Especialidad y Biografía) son requeridos", Toast.LENGTH_LONG).show();
                        return;
                    }
                    sendRoleChangeToApi(headline, specialty, bio);
                })
                .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void sendRoleChangeToApi(String headline, String specialty, String bio) {
        Toast.makeText(this, "Enviando datos al servidor...", Toast.LENGTH_SHORT).show();
        Executors.newSingleThreadExecutor().execute(() -> {
            BackgroundWorker.ApiResult result = BackgroundWorker.changeRoleToInstructor(this, headline, specialty, bio);
            runOnUiThread(() -> {
                if (result.isSuccess()) {
                    SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
                    prefs.edit().putString(BackgroundWorker.KEY_ROLE, "instructor").apply();

                    Toast.makeText(Profile.this, "¡Felicidades! Ahora eres Instructor", Toast.LENGTH_LONG).show();
                    loadUserData();
                } else {
                    String detailMsg = "Código HTTP " + result.code + "\n" + BackgroundWorker.humanMessage(result.body);
                    new AlertDialog.Builder(Profile.this)
                            .setTitle("Error del servidor")
                            .setMessage(detailMsg)
                            .setPositiveButton("Entendido", (d, w) -> d.dismiss())
                            .show();
                }
            });
        });
    }

    private void uploadPhotoToApi(Uri imageUri) {
        Toast.makeText(this, "Subiendo foto de perfil al servidor...", Toast.LENGTH_SHORT).show();
        Executors.newSingleThreadExecutor().execute(() -> {
            BackgroundWorker.ApiResult result = BackgroundWorker.uploadPhoto(this, imageUri);
            runOnUiThread(() -> {
                if (result.isSuccess()) {
                    try {
                        org.json.JSONObject json = new org.json.JSONObject(result.body);
                        String photoUrl = json.optString("photo_url", json.optString("url", json.optString("photo", "")));
                        if (!photoUrl.isEmpty()) {
                            getSharedPreferences(PREFS, MODE_PRIVATE)
                                    .edit().putString(KEY_PROFILE_IMAGE, photoUrl).apply();
                        } else {
                            getSharedPreferences(PREFS, MODE_PRIVATE)
                                    .edit().putString(KEY_PROFILE_IMAGE, imageUri.toString()).apply();
                        }
                    } catch (Exception e) {
                        getSharedPreferences(PREFS, MODE_PRIVATE)
                                .edit().putString(KEY_PROFILE_IMAGE, imageUri.toString()).apply();
                    }
                    loadProfileImage(imageUri.toString());
                    Toast.makeText(Profile.this, "¡Foto de perfil actualizada en el servidor!", Toast.LENGTH_SHORT).show();
                } else {
                    String detailMsg = "Código HTTP " + result.code + "\n" + BackgroundWorker.humanMessage(result.body);
                    new AlertDialog.Builder(Profile.this)
                            .setTitle("Error al subir foto")
                            .setMessage(detailMsg)
                            .setPositiveButton("Entendido", (d, w) -> d.dismiss())
                            .show();
                }
            });
        });
    }
}
