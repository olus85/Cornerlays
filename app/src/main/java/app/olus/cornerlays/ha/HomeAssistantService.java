package app.olus.cornerlays.ha;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import app.olus.cornerlays.R;
import app.olus.cornerlays.SettingsManager;
import app.olus.cornerlays.ha.model.HAOverlay;
import app.olus.cornerlays.ha.model.HARule;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class HomeAssistantService extends Service {

    private static final String TAG = "HA_DEBUG";
    private static final int NOTIFICATION_ID = 4;
    private static final String NOTIFICATION_CHANNEL_ID = "HomeAssistantServiceChannel";
    private static final long PING_INTERVAL_MS = 20_000; // 20 Sekunden
    private static final long PING_TIMEOUT_MS = 10_000;  // 10 Sekunden
    private static final long RECONNECT_INITIAL_DELAY_MS = 1_000;
    private static final long RECONNECT_MAX_DELAY_MS = 60_000;

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
    private final Map<String, JsonObject> entityAttributes = new ConcurrentHashMap<>();

    private boolean isPositioningMode = false;
    private int positioningSlotIndex = -1;

    private final AtomicBoolean isConnected = new AtomicBoolean(false);
    private long reconnectDelay = RECONNECT_INITIAL_DELAY_MS;

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
                .pingInterval(0, TimeUnit.MILLISECONDS)
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
        removeOverlays();
        createOverlays();

        reconnect();

        return START_STICKY;
    }

    private void connect() {
        if (isConnected.get()) {
            return;
        }
        String url = prefs.getString(SettingsManager.KEY_HA_URL, "");
        if (TextUtils.isEmpty(url) || !isAnyOverlayConfigured()) {
            Log.d(TAG, "connect: Cancelled. URL is empty or no overlays configured.");
            stopSelf();
            return;
        }

        String wsUrl = url.replace("http", "ws") + "/api/websocket";
        Log.d(TAG, "connect: Connecting to " + wsUrl);
        Request request = new Request.Builder().url(wsUrl).build();

        broadcastConnectionStatus(getString(R.string.ha_status_connecting));
        webSocket = httpClient.newWebSocket(request, new HaWebSocketListener());
    }

    private void disconnect() {
        handler.removeCallbacks(pingRunnable);
        handler.removeCallbacks(pingTimeoutRunnable);
        if (webSocket != null) {
            webSocket.close(1000, "Service initiated disconnect");
            webSocket = null;
        }
        isConnected.set(false);
        handler.removeCallbacks(updateRunnable);
    }

    private void reconnect() {
        disconnect();
        handler.removeCallbacks(reconnectionRunnable);
        reconnectDelay = RECONNECT_INITIAL_DELAY_MS;
        handler.post(reconnectionRunnable);
    }

    private void scheduleNextReconnect() {
        if (isPositioningMode) return;
        disconnect();
        handler.removeCallbacks(reconnectionRunnable);
        handler.postDelayed(reconnectionRunnable, reconnectDelay);
        reconnectDelay = Math.min(reconnectDelay * 2, RECONNECT_MAX_DELAY_MS);
    }

    private final Runnable reconnectionRunnable = this::connect;

    private final Runnable pingRunnable = new Runnable() {
        @Override
        public void run() {
            if (webSocket != null && isConnected.get()) {
                JsonObject pingMessage = new JsonObject();
                pingMessage.addProperty("id", messageId.getAndIncrement());
                pingMessage.addProperty("type", "ping");
                if (webSocket.send(pingMessage.toString())) {
                    handler.postDelayed(pingTimeoutRunnable, PING_TIMEOUT_MS);
                } else {
                    scheduleNextReconnect();
                }
            }
        }
    };

    private final Runnable pingTimeoutRunnable = this::scheduleNextReconnect;


    private class HaWebSocketListener extends WebSocketListener {
        @Override
        public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
            Log.d(TAG, "WebSocket onOpen called.");
        }

        @Override
        public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
            Log.d(TAG, "WebSocket onMessage: " + text);
            handler.post(() -> handleWebSocketMessage(text));
        }

        @Override
        public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, @Nullable Response response) {
            Log.e(TAG, "WebSocket onFailure: ", t);
            broadcastConnectionStatus(getString(R.string.ha_status_error));
            scheduleNextReconnect();
        }

        @Override
        public void onClosing(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
            Log.d(TAG, "WebSocket onClosing: " + reason);
        }

        @Override
        public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
            Log.d(TAG, "WebSocket onClosed: " + reason);
            broadcastConnectionStatus(getString(R.string.ha_status_disconnected));
            scheduleNextReconnect();
        }
    }

    private void handleWebSocketMessage(String text) {
        try {
            JsonObject message = JsonParser.parseString(text).getAsJsonObject();
            if (message == null || !message.has("type") || message.get("type").isJsonNull()) {
                return;
            }
            String type = message.get("type").getAsString();

            switch (type) {
                case "auth_required":
                    authenticate();
                    break;
                case "auth_ok":
                    isConnected.set(true);
                    reconnectDelay = RECONNECT_INITIAL_DELAY_MS;
                    broadcastConnectionStatus(getString(R.string.ha_status_connected));
                    subscribeToStates();
                    handler.post(updateRunnable);
                    handler.post(pingRunnable);
                    break;
                case "auth_invalid":
                    broadcastConnectionStatus(getString(R.string.ha_auth_failed));
                    scheduleNextReconnect();
                    break;
                case "result":
                    if (message.has("success") && message.get("success").getAsBoolean() && message.has("result") && message.get("result").isJsonArray()) {
                        JsonArray states = message.getAsJsonArray("result");
                        for (JsonElement stateElem : states) {
                            JsonObject stateObj = stateElem.getAsJsonObject();
                            String entityId = stateObj.get("entity_id").getAsString();
                            String state = stateObj.get("state").getAsString();
                            entityStates.put(entityId, state);
                            if (stateObj.has("attributes") && !stateObj.get("attributes").isJsonNull()) {
                                entityAttributes.put(entityId, stateObj.getAsJsonObject("attributes"));
                            }
                        }
                    }
                    break;
                case "event":
                    handleEvent(message);
                    break;
                case "pong":
                    handler.removeCallbacks(pingTimeoutRunnable);
                    handler.postDelayed(pingRunnable, PING_INTERVAL_MS);
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "handleWebSocketMessage: Fehler beim Parsen der JSON-Nachricht.", e);
        }
    }

    private void authenticate() {
        String token = prefs.getString(SettingsManager.KEY_HA_TOKEN, "");
        if (TextUtils.isEmpty(token)) {
            broadcastConnectionStatus("Error: Token is empty");
            scheduleNextReconnect();
            return;
        }
        JsonObject authMessage = new JsonObject();
        authMessage.addProperty("type", "auth");
        authMessage.addProperty("access_token", token);
        if (webSocket != null) {
            webSocket.send(authMessage.toString());
        }
    }
    private void subscribeToStates() {
        List<String> monitoredEntities = new ArrayList<>();
        for (HAOverlay c : overlayConfigs) {
            if (c.isEnabled()) {
                if (!TextUtils.isEmpty(c.getEntityId())) {
                    monitoredEntities.add(c.getEntityId());
                }
                if (c.getVisibilityMode() == 3 && !TextUtils.isEmpty(c.getTriggerEntityId())) {
                    monitoredEntities.add(c.getTriggerEntityId());
                }
            }
        }
        monitoredEntities = monitoredEntities.stream().distinct().collect(Collectors.toList());

        if (monitoredEntities.isEmpty()) return;

        JsonObject subscribeMessage = new JsonObject();
        subscribeMessage.addProperty("id", messageId.getAndIncrement());
        subscribeMessage.addProperty("type", "subscribe_entities");

        JsonArray entityIdsArray = new JsonArray();
        for (String entityId : monitoredEntities) entityIdsArray.add(entityId);
        subscribeMessage.add("entity_ids", entityIdsArray);
        if (webSocket != null) webSocket.send(subscribeMessage.toString());

        JsonObject fetchStatesMessage = new JsonObject();
        fetchStatesMessage.addProperty("id", messageId.getAndIncrement());
        fetchStatesMessage.addProperty("type", "get_states");
        if (webSocket != null) webSocket.send(fetchStatesMessage.toString());
    }

    private void handleEvent(JsonObject message) {
        try {
            JsonObject eventData = message.getAsJsonObject("event");
            if (eventData == null) {
                return;
            }
            if (eventData.has("a")) {
                JsonObject addedEntities = eventData.getAsJsonObject("a");
                for (String entityId : addedEntities.keySet()) {
                    JsonObject added = addedEntities.getAsJsonObject(entityId);
                    if (added != null) {
                        if (added.has("s")) {
                            JsonElement sVal = added.get("s");
                            if (sVal != null && !sVal.isJsonNull()) {
                                entityStates.put(entityId, sVal.getAsString());
                            }
                        }
                        if (added.has("a")) {
                            JsonObject aVal = added.getAsJsonObject("a");
                            if (aVal != null) {
                                entityAttributes.put(entityId, aVal);
                            }
                        }
                    }
                }
            }
            if (eventData.has("c")) {
                JsonObject changedEntities = eventData.getAsJsonObject("c");
                for (String entityId : changedEntities.keySet()) {
                    JsonObject change = changedEntities.getAsJsonObject(entityId);
                    if (change != null) {
                        if (change.has("+")) {
                            JsonObject plus = change.getAsJsonObject("+");
                            if (plus.has("s")) {
                                JsonElement sVal = plus.get("s");
                                if (sVal != null && !sVal.isJsonNull()) {
                                    entityStates.put(entityId, sVal.getAsString());
                                }
                            }
                            if (plus.has("a")) {
                                JsonObject aVal = plus.getAsJsonObject("a");
                                if (aVal != null) {
                                    mergeAttributes(entityId, aVal);
                                }
                            }
                        }
                        if (change.has("-")) {
                            JsonObject minus = change.getAsJsonObject("-");
                            if (minus.has("a")) {
                                JsonArray removedAttrs = minus.getAsJsonArray("a");
                                if (removedAttrs != null) {
                                    JsonObject current = entityAttributes.get(entityId);
                                    if (current != null) {
                                        for (JsonElement el : removedAttrs) {
                                            if (el.isJsonPrimitive()) {
                                                current.remove(el.getAsString());
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (eventData.has("event_type") && !eventData.get("event_type").isJsonNull() && "state_changed".equals(eventData.get("event_type").getAsString())) {
                JsonObject data = eventData.getAsJsonObject("data");
                if (data != null && data.has("entity_id") && !data.get("entity_id").isJsonNull()) {
                    String entityId = data.get("entity_id").getAsString();
                    if (isEntityMonitored(entityId)) {
                        JsonObject newState = data.getAsJsonObject("new_state");
                        if (newState != null) {
                            if (newState.has("state")) {
                                String state = newState.get("state").isJsonNull() ? "unavailable" : newState.get("state").getAsString();
                                entityStates.put(entityId, state);
                            }
                            if (newState.has("attributes")) {
                                JsonObject attrs = newState.getAsJsonObject("attributes");
                                if (attrs != null) {
                                    entityAttributes.put(entityId, attrs);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Fehler beim Verarbeiten eines Events", e);
        }
    }

    private void mergeAttributes(String entityId, JsonObject newAttrs) {
        JsonObject current = entityAttributes.get(entityId);
        if (current == null) {
            current = new JsonObject();
            entityAttributes.put(entityId, current);
        }
        for (String key : newAttrs.keySet()) {
            current.add(key, newAttrs.get(key));
        }
    }


    private boolean isEntityMonitored(String entityId) {
        if(overlayConfigs == null) return false;
        for (HAOverlay config : overlayConfigs) {
            if (config.isEnabled()) {
                if (entityId.equals(config.getEntityId())) return true;
                if (config.getVisibilityMode() == 3 && entityId.equals(config.getTriggerEntityId())) return true;
            }
        }
        return false;
    }

    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            if (isConnected.get()) {
                updateOverlays();
                handler.postDelayed(this, 1000);
            }
        }
    };

    private void createOverlays() {
        removeOverlays();
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        if (overlayConfigs == null) return;
        for (int i = 0; i < overlayConfigs.size(); i++) {
            HAOverlay config = overlayConfigs.get(i);
            if (!config.isEnabled() || TextUtils.isEmpty(config.getEntityId())) {
                overlayViews.add(null);
                continue;
            }
            View overlayView = inflater.inflate(R.layout.overlay_clock, null);
            overlayViews.add(overlayView);
            applySettingsToView(overlayView, config);
            windowManager.addView(overlayView, getLayoutParams(config));
        }
    }

    private void removeOverlays() {
        for (View view : overlayViews) {
            if (view != null && view.isAttachedToWindow()) windowManager.removeView(view);
        }
        overlayViews.clear();
    }


    private void updateOverlays() {
        if (overlayConfigs == null) return;
        for (int i = 0; i < overlayConfigs.size(); i++) {
            HAOverlay config = overlayConfigs.get(i);
            View view = (overlayViews.size() > i) ? overlayViews.get(i) : null;
            if (view == null) continue;

            if (!config.isEnabled() || TextUtils.isEmpty(config.getEntityId())) {
                view.setVisibility(View.GONE);
                continue;
            }

            TextView tv = view.findViewById(R.id.overlay_text_view);
            String entityId = config.getEntityId();

            // Resolve either main state or configured attribute
            String state;
            String attrName = config.getAttributeName();
            if (!TextUtils.isEmpty(attrName)) {
                JsonObject attrs = entityAttributes.get(entityId);
                JsonElement attrVal = (attrs != null) ? attrs.get(attrName) : null;
                if (attrVal != null && !attrVal.isJsonNull()) {
                    if (attrVal.isJsonPrimitive()) {
                        state = attrVal.getAsString();
                    } else {
                        state = attrVal.toString();
                    }
                } else {
                    state = getString(R.string.ha_unavailable);
                }
            } else {
                state = entityStates.getOrDefault(entityId, getString(R.string.ha_unavailable));
            }

            String textToShow = "";

            boolean isAvailable = state != null && !state.equalsIgnoreCase("unavailable") && !state.equalsIgnoreCase("unknown") && !state.equalsIgnoreCase("off");

            // Evaluate visibility rules
            boolean isVisible = true;
            int visibilityMode = config.getVisibilityMode();
            if (visibilityMode == 1) {
                if (!isAvailable) {
                    isVisible = false;
                }
            } else if (visibilityMode == 2) {
                String triggerState = config.getTriggerState();
                if (triggerState != null) {
                    String currentState = entityStates.getOrDefault(entityId, "");
                    if (!currentState.equalsIgnoreCase(triggerState.trim())) {
                        isVisible = false;
                    }
                }
            } else if (visibilityMode == 3) {
                String triggerEntity = config.getTriggerEntityId();
                String triggerState = config.getTriggerState();
                if (!TextUtils.isEmpty(triggerEntity) && triggerState != null) {
                    String currentState = entityStates.getOrDefault(triggerEntity, "");
                    if (!currentState.equalsIgnoreCase(triggerState.trim())) {
                        isVisible = false;
                    }
                }
            }

            if (!isVisible) {
                view.setVisibility(View.GONE);
                continue;
            }
            view.setVisibility(View.VISIBLE);

            String displayName = TextUtils.isEmpty(config.getDisplayName()) ? "" : config.getDisplayName();
            String unit = TextUtils.isEmpty(config.getUnit()) ? "" : config.getUnit();
            String displayMode = config.getDisplayMode();

            if (isAvailable) {
                try {
                    switch (displayMode) {
                        case "TimeOnly":
                        case "DateTime":
                        case "WeekdayTime":
                            OffsetDateTime odt = parseDateTime(state);
                            if (odt == null) {
                                throw new IllegalArgumentException("Could not parse date/time: " + state);
                            }
                            String pattern = "HH:mm"; // Default für TimeOnly
                            if ("DateTime".equals(displayMode)) pattern = "dd.MM. HH:mm";
                            if ("WeekdayTime".equals(displayMode)) pattern = "E, HH:mm";
                            textToShow = odt.format(DateTimeFormatter.ofPattern(pattern, Locale.GERMANY));
                            break;

                        case "Countdown":
                        case "CountdownHms":
                            OffsetDateTime countdownOdt = parseDateTime(state);
                            if (countdownOdt == null) {
                                throw new IllegalArgumentException("Could not parse countdown: " + state);
                            }
                            long secondsRemaining = ChronoUnit.SECONDS.between(Instant.now(), countdownOdt.toInstant());
                            if (secondsRemaining <= 0) {
                                textToShow = "00:00";
                            } else if ("CountdownHms".equals(displayMode)) {
                                long hours = secondsRemaining / 3600;
                                long minutes = (secondsRemaining % 3600) / 60;
                                long seconds = secondsRemaining % 60;
                                textToShow = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
                            } else {
                                long minutes = secondsRemaining / 60;
                                long seconds = secondsRemaining % 60;
                                textToShow = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
                            }
                            break;

                        case "Normal":
                        default:
                            textToShow = formatNumericState(state);
                            break;
                    }
                } catch (Exception e) {
                    textToShow = "Normal".equals(displayMode) ? formatNumericState(state) : "Invalid Date";
                }
            } else {
                textToShow = getString(R.string.ha_unavailable);
            }

            // Evaluate state rules (styling and alternative text)
            int finalColor = config.getColor();
            if (config.getRules() != null) {
                for (HARule rule : config.getRules()) {
                    if (rule.getCondition() != null && state.equalsIgnoreCase(rule.getCondition().trim())) {
                        if (!TextUtils.isEmpty(rule.getDisplayText())) {
                            textToShow = rule.getDisplayText();
                        }
                        if (rule.getColor() != null) {
                            finalColor = rule.getColor();
                        }
                        break;
                    }
                }
            }

            tv.setTextColor(finalColor);
            tv.setText((displayName + textToShow + unit).trim());
        }
    }

    private OffsetDateTime parseDateTime(String state) {
        if (state == null) return null;
        try {
            return OffsetDateTime.parse(state, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        } catch (Exception ignored) {}
        try {
            LocalDateTime ldt = LocalDateTime.parse(state, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            return ldt.atZone(ZoneId.systemDefault()).toOffsetDateTime();
        } catch (Exception ignored) {}
        try {
            ZonedDateTime zdt = ZonedDateTime.parse(state);
            return zdt.toOffsetDateTime();
        } catch (Exception ignored) {}
        try {
            return Instant.parse(state).atZone(ZoneId.systemDefault()).toOffsetDateTime();
        } catch (Exception ignored) {}
        try {
            LocalTime lt = LocalTime.parse(state);
            return lt.atDate(LocalDate.now()).atZone(ZoneId.systemDefault()).toOffsetDateTime();
        } catch (Exception ignored) {}
        return null;
    }

    private void applySettingsToView(View view, HAOverlay config) {
        TextView textView = view.findViewById(R.id.overlay_text_view);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, config.getSize());
        textView.setTextColor(config.getColor());
        textView.setShadowLayer(5.0f, 0, 0, config.getShadowColor());
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        view.setAlpha(config.getAlpha());
    }
    private WindowManager.LayoutParams getLayoutParams(HAOverlay config) {
        int layoutFlag = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, layoutFlag, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, PixelFormat.TRANSLUCENT);
        params.gravity = config.getGravity();
        params.x = config.getOffsetX();
        params.y = config.getOffsetY();
        return params;
    }
    private void broadcastConnectionStatus(String status) {
        prefs.edit().putString("ha_last_connection_status", status).apply();
        Intent intent = new Intent(SettingsManager.ACTION_HA_CONNECTION_STATUS_UPDATE);
        intent.putExtra(SettingsManager.EXTRA_HA_CONNECTION_STATUS, status);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        updateNotification(status);
    }
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "Home Assistant Service Channel", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(serviceChannel);
        }
    }
    private Notification buildNotification(String text) {
        return new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID).setContentTitle("Home Assistant Service").setContentText(text).setSmallIcon(R.mipmap.ic_launcher).setPriority(NotificationCompat.PRIORITY_LOW).build();
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
        disconnect();
        handler.removeCallbacksAndMessages(null);
        removeOverlays();
    }
    private void togglePositioningMode(int slotIndex) {
        View overlayView = (overlayViews.size() > slotIndex) ? overlayViews.get(slotIndex) : null;
        if (overlayView == null) return;
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
        Type listType = new TypeToken<ArrayList<HAOverlay>>() {}.getType();
        overlayConfigs = gson.fromJson(json, listType);
    }
    private boolean isAnyOverlayConfigured() {
        if (overlayConfigs == null || overlayConfigs.isEmpty()) return false;
        for (HAOverlay config : overlayConfigs) {
            if (config.isEnabled() && !TextUtils.isEmpty(config.getEntityId())) return true;
        }
        return false;
    }

    private String formatNumericState(String state) {
        if (state == null) return "";
        try {
            Double.parseDouble(state);
            java.text.DecimalFormatSymbols symbols = new java.text.DecimalFormatSymbols(Locale.getDefault());
            char localSeparator = symbols.getDecimalSeparator();
            if (localSeparator != '.') {
                return state.replace('.', localSeparator);
            }
        } catch (NumberFormatException ignored) {}
        return state;
    }
}