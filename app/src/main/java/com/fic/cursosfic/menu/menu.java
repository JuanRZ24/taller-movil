package com.fic.cursosfic.menu;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.fic.cursosfic.R;
import com.fic.cursosfic.courses.CourseDetail;
import com.fic.cursosfic.courses.CreateCourse;
import com.fic.cursosfic.courses.MyCourses;
import com.fic.cursosfic.users.BackgroundWorker;
import com.fic.cursosfic.users.Login;
import com.fic.cursosfic.users.Profile;
import com.google.android.material.navigation.NavigationView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.Executors;

import androidx.cardview.widget.CardView;

public class menu extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String PREFS = "auth_prefs";

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;
    private LinearLayout catalogContainer;
    private TextView catalogSubtitleTextView;

    // Inicializa la vista principal del menu
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        vincular();
        setToolbar();
        setupBackPress();
    }

    // Refresca el menu y sus permisos cada vez que la pantalla vuelve a estar visible
    @Override
    protected void onResume() {
        super.onResume();
        action();
        updateMenuPermissions();
        loadCatalog();
    }

    // Trae los cursos publicados y los pinta como tarjetas
    private void loadCatalog() {
        if (catalogContainer == null) return;

        Executors.newSingleThreadExecutor().execute(() -> {
            BackgroundWorker.ApiResult result = BackgroundWorker.getCatalog(menu.this);
            runOnUiThread(() -> {
                catalogContainer.removeAllViews();

                if (!result.isSuccess()) {
                    catalogSubtitleTextView.setText("No se pudo conectar con el servidor.");
                    return;
                }

                try {
                    JSONObject json = new JSONObject(result.body);
                    JSONArray cursos = json.optJSONArray("courses");
                    if (cursos == null) cursos = json.optJSONArray("data");

                    if (cursos == null || cursos.length() == 0) {
                        catalogSubtitleTextView.setText("Todavía no hay cursos publicados.");
                        return;
                    }

                    catalogSubtitleTextView.setText(cursos.length() + (cursos.length() == 1 ? " curso disponible" : " cursos disponibles"));

                    for (int i = 0; i < cursos.length(); i++) {
                        JSONObject curso = cursos.optJSONObject(i);
                        if (curso != null) catalogContainer.addView(buildCourseCard(curso));
                    }
                } catch (Exception e) {
                    catalogSubtitleTextView.setText("No se pudieron leer los cursos.");
                }
            });
        });
    }

    // Arma una tarjeta con los datos de un curso del catalogo
    private CardView buildCourseCard(JSONObject curso) {
        String title = curso.optString("title", "Curso sin título");
        String description = curso.optString("description", "Sin descripción");
        String level = curso.optString("level", "beginner");

        JSONObject catObj = curso.optJSONObject("category");
        String categoryName = (catObj != null) ? catObj.optString("name", "General") : "General";

        JSONObject insObj = curso.optJSONObject("instructor");
        String instructorName = (insObj != null) ? insObj.optString("name", "Sin asignar") : "Sin asignar";

        CardView card = new CardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, 24);
        card.setLayoutParams(cardParams);
        card.setRadius(14f);
        card.setCardElevation(3f);
        card.setCardBackgroundColor(Color.WHITE);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        layout.addView(buildCover(curso.optInt("id", 0), title));

        LinearLayout texto = new LinearLayout(this);
        texto.setOrientation(LinearLayout.VERTICAL);
        texto.setPadding(28, 22, 28, 26);

        TextView tvMeta = new TextView(this);
        tvMeta.setText(categoryName.toUpperCase() + "  ·  " + nombreDeNivel(level));
        tvMeta.setTextSize(11f);
        tvMeta.setTextColor(Color.parseColor("#205392"));
        tvMeta.setTypeface(null, android.graphics.Typeface.BOLD);
        tvMeta.setLetterSpacing(0.05f);
        tvMeta.setPadding(0, 0, 0, 8);
        texto.addView(tvMeta);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextSize(17f);
        tvTitle.setTextColor(Color.parseColor("#003564"));
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setPadding(0, 0, 0, 8);
        texto.addView(tvTitle);

        TextView tvDesc = new TextView(this);
        tvDesc.setText(description);
        tvDesc.setTextSize(14f);
        tvDesc.setTextColor(Color.parseColor("#5A6472"));
        tvDesc.setMaxLines(3);
        tvDesc.setEllipsize(android.text.TextUtils.TruncateAt.END);
        tvDesc.setPadding(0, 0, 0, 12);
        texto.addView(tvDesc);

        TextView tvInstructor = new TextView(this);
        tvInstructor.setText("Por " + instructorName);
        tvInstructor.setTextSize(12f);
        tvInstructor.setTextColor(Color.parseColor("#6B7280"));
        texto.addView(tvInstructor);

        TextView tvVer = new TextView(this);
        tvVer.setText("Ver temario ›");
        tvVer.setTextSize(13f);
        tvVer.setTextColor(Color.parseColor("#205392"));
        tvVer.setTypeface(null, android.graphics.Typeface.BOLD);
        tvVer.setPadding(0, 12, 0, 0);
        texto.addView(tvVer);

        layout.addView(texto);
        card.addView(layout);

        final int cursoId = curso.optInt("id", 0);
        card.setClickable(true);
        card.setForeground(obtenerFondoTactil());
        card.setOnClickListener(v -> {
            Intent intent = new Intent(menu.this, CourseDetail.class);
            intent.putExtra("course_id", cursoId);
            startActivity(intent);
        });

        return card;
    }

    // Devuelve el efecto de toque del tema, para que la tarjeta responda al picarla
    private android.graphics.drawable.Drawable obtenerFondoTactil() {
        android.util.TypedValue valor = new android.util.TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackground, valor, true);
        return ContextCompat.getDrawable(this, valor.resourceId);
    }

    // Portada generada con la inicial del curso, porque todavia no hay imagenes
    private TextView buildCover(int courseId, String title) {
        String[][] paletas = {
                {"#2A6BB0", "#003564"},
                {"#2E9010", "#15520A"},
                {"#7B4BB7", "#3A1F63"},
                {"#C2703A", "#6D3410"},
                {"#1F8A8A", "#0C4747"}
        };
        String[] tonos = paletas[Math.abs(courseId) % paletas.length];

        android.graphics.drawable.GradientDrawable fondo = new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
                new int[]{Color.parseColor(tonos[0]), Color.parseColor(tonos[1])}
        );

        String inicial = title.trim().isEmpty() ? "?" : title.trim().substring(0, 1).toUpperCase();

        TextView cover = new TextView(this);
        cover.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (int) (140 * getResources().getDisplayMetrics().density)
        ));
        cover.setBackground(fondo);
        cover.setText(inicial);
        cover.setTextColor(Color.WHITE);
        cover.setTextSize(44f);
        cover.setTypeface(null, android.graphics.Typeface.BOLD);
        cover.setGravity(android.view.Gravity.CENTER);
        return cover;
    }

    private String nombreDeNivel(String level) {
        if ("beginner".equalsIgnoreCase(level)) return "Principiante";
        if ("intermediate".equalsIgnoreCase(level)) return "Intermedio";
        if ("advanced".equalsIgnoreCase(level)) return "Avanzado";
        return level;
    }

    // Vincula las vistas XML con las variables Java
    private void vincular() {
        drawerLayout = findViewById(R.id.main);
        navigationView = findViewById(R.id.nav_view);
        toolbar = findViewById(R.id.toolbar);
        catalogContainer = findViewById(R.id.catalogContainer);
        catalogSubtitleTextView = findViewById(R.id.catalogSubtitleTextView);
    }

    // Asigna la barra de herramientas superior
    protected void setToolbar() {
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }
    }

    // Configura el menu lateral y activa el boton de hamburguesa en la barra superior
    protected void action() {
        navigationView.bringToFront();
        if (toolbar != null && drawerLayout != null) {
            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                    this,
                    drawerLayout,
                    toolbar,
                    R.string.navigation_drawer_open,
                    R.string.navigation_drawer_close
            );
            drawerLayout.addDrawerListener(toggle);
            toggle.syncState();
        }
        navigationView.setNavigationItemSelectedListener(this);
    }

    // Oculta o muestra las opciones de Instructor ("Mis Cursos" y "Crear Curso") segun el rol del usuario
    private void updateMenuPermissions() {
        if (navigationView != null) {
            SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
            String role = prefs.getString(BackgroundWorker.KEY_ROLE, "estudiante");
            boolean isInstructor = "instructor".equalsIgnoreCase(role) || "profesor".equalsIgnoreCase(role);

            Menu navMenu = navigationView.getMenu();
            MenuItem crearCursoItem = navMenu.findItem(R.id.nav_crear_curso);
            if (crearCursoItem != null) {
                crearCursoItem.setVisible(isInstructor);
            }

            MenuItem misCursosItem = navMenu.findItem(R.id.nav_mis_cursos);
            if (misCursosItem != null) {
                misCursosItem.setVisible(isInstructor);
            }
        }
    }

    // Evita cerrar la app si se presiona el boton atras teniendo el menu lateral abierto
    private void setupBackPress() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    // Escucha los clics del menu lateral para abrir Inicio, Perfil, Mis Cursos, Crear Curso o Cerrar Sesion
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        int id = menuItem.getItemId();

        if (id == R.id.nav_logout) {
            // Elimina el token guardado en SharedPreferences y regresa a la pantalla de login
            getSharedPreferences(PREFS, MODE_PRIVATE)
                    .edit()
                    .clear()
                    .apply();

            Toast.makeText(this, "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(menu.this, Login.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();

        } else if (id == R.id.nav_perfil) {
            Intent intent = new Intent(menu.this, Profile.class);
            startActivity(intent);

        } else if (id == R.id.nav_mis_cursos) {
            Intent intent = new Intent(menu.this, MyCourses.class);
            startActivity(intent);

        } else if (id == R.id.nav_crear_curso) {
            Intent intent = new Intent(menu.this, CreateCourse.class);
            startActivity(intent);

        } else if (id == R.id.nav_inicio) {
            Toast.makeText(this, "Inicio", Toast.LENGTH_SHORT).show();
        }

        if (drawerLayout != null) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
        return true;
    }
}
