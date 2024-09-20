package com.pro.vayana;

import android.content.Context;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class AdminSessionManager {
    private static final String TAG = "AdminSessionManager";
    private static final String ADMIN_SESSION_NODE = "adminSession";

    private Context context;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference adminSessionRef;

    public AdminSessionManager(Context context) {
        this.context = context;
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.adminSessionRef = FirebaseDatabase.getInstance().getReference(ADMIN_SESSION_NODE);
    }

    public interface AdminLoginCallback {
        void onSuccess();
        void onFailure(String error);
    }

    public void loginAdmin(String email, String password, final AdminLoginCallback callback) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null) {
                            checkAndUpdateAdminSession(user.getUid(), callback);
                        } else {
                            callback.onFailure("Failed to get user details");
                        }
                    } else {
                        callback.onFailure("Authentication failed");
                    }
                });
    }

    private void checkAndUpdateAdminSession(String adminUid, final AdminLoginCallback callback) {
        adminSessionRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists() || dataSnapshot.child("adminUid").getValue().equals(adminUid)) {
                    // No active session or same admin, update the session
                    Map<String, Object> sessionUpdates = new HashMap<>();
                    sessionUpdates.put("adminUid", adminUid);
                    sessionUpdates.put("timestamp", System.currentTimeMillis());
                    sessionUpdates.put("deviceId", android.os.Build.SERIAL); // Consider using a more reliable device identifier

                    adminSessionRef.setValue(sessionUpdates)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Admin session updated successfully");
                                callback.onSuccess();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to update admin session", e);
                                callback.onFailure("Failed to update admin session");
                            });
                } else {
                    // Another admin is already logged in
                    callback.onFailure("Another admin is already logged in");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Admin session check failed", databaseError.toException());
                callback.onFailure("Failed to check admin session");
            }
        });
    }

    public void logoutAdmin(final AdminLoginCallback callback) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            adminSessionRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists() && dataSnapshot.child("adminUid").getValue().equals(currentUser.getUid())) {
                        // Clear the admin session
                        adminSessionRef.removeValue()
                                .addOnSuccessListener(aVoid -> {
                                    firebaseAuth.signOut();
                                    Log.d(TAG, "Admin logged out successfully");
                                    callback.onSuccess();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Failed to clear admin session", e);
                                    callback.onFailure("Failed to logout");
                                });
                    } else {
                        // Session doesn't exist or belongs to another admin
                        firebaseAuth.signOut();
                        callback.onSuccess();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.e(TAG, "Logout failed", databaseError.toException());
                    callback.onFailure("Failed to logout");
                }
            });
        } else {
            // No user signed in
            callback.onSuccess();
        }
    }

    public void checkAdminSession(final AdminLoginCallback callback) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            adminSessionRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists() &&
                            dataSnapshot.child("adminUid").getValue().equals(currentUser.getUid()) &&
                            dataSnapshot.child("deviceId").getValue().equals(android.os.Build.SERIAL)) {
                        // Valid admin session
                        callback.onSuccess();
                    } else {
                        // Invalid session, log out the user
                        firebaseAuth.signOut();
                        callback.onFailure("Admin session expired or invalid");
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.e(TAG, "Admin session check failed", databaseError.toException());
                    callback.onFailure("Failed to check admin session");
                }
            });
        } else {
            callback.onFailure("No user signed in");
        }
    }
}