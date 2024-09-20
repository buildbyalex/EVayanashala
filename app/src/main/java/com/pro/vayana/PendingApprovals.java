package com.pro.vayana;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.view.MenuItem;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class PendingApprovals extends AppCompatActivity {

    private static final String TAG = "PendingApprovals";
    private TableLayout tablePendingApprovals;
    private TableLayout tableBooksOnHold;
    private DatabaseReference databaseReference;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pendingapprovals);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        tablePendingApprovals = findViewById(R.id.table_pending_approvals);
        tableBooksOnHold = findViewById(R.id.table_books_on_hold);

        databaseReference = FirebaseDatabase.getInstance().getReference();

        setupNavigation();
        loadPendingApprovals();
        loadBooksOnHold();
    }

    private void setupNavigation() {
        drawerLayout = findViewById(R.id.drawer_layout);
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.open, R.string.close);

        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();


        NavigationView navigationView = findViewById(R.id.nav_view);

        // Check if user is admin and inflate the appropriate menu
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
                    intent = new Intent(PendingApprovals.this, Home.class);
                } else if (id == R.id.profile) {
                    intent = new Intent(PendingApprovals.this, profile.class);
                } else if (id == R.id.pending_approvals) {
                    // We're already here, so do nothing
                    drawerLayout.closeDrawers();
                    return true;
                } else if (id == R.id.user_records) {
                    intent = new Intent(PendingApprovals.this, UserRecords.class);
                } else if (id == R.id.rent) {
                    intent = new Intent(PendingApprovals.this, Rent.class);
                } else if (id == R.id.about) {
                    intent = new Intent(PendingApprovals.this, About.class);
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

    private void loadPendingApprovals() {
        Log.d(TAG, "Loading pending approvals");
        databaseReference.child("pendingRequests").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d(TAG, "Pending approvals data changed. Child count: " + dataSnapshot.getChildrenCount());
                tablePendingApprovals.removeAllViews();
                addTableHeader(tablePendingApprovals, "User", "Book Title", "Actions");

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String userId = snapshot.child("userId").getValue(String.class);
                    String bookTitle = snapshot.child("bookTitle").getValue(String.class);
                    String requestId = snapshot.getKey();

                    // Fetch user name
                    databaseReference.child("users").child(userId).child("name").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                            String userName = userSnapshot.getValue(String.class);
                            if (userName == null) {
                                userName = "Unknown User";
                            }
                            Log.d(TAG, "Pending approval - UserName: " + userName + ", BookTitle: " + bookTitle + ", RequestId: " + requestId);
                            addRowToPendingApprovals(userName, bookTitle, requestId);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            Log.e(TAG, "Failed to load user name", databaseError.toException());
                            addRowToPendingApprovals("Unknown User", bookTitle, requestId);
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Failed to load pending approvals", databaseError.toException());
                Toast.makeText(PendingApprovals.this, "Failed to load pending approvals", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadBooksOnHold() {
        Log.d(TAG, "Loading books on hold");
        databaseReference.child("approvedRequests").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d(TAG, "Books on hold data changed. Child count: " + dataSnapshot.getChildrenCount());
                tableBooksOnHold.removeAllViews();
                addTableHeader(tableBooksOnHold, "User", "Book Title", "Action");

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String userId = snapshot.child("userId").getValue(String.class);
                    String bookTitle = snapshot.child("bookTitle").getValue(String.class);
                    String requestId = snapshot.getKey();

                    // Fetch user name
                    databaseReference.child("users").child(userId).child("name").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                            String userName = userSnapshot.getValue(String.class);
                            if (userName == null) {
                                userName = "Unknown User";
                            }
                            Log.d(TAG, "Book on hold - UserName: " + userName + ", BookTitle: " + bookTitle + ", RequestId: " + requestId);
                            addRowToBooksOnHold(userName, bookTitle, requestId);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            Log.e(TAG, "Failed to load user name", databaseError.toException());
                            addRowToBooksOnHold("Unknown User", bookTitle, requestId);
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Failed to load books on hold", databaseError.toException());
                Toast.makeText(PendingApprovals.this, "Failed to load books on hold", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addTableHeader(TableLayout table, String col1, String col2, String col3) {
        TableRow headerRow = new TableRow(this);
        headerRow.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));
        headerRow.addView(createTextView(col1, true));
        headerRow.addView(createTextView(col2, true));
        headerRow.addView(createTextView(col3, true));
        table.addView(headerRow);
    }

    private void addRowToPendingApprovals(String userName, String bookTitle, final String requestId) {
        TableRow row = new TableRow(this);
        row.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));

        row.addView(createTextView(userName, false));
        row.addView(createTextView(bookTitle, false));

        TableRow.LayoutParams buttonParams = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f);
        buttonParams.setMargins(2, 2, 2, 2);

        ImageButton approveButton = new ImageButton(this);
        approveButton.setImageResource(R.drawable.ic_check_white);
        approveButton.setBackgroundResource(R.drawable.round_button_green);
        approveButton.setLayoutParams(buttonParams);
        approveButton.setOnClickListener(v -> approveRequest(requestId));

        ImageButton denyButton = new ImageButton(this);
        denyButton.setImageResource(R.drawable.ic_close_white);
        denyButton.setBackgroundResource(R.drawable.round_button_red);
        denyButton.setLayoutParams(buttonParams);
        denyButton.setOnClickListener(v -> denyRequest(requestId));

        TableRow buttonsRow = new TableRow(this);
        buttonsRow.addView(approveButton);
        buttonsRow.addView(denyButton);

        TableRow.LayoutParams cellParams = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f);
        buttonsRow.setLayoutParams(cellParams);

        row.addView(buttonsRow);
        tablePendingApprovals.addView(row);
    }

    private void addRowToBooksOnHold(String userName, String bookTitle, final String requestId) {
        TableRow row = new TableRow(this);
        row.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));

        row.addView(createTextView(userName, false));
        row.addView(createTextView(bookTitle, false));

        Button returnButton = new Button(this);
        returnButton.setText("Mark as Returned");
        returnButton.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f));
        returnButton.setOnClickListener(v -> markAsReturned(requestId));

        row.addView(returnButton);
        tableBooksOnHold.addView(row);
    }

    private TextView createTextView(String text, boolean isHeader) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f));
        textView.setPadding(4, 4, 4, 4);
        textView.setGravity(Gravity.CENTER);
        textView.setMaxLines(Integer.MAX_VALUE);
        textView.setEllipsize(null);
        textView.setHorizontallyScrolling(false);
        if (isHeader) {
            textView.setTypeface(null, Typeface.BOLD);
        }
        return textView;
    }

    private void approveRequest(String requestId) {
        Log.d(TAG, "Approving request: " + requestId);
        DatabaseReference requestRef = databaseReference.child("pendingRequests").child(requestId);
        requestRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String bookTitle = dataSnapshot.child("bookTitle").getValue(String.class);
                if (bookTitle != null) {
                    // Move the request to an "approvedRequests" node
                    databaseReference.child("approvedRequests").child(requestId).setValue(dataSnapshot.getValue())
                            .addOnSuccessListener(aVoid -> {
                                // Remove from pendingRequests
                                requestRef.removeValue();
                                Log.d(TAG, "Request approved successfully: " + requestId);
                                Toast.makeText(PendingApprovals.this, "Request approved", Toast.LENGTH_SHORT).show();
                                updateBookStatus(bookTitle, true); // Update onHold to true
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to approve request: " + requestId, e);
                                Toast.makeText(PendingApprovals.this, "Failed to approve request", Toast.LENGTH_SHORT).show();
                            });
                } else {
                    Log.e(TAG, "Book title is null for request: " + requestId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Failed to read request data", databaseError.toException());
            }
        });
        updateNotificationDotInHome();
    }


    private void denyRequest(String requestId) {
        Log.d(TAG, "Denying request: " + requestId);
        databaseReference.child("pendingRequests").child(requestId).removeValue()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Request denied successfully: " + requestId);
                    Toast.makeText(PendingApprovals.this, "Request denied", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to deny request: " + requestId, e);
                    Toast.makeText(PendingApprovals.this, "Failed to deny request", Toast.LENGTH_SHORT).show();
                });
        updateNotificationDotInHome();
    }
    private void updateNotificationDotInHome() {
        Intent intent = new Intent("UPDATE_NOTIFICATION_DOT");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void markAsReturned(String requestId) {
        Log.d(TAG, "Marking request as returned: " + requestId);
        databaseReference.child("approvedRequests").child(requestId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String bookTitle = dataSnapshot.child("bookTitle").getValue(String.class);
                if (bookTitle != null) {
                    // Remove from approvedRequests
                    dataSnapshot.getRef().removeValue()
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Request marked as returned successfully: " + requestId);
                                Toast.makeText(PendingApprovals.this, "Book marked as returned", Toast.LENGTH_SHORT).show();
                                updateBookStatus(bookTitle, false); // Update onHold to false
                                loadBooksOnHold(); // Refresh the books on hold table
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to mark request as returned: " + requestId, e);
                                Toast.makeText(PendingApprovals.this, "Failed to mark book as returned", Toast.LENGTH_SHORT).show();
                            });
                } else {
                    Log.e(TAG, "Book title is null for request: " + requestId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Failed to read request data", databaseError.toException());
            }
        });
    }


    private void updateBookStatus(String bookTitle, boolean onHold) {
        Log.d(TAG, "Updating book status: " + bookTitle + ", onHold: " + onHold);
        databaseReference.child("books").child(bookTitle).child("onHold").setValue(onHold)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Book status updated successfully"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to update book status", e));
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            Intent intent = new Intent(PendingApprovals.this, Home.class);
            startActivity(intent);
            super.onBackPressed();
        }
    }
}
