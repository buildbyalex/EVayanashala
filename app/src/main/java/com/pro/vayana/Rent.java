package com.pro.vayana;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import android.view.MenuItem;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class Rent extends AppCompatActivity {

    private TableLayout tablePendingRequests;
    private TableLayout tableApprovedRequests;
    private DatabaseReference databaseReference;
    private String userId;
    private Map<String, String> rentalStatusMap;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rent);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        tablePendingRequests = findViewById(R.id.table_pending_requests);
        tableApprovedRequests = findViewById(R.id.table_approved_requests);
        databaseReference = FirebaseDatabase.getInstance().getReference();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        rentalStatusMap = new HashMap<>();

        setupNavigation();
        loadRentalStatus();
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
                    intent = new Intent(Rent.this, Home.class);
                } else if (id == R.id.profile) {
                    intent = new Intent(Rent.this, profile.class);
                } else if (id == R.id.pending_approvals) {
                    intent = new Intent(Rent.this, PendingApprovals.class);
                } else if (id == R.id.user_records) {
                    intent = new Intent(Rent.this, UserRecords.class);
                } else if (id == R.id.rent) {
                    // We're already here, so do nothing
                    drawerLayout.closeDrawers();
                    return true;
                } else if (id == R.id.about) {
                    intent = new Intent(Rent.this, About.class);
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

    private void loadRentalStatus() {
        DatabaseReference pendingRequestsRef = databaseReference.child("pendingRequests");
        DatabaseReference approvedRequestsRef = databaseReference.child("approvedRequests");

        // Load pending requests
        pendingRequestsRef.orderByChild("userId").equalTo(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                rentalStatusMap.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String bookTitle = snapshot.child("bookTitle").getValue(String.class);
                    if (bookTitle != null) {
                        rentalStatusMap.put(bookTitle, "Pending");
                    }
                }
                loadApprovedRequests();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(Rent.this, "Failed to load pending requests: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadApprovedRequests() {
        DatabaseReference approvedRequestsRef = databaseReference.child("approvedRequests");
        approvedRequestsRef.orderByChild("userId").equalTo(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String bookTitle = snapshot.child("bookTitle").getValue(String.class);
                    if (bookTitle != null) {
                        // Update status to "Approved"
                        rentalStatusMap.put(bookTitle, "Approved");
                    }
                }
                updateTables();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(Rent.this, "Failed to load approved requests: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateTables() {
        tablePendingRequests.removeAllViews(); // Clear existing rows
        tableApprovedRequests.removeAllViews(); // Clear existing rows

        // Add header row for pending requests
        TableRow headerRowPending = new TableRow(this);
        headerRowPending.addView(createTextView("Book Title", true));
        headerRowPending.addView(createTextView("Status", true));
        tablePendingRequests.addView(headerRowPending);

        // Add data rows for pending requests
        for (Map.Entry<String, String> entry : rentalStatusMap.entrySet()) {
            if ("Pending".equals(entry.getValue())) {
                TableRow row = new TableRow(this);
                row.addView(createTextView(entry.getKey(), false));
                row.addView(createTextView(entry.getValue(), false));
                tablePendingRequests.addView(row);
            }
        }

        // Add header row for approved requests
        TableRow headerRowApproved = new TableRow(this);
        headerRowApproved.addView(createTextView("Book Title", true));
        headerRowApproved.addView(createTextView("Status", true));
        tableApprovedRequests.addView(headerRowApproved);

        // Add data rows for approved requests
        for (Map.Entry<String, String> entry : rentalStatusMap.entrySet()) {
            if ("Approved".equals(entry.getValue())) {
                TableRow row = new TableRow(this);
                row.addView(createTextView(entry.getKey(), false));
                row.addView(createTextView(entry.getValue(), false));
                tableApprovedRequests.addView(row);
            }
        }
    }

    private TextView createTextView(String text, boolean isHeader) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setPadding(8, 8, 8, 8);
        textView.setGravity(Gravity.CENTER);
        if (isHeader) {
            textView.setTextColor(Color.BLACK);
            textView.setTextSize(16);
            textView.setTypeface(null, Typeface.BOLD);
        } else {
            textView.setTextColor(Color.DKGRAY);
        }
        return textView;
    }
    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            Intent intent = new Intent(Rent.this, Home.class);
            startActivity(intent);
            super.onBackPressed();
        }
    }
}
