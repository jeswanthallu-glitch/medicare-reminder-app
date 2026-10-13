package com.medicarereminder.app;

import android.os.Bundle;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {

        registerPlugin(VoiceReminderPlugin.class);

        super.onCreate(savedInstanceState);
    }
}