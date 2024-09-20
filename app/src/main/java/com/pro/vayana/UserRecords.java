package com.pro.vayana;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class UserRecords extends AppCompatActivity {

    private RecyclerView recyclerViewActiveUsers;
    private RecyclerView recyclerViewBlockedUsers;
    private UserAdapter activeUsersAdapter;
    private UserAdapter blockedUsersAdapter;
    private List<User> activeUserList;
    private List<User> blockedUserList;
    private DatabaseReference mDatabase;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_userrecords);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        recyclerViewActiveUsers = findViewById(R.id.recyclerViewActiveUsers);
        recyclerViewBlockedUsers = findViewById(R.id.recyclerViewBlockedUsers);

        recyclerViewActiveUsers.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewBlockedUsers.setLayoutManager(new LinearLayoutManager(this));

        activeUserList = new ArrayList<>();
        blockedUserList = new ArrayList<>();

        activeUsersAdapter = new UserAdapter(activeUserList, new UserAdapter.OnUserActionListener() {
            @Override
            public void onBlockUser(User user) {
                toggleUserBlock(user, true);
            }

            @Override
            public void onUnblockUser(User user) {
                // This should not be called for active users
            }
        });

        blockedUsersAdapter = new UserAdapter(blockedUserList, new UserAdapter.OnUserActionListener() {
            @Override
            public void onBlockUser(User user) {
                // This should not be called for blocked users
            }

            @Override
            public void onUnblockUser(User user) {
                toggleUserBlock(user, false);
            }
        });

        recyclerViewActiveUsers.setAdapter(activeUsersAdapter);
        recyclerViewBlockedUsers.setAdapter(blockedUsersAdapter);

        mDatabase = FirebaseDatabase.getInstance().getReference().child("users");
        loadUsers();
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
                    intent = new Intent(UserRecords.this, Home.class);
                } else if (id == R.id.profile) {
                    intent = new Intent(UserRecords.this, profile.class);
                } else if (id == R.id.pending_approvals) {
                    intent = new Intent(UserRecords.this, PendingApprovals.class);
                } else if (id == R.id.rent) {
                    intent = new Intent(UserRecords.this, Rent.class);
                } else if (id == R.id.user_records) {
                    // We're already here, so do nothing
                    drawerLayout.closeDrawers();
                    return true;
                } else if (id == R.id.about) {
                    intent = new Intent(UserRecords.this, About.class);
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

    private void loadUsers() {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                activeUserList.clear();
                blockedUserList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    User user = snapshot.getValue(User.class);
                    if (user != null && !user.isAdmin()) {
                        user.setId(snapshot.getKey());
                        if (user.isBlocked()) {
                            blockedUserList.add(user);
                        } else {
                            activeUserList.add(user);
                        }
                    }
                }
                activeUsersAdapter.notifyDataSetChanged();
                blockedUsersAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(UserRecords.this, "Failed to load users: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void toggleUserBlock(User user, boolean block) {
        mDatabase.child(user.getId()).child("blocked").setValue(block)
                .addOnSuccessListener(aVoid -> {
                    String message = block ? "User blocked successfully" : "User unblocked successfully";
                    Toast.makeText(UserRecords.this, message, Toast.LENGTH_SHORT).show();
                    loadUsers(); // Reload users to update the lists
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(UserRecords.this, "Failed to update user status: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            Intent intent = new Intent(UserRecords.this, Home.class);
            startActivity(intent);
            super.onBackPressed();
        }
    }
}

