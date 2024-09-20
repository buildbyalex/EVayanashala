package com.pro.vayana;

import android.Manifest;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.Build;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.cursoradapter.widget.SimpleCursorAdapter;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class Home extends AppCompatActivity {
    private static final String TAG = "Home";

    private FloatingActionButton fabAddBook;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    private RecyclerView recyclerView;
    private CardAdapter cardAdapter;
    private List<CardItem> cardItems;
    private SearchView searchView;
    private List<CardItem> latestBooks;
    private SimpleCursorAdapter cursorAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private Toolbar toolbar;
    private CustomNavigationView navigationView;

    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (!isUserLoggedIn()) {
            redirectToLogin();
            return;
        }

        initializeViews();
        setupDrawer();
        setupNavigation();
        setupRecyclerView();
        setupSearchView();
        setupSwipeRefreshLayout();

        loadLatestBooks();
        setupPendingRequestsListener();

        setupNotificationPermission();
    }

    private void setupNotificationPermission() {
        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                // Permission granted, you can send notifications
            } else {
                // Permission denied, explain to the user that notifications won't work
                Toast.makeText(this, "Notification permission denied. Some features may not work properly.", Toast.LENGTH_LONG).show();
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void setupPendingRequestsListener() {
        DatabaseReference pendingRequestsRef = FirebaseDatabase.getInstance().getReference("pendingRequests");
        pendingRequestsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                boolean hasPendingRequests = dataSnapshot.getChildrenCount() > 0;
                updateNotificationDot(hasPendingRequests);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle possible errors
            }
        });
    }

    public void updateNotificationDot(boolean show) {
        navigationView.setShowNotificationDot(show);
    }

    private boolean isUserLoggedIn() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null;
    }

    private void redirectToLogin() {
        Intent intent = new Intent(Home.this, Login.class);
        startActivity(intent);
        finish();
    }

    private void initializeViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        fabAddBook = findViewById(R.id.fab);
        recyclerView = findViewById(R.id.recycler_view);
        searchView = findViewById(R.id.search_view);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
    }

    private void setupDrawer() {
        drawerToggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                toolbar,
                R.string.open,
                R.string.close
        );
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }
    }

    private void setupNavigation() {
        EVayanashala app = (EVayanashala) getApplication();
        app.setupNavigation(this);

        if (app.isAdmin()) {
            fabAddBook.show();
            fabAddBook.setOnClickListener(view -> {
                Intent addBook = new Intent(Home.this, AddBookActivity.class);
                startActivity(addBook);
            });
        } else {
            fabAddBook.hide();
        }
    }

    private void setupRecyclerView() {
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2);
        recyclerView.setLayoutManager(gridLayoutManager);

        cardItems = new ArrayList<>();
        latestBooks = new ArrayList<>();
        cardAdapter = new CardAdapter(cardItems, this::openBookDetails);
        recyclerView.setAdapter(cardAdapter);
    }

    private void loadLatestBooks() {
        DatabaseReference booksRef = FirebaseDatabase.getInstance().getReference("books");
        Query query = booksRef.orderByChild("timestamp").limitToLast(20);
        query.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, String previousChildName) {
                String title = dataSnapshot.child("title").getValue(String.class);
                String imageUrl = dataSnapshot.child("imageUrl").getValue(String.class);
                Long timestamp = dataSnapshot.child("timestamp").getValue(Long.class);

                if (title != null && imageUrl != null && timestamp != null) {
                    CardItem newBook = new CardItem(imageUrl, title, timestamp);

                    // Insert the new book in the correct position based on timestamp
                    int insertIndex = 0;
                    for (int i = 0; i < latestBooks.size(); i++) {
                        if (timestamp > latestBooks.get(i).getTimestamp()) {
                            break;
                        }
                        insertIndex++;
                    }
                    latestBooks.add(insertIndex, newBook);

                    if (latestBooks.size() > 20) {
                        latestBooks.remove(latestBooks.size() - 1);
                    }

                    updateCardItems();
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, String previousChildName) {
                // Handle updates if necessary
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                // Handle removals if necessary
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, String previousChildName) {
                // Handle moves if necessary
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(Home.this, "Failed to load books.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateCardItems() {
        cardItems.clear();
        cardItems.addAll(latestBooks);
        cardAdapter.notifyDataSetChanged();
    }

    private void setupSwipeRefreshLayout() {
        swipeRefreshLayout.setOnRefreshListener(this::refreshBooks);
    }

    private void refreshBooks() {
        latestBooks.clear();
        cardItems.clear();
        cardAdapter.notifyDataSetChanged();

        DatabaseReference booksRef = FirebaseDatabase.getInstance().getReference("books");
        Query query = booksRef.orderByChild("timestamp").limitToLast(20);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot bookSnapshot : dataSnapshot.getChildren()) {
                    String title = bookSnapshot.child("title").getValue(String.class);
                    String imageUrl = bookSnapshot.child("imageUrl").getValue(String.class);
                    Long timestamp = bookSnapshot.child("timestamp").getValue(Long.class);

                    if (title != null && imageUrl != null && timestamp != null) {
                        CardItem newBook = new CardItem(imageUrl, title, timestamp);
                        newBook.setTimestamp(timestamp);
                        latestBooks.add(0, newBook);
                    }
                }

                updateCardItems();
                swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(Home.this, "Failed to load books.", Toast.LENGTH_SHORT).show();
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private void setupSearchView() {
        Log.d(TAG, "Setting up search view");
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Log.d(TAG, "Query text submit: " + query);
                searchBooks(query);
                hideKeyboard();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Log.d(TAG, "Query text change: " + newText);
                if (newText.isEmpty()) {
                    cardItems.clear();
                    cardItems.addAll(latestBooks);
                    cardAdapter.notifyDataSetChanged();
                } else {
                    showSuggestions(newText);
                }
                return true;
            }
        });

        searchView.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
            @Override
            public boolean onSuggestionSelect(int position) {
                return false;
            }

            @Override
            public boolean onSuggestionClick(int position) {
                Log.d(TAG, "Suggestion click at position: " + position);
                Cursor cursor = (Cursor) searchView.getSuggestionsAdapter().getItem(position);
                int index = cursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_TEXT_1);
                if (index >= 0) {
                    String suggestion = cursor.getString(index);
                    searchView.setQuery(suggestion, true);
                }
                return true;
            }
        });

        final String[] from = new String[]{SearchManager.SUGGEST_COLUMN_TEXT_1};
        final int[] to = new int[]{R.id.suggestion_title};
        cursorAdapter = new SimpleCursorAdapter(this, R.layout.suggestion_item, null, from, to, 0);
        searchView.setSuggestionsAdapter(cursorAdapter);
    }

    private void showSuggestions(String query) {
        List<String> suggestions = new ArrayList<>();
        for (CardItem item : latestBooks) {
            if (item.getTitle().toLowerCase().contains(query.toLowerCase())) {
                suggestions.add(item.getTitle());
            }
        }

        MatrixCursor cursor = new MatrixCursor(new String[]{BaseColumns._ID, SearchManager.SUGGEST_COLUMN_TEXT_1});
        for (int i = 0; i < suggestions.size(); i++) {
            cursor.addRow(new Object[]{i, suggestions.get(i)});
        }
        cursorAdapter.changeCursor(cursor);
    }

    private void searchBooks(String query) {
        List<CardItem> filteredBooks = new ArrayList<>();
        for (CardItem item : latestBooks) {
            if (item.getTitle().toLowerCase().contains(query.toLowerCase())) {
                filteredBooks.add(item);
            }
        }

        cardItems.clear();
        cardItems.addAll(filteredBooks);
        cardAdapter.notifyDataSetChanged();
    }

    private void openBookDetails(CardItem cardItem) {
        Intent intent = new Intent(Home.this, bookdetails.class);
        intent.putExtra("BOOK_TITLE", cardItem.getTitle());
        intent.putExtra("BOOK_IMAGE_URL", cardItem.getImageUrl());
        startActivity(intent);
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(searchView.getWindowToken(), 0);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("UPDATE_NOTIFICATION_DOT".equals(intent.getAction())) {
                setupPendingRequestsListener(); // This will check and update the dot
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter("UPDATE_NOTIFICATION_DOT"));
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        super.onPause();
    }
}