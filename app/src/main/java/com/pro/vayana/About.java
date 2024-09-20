package com.pro.vayana;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import androidx.appcompat.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

public class About extends AppCompatActivity {
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setupNavigation();
    }
    private void setupNavigation() {
        drawerLayout = findViewById(R.id.drawer_layout);
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.open, R.string.close);

        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        EVayanashala app = (EVayanashala) getApplication();

        if (app.isAdmin()) {
            navigationView.inflateMenu(R.menu.menu_drawer_admin);
        } else {
            navigationView.inflateMenu(R.menu.menu_drawer_user);
        }
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                Intent intent = null;

                if (id == R.id.home) {
                    intent = new Intent(About.this, Home.class);
                } else if (id == R.id.profile) {
                    intent = new Intent(About.this, profile.class);
                } else if (id == R.id.pending_approvals) {
                    intent = new Intent(About.this, PendingApprovals.class);
                } else if (id == R.id.user_records) {
                    intent = new Intent(About.this, UserRecords.class);
                } else if (id == R.id.about) {
                    // We're already here, so do nothing
                    drawerLayout.closeDrawers();
                    return true;
                } else if (id == R.id.rent) {
                    intent = new Intent(About.this, Rent.class);
                }

                if (intent != null) {
                    startActivity(intent);
                    finish();
                }

                drawerLayout.closeDrawers();
                return true;
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            Intent intent = new Intent(About.this, Home.class);
            startActivity(intent);
            super.onBackPressed();
        }
    }
}

