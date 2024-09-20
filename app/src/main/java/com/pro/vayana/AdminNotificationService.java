package com.pro.vayana;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class AdminNotificationService {
    private static final String CHANNEL_ID = "RentalRequestChannel";
    private static final int NOTIFICATION_ID = 1;
    private static final String TAG = "AdminNotificationService";

    private Context context;
    private DatabaseReference pendingRequestsRef;
    private DatabaseReference usersRef;

    public AdminNotificationService(Context context) {
        this.context = context;
        this.pendingRequestsRef = FirebaseDatabase.getInstance().getReference("pendingRequests");
        this.usersRef = FirebaseDatabase.getInstance().getReference("users");
        createNotificationChannel();
        Log.d(TAG, "AdminNotificationService initialized");
    }

    public void startListeningForRequests() {
        Log.d(TAG, "Starting to listen for admin requests");
        pendingRequestsRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                Log.d(TAG, "onChildAdded: " + dataSnapshot.getKey());
                String bookTitle = dataSnapshot.child("bookTitle").getValue(String.class);
                String userId = dataSnapshot.child("userId").getValue(String.class);
                fetchUserNameAndSendNotification(userId, bookTitle);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                Log.d(TAG, "onChildChanged: " + dataSnapshot.getKey());
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                Log.d(TAG, "onChildRemoved: " + dataSnapshot.getKey());
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {
                Log.d(TAG, "onChildMoved: " + dataSnapshot.getKey());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "onCancelled: ", databaseError.toException());
            }
        });
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Rental Request Notifications";
            String description = "Notifications for new rental requests";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                Log.d(TAG, "Admin notification channel created.");
            } else {
                Log.d(TAG, "Admin NotificationManager is null.");
            }
        }
    }

    private void fetchUserNameAndSendNotification(String userId, String bookTitle) {
        Log.d(TAG, "Fetching user name for userId: " + userId);
        usersRef.child(userId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DataSnapshot dataSnapshot = task.getResult();
                if (dataSnapshot.exists()) {
                    String userName = dataSnapshot.child("name").getValue(String.class);
                    Log.d(TAG, "User name fetched: " + userName);
                    sendNotification("New Rental Request", "User " + userName + " requested '" + bookTitle + "'");
                } else {
                    Log.d(TAG, "User data not found for userId: " + userId);
                    sendNotification("New Rental Request", "A user requested '" + bookTitle + "'");
                }
            } else {
                Log.e(TAG, "Failed to fetch user data", task.getException());
                sendNotification("New Rental Request", "A user requested '" + bookTitle + "'");
            }
        });
    }

    private void sendNotification(String title, String content) {
        Intent intent = new Intent(context, PendingApprovals.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.blue_logoo)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        try {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
            Log.d(TAG, "Admin notification sent: " + title + " - " + content);
        } catch (SecurityException e) {
            Log.e(TAG, "Failed to send admin notification. Permission might be missing.", e);
        }

        if (context instanceof Home) {
            ((Home) context).runOnUiThread(() ->
                    ((Home) context).updateNotificationDot(true)
            );
        }
    }
}