package app.olus.cornerlays;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.text.TextUtils;

import app.olus.cornerlays.ha.HomeAssistantService;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            SharedPreferences prefs = context.getSharedPreferences(SettingsManager.PREFS_NAME, Context.MODE_PRIVATE);

            if (prefs.getBoolean(SettingsManager.KEY_CLOCK_ENABLED, false)) {
                startOverlayService(context, ClockService.class);
            }
            if (prefs.getBoolean(SettingsManager.KEY_DATE_ENABLED, false)) {
                startOverlayService(context, DateService.class);
            }
            if (prefs.getBoolean(SettingsManager.KEY_WEATHER_ENABLED, false)) {
                startOverlayService(context, WeatherService.class);
            }
            String haJson = prefs.getString(SettingsManager.KEY_HA_OVERLAYS_JSON, "[]");
            if (!TextUtils.isEmpty(haJson) && !haJson.equals("[]")) {
                startOverlayService(context, HomeAssistantService.class);
            }
        }
    }

    private void startOverlayService(Context context, Class<?> serviceClass) {
        Intent serviceIntent = new Intent(context, serviceClass);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }
}