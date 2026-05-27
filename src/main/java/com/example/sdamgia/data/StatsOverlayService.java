package com.example.sdamgia.data;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.core.app.NotificationCompat;

import com.example.sdamgia.model.GameState;

public class StatsOverlayService extends Service {

    private static final String CHANNEL_ID = "overlay_stats";
    private static final int NOTIFICATION_ID = 2001;
    private static final long UPDATE_INTERVAL_MS = 3000L;
    private static final String TAG = "StatsOverlay";

    private WindowManager windowManager;
    private View overlayView;
    private PreferencesManager preferencesManager;
    private WindowManager.LayoutParams params;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                if (preferencesManager != null) {
                    GameState state = preferencesManager.loadGameState();
                    updateOverlay(state);
                }
            } catch (Exception ignored) {}
            handler.postDelayed(this, UPDATE_INTERVAL_MS);
        }
    };

    private int hungerBarId = View.generateViewId();
    private int happinessBarId = View.generateViewId();
    private int hygieneBarId = View.generateViewId();
    private int energyBarId = View.generateViewId();
    private int nameTextId = View.generateViewId();

    public static void start(Context context) {
        try {
            Intent intent = new Intent(context, StatsOverlayService.class);
            context.startForegroundService(intent);
        } catch (Exception e) {
            android.util.Log.w(TAG, "Failed to start overlay service: " + e.getMessage());
        }
    }

    public static void stop(Context context) {
        try {
            context.stopService(new Intent(context, StatsOverlayService.class));
        } catch (Exception ignored) {}
    }

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            preferencesManager = new PreferencesManager(this);
            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            createNotificationChannel();
            startForeground(NOTIFICATION_ID, createNotification());
            showOverlay();
            handler.post(updateRunnable);
        } catch (Exception e) {
            android.util.Log.w(TAG, "onCreate failed: " + e.getMessage(), e);
            stopSelf();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(updateRunnable);
        try {
            if (overlayView != null) windowManager.removeView(overlayView);
        } catch (Exception ignored) {}
        super.onDestroy();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "Оверлей питомца",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Показывает статы питомца поверх приложений");
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Статы питомца")
            .setContentText("Оверлей активен")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build();
    }

    private void showOverlay() {
        float density = getResources().getDisplayMetrics().density;

        TextView nameText = new TextView(this);
        nameText.setId(nameTextId);
        nameText.setTextColor(Color.WHITE);
        nameText.setTextSize(12f);

        View hungerBar = createStatBar(density, Color.rgb(255, 112, 67));
        hungerBar.setId(hungerBarId);
        View happinessBar = createStatBar(density, Color.rgb(255, 213, 79));
        happinessBar.setId(happinessBarId);
        View hygieneBar = createStatBar(density, Color.rgb(77, 208, 225));
        hygieneBar.setId(hygieneBarId);
        View energyBar = createStatBar(density, Color.rgb(129, 199, 132));
        energyBar.setId(energyBarId);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(
            (int) (8 * density), (int) (6 * density),
            (int) (8 * density), (int) (6 * density)
        );
        layout.setBackgroundColor(Color.argb(200, 30, 30, 30));
        layout.setOnTouchListener(new OverlayTouchListener());

        layout.addView(nameText,
            new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        );
        layout.addView(hungerBar,
            new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        );
        layout.addView(happinessBar,
            new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        );
        layout.addView(hygieneBar,
            new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        );
        layout.addView(energyBar,
            new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        );

        overlayView = layout;

        params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.START | Gravity.TOP;
        params.x = 0;
        params.y = (int) (100 * density);

        try {
            windowManager.addView(overlayView, params);
        } catch (Exception e) {
            android.util.Log.w(TAG, "addView failed: " + e.getMessage());
        }
    }

    private View createStatBar(float density, int color) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, (int) (2 * density), 0, (int) (2 * density));

        ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        bar.setLayoutParams(new LinearLayout.LayoutParams(
            (int) (80 * density), (int) (6 * density)
        ));
        bar.setProgressDrawable(
            new ClipDrawable(new ColorDrawable(color), Gravity.START, ClipDrawable.HORIZONTAL)
        );

        row.addView(bar);
        return row;
    }

    private void updateOverlay(GameState state) {
        try {
            if (overlayView == null) return;
            TextView nameText = overlayView.findViewById(nameTextId);
            if (nameText != null) {
                nameText.setText(state.getPetName() + "  Lv." + state.getLevel());
            }
            updateBarValue(overlayView.findViewById(hungerBarId), (int) state.getHunger());
            updateBarValue(overlayView.findViewById(happinessBarId), (int) state.getHappiness());
            updateBarValue(overlayView.findViewById(hygieneBarId), (int) state.getHygiene());
            updateBarValue(overlayView.findViewById(energyBarId), (int) state.getEnergy());
        } catch (Exception ignored) {}
    }

    private void updateBarValue(View row, int value) {
        if (!(row instanceof LinearLayout)) return;
        View child = ((LinearLayout) row).getChildAt(0);
        if (child instanceof ProgressBar) {
            ((ProgressBar) child).setProgress(value);
        }
    }

    private class OverlayTouchListener implements View.OnTouchListener {
        private int initialX;
        private int initialY;
        private float initialTouchX;
        private float initialTouchY;
        private boolean isDragging = false;
        private long downTime = 0;
        private final Handler touchHandler = new Handler(Looper.getMainLooper());
        private final Runnable longPressRunnable = new Runnable() {
            @Override
            public void run() {
                onLongPress();
            }
        };

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (params == null) return false;
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    initialX = params.x;
                    initialY = params.y;
                    initialTouchX = event.getRawX();
                    initialTouchY = event.getRawY();
                    isDragging = false;
                    downTime = System.currentTimeMillis();
                    touchHandler.postDelayed(longPressRunnable, 600L);
                    return true;
                case MotionEvent.ACTION_MOVE:
                    int dx = (int) (event.getRawX() - initialTouchX);
                    int dy = (int) (event.getRawY() - initialTouchY);
                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                        isDragging = true;
                        touchHandler.removeCallbacks(longPressRunnable);
                    }
                    if (isDragging) {
                        params.x = initialX + dx;
                        params.y = initialY + dy;
                        windowManager.updateViewLayout(v, params);
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                    touchHandler.removeCallbacks(longPressRunnable);
                    if (!isDragging) {
                        onTap();
                    }
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    touchHandler.removeCallbacks(longPressRunnable);
                    return true;
            }
            return false;
        }

        private void onTap() {
            try {
                Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                }
            } catch (Exception ignored) {}
        }

        private void onLongPress() {
            try {
                GameState state = preferencesManager.loadGameState();
                state.setOverlayEnabled(false);
                preferencesManager.saveGameState(state);
            } catch (Exception ignored) {}
            stopSelf();
        }
    }
}
