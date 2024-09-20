package com.pro.vayana;

import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;

public class EVayanashala extends Application {
    private static final String PREFS_NAME = "EVayanashalaPrefs";
    private static final String KEY_IS_ADMIN = "isAdmin";
    private UserNotificationService userNotificationService;
    private AdminNotificationService adminNotificationService;
    private AdminSessionManager adminSessionManager;

    @Override
    public void onCreate() {
        super.onCreate();

        FirebaseOptions options = new FirebaseOptions.Builder()
                .setApplicationId("YOUR APP ID")
                .setApiKey("YOUR API KEY")
                .setDatabaseUrl("YOUR FIREBASE DB")
                .build();
        FirebaseApp.initializeApp(this, options);

        adminSessionManager = new AdminSessionManager(this);

        if (isAdmin()) {
            initializeAdminServices();
        } else {
            initializeUserServices();
        }
    }

    private void initializeAdminServices() {
        adminNotificationService = new AdminNotificationService(this);
        adminSessionManager.checkAdminSession(new AdminSessionManager.AdminLoginCallback() {
            @Override
            public void onSuccess() {
                adminNotificationService.startListeningForRequests();
            }

            @Override
            public void onFailure(String error) {
                // Admin session is invalid, log out
                setAdmin(false);
                FirebaseAuth.getInstance().signOut();
                // You might want to start the Login activity here
                Intent intent = new Intent(getApplicationContext(), Login.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        });
    }

    private void initializeUserServices() {
        userNotificationService = new UserNotificationService(this);
        initializeUserNotificationService();
    }

    public void initializeUserNotificationService() {
        if (!isAdmin() && userNotificationService != null) {
            userNotificationService.initializeUserListener();
        }
    }

    public void setupNavigation(final AppCompatActivity activity) {
        if (activity instanceof Login || activity instanceof Signup) {
            return; // Don't setup navigation for login/signup
        }

        DrawerLayout drawerLayout = activity.findViewById(R.id.drawer_layout);
        NavigationView navigationView = activity.findViewById(R.id.nav_view);

        // Inflate the appropriate menu based on user role
        if (isAdmin()) {
            navigationView.inflateMenu(R.menu.menu_drawer_admin);
        } else {
            navigationView.inflateMenu(R.menu.menu_drawer_user);
        }

        // Set the home item as checked by default
        navigationView.setCheckedItem(R.id.home);

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {
                Intent intent = null;
                switch (menuItem.getItemId()) {
                    case R.id.home:
                        intent = new Intent(activity, Home.class);
                        break;
                    case R.id.profile:
                        intent = new Intent(activity, profile.class);
                        break;
                    case R.id.pending_approvals:
                        intent = new Intent(activity, PendingApprovals.class);
                        break;
                    case R.id.user_records:
                        intent = new Intent(activity, UserRecords.class);
                        break;
                    case R.id.rent:
                        intent = new Intent(activity, Rent.class);
                        break;
                    case R.id.about:
                        intent = new Intent(activity, About.class);
                        break;
                }

                if (intent != null) {
                    activity.startActivity(intent);
                    activity.finish(); // Close the current activity
                }

                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            }
        });

        // Set the correct item as checked based on the current activity
        setCheckedItemForActivity(activity, navigationView);
    }

    private void logout(final AppCompatActivity activity) {
        if (isAdmin()) {
            adminSessionManager.logoutAdmin(new AdminSessionManager.AdminLoginCallback() {
                @Override
                public void onSuccess() {
                    completeLogout(activity);
                }

                @Override
                public void onFailure(String error) {
                    Toast.makeText(activity, "Logout failed: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            FirebaseAuth.getInstance().signOut();
            completeLogout(activity);
        }
    }

    private void completeLogout(AppCompatActivity activity) {
        setAdmin(false);
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.clear();
        editor.apply();

        Intent intent = new Intent(activity, Login.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
    }

    private void setCheckedItemForActivity(AppCompatActivity activity, NavigationView navigationView) {
        if (activity instanceof Home) {
            navigationView.setCheckedItem(R.id.home);
        } else if (activity instanceof profile) {
            navigationView.setCheckedItem(R.id.profile);
        } else if (activity instanceof PendingApprovals) {
            navigationView.setCheckedItem(R.id.pending_approvals);
        } else if (activity instanceof UserRecords) {
            navigationView.setCheckedItem(R.id.user_records);
        } else if (activity instanceof Rent) {
            navigationView.setCheckedItem(R.id.rent);
        } else if (activity instanceof About) {
            navigationView.setCheckedItem(R.id.about);
        }
    }

    public void setAdmin(boolean isAdmin) {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putBoolean(KEY_IS_ADMIN, isAdmin);
        editor.apply();

        if (isAdmin) {
            initializeAdminServices();
        } else {
            initializeUserServices();
        }
    }

    public boolean isAdmin() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getBoolean(KEY_IS_ADMIN, false);
    }
}