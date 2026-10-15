package com.fic.cursosfic.menu;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.fic.cursosfic.R;
import com.fic.cursosfic.courses.CreateCourse;
import com.fic.cursosfic.users.BackgroundWorker;
import com.fic.cursosfic.users.Login;
import com.fic.cursosfic.users.Profile;
import com.google.android.material.navigation.NavigationView;

public class menu extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String PREFS = "auth_prefs";

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;

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
    }

    // Vincula las vistas XML con las variables Java
    private void vincular() {
        drawerLayout = findViewById(R.id.main);
        navigationView = findViewById(R.id.nav_view);
        toolbar = findViewById(R.id.toolbar);
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

    // Oculta o muestra la opcion de Crear Curso segun si el usuario es Instructor o Estudiante
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

    // Escucha los clics del menu lateral para abrir Inicio, Perfil, Crear Curso o Cerrar Sesion
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
