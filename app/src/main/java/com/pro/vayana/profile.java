package com.pro.vayana;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class profile extends AppCompatActivity {
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle drawerToggle;
    private boolean isAdmin = false; // Default to false
    private Button exitButton;
    private Button deleteAccountButton;
    private TextView nameTextView, emailTextView, phoneTextView;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Initialize views
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        exitButton = findViewById(R.id.btnexit);
        deleteAccountButton = findViewById(R.id.btnDeleteAccount);
        nameTextView = findViewById(R.id.name);
        emailTextView = findViewById(R.id.email);
        phoneTextView = findViewById(R.id.phone);

        // Initialize ActionBarDrawerToggle
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.open, R.string.close);
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();

        // Set click listeners
        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLogoutConfirmationDialog();
            }
        });

        deleteAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDeleteAccountConfirmationDialog();
            }
        });

        // Set navigation view listener
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                return handleNavigationItemSelected(item);
            }
        });

        // Retrieve isAdmin flag from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("EVayanashalaPrefs", MODE_PRIVATE);
        isAdmin = sharedPreferences.getBoolean("isAdmin", false);

        // Update navigation menu based on isAdmin flag
        updateNavigationMenu();

        // Load user data (you'll need to implement this method)
        loadUserData();
    }

    private void loadUserData() {
        // Get the current user's UID
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Fetch user data from Firebase Realtime Database
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference ref = database.getReference("users").child(uid);

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                if (user != null) {
                    nameTextView.setText("Name: " + user.name);
                    emailTextView.setText("Email: " + user.email);
                    phoneTextView.setText("Phone: " + user.phone);
                } else {
                    Log.w("loadUserData", "User data is null");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w("loadUserData", "loadUserData:onCancelled", databaseError.toException());
            }
        });
    }

    private boolean handleNavigationItemSelected(MenuItem item) {
        // Handle navigation item clicks here
        item.setChecked(true); // Mark the selected item as checked
        drawerLayout.closeDrawer(GravityCompat.START);
        switch (item.getItemId()) {
            case R.id.home:
                startActivity(new Intent(profile.this, Home.class));
                finish(); // Finish current activity to prevent back navigation issues
                break;
            case R.id.profile:
                // Already in profile activity, no action needed
                break;
            case R.id.rent:
                startActivity(new Intent(profile.this, Rent.class));
                finish(); // Finish current activity to prevent back navigation issues
                break;
            case R.id.user_records:
                startActivity(new Intent(profile.this,UserRecords.class));
                finish();
                break;
            case R.id.pending_approvals:
                    startActivity(new Intent(profile.this,PendingApprovals.class));
                    finish();
                    break;
            case R.id.about:
                startActivity(new Intent(profile.this, About.class));
                finish(); // Finish current activity to prevent back navigation issues
                break;
            default:
                return false;
        }
        return true;
    }

    private void updateNavigationMenu() {
        // Clear existing menu items
        navigationView.getMenu().clear();

        // Inflate the menu based on isAdmin flag
        if (isAdmin) {
            navigationView.inflateMenu(R.menu.menu_drawer_admin);
        } else {
            navigationView.inflateMenu(R.menu.menu_drawer_user);
        }
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            // Instead of super.onBackPressed(), start the Home activity
            startActivity(new Intent(profile.this, Home.class));
            super.onBackPressed();
            finish(); // Finish the current activity to prevent back navigation issues
        }
    }


    private void showLogoutConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirm Logout");
        builder.setMessage("Are you sure you want to Logout?");

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                FirebaseAuth.getInstance().signOut();
                redirectToLogin();
                Toast.makeText(profile.this, "Logout successful! Please login to continue.", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss(); // Dismiss the dialog
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showDeleteAccountConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirm Delete Account");
        builder.setMessage("Are you sure you want to delete your account? This action cannot be undone.");

        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deleteUserAccount();
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss(); // Dismiss the dialog
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void deleteUserAccount() {
        // Get the current user's UID
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String uid = user.getUid();

            // Remove user data from the Realtime Database
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);
            userRef.removeValue().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // Delete the user account in Firebase Authentication
                    user.delete().addOnCompleteListener(deleteTask -> {
                        if (deleteTask.isSuccessful()) {
                            Toast.makeText(profile.this, "Account deleted successfully", Toast.LENGTH_SHORT).show();
                            redirectToLogin();
                        } else {
                            Toast.makeText(profile.this, "Account deletion failed", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Toast.makeText(profile.this, "Failed to delete user data", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void redirectToLogin() {
        Intent intent = new Intent(profile.this, Login.class);
        startActivity(intent);
        finish();
    }
}
