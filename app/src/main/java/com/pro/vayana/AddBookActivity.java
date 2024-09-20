package com.pro.vayana;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class AddBookActivity extends AppCompatActivity {

    private ImageView imageView;
    private EditText titleEditText, bookLocationEditText;
    private EditText authorEditText;
    private EditText genreEditText;
    private EditText publishedYearEditText;
    private EditText dateAddedEditText;
    private EditText priceEditText;
    private EditText summaryEditText;
    private Button addButton;
    private DatabaseReference databaseReference;
    private StorageReference storageReference;

    private Uri imageUri;
    private Bitmap bitmap;
    private static final int PICK_IMAGE_REQUEST = 1;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_addbook);

        imageView = findViewById(R.id.image_view);
        titleEditText = findViewById(R.id.title_edit_text);
        authorEditText = findViewById(R.id.author_edit_text);
        genreEditText = findViewById(R.id.genre_edit_text);
        publishedYearEditText = findViewById(R.id.published_year_edit_text);
        dateAddedEditText = findViewById(R.id.date_added_edit_text);
        priceEditText = findViewById(R.id.price_edit_text);
        summaryEditText = findViewById(R.id.summary_edit_text);
        addButton = findViewById(R.id.add_book_button);
        bookLocationEditText = findViewById(R.id.book_pos);
        databaseReference = FirebaseDatabase.getInstance().getReference("books");
        FirebaseStorage storage = FirebaseStorage.getInstance("gs://e-vayanashala-87694.appspot.com");
        storageReference = storage.getReference();

        // Initialize ProgressDialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Adding Book...");
        progressDialog.setCancelable(false);

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openImageChooser();
            }
        });

        dateAddedEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker(dateAddedEditText);
            }
        });

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validateFields()) {
                    // Show ProgressDialog
                    progressDialog.show();
                    addBook();
                }
            }
        });
    }

    private void openImageChooser() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            imageUri = data.getData();
            try {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                imageView.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void showDatePicker(final EditText editText) {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                editText.setText(dayOfMonth + "/" + (month + 1) + "/" + year);
            }
        }, year, month, day);
        datePickerDialog.show();
    }

    private boolean validateFields() {
        if (titleEditText.getText().toString().trim().isEmpty()) {
            titleEditText.setError("Please enter title");
            titleEditText.requestFocus();
            return false;
        }
        if (authorEditText.getText().toString().trim().isEmpty()) {
            authorEditText.setError("Please enter author");
            authorEditText.requestFocus();
            return false;
        }
        if (genreEditText.getText().toString().trim().isEmpty()) {
            genreEditText.setError("Please enter genre");
            genreEditText.requestFocus();
            return false;
        }
        if (publishedYearEditText.getText().toString().trim().isEmpty()) {
            publishedYearEditText.setError("Please enter published year");
            publishedYearEditText.requestFocus();
            return false;
        }
        if (dateAddedEditText.getText().toString().trim().isEmpty()) {
            dateAddedEditText.setError("Please enter date added");
            dateAddedEditText.requestFocus();
            return false;
        }
        if (priceEditText.getText().toString().trim().isEmpty()) {
            priceEditText.setError("Please enter price");
            priceEditText.requestFocus();
            return false;
        }
        if (summaryEditText.getText().toString().trim().isEmpty()) {
            summaryEditText.setError("Please enter summary");
            summaryEditText.requestFocus();
            return false;
        }
        if (bookLocationEditText.getText().toString().trim().isEmpty()) {
            bookLocationEditText.setError("Please enter book location");
            bookLocationEditText.requestFocus();
            return false;
        }
        if (imageUri == null || bitmap == null) {
            Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void addBook() {
        final String bookName = titleEditText.getText().toString().trim();
        final String author = authorEditText.getText().toString().trim();
        final String genre = genreEditText.getText().toString().trim();
        final String publishedYear = publishedYearEditText.getText().toString().trim();
        final String dateAdded = dateAddedEditText.getText().toString().trim();
        final String price = priceEditText.getText().toString().trim();
        final String summary = summaryEditText.getText().toString().trim();
        final String bookLocation = bookLocationEditText.getText().toString().trim();
        boolean onHold = false;

        // Check image size
        if (!isImageSizeValid(imageUri)) {
            progressDialog.dismiss();
            Toast.makeText(this, "Please select an image of file size below 100MB", Toast.LENGTH_LONG).show();
            return;
        }

        // Upload image to Firebase Storage
        StorageReference imageRef = storageReference.child("book_images/" + bookName + ".jpg");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] imageData = baos.toByteArray();

        UploadTask uploadTask = imageRef.putBytes(imageData);
        uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // Get the download URL for the image
                imageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        String imageUrl = uri.toString();

                        // Create a map of book data
                        Map<String, Object> bookData = new HashMap<>();
                        bookData.put("title", bookName);
                        bookData.put("author", author);
                        bookData.put("genre", genre);
                        bookData.put("publishedYear", publishedYear);
                        bookData.put("dateAdded", dateAdded);
                        bookData.put("price", price);
                        bookData.put("summary", summary);
                        bookData.put("location", bookLocation);
                        bookData.put("imageUrl", imageUrl);
                        bookData.put("onHold", onHold);
                        bookData.put("timestamp", ServerValue.TIMESTAMP);

                        // Add book data to Realtime Database
                        databaseReference.child(bookName).setValue(bookData).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                progressDialog.dismiss();  // Dismiss ProgressDialog
                                Toast.makeText(AddBookActivity.this, "Book added successfully", Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                progressDialog.dismiss();  // Dismiss ProgressDialog
                                Toast.makeText(AddBookActivity.this, "Failed to add book: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();  // Dismiss ProgressDialog
                        Toast.makeText(AddBookActivity.this, "Failed to get image URL: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                progressDialog.dismiss();  // Dismiss ProgressDialog
                Toast.makeText(AddBookActivity.this, "Failed to upload image: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // Method to check the image size
    private boolean isImageSizeValid(Uri imageUri) {
        try {
            // Get the input stream of the image
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            // Get the size of the image
            int imageSize = inputStream.available();
            // Check if the size is less than 100MB (100 * 1024 * 1024 bytes)
            return imageSize < (100 * 1024 * 1024);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}