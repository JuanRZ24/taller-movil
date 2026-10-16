package com.fic.cursosfic.courses;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.fic.cursosfic.R;
import com.fic.cursosfic.users.BackgroundWorker;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class MyCourses extends AppCompatActivity {

    private TextView emptyCoursesTextView;
    private LinearLayout coursesContainer;

    private final List<JSONObject> courseJsonList = new ArrayList<>();

    // Inicializa la pantalla de Mis Cursos y la barra superior
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_courses);

        Toolbar toolbar = findViewById(R.id.toolbarMyCourses);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
            }
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        emptyCoursesTextView = findViewById(R.id.emptyCoursesTextView);
        coursesContainer = findViewById(R.id.coursesContainer);
    }

    // Refresca la lista de cursos desde el servidor cada vez que la pantalla vuelve a estar activa
    @Override
    protected void onResume() {
        super.onResume();
        loadInstructorCoursesFromApi();
    }

    // Consulta la API GET /instructors/me/courses para obtener los cursos creados por el instructor
    private void loadInstructorCoursesFromApi() {
        courseJsonList.clear();
        coursesContainer.removeAllViews();
        emptyCoursesTextView.setVisibility(View.GONE);

        Executors.newSingleThreadExecutor().execute(() -> {
            BackgroundWorker.ApiResult result = BackgroundWorker.getInstructorCourses(this);
            runOnUiThread(() -> {
                if (result.isSuccess()) {
                    try {
                        String bodyTrimmed = result.body.trim();
                        JSONArray array = null;
                        if (bodyTrimmed.startsWith("[")) {
                            array = new JSONArray(bodyTrimmed);
                        } else {
                            JSONObject json = new JSONObject(bodyTrimmed);
                            array = json.optJSONArray("courses");
                            if (array == null) array = json.optJSONArray("data");
                        }

                        if (array != null && array.length() > 0) {
                            for (int i = 0; i < array.length(); i++) {
                                JSONObject cObj = array.optJSONObject(i);
                                if (cObj != null) {
                                    courseJsonList.add(cObj);
                                }
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }

                renderCourses();
            });
        });
    }

    // Renderiza las tarjetas de los cursos pendientes o creados por el instructor
    private void renderCourses() {
        coursesContainer.removeAllViews();

        if (courseJsonList.isEmpty()) {
            emptyCoursesTextView.setVisibility(View.VISIBLE);
            return;
        }

        emptyCoursesTextView.setVisibility(View.GONE);

        for (int i = 0; i < courseJsonList.size(); i++) {
            final JSONObject courseObj = courseJsonList.get(i);

            String title = courseObj.optString("title", "Curso sin título");
            String description = courseObj.optString("description", "Sin descripción");
            String level = courseObj.optString("level", "beginner");
            String status = courseObj.optString("status", "draft");

            JSONObject catObj = courseObj.optJSONObject("category");
            String categoryName = (catObj != null) ? catObj.optString("name", "General") : "General";

            // Formato de insignia para el estado
            String statusBadge;
            int statusColor;
            if ("published".equalsIgnoreCase(status)) {
                statusBadge = "✅ Publicado";
                statusColor = Color.parseColor("#2E7D32");
            } else if ("pending".equalsIgnoreCase(status) || "review".equalsIgnoreCase(status)) {
                statusBadge = "⏳ En Revisión / Pendiente";
                statusColor = Color.parseColor("#E65100");
            } else if ("rejected".equalsIgnoreCase(status)) {
                statusBadge = "❌ Rechazado";
                statusColor = Color.parseColor("#C62828");
            } else {
                statusBadge = "📝 Borrador (Draft)";
                statusColor = Color.parseColor("#1565C0");
            }

            CardView card = new CardView(this);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            );
            cardParams.setMargins(0, 0, 0, 24);
            card.setLayoutParams(cardParams);
            card.setRadius(12f);
            card.setCardElevation(4f);
            card.setCardBackgroundColor(Color.WHITE);

            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(24, 24, 24, 24);

            TextView tvTitle = new TextView(this);
            tvTitle.setText(title);
            tvTitle.setTextSize(18f);
            tvTitle.setTextColor(Color.BLACK);
            tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
            layout.addView(tvTitle);

            TextView tvStatus = new TextView(this);
            tvStatus.setText("Estado: " + statusBadge);
            tvStatus.setTextSize(13f);
            tvStatus.setTextColor(statusColor);
            tvStatus.setTypeface(null, android.graphics.Typeface.BOLD);
            tvStatus.setPadding(0, 6, 0, 12);
            layout.addView(tvStatus);

            TextView tvMeta = new TextView(this);
            tvMeta.setText("Categoría: " + categoryName + "  |  Nivel: " + level);
            tvMeta.setTextSize(13f);
            tvMeta.setTextColor(Color.GRAY);
            tvMeta.setPadding(0, 0, 0, 8);
            layout.addView(tvMeta);

            TextView tvDesc = new TextView(this);
            tvDesc.setText(description);
            tvDesc.setTextSize(14f);
            tvDesc.setTextColor(Color.DKGRAY);
            tvDesc.setPadding(0, 0, 0, 16);
            layout.addView(tvDesc);

            Button btnManage = new Button(this);
            btnManage.setText("Gestionar Módulos y Contenidos");
            btnManage.setTextColor(Color.WHITE);
            btnManage.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.brand_header));
            btnManage.setOnClickListener(v -> {
                Intent intent = new Intent(MyCourses.this, CourseDetail.class);
                intent.putExtra("course_json", courseObj.toString());
                startActivity(intent);
            });
            layout.addView(btnManage);

            card.addView(layout);
            coursesContainer.addView(card);
        }
    }
}
