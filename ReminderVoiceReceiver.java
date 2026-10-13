package com.medicarereminder.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.os.PowerManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import java.util.Locale;

public class ReminderVoiceReceiver extends BroadcastReceiver {

    private static final String TAG = "ReminderVoiceReceiver";

    private static TextToSpeech activeTts = null;
    private static PowerManager.WakeLock activeWakeLock = null;
    private static BroadcastReceiver.PendingResult activePendingResult = null;

    @Override
    public void onReceive(Context context, Intent intent) {

        // ==========================================
        // STOP CURRENT VOICE
        // ==========================================
        if (intent != null &&
                "STOP_VOICE".equals(intent.getAction())) {

            Log.d(TAG, "STOP_VOICE received");

            if (activeTts != null) {
                try {
                    activeTts.stop();
                    activeTts.shutdown();
                } catch (Exception e) {
                    Log.e(TAG, "Error stopping TTS", e);
                }

                activeTts = null;
            }

            if (activeWakeLock != null &&
                    activeWakeLock.isHeld()) {

                try {
                    activeWakeLock.release();
                } catch (Exception e) {
                    Log.e(TAG, "Error releasing WakeLock", e);
                }

                activeWakeLock = null;
            }

            if (activePendingResult != null) {
                try {
                    activePendingResult.finish();
                } catch (Exception e) {
                    Log.e(TAG, "Error finishing receiver", e);
                }

                activePendingResult = null;
            }

            return;
        }

        // ==========================================
        // START VOICE REMINDER
        // ==========================================

        final BroadcastReceiver.PendingResult pendingResult =
                goAsync();

        activePendingResult = pendingResult;

        String medicineName =
                intent != null
                        ? intent.getStringExtra("medicineName")
                        : null;

        String dosage =
                intent != null
                        ? intent.getStringExtra("dosage")
                        : null;

        String instructions =
                intent != null
                        ? intent.getStringExtra("instructions")
                        : null;

        if (medicineName == null ||
                medicineName.trim().isEmpty()) {

            medicineName = "your medicine";
        }

        if (dosage == null ||
                dosage.trim().isEmpty()) {

            dosage = "your prescribed dose";
        }

        if (instructions == null) {
            instructions = "";
        }

        final String finalMedicineName = medicineName;
        final String finalDosage = dosage;
        final String finalInstructions = instructions;

        PowerManager powerManager =
                (PowerManager) context.getSystemService(
                        Context.POWER_SERVICE
                );

        activeWakeLock =
                powerManager.newWakeLock(
                        PowerManager.PARTIAL_WAKE_LOCK,
                        TAG + ":WakeLock"
                );

        activeWakeLock.acquire(
                2 * 60 * 1000L
        );

        activeTts =
                new TextToSpeech(
                        context.getApplicationContext(),
                        status -> {

                            if (status != TextToSpeech.SUCCESS) {

                                Log.e(
                                        TAG,
                                        "TTS initialization failed"
                                );

                                finishVoice();

                                return;
                            }

                            if (activeTts == null) {
                                return;
                            }

                            // ==========================================
                            // ALARM AUDIO
                            // ==========================================

                            activeTts.setAudioAttributes(
                                    new AudioAttributes.Builder()
                                            .setUsage(
                                                    AudioAttributes.USAGE_ALARM
                                            )
                                            .setContentType(
                                                    AudioAttributes.CONTENT_TYPE_SPEECH
                                            )
                                            .build()
                            );

                            int languageResult =
                                    activeTts.setLanguage(
                                            Locale.ENGLISH
                                    );

                            if (languageResult ==
                                    TextToSpeech.LANG_MISSING_DATA ||
                                languageResult ==
                                    TextToSpeech.LANG_NOT_SUPPORTED) {

                                Log.e(
                                        TAG,
                                        "English TTS language unavailable"
                                );

                                finishVoice();

                                return;
                            }

                            activeTts.setSpeechRate(0.9f);
                            activeTts.setPitch(1.0f);

                            activeTts.setOnUtteranceProgressListener(
                                    new UtteranceProgressListener() {

                                @Override
                                public void onStart(
                                        String utteranceId) {

                                    Log.d(
                                            TAG,
                                            "Voice started: "
                                                    + utteranceId
                                    );
                                }

                                @Override
                                public void onDone(
                                        String utteranceId) {

                                    Log.d(
                                            TAG,
                                            "Voice completed: "
                                                    + utteranceId
                                    );

                                    if ("REMINDER_5".equals(
                                            utteranceId)) {

                                        Log.d(
                                                TAG,
                                                "ALL 5 VOICE REMINDERS COMPLETED"
                                        );

                                        finishVoice();
                                    }
                                }

                                @Override
                                public void onError(
                                        String utteranceId) {

                                    Log.e(
                                            TAG,
                                            "TTS error: "
                                                    + utteranceId
                                    );

                                    finishVoice();
                                }
                            });

                            // ==========================================
                            // MEDICINE MESSAGE
                            // ==========================================

                            String message;

                            if (!finalInstructions
                                    .trim()
                                    .isEmpty()) {

                                message =
                                        "It is time to take "
                                        + finalMedicineName
                                        + ". "
                                        + finalDosage
                                        + ". "
                                        + finalInstructions
                                        + ".";

                            } else {

                                message =
                                        "It is time to take "
                                        + finalMedicineName
                                        + ". "
                                        + finalDosage
                                        + ".";
                            }

                            // ==========================================
                            // SPEAK 5 TIMES
                            // ==========================================

                            for (int i = 1; i <= 5; i++) {

                                if (activeTts == null) {
                                    break;
                                }

                                int queueMode =
                                        (i == 1)
                                                ? TextToSpeech.QUEUE_FLUSH
                                                : TextToSpeech.QUEUE_ADD;

                                activeTts.speak(
                                        message,
                                        queueMode,
                                        null,
                                        "REMINDER_" + i
                                );
                            }
                        }
                );
    }

    // ==========================================
    // CLEAN UP VOICE
    // ==========================================

    private static synchronized void finishVoice() {

        if (activeTts != null) {

            try {
                activeTts.stop();
                activeTts.shutdown();
            } catch (Exception e) {
                Log.e(TAG, "Error shutting down TTS", e);
            }

            activeTts = null;
        }

        if (activeWakeLock != null &&
                activeWakeLock.isHeld()) {

            try {
                activeWakeLock.release();
            } catch (Exception e) {
                Log.e(TAG, "Error releasing WakeLock", e);
            }

            activeWakeLock = null;
        }

        if (activePendingResult != null) {

            try {
                activePendingResult.finish();
            } catch (Exception e) {
                Log.e(TAG, "Error finishing PendingResult", e);
            }

            activePendingResult = null;
        }
    }
}