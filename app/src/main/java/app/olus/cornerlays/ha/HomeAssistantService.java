package app.olus.cornerlays.ha;

import android.annotation.SuppressLint;
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
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import app.olus.cornerlays.R;
import app.olus.cornerlays.SettingsManager;
import app.olus.cornerlays.ha.model.HAOverlay;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class HomeAssistantService extends Service {

    private static final String TAG = "HA_DEBUG"; // DEBUG-TAG
    private static final int NOTIFICATION_ID = 4;
    private static final String NOTIFICATION_CHANNEL_ID = "HomeAssistantServiceChannel";

    private WindowManager windowManager;
    private SharedPreferences prefs;
    private Gson gson;
    private OkHttpClient httpClient;
    private WebSocket webSocket;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final AtomicInteger messageId = new AtomicInteger(1);

    private List<HAOverlay> overlayConfigs;
    private final List<View> overlayViews = new ArrayList<>();
    private final Map<String, String> entityStates = new ConcurrentHashMap<>();

    private boolean isPositioningMode = false;
    private int positioningSlotIndex = -1;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: Service wird erstellt.");
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        prefs = getSharedPreferences(SettingsManager.PREFS_NAME, MODE_PRIVATE);
        gson = new Gson();
        httpClient = new OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .build();

        createNotificationChannel();
        Notification notification = buildNotification("Starting Service...");
        startForeground(NOTIFICATION_ID, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: Service-Befehl empfangen mit Aktion: " + (intent != null ? intent.getAction() : "null"));

        if (intent != null && Objects.equals(intent.getAction(), SettingsManager.ACTION_TOGGLE_HA_POSITIONING_MODE)) {
            // Wächter-Klausel für Positionierungsmodus bleibt erhalten, aber die eigentliche Logik wird in togglePositioningMode behandelt
            int slotIndex = intent.getIntExtra(SettingsManager.EXTRA_HA_SLOT_INDEX, -1);
            if (slotIndex != -1) {
                togglePositioningMode(slotIndex);
            }
            return START_STICKY;
        }

        if (isPositioningMode) {
            Log.d(TAG, "onStartCommand: Ignoriere Befehl, da im Positionierungsmodus.");
            return START_STICKY;
        }

        loadConfiguration();

        String url = prefs.getString(SettingsManager.KEY_HA_URL, "");
        boolean hasOverlays = isAnyOverlayConfigured();

        if (TextUtils.isEmpty(url) && !hasOverlays) {
            Log.d(TAG, "onStartCommand: Keine URL und keine Overlays. Stoppe Service.");
            stopSelf();
            return START_NOT_STICKY;
        }

        Log.d(TAG, "onStartCommand: Konfiguration wird angewendet. Baue Verbindung neu auf.");
        disconnect();
        removeOverlays();
        createOverlays();

        if (!TextUtils.isEmpty(url)) {
            connect();
        } else {
            Log.d(TAG, "onStartCommand: Keine URL, aber Overlays sind konfiguriert. Service läuft weiter, aber ohne Verbindung.");
            broadcastConnectionStatus(getString(R.string.ha_status_disconnected));
        }

        return START_STICKY;
    }

    private void togglePositioningMode(int slotIndex) {
        View overlayView = (overlayViews.size() > slotIndex) ? overlayViews.get(slotIndex) : null;
        if (overlayView == null) return;

        // BUGFIX: Prüfen, ob das Overlay sichtbar ist, BEVOR der Modus umgeschaltet wird.
        if (!isPositioningMode && overlayView.getVisibility() == View.GONE) {
            Toast.makeText(this, "Overlay muss sichtbar sein für Feintuning.", Toast.LENGTH_SHORT).show();
            return;
        }

        isPositioningMode = !isPositioningMode;

        TextView textView = overlayView.findViewById(R.id.overlay_text_view);
        WindowManager.LayoutParams params = (WindowManager.LayoutParams) overlayView.getLayoutParams();

        if (isPositioningMode) {
            positioningSlotIndex = slotIndex;
            params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
            textView.setShadowLayer(15f, 0f, 0f, ContextCompat.getColor(this, R.color.m3_primary));
            overlayView.setFocusableInTouchMode(true);
            overlayView.requestFocus();
            overlayView.setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    HAOverlay config = overlayConfigs.get(positioningSlotIndex);
                    boolean isGravityTop = (config.getGravity() & Gravity.VERTICAL_GRAVITY_MASK) == Gravity.TOP;
                    int horizontalGravity = config.getGravity() & Gravity.HORIZONTAL_GRAVITY_MASK;
                    boolean isGravityLeft = (horizontalGravity == Gravity.LEFT || horizontalGravity == Gravity.START);

                    switch (keyCode) {
                        case KeyEvent.KEYCODE_DPAD_UP: params.y += isGravityTop ? -5 : 5; break;
                        case KeyEvent.KEYCODE_DPAD_DOWN: params.y += isGravityTop ? 5 : -5; break;
                        case KeyEvent.KEYCODE_DPAD_LEFT: params.x += isGravityLeft ? -5 : 5; break;
                        case KeyEvent.KEYCODE_DPAD_RIGHT: params.x += isGravityLeft ? 5 : -5; break;
                        case KeyEvent.KEYCODE_BACK:
                        case KeyEvent.KEYCODE_DPAD_CENTER:
                            togglePositioningMode(positioningSlotIndex);
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
            HAOverlay config = overlayConfigs.get(positioningSlotIndex);
            textView.setShadowLayer(5.0f, 0, 0, config.getShadowColor());
            overlayView.setOnKeyListener(null);

            config.setOffsetX(params.x);
            config.setOffsetY(params.y);
            String json = gson.toJson(overlayConfigs);
            prefs.edit().putString(SettingsManager.KEY_HA_OVERLAYS_JSON, json).apply();

            positioningSlotIndex = -1;
            Toast.makeText(this, "Position gespeichert", Toast.LENGTH_SHORT).show();
        }
        windowManager.updateViewLayout(overlayView, params);
    }

    private void loadConfiguration() {
        String json = prefs.getString(SettingsManager.KEY_HA_OVERLAYS_JSON, "[]");
        Log.d(TAG, "loadConfiguration: Lade JSON: " + json);
        Type listType = new TypeToken<ArrayList<HAOverlay>>() {}.getType();
        overlayConfigs = gson.fromJson(json, listType);
    }

    private boolean isAnyOverlayConfigured() {
        if (overlayConfigs == null || overlayConfigs.isEmpty()) {
            return false;
        }
        for (HAOverlay config : overlayConfigs) {
            if (config.isEnabled() && !TextUtils.isEmpty(config.getEntityId())) {
                return true;
            }
        }
        return false;
    }

    private void connect() {
        String url = prefs.getString(SettingsManager.KEY_HA_URL, "");
        Log.d(TAG, "connect: Verbindungsversuch mit URL: " + url);

        String wsUrl = url.replace("http", "ws") + "/api/websocket";
        Log.d(TAG, "connect: Transformierte WebSocket URL: " + wsUrl);
        Request request = new Request.Builder().url(wsUrl).build();

        Log.d(TAG, "connect: Erstelle neuen WebSocket...");
        broadcastConnectionStatus(getString(R.string.ha_status_connecting));
        webSocket = httpClient.newWebSocket(request, new HaWebSocketListener());
    }

    private void disconnect() {
        if (webSocket != null) {
            Log.d(TAG, "disconnect: Schließe existierenden WebSocket.");
            webSocket.close(1000, "Service stopping");
            webSocket = null;
        }
        handler.removeCallbacks(updateRunnable);
    }

    private class HaWebSocketListener extends WebSocketListener {
        @Override
        public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
            Log.d(TAG, "HaWebSocketListener.onOpen: WebSocket Verbindung geöffnet.");
        }

        @Override
        public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
            Log.d(TAG, "HaWebSocketListener.onMessage: Nachricht empfangen: " + text);
            handler.post(() -> handleWebSocketMessage(text));
        }

        @Override
        public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, @Nullable Response response) {
            Log.e(TAG, "HaWebSocketListener.onFailure: WebSocket Fehler: " + t.getMessage(), t);
            broadcastConnectionStatus(getString(R.string.ha_status_error) + ": " + t.getMessage());
        }

        @Override
        public void onClosing(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
            Log.d(TAG, "HaWebSocketListener.onClosing: WebSocket wird geschlossen. Code: " + code + ", Grund: " + reason);
            webSocket.close(1000, null);
        }

        @Override
        public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
            Log.d(TAG, "HaWebSocketListener.onClosed: WebSocket geschlossen. Code: " + code + ", Grund: " + reason);
            broadcastConnectionStatus(getString(R.string.ha_status_disconnected));
        }
    }

    private void handleWebSocketMessage(String text) {
        try {
            JsonObject message = JsonParser.parseString(text).getAsJsonObject();
            String type = message.get("type").getAsString();
            Log.d(TAG, "handleWebSocketMessage: Verarbeite Nachricht vom Typ '" + type + "'");

            switch (type) {
                case "auth_required":
                    authenticate();
                    break;
                case "auth_ok":
                    Log.i(TAG, "handleWebSocketMessage: Authentifizierung erfolgreich!");
                    broadcastConnectionStatus(getString(R.string.ha_status_connected));
                    subscribeToStates();
                    handler.removeCallbacks(updateRunnable);
                    handler.post(updateRunnable);
                    break;
                case "auth_invalid":
                    String authError = message.get("message").getAsString();
                    Log.e(TAG, "handleWebSocketMessage: Authentifizierung fehlgeschlagen: " + authError);
                    broadcastConnectionStatus(getString(R.string.ha_auth_failed) + ": " + authError);
                    break;
                case "result":
                    if (message.has("success") && message.get("success").getAsBoolean() && message.has("result") && message.get("result").isJsonArray()) {
                        JsonArray states = message.getAsJsonArray("result");
                        for (JsonElement stateElem : states) {
                            JsonObject stateObj = stateElem.getAsJsonObject();
                            String entityId = stateObj.get("entity_id").getAsString();
                            String state = stateObj.get("state").getAsString();
                            entityStates.put(entityId, state);
                        }
                    } else {
                        Log.w(TAG, "handleWebSocketMessage: 'result' war nicht erfolgreich oder hatte ein unerwartetes Format.");
                    }
                    break;
                case "event":
                    handleEvent(message);
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "handleWebSocketMessage: Fehler beim Parsen der JSON-Nachricht.", e);
        }
    }

    private void authenticate() {
        String token = prefs.getString(SettingsManager.KEY_HA_TOKEN, "");
        if (TextUtils.isEmpty(token)) {
            Log.e(TAG, "authenticate: Zugriffstoken ist leer. Breche Authentifizierung ab.");
            broadcastConnectionStatus("Error: Token is empty");
            return;
        }
        Log.d(TAG, "authenticate: Sende Authentifizierungs-Nachricht.");
        JsonObject authMessage = new JsonObject();
        authMessage.addProperty("type", "auth");
        authMessage.addProperty("access_token", token);
        webSocket.send(authMessage.toString());
    }

    private void subscribeToStates() {
        List<String> monitoredEntities = overlayConfigs.stream()
                .filter(c -> c.isEnabled() && !TextUtils.isEmpty(c.getEntityId()))
                .map(HAOverlay::getEntityId)
                .distinct()
                .collect(Collectors.toList());

        if (monitoredEntities.isEmpty()) {
            Log.d(TAG, "subscribeToStates: Keine Overlays zum Abonnieren, überspringe.");
            return;
        }

        JsonObject subscribeMessage = new JsonObject();
        subscribeMessage.addProperty("id", messageId.getAndIncrement());
        subscribeMessage.addProperty("type", "subscribe_entities");

        JsonArray entityIdsArray = new JsonArray();
        for (String entityId : monitoredEntities) {
            entityIdsArray.add(entityId);
        }
        subscribeMessage.add("entity_ids", entityIdsArray);

        webSocket.send(subscribeMessage.toString());

        Log.d(TAG, "subscribeToStates: Fordere initialen Status aller Entitäten an.");
        JsonObject fetchStatesMessage = new JsonObject();
        fetchStatesMessage.addProperty("id", messageId.getAndIncrement());
        fetchStatesMessage.addProperty("type", "get_states");
        webSocket.send(fetchStatesMessage.toString());
    }

    private void handleEvent(JsonObject message) {
        try {
            JsonObject eventData = message.getAsJsonObject("event");
            if (eventData.has("c")) {
                JsonObject changedEntities = eventData.getAsJsonObject("c");
                Set<String> entityIds = changedEntities.keySet();
                for (String entityId : entityIds) {
                    JsonObject change = changedEntities.getAsJsonObject(entityId);
                    if (change.has("+")) {
                        JsonObject stateData = change.getAsJsonObject("+");
                        if (stateData.has("s")) {
                            String state = stateData.get("s").getAsString();
                            entityStates.put(entityId, state);
                            Log.d(TAG, "handleEvent (compact): Zustand für '" + entityId + "' aktualisiert auf: " + state);
                        }
                    }
                }
            } else if (eventData.has("event_type") && "state_changed".equals(eventData.get("event_type").getAsString())) {
                JsonObject data = eventData.getAsJsonObject("data");
                String entityId = data.get("entity_id").getAsString();
                if (isEntityMonitored(entityId)) {
                    JsonObject newState = data.getAsJsonObject("new_state");
                    String state = newState.get("state").isJsonNull() ? "unavailable" : newState.get("state").getAsString();
                    entityStates.put(entityId, state);
                    Log.d(TAG, "handleEvent (state_changed): Zustand für '" + entityId + "' aktualisiert auf: " + state);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Fehler beim Verarbeiten eines Events", e);
        }
    }


    private boolean isEntityMonitored(String entityId) {
        for (HAOverlay config : overlayConfigs) {
            if (config.isEnabled() && entityId.equals(config.getEntityId())) {
                return true;
            }
        }
        return false;
    }

    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            updateOverlays();
            handler.postDelayed(this, 1000);
        }
    };

    private void createOverlays() {
        removeOverlays();
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);

        for (int i = 0; i < overlayConfigs.size(); i++) {
            HAOverlay config = overlayConfigs.get(i);
            if (!config.isEnabled() || TextUtils.isEmpty(config.getEntityId())) {
                overlayViews.add(null);
                continue;
            }

            @SuppressLint("InflateParams")
            View overlayView = inflater.inflate(R.layout.overlay_clock, null);
            overlayViews.add(overlayView);
            applySettingsToView(overlayView, config);
            windowManager.addView(overlayView, getLayoutParams(config));
        }
    }

    private void removeOverlays() {
        for (View view : overlayViews) {
            if (view != null && view.isAttachedToWindow()) {
                windowManager.removeView(view);
            }
        }
        overlayViews.clear();
    }


    private void updateOverlays() {
        for (int i = 0; i < overlayConfigs.size(); i++) {
            HAOverlay config = overlayConfigs.get(i);
            View view = (overlayViews.size() > i) ? overlayViews.get(i) : null;

            if (view == null || !config.isEnabled() || TextUtils.isEmpty(config.getEntityId())) {
                continue;
            }

            TextView tv = view.findViewById(R.id.overlay_text_view);
            String entityId = config.getEntityId();
            String state = entityStates.getOrDefault(entityId, getString(R.string.ha_unavailable));
            String textToShow;

            boolean isAvailable = state != null && !state.equalsIgnoreCase("unavailable") && !state.equalsIgnoreCase("unknown") && !state.equalsIgnoreCase("off");

            if (!isAvailable && config.isHideWhenUnavailable()) {
                view.setVisibility(View.GONE);
                continue;
            } else {
                view.setVisibility(View.VISIBLE);
            }

            if ("Countdown".equals(config.getDisplayMode()) && isAvailable) {
                try {
                    OffsetDateTime targetTime = OffsetDateTime.parse(state, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                    long secondsRemaining = ChronoUnit.SECONDS.between(Instant.now(), targetTime.toInstant());

                    if (secondsRemaining > 0) {
                        long minutes = secondsRemaining / 60;
                        long seconds = secondsRemaining % 60;
                        textToShow = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
                    } else {
                        textToShow = "00:00";
                    }
                } catch (Exception e) {
                    textToShow = "Invalid Date";
                }
            } else {
                String displayName = TextUtils.isEmpty(config.getDisplayName()) ? "" : config.getDisplayName() + " ";
                String unit = TextUtils.isEmpty(config.getUnit()) ? "" : " " + config.getUnit();
                textToShow = displayName + (isAvailable ? state : getString(R.string.ha_unavailable)) + unit;
            }

            tv.setText(textToShow.trim());
        }
    }

    private void applySettingsToView(View view, HAOverlay config) {
        TextView textView = view.findViewById(R.id.overlay_text_view);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, config.getSize());
        textView.setTextColor(config.getColor());
        textView.setShadowLayer(5.0f, 0, 0, config.getShadowColor());
        textView.setTypeface(Typeface.DEFAULT_BOLD);
    }

    private WindowManager.LayoutParams getLayoutParams(HAOverlay config) {
        int layoutFlag = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = config.getGravity();
        params.x = config.getOffsetX();
        params.y = config.getOffsetY();
        return params;
    }

    private void broadcastConnectionStatus(String status) {
        Log.d(TAG, "broadcastConnectionStatus: Sende neuen Status: '" + status + "'");

        prefs.edit().putString("ha_last_connection_status", status).apply();

        Intent intent = new Intent(SettingsManager.ACTION_HA_CONNECTION_STATUS_UPDATE);
        intent.putExtra(SettingsManager.EXTRA_HA_CONNECTION_STATUS, status);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        updateNotification(status);
    }


    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "Home Assistant Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            getSystemService(NotificationManager.class).createNotificationChannel(serviceChannel);
        }
    }

    private Notification buildNotification(String text) {
        return new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("Home Assistant Service")
                .setContentText(text)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void updateNotification(String text) {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, buildNotification(text));
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: Service wird zerstört.");
        disconnect();
    }
}