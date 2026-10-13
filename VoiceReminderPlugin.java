package com.medicarereminder.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.net.Uri;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "VoiceReminder")
public class VoiceReminderPlugin extends Plugin {

    @PluginMethod
    public void schedule(PluginCall call) {

        String medicineName =
                call.getString(
                        "medicineName",
                        "your medicine"
                );

        String dosage =
                call.getString(
                        "dosage",
                        ""
                );

        String instructions =
                call.getString(
                        "instructions",
                        ""
                );

        long triggerAt =
                call.getLong(
                        "triggerAt",
                        0L
                );

        int reminderId =
                call.getInt(
                        "reminderId",
                        1000
                );

        if (triggerAt <= 0) {
            call.reject("Invalid trigger time");
            return;
        }

        Context context = getContext();

        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(
                        Context.ALARM_SERVICE
                );

        Intent intent =
                new Intent(
                        context,
                        ReminderVoiceReceiver.class
                );

        intent.putExtra(
                "medicineName",
                medicineName
        );

        intent.putExtra(
                "dosage",
                dosage
        );

        intent.putExtra(
                "instructions",
                instructions
        );

        PendingIntent pendingIntent =
                PendingIntent.getBroadcast(
                        context,
                        reminderId,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT |
                        PendingIntent.FLAG_IMMUTABLE
                );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            if (!alarmManager.canScheduleExactAlarms()) {

                Intent settingsIntent =
                        new Intent(
                                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                        );

                settingsIntent.setData(
                        Uri.parse(
                                "package:" +
                                context.getPackageName()
                        )
                );

                settingsIntent.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                );

                context.startActivity(settingsIntent);

                call.reject(
                        "Exact alarm permission required"
                );

                return;
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent
            );

        } else {

            alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent
            );
        }

        JSObject result =
                new JSObject();

        result.put(
                "scheduled",
                true
        );

        call.resolve(result);
    }
    @PluginMethod
public void cancel(PluginCall call) {

    int reminderId =
            call.getInt("reminderId", 0);

    if (reminderId == 0) {
        call.reject("Invalid reminder ID");
        return;
    }

    Context context = getContext();

    AlarmManager alarmManager =
            (AlarmManager) context.getSystemService(
                    Context.ALARM_SERVICE
            );

    Intent intent =
            new Intent(
                    context,
                    ReminderVoiceReceiver.class
            );

    PendingIntent pendingIntent =
            PendingIntent.getBroadcast(
                    context,
                    reminderId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT |
                    PendingIntent.FLAG_IMMUTABLE
            );

    alarmManager.cancel(pendingIntent);

    pendingIntent.cancel();

    JSObject result = new JSObject();
    result.put("cancelled", true);

    call.resolve(result);
}
@PluginMethod
public void stop(PluginCall call) {

    Context context = getContext();

    Intent stopIntent =
            new Intent(
                    context,
                    ReminderVoiceReceiver.class
            );

    stopIntent.setAction("STOP_VOICE");

    context.sendBroadcast(stopIntent);

    call.resolve();
}
}