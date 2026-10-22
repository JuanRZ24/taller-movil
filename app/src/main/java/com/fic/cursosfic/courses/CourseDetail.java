package com.fic.cursosfic.courses;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
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

public class CourseDetail extends AppCompatActivity {

    private int courseId;
    private String courseTitle, courseDescription, categoryName, courseLevel, courseStatus;

    private TextView courseTitleTextView, courseMetaTextView, courseDescriptionTextView, emptyModulesTextView;
    private Button addModuleButton, publishCourseButton;
    private boolean soloLectura = false;
    private LinearLayout modulesContainer;

    private final List<ModuleItem> modulesList = new ArrayList<>();

    public static class ModuleItem {
        public int id;
        public String title;
        public int position;
        public List<ContentItem> contents = new ArrayList<>();
    }

    public static class ContentItem {
        public int id;
        public String title;
        public String type; // "text", "video", "link"
        public String body;
        public String url;
    }

    // Inicializa la pantalla de detalle y gestion del curso cargando los datos pasados por el Intent
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_detail);

        Toolbar toolbar = findViewById(R.id.toolbarCourseDetail);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
            }
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        courseTitleTextView = findViewById(R.id.courseTitleTextView);
        courseMetaTextView = findViewById(R.id.courseMetaTextView);
        courseDescriptionTextView = findViewById(R.id.courseDescriptionTextView);
        emptyModulesTextView = findViewById(R.id.emptyModulesTextView);
        addModuleButton = findViewById(R.id.addModuleButton);
        publishCourseButton = findViewById(R.id.publishCourseButton);
        modulesContainer = findViewById(R.id.modulesContainer);

        extractIntentData();
        aplicarModoLectura();
        displayCourseHeader();

        addModuleButton.setOnClickListener(v -> showAddModuleDialog());
        publishCourseButton.setVisibility(View.GONE);

        renderModules();
        loadCourseFromApi();
    }

    // Extrae el JSON del curso recién creado enviado desde la pantalla anterior
    private void extractIntentData() {
        if (getIntent() != null && getIntent().hasExtra("course_id")) {
            courseId = getIntent().getIntExtra("course_id", 0);
        }

        if (getIntent() != null && getIntent().hasExtra("course_json")) {
            parseCourse(getIntent().getStringExtra("course_json"));
        }
    }

    // Pide el curso completo a la API, porque el listado no incluye modulos ni contenidos
    private void loadCourseFromApi() {
        if (courseId <= 0) return;

        Executors.newSingleThreadExecutor().execute(() -> {
            BackgroundWorker.ApiResult result = BackgroundWorker.getCourseDetail(CourseDetail.this, courseId);
            runOnUiThread(() -> {
                if (!result.isSuccess()) return;
                modulesList.clear();
                parseCourse(result.body);
                aplicarModoLectura();
                displayCourseHeader();
                renderModules();
            });
        });
    }

    // Un curso en revision o publicado no se puede modificar, asi que la pantalla queda de consulta
    private void aplicarModoLectura() {
        soloLectura = !("draft".equalsIgnoreCase(courseStatus) || "rejected".equalsIgnoreCase(courseStatus));
        addModuleButton.setVisibility(soloLectura ? View.GONE : View.VISIBLE);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(soloLectura ? "Temario del curso" : "Gestión del Curso");
        }
        if (soloLectura && emptyModulesTextView != null) {
            emptyModulesTextView.setText("Este curso todavía no tiene temario.");
        }
    }

    // Lee el curso, sus modulos y sus contenidos de una respuesta de la API
    private void parseCourse(String body) {
        if (body == null || body.isEmpty()) return;
        {
            try {
                JSONObject json = new JSONObject(body);
                JSONObject courseObj = json.optJSONObject("course");
                if (courseObj == null) courseObj = json;

                courseId = courseObj.optInt("id", courseId);
                courseTitle = courseObj.optString("title", "Curso sin título");
                courseDescription = courseObj.optString("description", "Sin descripción");
                courseLevel = courseObj.optString("level", "beginner");
                
                String statusRaw = courseObj.optString("status", "draft");
                if (statusRaw == null || statusRaw.isEmpty() || "null".equalsIgnoreCase(statusRaw)) {
                    courseStatus = "draft";
                } else {
                    courseStatus = statusRaw;
                }

                JSONObject catObj = courseObj.optJSONObject("category");
                categoryName = (catObj != null) ? catObj.optString("name", "General") : "General";

                JSONArray modulesArr = courseObj.optJSONArray("modules");
                if (modulesArr != null && modulesArr.length() > 0) {
                    for (int i = 0; i < modulesArr.length(); i++) {
                        JSONObject modObj = modulesArr.optJSONObject(i);
                        if (modObj != null) {
                            ModuleItem mod = new ModuleItem();
                            mod.id = modObj.optInt("id");
                            mod.title = modObj.optString("title", "Módulo " + (i + 1));
                            mod.position = modObj.optInt("position", i + 1);

                            JSONArray contentsArr = modObj.optJSONArray("contents");
                            if (contentsArr != null) {
                                for (int j = 0; j < contentsArr.length(); j++) {
                                    JSONObject cntObj = contentsArr.optJSONObject(j);
                                    if (cntObj != null) {
                                        ContentItem cnt = new ContentItem();
                                        cnt.id = cntObj.optInt("id");
                                        cnt.title = cntObj.optString("title", "Contenido");
                                        cnt.type = cntObj.optString("type", "text");
                                        cnt.body = cntObj.optString("body", "");
                                        cnt.url = cntObj.optString("url", "");
                                        mod.contents.add(cnt);
                                    }
                                }
                            }
                            modulesList.add(mod);
                        }
                    }
                }
            } catch (Exception e) {
                courseTitle = "Nuevo Curso";
                courseDescription = "";
                categoryName = "General";
                courseLevel = "beginner";
                courseStatus = "draft";
            }
        }
    }

    // Muestra el titulo, categoria, nivel y descripcion del curso en el encabezado
    private void displayCourseHeader() {
        courseTitleTextView.setText(courseTitle);
        courseMetaTextView.setText(categoryName + "  ·  " + nombreDeNivel(courseLevel) + "  ·  " + nombreDeEstado(courseStatus));
        courseDescriptionTextView.setText(courseDescription);
    }

    private String nombreDeNivel(String level) {
        if ("beginner".equalsIgnoreCase(level)) return "Principiante";
        if ("intermediate".equalsIgnoreCase(level)) return "Intermedio";
        if ("advanced".equalsIgnoreCase(level)) return "Avanzado";
        return level;
    }

    private String nombreDeEstado(String status) {
        if ("draft".equalsIgnoreCase(status)) return "Borrador";
        if ("pending".equalsIgnoreCase(status)) return "En revisión";
        if ("published".equalsIgnoreCase(status)) return "Publicado";
        if ("rejected".equalsIgnoreCase(status)) return "Rechazado";
        return status;
    }

    // Renderiza la lista dinamica de modulos y sus contenidos en el contenedor vertical
    private void renderModules() {
        modulesContainer.removeAllViews();

        if (modulesList.isEmpty()) {
            emptyModulesTextView.setVisibility(View.VISIBLE);
            return;
        }

        emptyModulesTextView.setVisibility(View.GONE);

        for (int i = 0; i < modulesList.size(); i++) {
            final ModuleItem module = modulesList.get(i);

            // Card principal del modulo
            CardView card = new CardView(this);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            );
            cardParams.setMargins(0, 0, 0, 24);
            card.setLayoutParams(cardParams);
            card.setRadius(12f);
            card.setCardElevation(4f);
            card.setCardBackgroundColor(Color.WHITE);

            LinearLayout moduleLayout = new LinearLayout(this);
            moduleLayout.setOrientation(LinearLayout.VERTICAL);
            moduleLayout.setPadding(20, 20, 20, 20);

            // Encabezado del modulo
            LinearLayout headerLayout = new LinearLayout(this);
            headerLayout.setOrientation(LinearLayout.HORIZONTAL);

            TextView tvModTitle = new TextView(this);
            tvModTitle.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            tvModTitle.setText((i + 1) + ". " + module.title);
            tvModTitle.setTextSize(16f);
            tvModTitle.setTextColor(ContextCompat.getColor(this, R.color.brand_dark));
            tvModTitle.setTypeface(null, android.graphics.Typeface.BOLD);
            headerLayout.addView(tvModTitle);

            // Boton para editar modulo
            TextView tvEditMod = new TextView(this);
            tvEditMod.setText(" ✏️ ");
            tvEditMod.setTextSize(16f);
            tvEditMod.setOnClickListener(v -> showEditModuleDialog(module));
            if (!soloLectura) headerLayout.addView(tvEditMod);

            // Boton para agregar contenido
            Button btnAddContent = new Button(this);
            btnAddContent.setText("+ Contenido");
            btnAddContent.setTextSize(12f);
            btnAddContent.setTextColor(Color.WHITE);
            btnAddContent.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.brand_header));
            btnAddContent.setOnClickListener(v -> showAddContentDialog(module));
            if (!soloLectura) headerLayout.addView(btnAddContent);

            // Boton para borrar modulo
            Button btnDelMod = new Button(this);
            btnDelMod.setText("X");
            btnDelMod.setTextSize(12f);
            btnDelMod.setTextColor(Color.WHITE);
            btnDelMod.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.error_red));
            btnDelMod.setOnClickListener(v -> confirmDeleteModule(module));
            if (!soloLectura) headerLayout.addView(btnDelMod);

            moduleLayout.addView(headerLayout);

            // Contenedor de contenidos del modulo
            if (module.contents.isEmpty()) {
                TextView tvEmptyCnt = new TextView(this);
                tvEmptyCnt.setText("Aún no hay contenidos en este módulo.");
                tvEmptyCnt.setTextSize(13f);
                tvEmptyCnt.setTextColor(Color.GRAY);
                tvEmptyCnt.setPadding(0, 12, 0, 0);
                moduleLayout.addView(tvEmptyCnt);
            } else {
                for (int j = 0; j < module.contents.size(); j++) {
                    final ContentItem content = module.contents.get(j);

                    LinearLayout cntRow = new LinearLayout(this);
                    cntRow.setOrientation(LinearLayout.HORIZONTAL);
                    cntRow.setPadding(12, 12, 12, 12);
                    cntRow.setBackgroundColor(Color.parseColor("#F1F3F5"));
                    LinearLayout.LayoutParams cntParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    cntParams.setMargins(0, 12, 0, 0);
                    cntRow.setLayoutParams(cntParams);

                    String iconType = "text".equalsIgnoreCase(content.type) ? "📄 [Texto] " :
                            ("video".equalsIgnoreCase(content.type) ? "🎬 [Vídeo] " : "🔗 [Enlace] ");

                    TextView tvCntInfo = new TextView(this);
                    tvCntInfo.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                    String detail = "text".equalsIgnoreCase(content.type) ? content.body : content.url;
                    tvCntInfo.setText(iconType + content.title + "\n" + detail);
                    tvCntInfo.setTextSize(13f);
                    tvCntInfo.setTextColor(Color.BLACK);
                    cntRow.addView(tvCntInfo);

                    TextView tvEditCnt = new TextView(this);
                    tvEditCnt.setText(" ✏️ ");
                    tvEditCnt.setTextSize(16f);
                    tvEditCnt.setOnClickListener(v -> showEditContentDialog(module, content));
                    if (!soloLectura) cntRow.addView(tvEditCnt);

                    TextView tvDelCnt = new TextView(this);
                    tvDelCnt.setText(" 🗑️ ");
                    tvDelCnt.setTextSize(16f);
                    tvDelCnt.setOnClickListener(v -> confirmDeleteContent(module, content));
                    if (!soloLectura) cntRow.addView(tvDelCnt);

                    moduleLayout.addView(cntRow);
                }
            }

            card.addView(moduleLayout);
            modulesContainer.addView(card);
        }
    }

    // Muestra un dialogo emergente para crear un nuevo modulo mediante POST /courses/{id}/modules
    private void showAddModuleDialog() {
        final EditText etModTitle = new EditText(this);
        etModTitle.setHint("Nombre del módulo");

        new AlertDialog.Builder(this)
                .setTitle("Añadir Módulo")
                .setView(etModTitle)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    String title = etModTitle.getText().toString().trim();
                    if (title.isEmpty()) {
                        Toast.makeText(this, "El nombre del módulo es requerido", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    saveModuleToApi(title);
                })
                .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss())
                .show();
    }

    // Muestra un dialogo emergente para editar el nombre de un modulo existente via PATCH /modules/{id}
    private void showEditModuleDialog(ModuleItem module) {
        final EditText etModTitle = new EditText(this);
        etModTitle.setText(module.title);

        new AlertDialog.Builder(this)
                .setTitle("Editar Módulo")
                .setView(etModTitle)
                .setPositiveButton("Actualizar", (dialog, which) -> {
                    String newTitle = etModTitle.getText().toString().trim();
                    if (newTitle.isEmpty()) {
                        Toast.makeText(this, "El nombre del módulo no puede estar vacío", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    updateModuleInApi(module, newTitle);
                })
                .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss())
                .show();
    }

    // Peticion HTTP en segundo plano PATCH /modules/{id} para actualizar el nombre del modulo
    private void updateModuleInApi(ModuleItem module, String newTitle) {
        Toast.makeText(this, "Actualizando módulo...", Toast.LENGTH_SHORT).show();
        Executors.newSingleThreadExecutor().execute(() -> {
            BackgroundWorker.ApiResult result = BackgroundWorker.editModule(this, module.id, newTitle);
            runOnUiThread(() -> {
                if (result.isSuccess()) {
                    module.title = newTitle;
                    renderModules();
                    Toast.makeText(CourseDetail.this, "Módulo actualizado", Toast.LENGTH_SHORT).show();
                } else {
                    String msg = (result.code == -1) ? "Servicio no disponible" : BackgroundWorker.humanMessage(result.body);
                    Toast.makeText(CourseDetail.this, msg, Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    // Peticion HTTP en segundo plano POST /courses/{id}/modules para registrar el modulo en la API
    private void saveModuleToApi(String title) {
        Toast.makeText(this, "Guardando módulo...", Toast.LENGTH_SHORT).show();
        Executors.newSingleThreadExecutor().execute(() -> {
            BackgroundWorker.ApiResult result = BackgroundWorker.createModule(this, courseId, title);
            runOnUiThread(() -> {
                if (result.isSuccess()) {
                    try {
                        JSONObject json = new JSONObject(result.body);
                        JSONObject modObj = json.optJSONObject("module");
                        if (modObj == null) modObj = json;

                        ModuleItem mod = new ModuleItem();
                        mod.id = modObj.optInt("id", (int) System.currentTimeMillis());
                        mod.title = modObj.optString("title", title);
                        mod.position = modObj.optInt("position", modulesList.size() + 1);

                        modulesList.add(mod);
                        renderModules();
                        Toast.makeText(CourseDetail.this, "¡Módulo añadido con éxito!", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(CourseDetail.this, "Módulo guardado", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    String msg = (result.code == -1) ? "Servicio no disponible" : BackgroundWorker.humanMessage(result.body);
                    Toast.makeText(CourseDetail.this, msg, Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    // Confirma la eliminacion de un modulo con DELETE /modules/{id}
    private void confirmDeleteModule(ModuleItem module) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar Módulo")
                .setMessage("¿Deseas eliminar el módulo '" + module.title + "'?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    Executors.newSingleThreadExecutor().execute(() -> {
                        BackgroundWorker.ApiResult result = BackgroundWorker.deleteModule(this, module.id);
                        runOnUiThread(() -> {
                            if (result.isSuccess()) {
                                modulesList.remove(module);
                                renderModules();
                                Toast.makeText(CourseDetail.this, "Módulo eliminado", Toast.LENGTH_SHORT).show();
                            } else {
                                String msg = (result.code == -1) ? "Servicio no disponible" : BackgroundWorker.humanMessage(result.body);
                                Toast.makeText(CourseDetail.this, msg, Toast.LENGTH_LONG).show();
                            }
                        });
                    });
                })
                .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss())
                .show();
    }

    // Muestra un dialogo con tipo, titulo y contenido/URL para agregar un contenido al modulo
    private void showAddContentDialog(ModuleItem module) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 10);

        final Spinner spinnerType = new Spinner(this);
        String[] typesDisplay = {"Texto (text)", "Vídeo YouTube (video)", "Enlace Documentación (link)"};
        final String[] typeValues = {"text", "video", "link"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, typesDisplay);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(adapter);
        layout.addView(spinnerType);

        final EditText etTitle = new EditText(this);
        etTitle.setHint("Título del contenido");
        layout.addView(etTitle);

        final EditText etBody = new EditText(this);
        etBody.setHint("Texto explicativo");
        layout.addView(etBody);

        final EditText etUrl = new EditText(this);
        etUrl.setHint("URL (ej. https://youtube.com/...)");
        etUrl.setVisibility(View.GONE);
        layout.addView(etUrl);

        spinnerType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) { // text
                    etBody.setVisibility(View.VISIBLE);
                    etUrl.setVisibility(View.GONE);
                } else { // video / link
                    etBody.setVisibility(View.GONE);
                    etUrl.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        new AlertDialog.Builder(this)
                .setTitle("Añadir Contenido a " + module.title)
                .setView(layout)
                .setPositiveButton("Guardar Contenido", (dialog, which) -> {
                    String title = etTitle.getText().toString().trim();
                    int pos = spinnerType.getSelectedItemPosition();
                    String type = typeValues[pos];
                    String body = etBody.getText().toString().trim();
                    String url = etUrl.getText().toString().trim();

                    if (title.isEmpty()) {
                        Toast.makeText(this, "El título del contenido es requerido", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    saveContentToApi(module, title, type, body, url);
                })
                .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss())
                .show();
    }

    // Muestra un dialogo emergente para editar un contenido existente via PATCH /contents/{id}
    private void showEditContentDialog(ModuleItem module, ContentItem content) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 10);

        final Spinner spinnerType = new Spinner(this);
        String[] typesDisplay = {"Texto (text)", "Vídeo YouTube (video)", "Enlace Documentación (link)"};
        final String[] typeValues = {"text", "video", "link"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, typesDisplay);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(adapter);
        layout.addView(spinnerType);

        if ("video".equalsIgnoreCase(content.type)) spinnerType.setSelection(1);
        else if ("link".equalsIgnoreCase(content.type)) spinnerType.setSelection(2);
        else spinnerType.setSelection(0);

        final EditText etTitle = new EditText(this);
        etTitle.setText(content.title);
        layout.addView(etTitle);

        final EditText etBody = new EditText(this);
        etBody.setText(content.body);
        layout.addView(etBody);

        final EditText etUrl = new EditText(this);
        etUrl.setText(content.url);
        layout.addView(etUrl);

        spinnerType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) { // text
                    etBody.setVisibility(View.VISIBLE);
                    etUrl.setVisibility(View.GONE);
                } else { // video / link
                    etBody.setVisibility(View.GONE);
                    etUrl.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        new AlertDialog.Builder(this)
                .setTitle("Editar Contenido")
                .setView(layout)
                .setPositiveButton("Actualizar", (dialog, which) -> {
                    String title = etTitle.getText().toString().trim();
                    int pos = spinnerType.getSelectedItemPosition();
                    String type = typeValues[pos];
                    String body = etBody.getText().toString().trim();
                    String url = etUrl.getText().toString().trim();

                    if (title.isEmpty()) {
                        Toast.makeText(this, "El título del contenido es requerido", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    updateContentInApi(module, content, title, type, body, url);
                })
                .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss())
                .show();
    }

    // Peticion HTTP PATCH /contents/{id} para actualizar un contenido en la API
    private void updateContentInApi(ModuleItem module, ContentItem content, String title, String type, String bodyText, String urlString) {
        Toast.makeText(this, "Actualizando contenido...", Toast.LENGTH_SHORT).show();
        Executors.newSingleThreadExecutor().execute(() -> {
            BackgroundWorker.ApiResult result = BackgroundWorker.editContent(this, content.id, title, type, bodyText, urlString);
            runOnUiThread(() -> {
                if (result.isSuccess()) {
                    content.title = title;
                    content.type = type;
                    content.body = bodyText;
                    content.url = urlString;
                    renderModules();
                    Toast.makeText(CourseDetail.this, "Contenido actualizado", Toast.LENGTH_SHORT).show();
                } else {
                    String msg = (result.code == -1) ? "Servicio no disponible" : BackgroundWorker.humanMessage(result.body);
                    Toast.makeText(CourseDetail.this, msg, Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    // Peticion HTTP POST /modules/{id}/contents para guardar el nuevo contenido en el servidor
    private void saveContentToApi(ModuleItem module, String title, String type, String bodyText, String urlString) {
        Toast.makeText(this, "Guardando contenido...", Toast.LENGTH_SHORT).show();
        Executors.newSingleThreadExecutor().execute(() -> {
            BackgroundWorker.ApiResult result = BackgroundWorker.createContent(this, module.id, title, type, bodyText, urlString);
            runOnUiThread(() -> {
                if (result.isSuccess()) {
                    try {
                        JSONObject json = new JSONObject(result.body);
                        JSONObject cntObj = json.optJSONObject("content");
                        if (cntObj == null) cntObj = json;

                        ContentItem cnt = new ContentItem();
                        cnt.id = cntObj.optInt("id", (int) System.currentTimeMillis());
                        cnt.title = cntObj.optString("title", title);
                        cnt.type = cntObj.optString("type", type);
                        cnt.body = cntObj.optString("body", bodyText);
                        cnt.url = cntObj.optString("url", urlString);

                        module.contents.add(cnt);
                        renderModules();
                        Toast.makeText(CourseDetail.this, "¡Contenido añadido con éxito!", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(CourseDetail.this, "Contenido guardado", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    String msg = (result.code == -1) ? "Servicio no disponible" : BackgroundWorker.humanMessage(result.body);
                    Toast.makeText(CourseDetail.this, msg, Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    // Confirma la eliminacion de un contenido especifico con DELETE /contents/{id}
    private void confirmDeleteContent(ModuleItem module, ContentItem content) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar Contenido")
                .setMessage("¿Deseas eliminar '" + content.title + "'?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    Executors.newSingleThreadExecutor().execute(() -> {
                        BackgroundWorker.ApiResult result = BackgroundWorker.deleteContent(this, content.id);
                        runOnUiThread(() -> {
                            if (result.isSuccess()) {
                                module.contents.remove(content);
                                renderModules();
                                Toast.makeText(CourseDetail.this, "Contenido eliminado", Toast.LENGTH_SHORT).show();
                            } else {
                                String msg = (result.code == -1) ? "Servicio no disponible" : BackgroundWorker.humanMessage(result.body);
                                Toast.makeText(CourseDetail.this, msg, Toast.LENGTH_LONG).show();
                            }
                        });
                    });
                })
                .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss())
                .show();
    }
}
