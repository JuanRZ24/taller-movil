package com.fic.cursosfic.courses;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.fic.cursosfic.R;
import com.fic.cursosfic.users.BackgroundWorker;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class CreateCourse extends AppCompatActivity {

    private EditText etCourseTitle, etCourseDescription;
    private Spinner spinnerCategory, spinnerLevel;
    private Button btnPublishCourse;

    private final List<String> categoryDisplayList = new ArrayList<>();
    private final List<Integer> categoryIdList = new ArrayList<>();

    // Niveles soportados por la API
    private final String[] levelDisplay = {
            "Principiante",
            "Intermedio",
            "Avanzado"
    };
    private final String[] levelValues = {"beginner", "intermediate", "advanced"};

    // Inicializa la pantalla de crear curso y configura la barra superior
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_course);

        Toolbar toolbar = findViewById(R.id.toolbarCreateCourse);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
            }
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        etCourseTitle = findViewById(R.id.etCourseTitle);
        etCourseDescription = findViewById(R.id.etCourseDescription);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerLevel = findViewById(R.id.spinnerLevel);
        btnPublishCourse = findViewById(R.id.btnPublishCourse);

        setupLevelSpinner();
        loadCategoriesFromApi();

        btnPublishCourse.setOnClickListener(v -> publishCourse());
    }

    // Configura el menu desplegable para elegir el nivel de dificultad del curso
    private void setupLevelSpinner() {
        ArrayAdapter<String> levelAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, levelDisplay
        );
        levelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLevel.setAdapter(levelAdapter);
    }

    // Consulta la API mediante GET /categories para llenar el menu desplegable con categorias reales
    private void loadCategoriesFromApi() {
        categoryDisplayList.clear();
        categoryIdList.clear();

        Executors.newSingleThreadExecutor().execute(() -> {
            BackgroundWorker.ApiResult result = BackgroundWorker.getCategories(this);
            runOnUiThread(() -> {
                if (result.isSuccess()) {
                    try {
                        String bodyTrimmed = result.body.trim();
                        JSONArray array = null;
                        if (bodyTrimmed.startsWith("[")) {
                            array = new JSONArray(bodyTrimmed);
                        } else {
                            JSONObject json = new JSONObject(bodyTrimmed);
                            array = json.optJSONArray("categories");
                        }

                        if (array != null && array.length() > 0) {
                            for (int i = 0; i < array.length(); i++) {
                                JSONObject catObj = array.optJSONObject(i);
                                if (catObj != null && catObj.has("id")) {
                                    int id = catObj.optInt("id");
                                    String name = catObj.optString("name", "Categoría " + id);
                                    categoryIdList.add(id);
                                    categoryDisplayList.add(id + " - " + name);
                                }
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }

                if (categoryDisplayList.isEmpty()) {
                    Toast.makeText(CreateCourse.this, "Aviso: No se encontraron categorías en la base de datos (GET /categories)", Toast.LENGTH_LONG).show();
                } else {
                    ArrayAdapter<String> catAdapter = new ArrayAdapter<>(
                            CreateCourse.this, android.R.layout.simple_spinner_item, categoryDisplayList
                    );
                    catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerCategory.setAdapter(catAdapter);
                }
            });
        });
    }

    // Valida el formulario y manda la peticion POST /courses al servidor para publicar el curso
    private void publishCourse() {
        String title = etCourseTitle.getText().toString().trim();
        String description = etCourseDescription.getText().toString().trim();

        if (title.isEmpty()) {
            etCourseTitle.setError("El título del curso es requerido");
            etCourseTitle.requestFocus();
            return;
        }

        if (description.isEmpty()) {
            etCourseDescription.setError("La descripción es requerida");
            etCourseDescription.requestFocus();
            return;
        }

        if (categoryIdList.isEmpty()) {
            Toast.makeText(this, "No hay categorías válidas cargadas desde el servidor", Toast.LENGTH_LONG).show();
            return;
        }

        int catPos = spinnerCategory.getSelectedItemPosition();
        int categoryId = (catPos >= 0 && catPos < categoryIdList.size()) ? categoryIdList.get(catPos) : categoryIdList.get(0);

        int levelPos = spinnerLevel.getSelectedItemPosition();
        String level = (levelPos >= 0 && levelPos < levelValues.length) ? levelValues[levelPos] : "beginner";

        btnPublishCourse.setEnabled(false);
        Toast.makeText(this, "Publicando curso en el servidor...", Toast.LENGTH_SHORT).show();

        Executors.newSingleThreadExecutor().execute(() -> {
            BackgroundWorker.ApiResult result = BackgroundWorker.createCourse(
                    CreateCourse.this, title, description, categoryId, level
            );

            runOnUiThread(() -> {
                btnPublishCourse.setEnabled(true);
                if (result.isSuccess()) {
                    Toast.makeText(CreateCourse.this, "¡Curso creado con éxito!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(CreateCourse.this, CourseDetail.class);
                    intent.putExtra("course_json", result.body);
                    startActivity(intent);
                    finish();
                } else {
                    String detailMsg = "Código HTTP " + result.code + "\n" + BackgroundWorker.humanMessage(result.body);
                    new AlertDialog.Builder(CreateCourse.this)
                            .setTitle("Error al publicar el curso")
                            .setMessage(detailMsg)
                            .setPositiveButton("Entendido", (dialog, which) -> dialog.dismiss())
                            .show();
                }
            });
        });
    }
}
