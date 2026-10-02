package com.fic.cursosfic.menu;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.widget.Toolbar;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.fic.cursosfic.R;
import com.google.android.material.navigation.NavigationView;

public class menu extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    DrawerLayout drawerLayout;
    NavigationView navigationView;
    Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        vincular();
        setToolbar();
        setupBackPress();
    }

    @Override
    protected void onResume() {
        super.onResume();
        hide();
        action();
    }

    //vinculamos los elementos
    private void vincular() {
        drawerLayout = findViewById(R.id.main);
        navigationView = findViewById(R.id.nav_view);
    }

    protected void setToolbar() {
        setSupportActionBar(toolbar);
    }

    //menu lateral
    protected void action() {
        navigationView.bringToFront();
        /*ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();*/
        navigationView.setNavigationItemSelectedListener(this);
    }

    //dependiendo de la variable isLogged mostrara ciertas opciones del menu
    public void hide() {
        Menu menu = navigationView.getMenu();
        /*if (MainActivity.isLoggedIn) {
            menu.findItem(R.id.nav_logout).setVisible(true);
            menu.findItem(R.id.nav_user).setVisible(true);
            menu.findItem(R.id.nav_register).setVisible(false);
            menu.findItem(R.id.nav_login).setVisible(false);
        } else {
            menu.findItem(R.id.nav_logout).setVisible(false);
            menu.findItem(R.id.nav_user).setVisible(false);
            menu.findItem(R.id.nav_register).setVisible(true);
            menu.findItem(R.id.nav_login).setVisible(true);
        }*/
    }

    //si el menu lateral esta activo este metodo evita que cierre la app y en su lugar cierra el menu
    private void setupBackPress() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {

                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    //este metodo es quien se encarga de manejar los clicks del menu lateral y redirigir ala activity correpondiente
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {

        int id = menuItem.getItemId();
        /*if (id == R.id.nav_inicio) {
            Intent intent5 = new Intent(MainActivity.this, MainActivity.class);
            startActivity(intent5);
        } else if (id == R.id.nav_transparencia) {
            Intent intent0 = new Intent(MainActivity.this, Transparencia.class);
            startActivity(intent0);
        } else if (id==R.id.nav_servicio) {
            Intent intent1 = new Intent(MainActivity.this, Servicios.class);
            startActivity(intent1);
        } else if (id == R.id.nav_denuncia) {
            Intent intent2 = new Intent(MainActivity.this, Denuncia.class);
            startActivity(intent2);
        } else if (id == R.id.nav_register) {
            Intent intent3 = new Intent(MainActivity.this, Register.class);
            startActivity(intent3);
        } else if (id == R.id.nav_login) {
            Intent intent4 = new Intent(MainActivity.this, Login.class);
            startActivity(intent4);
        } else if (id == R.id.nav_user) {
            Intent intent6 = new Intent(MainActivity.this, User.class);
            startActivity(intent6);
        } else if (id == R.id.nav_logout) {
            MainActivity.isLoggedIn = false;
            hide();
            // Borrar el email del usuario de SharedPreferences
            SessionManager sessionManager = new SessionManager(this);
            sessionManager.clearSession();
            Toast.makeText(this, "Cerrando sesión", Toast.LENGTH_SHORT).show();
        }*/
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }
}