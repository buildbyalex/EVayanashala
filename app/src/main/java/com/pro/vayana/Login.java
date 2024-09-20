package com.pro.vayana;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Login extends AppCompatActivity {

    private static final String TAG = "Login";
    private EditText etUsername, etPassword;
    private Button btnLogin;
    private TextView signup;
    private ProgressBar progressBar;
    private SharedPreferences sharedPreferences;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private AdminSessionManager adminSessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        sharedPreferences = getSharedPreferences("loginPrefs", MODE_PRIVATE);
        adminSessionManager = new AdminSessionManager(this);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnlogin);
        signup = findViewById(R.id.btnsignup);
        progressBar = findViewById(R.id.progressBar);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validateLogin();
            }
        });

        signup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Login.this, Signup.class);
                startActivity(intent);
            }
        });
    }

    private void validateLogin() {
        String email = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etUsername.setError("Please enter Email");
            etUsername.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Please enter Password");
            etPassword.requestFocus();
            return;
        }

        loginUser(email, password);
    }

    private void loginUser(final String email, final String password) {
        progressBar.setVisibility(View.VISIBLE);
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            String userId = mAuth.getCurrentUser().getUid();
                            checkAdminStatus(userId, email, password);
                        } else {
                            progressBar.setVisibility(View.GONE);
                            Log.e(TAG, "Error: " + task.getException().getMessage());
                            Toast.makeText(Login.this, "Login Failed. Try again", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void checkAdminStatus(final String userId, final String email, final String password) {
        mDatabase.child("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    User user = dataSnapshot.getValue(User.class);
                    if (user != null) {
                        if (user.isBlocked()) {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(Login.this, "Your account has been blocked. Please contact the administrator.", Toast.LENGTH_LONG).show();
                            FirebaseAuth.getInstance().signOut();
                        } else if (user.isAdmin) {
                            adminLogin(email, password);
                        } else {
                            completeLogin(false);
                        }
                    } else {
                        completeLogin(false);
                    }
                } else {
                    completeLogin(false);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                progressBar.setVisibility(View.GONE);
                Log.e(TAG, "Error: " + databaseError.getMessage());
                Toast.makeText(Login.this, "Login Failed. Try again", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void adminLogin(String email, String password) {
        adminSessionManager.loginAdmin(email, password, new AdminSessionManager.AdminLoginCallback() {
            @Override
            public void onSuccess() {
                completeLogin(true);
            }

            @Override
            public void onFailure(String error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(Login.this, error, Toast.LENGTH_SHORT).show();
                FirebaseAuth.getInstance().signOut();
            }
        });
    }

    private void completeLogin(boolean isAdmin) {
        progressBar.setVisibility(View.GONE);
        Toast.makeText(Login.this, "Login successful", Toast.LENGTH_SHORT).show();

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("username", etUsername.getText().toString().trim());
        editor.putBoolean("isLoggedIn", true);
        editor.putBoolean("isAdmin", isAdmin);
        editor.apply();

        ((EVayanashala) getApplication()).setAdmin(isAdmin);
        ((EVayanashala) getApplication()).initializeUserNotificationService();

        Intent intent = new Intent(Login.this, Home.class);
        startActivity(intent);
        finish();
    }
}