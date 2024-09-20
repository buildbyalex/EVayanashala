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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class UserNotificationService {
    private static final String CHANNEL_ID = "RentalApprovalChannel";
    private static final int NOTIFICATION_ID = 2;
    private static final String TAG = "UserNotificationService";

    private Context context;
    private DatabaseReference userRequestsRef;

    public UserNotificationService(Context context) {
        this.context = context;
        createNotificationChannel();
    }

    public void initializeUserListener() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            this.userRequestsRef = FirebaseDatabase.getInstance().getReference("userRequests").child(userId);
            Log.d(TAG, "UserListener initialized for user: " + userId);
            startListeningForApprovals();
        } else {
            Log.d(TAG, "No current user found.");
        }
    }

    private void startListeningForApprovals() {
        if (userRequestsRef != null) {
            userRequestsRef.addChildEventListener(new ChildEventListener() {
                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                    String status = dataSnapshot.child("status").getValue(String.class);
                    String bookTitle = dataSnapshot.child("bookTitle").getValue(String.class);
                    Log.d(TAG, "onChildChanged: status=" + status + ", bookTitle=" + bookTitle);
                    if ("approved".equals(status)) {
                        sendNotification("Rental Request Approved", "Your request for '" + bookTitle + "' has been approved!");
                    }
                }

                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                    Log.d(TAG, "onChildAdded: " + dataSnapshot.getKey());
                    // Implement if needed
                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {
                    Log.d(TAG, "onChildRemoved: " + dataSnapshot.getKey());
                    // Implement if needed
                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {
                    Log.d(TAG, "onChildMoved: " + dataSnapshot.getKey());
                    // Implement if needed
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.e(TAG, "onCancelled: ", databaseError.toException());
                    // Handle potential errors
                }
            });
        } else {
            Log.d(TAG, "userRequestsRef is null.");
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Rental Approval Notifications";
            String description = "Notifications for approved rental requests";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                Log.d(TAG, "Notification channel created.");
            } else {
                Log.d(TAG, "NotificationManager is null.");
            }
        }
    }

    private void sendNotification(String title, String content) {
        Intent intent = new Intent(context, Home.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.blue_logoo)  // Ensure you have this drawable resource in your project
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
        Log.d(TAG, "Notification sent: " + title + " - " + content);
    }
}
