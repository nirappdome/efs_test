package com.example.efs_read_write_file;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ThreatEventsReceiver extends BroadcastReceiver {

    private static final String TAG = "ThreatEventsReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            Log.i(TAG, "Automation onReceive - action = " + intent.getAction());
            String eventID = intent.getAction();
            String message = intent.getStringExtra("message"); // Message shown to the user
            String reasonData = intent.getStringExtra("reasonData"); // Threat detection cause
            String reasonCode = intent.getStringExtra("reasonCode"); // Event reason code
            String currentThreatEventScore = intent.getStringExtra("currentThreatEventScore"); // Current threat event score
            String threatEventsScore = intent.getStringExtra("threatEventsScore"); // Total threat events score
            String variable = intent.getStringExtra("<Context Key>"); // Any other event specific context key
            Log.i(TAG, message);
            Log.i(TAG, reasonData);
            Log.i(TAG, reasonCode);
        }
    }
}
