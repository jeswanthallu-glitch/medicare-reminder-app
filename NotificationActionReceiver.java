package com.medicarereminder.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class NotificationActionReceiver extends BroadcastReceiver {

    private static final String TAG = "MediCareAction";

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent == null) {
            return;
        }

        String actionId = intent.getStringExtra("actionId");
        String scheduleId = intent.getStringExtra("scheduleId");

        Log.d(TAG,
                "Notification action received. actionId="
                        + actionId
                        + ", scheduleId="
                        + scheduleId
        );

        if (scheduleId == null || scheduleId.trim().isEmpty()) {
            Log.e(TAG, "No scheduleId received.");
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> update = new HashMap<>();

        if ("TOOK_IT".equals(actionId)) {

            update.put("status", "taken");
            update.put("patientResponse", "I took it");
            update.put("caretakerResponseAlertSent", null);

        } else if ("NO".equals(actionId)) {

            update.put("status", "pending");
            update.put("patientResponse", "No");
            update.put("caretakerResponseAlertSent", null);

        } else {

            Log.w(TAG, "Unknown notification action: " + actionId);
            return;
        }

        db.collection("schedules")
                .document(scheduleId)
                .update(update)
                .addOnSuccessListener(unused ->
                        Log.d(TAG,
                                "Medicine response saved successfully: "
                                        + actionId
                        )
                )
                .addOnFailureListener(error ->
                        Log.e(TAG,
                                "Could not update medicine response",
                                error
                        )
                );
    }
}