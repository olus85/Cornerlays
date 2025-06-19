package app.olus.cornerlays;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

public abstract class BaseOverlayService extends Service {

    private WindowManager windowManager;
    private View overlayView;
    private TextView overlayTextView;
    private WindowManager.LayoutParams params;
    protected SharedPreferences prefs;

    protected final Handler updateHandler = new Handler();
    private boolean isPositioningMode = false;

    protected final Runnable updateTextRunnable = new Runnable() {
        @Override
        public void run() {
            if (overlayTextView != null) {
                updateTextView(overlayTextView);
            }
            updateHandler.postDelayed(this, getUpdateDelay());
        }
    };

    protected abstract @LayoutRes int getLayoutResId();
    protected abstract String getLogTag();
    protected abstract void updateTextView(TextView tv);
    protected abstract long getUpdateDelay();

    protected abstract String getGravityKey();
    protected abstract String getOffsetXKey();
    protected abstract String getOffsetYKey();
    protected abstract String getSizeKey();
    protected abstract String getColorKey();
    protected abstract String getShadowColorKey();

    @Nullable @Override public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(getLogTag(), "onCreate: Service is being created.");

        prefs = getSharedPreferences(SettingsManager.PREFS_NAME, Context.MODE_PRIVATE);
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        overlayView = inflater.inflate(getLayoutResId(), null);
        overlayTextView = overlayView.findViewById(R.id.overlay_text_view);

        int layoutFlag = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT
        );

        applySettings();
        windowManager.addView(overlayView, params);
        updateHandler.post(updateTextRunnable);
    }

    private void applySettings() {
        params.gravity = prefs.getInt(getGravityKey(), Gravity.TOP | Gravity.END);
        params.x = prefs.getInt(getOffsetXKey(), 20);
        params.y = prefs.getInt(getOffsetYKey(), 20);

        int size = prefs.getInt(getSizeKey(), 22);
        int color = prefs.getInt(getColorKey(), ContextCompat.getColor(this, R.color.white));
        int shadowColor = prefs.getInt(getShadowColorKey(), Color.BLACK);

        overlayTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
        overlayTextView.setTextColor(color);
        overlayTextView.setShadowLayer(5.0f, 0, 0, shadowColor);
        overlayTextView.setTypeface(Typeface.DEFAULT_BOLD);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && SettingsManager.ACTION_TOGGLE_POSITIONING_MODE.equals(intent.getAction())) {
            togglePositioningMode();
        }
        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(this, "OverlayServiceChannel")
                .setContentTitle(getLogTag() + " Service")
                .setContentText("Overlay wird angezeigt.")
                .setSmallIcon(R.mipmap.ic_launcher)
                .build();
        startForeground(getNotificationId(), notification);
        return START_STICKY;
    }

    private int getNotificationId() { return this.getClass().hashCode(); }

    private void togglePositioningMode() {
        isPositioningMode = !isPositioningMode;
        if (isPositioningMode) {
            params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
            overlayTextView.setShadowLayer(15f, 0f, 0f, ContextCompat.getColor(this, R.color.m3_primary));
            overlayView.setFocusableInTouchMode(true);
            overlayView.requestFocus();
            overlayView.setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    boolean isGravityTop = (params.gravity & Gravity.VERTICAL_GRAVITY_MASK) == Gravity.TOP;
                    int horizontalGravity = params.gravity & Gravity.HORIZONTAL_GRAVITY_MASK;
                    boolean isGravityLeft = (horizontalGravity == Gravity.LEFT || horizontalGravity == Gravity.START);

                    switch (keyCode) {
                        case KeyEvent.KEYCODE_DPAD_UP: params.y += isGravityTop ? -5 : 5; break;
                        case KeyEvent.KEYCODE_DPAD_DOWN: params.y += isGravityTop ? 5 : -5; break;
                        case KeyEvent.KEYCODE_DPAD_LEFT: params.x += isGravityLeft ? -5 : 5; break;
                        case KeyEvent.KEYCODE_DPAD_RIGHT: params.x += isGravityLeft ? 5 : -5; break;
                        case KeyEvent.KEYCODE_BACK:
                        case KeyEvent.KEYCODE_DPAD_CENTER:
                            togglePositioningMode();
                            return true;
                        default: return false;
                    }
                    params.x = Math.max(0, params.x);
                    params.y = Math.max(0, params.y);
                    windowManager.updateViewLayout(overlayView, params);
                    return true;
                }
                return false;
            });
            Toast.makeText(this, "Positionierungsmodus Aktiviert", Toast.LENGTH_SHORT).show();
        } else {
            params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
            int shadowColor = prefs.getInt(getShadowColorKey(), Color.BLACK);
            overlayTextView.setShadowLayer(5.0f, 0, 0, shadowColor);
            overlayView.setOnKeyListener(null);

            prefs.edit()
                    .putInt(getOffsetXKey(), params.x)
                    .putInt(getOffsetYKey(), params.y)
                    .apply();
            Toast.makeText(this, "Position gespeichert", Toast.LENGTH_SHORT).show();
        }
        windowManager.updateViewLayout(overlayView, params);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(getLogTag(), "onDestroy: Service is being destroyed.");
        updateHandler.removeCallbacks(updateTextRunnable);
        if (overlayView != null && windowManager != null) {
            windowManager.removeView(overlayView);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    "OverlayServiceChannel", "Overlay Service Channel", NotificationManager.IMPORTANCE_DEFAULT);
            getSystemService(NotificationManager.class).createNotificationChannel(serviceChannel);
        }
    }
}