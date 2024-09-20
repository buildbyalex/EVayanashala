package com.pro.vayana;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class bookdetails extends AppCompatActivity {

    private TextView titleTextView, authorTextView, genreTextView, publishedYearTextView,
            dateAddedTextView, priceTextView, summaryTextView, locationTextView;
    private ImageView coverImageView;
    private Button rentBookButton;
    private String bookTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bookdetails);

        initializeViews();

        if (isAdmin()) {
            rentBookButton.setVisibility(View.GONE);
        } else {
            rentBookButton.setVisibility(View.VISIBLE);
        }

        bookTitle = getIntent().getStringExtra("BOOK_TITLE");
        if (bookTitle != null) {
            loadBookDetails(bookTitle);
        } else {
            Toast.makeText(this, "Error: Book title not provided", Toast.LENGTH_SHORT).show();
            finish();
        }

        rentBookButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendRentalRequest();
            }
        });
    }

    private void initializeViews() {
        titleTextView = findViewById(R.id.title_text_view);
        authorTextView = findViewById(R.id.author_text_view);
        genreTextView = findViewById(R.id.genre_text_view);
        publishedYearTextView = findViewById(R.id.published_year_text_view);
        dateAddedTextView = findViewById(R.id.date_added_text_view);
        priceTextView = findViewById(R.id.price_text_view);
        summaryTextView = findViewById(R.id.summary_text_view);
        locationTextView = findViewById(R.id.location_text_view);
        coverImageView = findViewById(R.id.cover_image_view);
        rentBookButton = findViewById(R.id.rent_book_button);
    }

    private boolean isAdmin() {
        EVayanashala app = (EVayanashala) getApplication();
        return app.isAdmin();
    }

    private void loadBookDetails(String bookTitle) {
        DatabaseReference bookRef = FirebaseDatabase.getInstance().getReference("books").child(bookTitle);
        bookRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String title = dataSnapshot.child("title").getValue(String.class);
                    String author = dataSnapshot.child("author").getValue(String.class);
                    String genre = dataSnapshot.child("genre").getValue(String.class);
                    String publishedYear = dataSnapshot.child("publishedYear").getValue(String.class);
                    String dateAdded = dataSnapshot.child("dateAdded").getValue(String.class);
                    String price = dataSnapshot.child("price").getValue(String.class);
                    String summary = dataSnapshot.child("summary").getValue(String.class);
                    String location = dataSnapshot.child("location").getValue(String.class);
                    String imageUrl = dataSnapshot.child("imageUrl").getValue(String.class);

                    titleTextView.setText(title);
                    authorTextView.setText(author);
                    genreTextView.setText(genre);
                    publishedYearTextView.setText(publishedYear);
                    dateAddedTextView.setText(dateAdded);
                    priceTextView.setText(price);
                    summaryTextView.setText(summary);
                    locationTextView.setText(location);

                    Glide.with(bookdetails.this)
                            .load(imageUrl)
                            .into(coverImageView);

                    checkRentalRequestStatus();
                } else {
                    Toast.makeText(bookdetails.this, "Book not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(bookdetails.this, "Failed to load book details: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void checkRentalRequestStatus() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference rentalRequestsRef = FirebaseDatabase.getInstance().getReference("pendingRequests");
        rentalRequestsRef.orderByChild("userId").equalTo(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                boolean requestFound = false;
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String requestedBookTitle = snapshot.child("bookTitle").getValue(String.class);
                    if (bookTitle.equals(requestedBookTitle)) {
                        rentBookButton.setEnabled(false);
                        rentBookButton.setText("Request Sent");
                        requestFound = true;
                        break;
                    }
                }

                if (!requestFound) {
                    checkApprovedRequestStatus();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(bookdetails.this, "Failed to check rental request status: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkApprovedRequestStatus() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference approvedRequestsRef = FirebaseDatabase.getInstance().getReference("approvedRequests");
        approvedRequestsRef.orderByChild("bookTitle").equalTo(bookTitle).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                boolean bookOnHold = false;
                boolean currentUserApproved = false;
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String approvedUserId = snapshot.child("userId").getValue(String.class);
                    bookOnHold = true;
                    if (currentUserId.equals(approvedUserId)) {
                        currentUserApproved = true;
                        break;
                    }
                }

                if (currentUserApproved) {
                    rentBookButton.setEnabled(false);
                    rentBookButton.setText("Request Approved");
                } else if (bookOnHold) {
                    rentBookButton.setEnabled(false);
                    rentBookButton.setText("Book on Hold");
                } else {
                    rentBookButton.setEnabled(true);
                    rentBookButton.setText("Rent Book");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(bookdetails.this, "Failed to check approved request status: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendRentalRequest() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference rentalRequestsRef = FirebaseDatabase.getInstance().getReference("pendingRequests");
        String requestId = rentalRequestsRef.push().getKey();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String currentDateTime = sdf.format(new Date());

        Map<String, Object> rentalRequest = new HashMap<>();
        rentalRequest.put("userId", userId);
        rentalRequest.put("bookTitle", bookTitle);
        rentalRequest.put("requestDate", currentDateTime);

        rentalRequestsRef.child(requestId).setValue(rentalRequest)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(bookdetails.this, "Rental request sent successfully", Toast.LENGTH_SHORT).show();
                    rentBookButton.setEnabled(false);
                    rentBookButton.setText("Request Sent");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(bookdetails.this, "Failed to send rental request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}