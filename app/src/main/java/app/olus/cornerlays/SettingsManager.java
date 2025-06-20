package app.olus.cornerlays;

public class SettingsManager {

    public static final String PREFS_NAME = "OnScreenHASettings";
    public static final String ACTION_TOGGLE_POSITIONING_MODE = "app.olus.cornerlays.action.TOGGLE_POSITIONING_MODE";

    // --- Clock Specific ---
    public static final String KEY_CLOCK_ENABLED = "clock_enabled";
    public static final String KEY_CLOCK_SHOW_SECONDS = "clock_show_seconds";
    public static final String KEY_CLOCK_24H_FORMAT = "clock_24h_format";
    public static final String KEY_CLOCK_GRAVITY = "clock_gravity";
    public static final String KEY_CLOCK_OFFSET_X = "clock_offset_x";
    public static final String KEY_CLOCK_OFFSET_Y = "clock_offset_y";
    public static final String KEY_CLOCK_SIZE = "clock_size";
    public static final String KEY_CLOCK_COLOR = "clock_color";
    public static final String KEY_CLOCK_SHADOW_COLOR = "clock_shadow_color";


    // --- Date Specific ---
    public static final String KEY_DATE_ENABLED = "date_enabled";
    public static final String KEY_DATE_FORMAT = "date_format";
    public static final String KEY_DATE_GRAVITY = "date_gravity";
    public static final String KEY_DATE_OFFSET_X = "date_offset_x";
    public static final String KEY_DATE_OFFSET_Y = "date_offset_y";
    public static final String KEY_DATE_SIZE = "date_size";
    public static final String KEY_DATE_COLOR = "date_color";
    public static final String KEY_DATE_SHADOW_COLOR = "date_shadow_color";

    // --- Weather Specific ---
    public static final String KEY_WEATHER_ENABLED = "weather_enabled";
    public static final String KEY_WEATHER_CITY = "weather_city";
    public static final String KEY_WEATHER_FAHRENHEIT = "weather_fahrenheit";
    public static final String KEY_WEATHER_GRAVITY = "weather_gravity";
    public static final String KEY_WEATHER_OFFSET_X = "weather_offset_x";
    public static final String KEY_WEATHER_OFFSET_Y = "weather_offset_y";
    public static final String KEY_WEATHER_SIZE = "weather_size";
    public static final String KEY_WEATHER_COLOR = "weather_color";
    public static final String KEY_WEATHER_SHADOW_COLOR = "weather_shadow_color";
    public static final String ACTION_FORCE_WEATHER_UPDATE = "app.olus.cornerlays.action.FORCE_WEATHER_UPDATE";

    // --- Home Assistant Specific ---
    public static final String KEY_HA_URL = "ha_url";
    public static final String KEY_HA_TOKEN = "ha_token";
    public static final String KEY_HA_OVERLAYS_JSON = "ha_overlays_json";
    public static final String ACTION_HA_CONNECTION_STATUS_UPDATE = "app.olus.cornerlays.action.HA_CONNECTION_STATUS_UPDATE";
    public static final String EXTRA_HA_CONNECTION_STATUS = "extra_ha_connection_status";
    public static final String ACTION_TOGGLE_HA_POSITIONING_MODE = "app.olus.cornerlays.action.TOGGLE_HA_POSITIONING_MODE";
    public static final String EXTRA_HA_SLOT_INDEX = "extra_ha_slot_index";

}