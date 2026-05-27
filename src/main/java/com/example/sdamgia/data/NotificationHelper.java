package com.example.sdamgia.data;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.HashMap;
import java.util.Map;

public class NotificationHelper {

    private static final String CHANNEL_ID = "pet_care";
    private static final int NOTIFICATION_ID = 1001;
    private final Context context;

    public NotificationHelper(Context context) {
        this.context = context;
    }

    public void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "Забота о питомце",
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Напоминания о состоянии питомца");
            NotificationManager manager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    public void notifyLowStat(String statName, int value) {
        Map<String, String> names = new HashMap<>();
        names.put("hunger", "голод");
        names.put("happiness", "грусть");
        names.put("hygiene", "грязь");
        names.put("energy", "усталость");
        String label = names.getOrDefault(statName, statName);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Питомец грустит")
            .setContentText("У питомца низкий показатель \"" + label + "\" (" + value + ")!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true);

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build());
        } catch (SecurityException ignored) {}
    }

    public void notifyDeath() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Питомец умер!")
            .setContentText("Воскреси его, чтобы продолжить игру.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true);

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID + 1, builder.build());
        } catch (SecurityException ignored) {}
    }
}
