package com.pro.vayana;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

public class UserRoleCheckActivity {
    private NavigationView navigationView;
    private FloatingActionButton fabAddBook;

    public UserRoleCheckActivity(NavigationView navigationView, FloatingActionButton fabAddBook) {
        this.navigationView = navigationView;
        this.fabAddBook = fabAddBook;
    }

    public void checkUserRole(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("loginPrefs", Context.MODE_PRIVATE);
        int isAdmin = sharedPreferences.getInt("isAdmin", 0);

        if (isAdmin == 1) {
            showAdminView();
        } else {
            showUserView();
        }
    }

    private void showAdminView() {
        navigationView.getMenu().clear();
        navigationView.inflateMenu(R.menu.menu_drawer_admin);
        fabAddBook.setVisibility(View.VISIBLE);
    }

    private void showUserView() {
        navigationView.getMenu().clear();
        navigationView.inflateMenu(R.menu.menu_drawer_user);
        fabAddBook.setVisibility(View.GONE);
    }
}