package app.olus.cornerlays;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import app.olus.cornerlays.ha.HomeAssistantService;


/* loaded from: classes5.dex */
public class SettingsManager {
    public static final String ACTION_FORCE_WEATHER_UPDATE = "app.olus.cornerlays.action.FORCE_WEATHER_UPDATE";
    public static final String ACTION_HA_CONNECTION_STATUS_UPDATE = "app.olus.cornerlays.action.HA_CONNECTION_STATUS_UPDATE";
    public static final String ACTION_MQTT_CONNECTION_STATUS_UPDATE = "app.olus.cornerlays.action.MQTT_CONNECTION_STATUS_UPDATE";
    public static final String ACTION_TOGGLE_HA_POSITIONING_MODE = "app.olus.cornerlays.action.TOGGLE_HA_POSITIONING_MODE";
    public static final String ACTION_TOGGLE_POSITIONING_MODE = "app.olus.cornerlays.action.TOGGLE_POSITIONING_MODE";
    public static final String EXTRA_HA_CONNECTION_STATUS = "extra_ha_connection_status";
    public static final String EXTRA_HA_SLOT_INDEX = "extra_ha_slot_index";
    public static final String EXTRA_MQTT_CONNECTION_STATUS = "extra_mqtt_connection_status";
    public static final String KEY_CLOCK_24H_FORMAT = "clock_24h_format";
    public static final String KEY_CLOCK_COLOR = "clock_color";
    public static final String KEY_CLOCK_ENABLED = "clock_enabled";
    public static final String KEY_CLOCK_GRAVITY = "clock_gravity";
    public static final String KEY_CLOCK_OFFSET_X = "clock_offset_x";
    public static final String KEY_CLOCK_OFFSET_Y = "clock_offset_y";
    public static final String KEY_CLOCK_SHADOW_COLOR = "clock_shadow_color";
    public static final String KEY_CLOCK_SHOW_SECONDS = "clock_show_seconds";
    public static final String KEY_CLOCK_SIZE = "clock_size";
    public static final String KEY_DATE_COLOR = "date_color";
    public static final String KEY_DATE_ENABLED = "date_enabled";
    public static final String KEY_DATE_FORMAT = "date_format";
    public static final String KEY_DATE_GRAVITY = "date_gravity";
    public static final String KEY_DATE_OFFSET_X = "date_offset_x";
    public static final String KEY_DATE_OFFSET_Y = "date_offset_y";
    public static final String KEY_DATE_SHADOW_COLOR = "date_shadow_color";
    public static final String KEY_DATE_SIZE = "date_size";
    public static final String KEY_HA_OVERLAYS_JSON = "ha_overlays_json";
    public static final String KEY_HA_TOKEN = "ha_token";
    public static final String KEY_HA_URL = "ha_url";
    public static final String KEY_MQTT_IP = "mqtt_ip";
    public static final String KEY_MQTT_PASS = "mqtt_pass";
    public static final String KEY_MQTT_PORT = "mqtt_port";
    public static final String KEY_MQTT_USER = "mqtt_user";
    public static final String KEY_WEATHER_CITY = "weather_city";
    public static final String KEY_WEATHER_COLOR = "weather_color";
    public static final String KEY_WEATHER_ENABLED = "weather_enabled";
    public static final String KEY_WEATHER_FAHRENHEIT = "weather_fahrenheit";
    public static final String KEY_WEATHER_GRAVITY = "weather_gravity";
    public static final String KEY_WEATHER_OFFSET_X = "weather_offset_x";
    public static final String KEY_WEATHER_OFFSET_Y = "weather_offset_y";
    public static final String KEY_WEATHER_SHADOW_COLOR = "weather_shadow_color";
    public static final String KEY_WEATHER_SIZE = "weather_size";
    public static final String PREFS_NAME = "OnScreenHASettings";

    public static void checkAndStartServices(Context context) {
        if (!Settings.canDrawOverlays(context)) {
            Log.e("Cornerlays", "Overlay permission missing, cannot start services");
            return;
        }
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        if (prefs.getBoolean(KEY_CLOCK_ENABLED, false)) {
            startOverlayService(context, ClockService.class);
        } else {
            context.stopService(new Intent(context, (Class<?>) ClockService.class));
        }
        if (prefs.getBoolean(KEY_DATE_ENABLED, false)) {
            startOverlayService(context, DateService.class);
        } else {
            context.stopService(new Intent(context, (Class<?>) DateService.class));
        }
        if (prefs.getBoolean(KEY_WEATHER_ENABLED, false)) {
            startOverlayService(context, WeatherService.class);
        } else {
            context.stopService(new Intent(context, (Class<?>) WeatherService.class));
        }
        String haJson = prefs.getString(KEY_HA_OVERLAYS_JSON, "[]");
        if (!TextUtils.isEmpty(haJson) && !haJson.equals("[]")) {
            startOverlayService(context, HomeAssistantService.class);
        } else {
            context.stopService(new Intent(context, (Class<?>) HomeAssistantService.class));
        }
    }

    private static void startOverlayService(Context context, Class<?> serviceClass) {
        Intent serviceIntent = new Intent(context, serviceClass);
        if (Build.VERSION.SDK_INT >= 26) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }
}
