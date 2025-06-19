package app.olus.cornerlays;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Locale;
import java.util.Objects;

public class WeatherService extends BaseOverlayService {

    private RequestQueue requestQueue;
    private static final String TAG = "WeatherService";

    @Override
    public void onCreate() {
        super.onCreate();
        requestQueue = Volley.newRequestQueue(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // DEBUGGING-PUNKT 2
        Log.d("WETTER_DEBUG", "2. WeatherService onStartCommand aufgerufen mit Aktion: " + (intent != null ? intent.getAction() : "null"));

        if (intent != null && Objects.equals(intent.getAction(), SettingsManager.ACTION_FORCE_WEATHER_UPDATE)) {
            // DEBUGGING-PUNKT 3
            Log.d("WETTER_DEBUG", "3. Korrekte Aktion empfangen. Erzwinge Update.");
            updateHandler.removeCallbacks(updateTextRunnable);
            updateHandler.post(updateTextRunnable);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.overlay_weather;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected void updateTextView(TextView tv) {
        String city = prefs.getString(SettingsManager.KEY_WEATHER_CITY, "").trim();
        // DEBUGGING-PUNKT 4
        Log.d("WETTER_DEBUG", "4. updateTextView aufgerufen. Gelesene Stadt: '" + city + "'");

        if (city.isEmpty()) {
            tv.setText(R.string.weather_no_city);
            return;
        }

        // DEBUGGING-PUNKT 5
        Log.d("WETTER_DEBUG", "5. Stadt ist nicht leer. Starte fetchCoordinates.");
        fetchCoordinates(city, tv);
    }

    private void fetchCoordinates(String city, TextView tv) {
        String geoUrl = "https://geocoding-api.open-meteo.com/v1/search?name=" + Uri.encode(city) + "&count=1";

        JsonObjectRequest geoRequest = new JsonObjectRequest(Request.Method.GET, geoUrl, null,
                response -> {
                    try {
                        // DEBUGGING-PUNKT 6
                        Log.d("WETTER_DEBUG", "6. Antwort von Geocoding-API erhalten.");
                        if (response.has("results")) {
                            JSONArray results = response.getJSONArray("results");
                            if (results.length() > 0) {
                                JSONObject location = results.getJSONObject(0);
                                double latitude = location.getDouble("latitude");
                                double longitude = location.getDouble("longitude");
                                // DEBUGGING-PUNKT 7
                                Log.d("WETTER_DEBUG", "7. Koordinaten gefunden: " + latitude + ", " + longitude + ". Starte fetchWeather.");
                                fetchWeather(latitude, longitude, tv);
                            } else {
                                tv.setText(R.string.weather_city_not_found);
                            }
                        } else {
                            tv.setText(R.string.weather_city_not_found);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Geocoding JSON parsing error", e);
                        tv.setText(R.string.weather_error);
                    }
                },
                error -> {
                    Log.e(TAG, "Geocoding API error", error);
                    tv.setText(R.string.weather_error);
                });

        requestQueue.add(geoRequest);
    }

    private void fetchWeather(double latitude, double longitude, TextView tv) {
        String weatherUrl = String.format(Locale.US,
                "https://api.open-meteo.com/v1/forecast?latitude=%f&longitude=%f&current_weather=true",
                latitude, longitude);

        JsonObjectRequest weatherRequest = new JsonObjectRequest(Request.Method.GET, weatherUrl, null,
                response -> {
                    try {
                        // DEBUGGING-PUNKT 8
                        Log.d("WETTER_DEBUG", "8. Antwort von Wetter-API erhalten.");
                        JSONObject currentWeather = response.getJSONObject("current_weather");
                        double temperature = currentWeather.getDouble("temperature");

                        String tempUnit = "°C";
                        double finalTemp = temperature;

                        boolean useFahrenheit = prefs.getBoolean(SettingsManager.KEY_WEATHER_FAHRENHEIT, false);
                        if(useFahrenheit) {
                            finalTemp = (temperature * 9/5) + 32;
                            tempUnit = "°F";
                        }

                        // DEBUGGING-PUNKT 9
                        Log.d("WETTER_DEBUG", "9. Wetter erfolgreich verarbeitet. Setze Text.");
                        tv.setText(String.format(Locale.getDefault(), "%.1f%s", finalTemp, tempUnit));

                    } catch (Exception e) {
                        Log.e(TAG, "Weather JSON parsing error", e);
                        tv.setText(R.string.weather_error);
                    }
                },
                error -> {
                    Log.e(TAG, "Weather API error", error);
                    tv.setText(R.string.weather_error);
                });

        requestQueue.add(weatherRequest);
    }


    @Override
    protected long getUpdateDelay() {
        return 1800000; // 30 Minuten
    }

    @Override
    protected String getGravityKey() {
        return SettingsManager.KEY_WEATHER_GRAVITY;
    }

    @Override
    protected String getOffsetXKey() {
        return SettingsManager.KEY_WEATHER_OFFSET_X;
    }

    @Override
    protected String getOffsetYKey() {
        return SettingsManager.KEY_WEATHER_OFFSET_Y;
    }

    @Override
    protected String getSizeKey() {
        return SettingsManager.KEY_WEATHER_SIZE;
    }

    @Override
    protected String getColorKey() {
        return SettingsManager.KEY_WEATHER_COLOR;
    }

    @Override
    protected String getShadowColorKey() {
        return SettingsManager.KEY_WEATHER_SHADOW_COLOR;
    }
}