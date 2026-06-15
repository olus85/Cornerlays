package app.olus.cornerlays;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import app.olus.cornerlays.MqttClientManager;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/* loaded from: classes5.dex */
public class DimmerService extends AccessibilityService implements MqttClientManager.MqttMessageListener {
    public static final String ACTION_SET_DIM_LEVEL = "app.olus.cornerlays.ACTION_SET_DIM_LEVEL";
    public static final String EXTRA_OPACITY = "opacity";
    private static final String[] LIGHTING_KEYWORDS = {"beleuchtung", "lampe", "licht", "light", "brightness", "helligkeit", "dimmer"};
    private static final String TOPIC_ACTION = "tv/command/action";
    private static final String TOPIC_APP = "tv/status/app";
    private static final String TOPIC_BUTTON_EVENT = "tv/event/button";
    private static final String TOPIC_CONTEXT_ACTION = "tv/event/context_action";
    private static final String TOPIC_DIM = "tv/command/dim";
    private static final String TOPIC_DIM_STATE = "tv/status/dim";
    private static final String TOPIC_FOCUS = "tv/status/focus";
    private static final String TOPIC_HIJACK_CONFIG = "tv/config/hijacked_keys";
    private static final String TOPIC_KEY = "tv/status/key";
    private static final String TOPIC_MEDIA = "tv/status/media";
    private FrameLayout dimView;
    private Handler handler;
    private WindowManager.LayoutParams layoutParams;
    private Runnable mediaStateChecker;
    private MqttClientManager mqttManager;
    private WindowManager windowManager;
    private int currentOpacity = 0;
    private final Set<Integer> hijackedKeycodes = Collections.synchronizedSet(new HashSet());
    private volatile String lastFocusText = "";
    private String lastFocusApp = "";
    private String lastFocusClassName = "";
    private boolean lastMediaPlaying = false;
    private String lastMediaTitle = "";
    private boolean mediaSessionAccessDenied = false;
    private final ExecutorService mqttExecutor = Executors.newSingleThreadExecutor();
    private final BroadcastReceiver configChangeReceiver = new BroadcastReceiver() { // from class: app.olus.cornerlays.DimmerService.1
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if ("app.olus.cornerlays.action.MQTT_CREDENTIALS_CHANGED".equals(intent.getAction())) {
                DimmerService.this.reconnectMqtt();
            }
        }
    };
    private BroadcastReceiver dimReceiver = new BroadcastReceiver() { // from class: app.olus.cornerlays.DimmerService.2
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if (DimmerService.ACTION_SET_DIM_LEVEL.equals(intent.getAction())) {
                int opacity = intent.getIntExtra(DimmerService.EXTRA_OPACITY, 0);
                DimmerService.this.setDimLevel(opacity);
            }
        }
    };

    @Override // android.accessibilityservice.AccessibilityService
    public void onServiceConnected() {
        super.onServiceConnected();
        this.windowManager = (WindowManager) getSystemService("window");
        this.dimView = new FrameLayout(this);
        this.dimView.setBackgroundColor(Color.argb(0, 0, 0, 0));
        this.dimView.setVisibility(8);
        this.layoutParams = new WindowManager.LayoutParams(-1, -1, 2038, 794, -3);
        this.layoutParams.dimAmount = 0.0f;
        this.windowManager.addView(this.dimView, this.layoutParams);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(this.dimReceiver, new IntentFilter(ACTION_SET_DIM_LEVEL), 2);
        } else {
            registerReceiver(this.dimReceiver, new IntentFilter(ACTION_SET_DIM_LEVEL));
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(this.configChangeReceiver, new IntentFilter("app.olus.cornerlays.action.MQTT_CREDENTIALS_CHANGED"));
        this.handler = new Handler(Looper.getMainLooper());
        this.mediaStateChecker = new Runnable() { // from class: app.olus.cornerlays.DimmerService.3
            @Override // java.lang.Runnable
            public void run() {
                DimmerService.this.checkMediaState();
                DimmerService.this.handler.postDelayed(this, 3000L);
            }
        };
        this.handler.post(this.mediaStateChecker);
        setupMqtt();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Removed duplicated region for block: B:10:0x025a  */
    /* JADX WARN: Removed duplicated region for block: B:13:0x026b A[ADDED_TO_REGION] */
    /* JADX WARN: Removed duplicated region for block: B:17:0x0275  */
    /* JADX WARN: Removed duplicated region for block: B:53:0x025c  */
    /* JADX WARN: Removed duplicated region for block: B:71:0x0080 A[Catch: Exception -> 0x0244, TryCatch #7 {Exception -> 0x0244, blocks: (B:71:0x0080, B:73:0x0086, B:74:0x008b, B:76:0x0091, B:82:0x00a3, B:83:0x00b2, B:85:0x00e8, B:87:0x00f0, B:88:0x00fb, B:90:0x0105, B:92:0x0112, B:93:0x011d, B:96:0x012f, B:98:0x0135, B:100:0x013d, B:102:0x0143, B:104:0x014b, B:106:0x0151, B:110:0x016f, B:113:0x017a, B:116:0x0185, B:117:0x0189, B:118:0x01f5, B:120:0x01fb, B:149:0x01dd, B:153:0x023e, B:161:0x0074, B:156:0x0066), top: B:155:0x0066, inners: #0 }] */
    public void checkMediaState() {
        if (this.mediaSessionAccessDenied) {
            return;
        }
        try {
            android.media.session.MediaSessionManager mm = (android.media.session.MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
            if (mm == null) return;
            
            java.util.List<android.media.session.MediaController> controllers = null;
            try {
                controllers = mm.getActiveSessions(new android.content.ComponentName(this, MediaNotificationListener.class));
            } catch (SecurityException se) {
                Log.w("DimmerService", "Notification Listener permission not granted for media sessions.");
                this.mediaSessionAccessDenied = true;
                return;
            }

            String state = "idle";
            String title = "";
            String artist = "";
            long duration = 0;
            long position = 0;
            String appPackage = "";

            if (controllers != null && !controllers.isEmpty()) {
                android.media.session.MediaController controller = controllers.get(0);
                appPackage = controller.getPackageName();
                
                android.media.session.PlaybackState playbackState = controller.getPlaybackState();
                if (playbackState != null) {
                    int s = playbackState.getState();
                    if (s == android.media.session.PlaybackState.STATE_PLAYING) {
                        state = "playing";
                    } else if (s == android.media.session.PlaybackState.STATE_PAUSED) {
                        state = "paused";
                    } else if (s == android.media.session.PlaybackState.STATE_STOPPED) {
                        state = "stopped";
                    } else if (s == android.media.session.PlaybackState.STATE_BUFFERING) {
                        state = "buffering";
                    }
                    position = playbackState.getPosition() / 1000; // to seconds
                }

                android.media.MediaMetadata metadata = controller.getMetadata();
                if (metadata != null) {
                    title = metadata.getString(android.media.MediaMetadata.METADATA_KEY_TITLE);
                    artist = metadata.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST);
                    duration = metadata.getLong(android.media.MediaMetadata.METADATA_KEY_DURATION) / 1000; // to seconds
                }
                
                if (title == null) title = "";
                if (artist == null) artist = "";
            }

            boolean isPlaying = "playing".equals(state);
            
            if (isPlaying || !state.equals(this.lastMediaPlaying ? "playing" : "idle") || !title.equals(this.lastMediaTitle)) {
                this.lastMediaPlaying = isPlaying;
                this.lastMediaTitle = title;

                if (this.mqttManager != null && this.mqttManager.isConnected()) {
                    try {
                        JSONObject json = new JSONObject();
                        json.put("state", state);
                        json.put("title", title);
                        json.put("artist", artist);
                        json.put("duration", duration);
                        json.put("position", position);
                        json.put("app", appPackage);
                        
                        publishInBackground(TOPIC_MEDIA, json.toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            Log.e("DimmerService", "Error in checkMediaState", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void reconnectMqtt() {
        if (this.mqttManager != null) {
            this.mqttManager.disconnect();
        }
        setupMqtt();
    }

    private void setupMqtt() {
        this.mqttManager = MqttClientManager.getInstance(this);
        this.mqttManager.setListener(this);
        SharedPreferences prefs = getSharedPreferences(SettingsManager.PREFS_NAME, 0);
        String ip = prefs.getString(SettingsManager.KEY_MQTT_IP, "").trim();
        String port = prefs.getString(SettingsManager.KEY_MQTT_PORT, "1883").trim();
        final String user = prefs.getString(SettingsManager.KEY_MQTT_USER, "").trim();
        final String pass = prefs.getString(SettingsManager.KEY_MQTT_PASS, "").trim();
        if (ip.isEmpty()) {
            return;
        }
        final String brokerUrl = "tcp://" + ip + ":" + port;
        final String clientId = "android_tv_dimmer_" + System.currentTimeMillis();
        new Thread(new Runnable() { // from class: app.olus.cornerlays.DimmerService$$ExternalSyntheticLambda7
            @Override // java.lang.Runnable
            public final void run() {
                DimmerService.this.lambda$setupMqtt$0(brokerUrl, clientId, user, pass);
            }
        }).start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupMqtt$0(String brokerUrl, String clientId, String user, String pass) {
        this.mqttManager.connect(brokerUrl, clientId, user, pass);
        try {
            Thread.sleep(2000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        onMqttConnected();
    }

    private void handleMqttAction(String action) {
        AudioManager audioManager = (AudioManager) getSystemService("audio");
        if ("home".equalsIgnoreCase(action)) {
            performGlobalAction(2);
            return;
        }
        if ("back".equalsIgnoreCase(action)) {
            performGlobalAction(1);
            return;
        }
        if ("power".equalsIgnoreCase(action)) {
            performGlobalAction(6);
            return;
        }
        if ("play_pause".equalsIgnoreCase(action)) {
            audioManager.dispatchMediaKeyEvent(new KeyEvent(0, 85));
            audioManager.dispatchMediaKeyEvent(new KeyEvent(1, 85));
            return;
        }
        if ("stop".equalsIgnoreCase(action)) {
            audioManager.dispatchMediaKeyEvent(new KeyEvent(0, 86));
            audioManager.dispatchMediaKeyEvent(new KeyEvent(1, 86));
            return;
        }
        if ("next".equalsIgnoreCase(action)) {
            audioManager.dispatchMediaKeyEvent(new KeyEvent(0, 87));
            audioManager.dispatchMediaKeyEvent(new KeyEvent(1, 87));
            return;
        }
        if ("prev".equalsIgnoreCase(action)) {
            audioManager.dispatchMediaKeyEvent(new KeyEvent(0, 88));
            audioManager.dispatchMediaKeyEvent(new KeyEvent(1, 88));
            return;
        }
        if ("rewind".equalsIgnoreCase(action)) {
            audioManager.dispatchMediaKeyEvent(new KeyEvent(0, 89));
            audioManager.dispatchMediaKeyEvent(new KeyEvent(1, 89));
            return;
        }
        if ("fast_forward".equalsIgnoreCase(action)) {
            audioManager.dispatchMediaKeyEvent(new KeyEvent(0, 90));
            audioManager.dispatchMediaKeyEvent(new KeyEvent(1, 90));
            return;
        }
        if ("vol_up".equalsIgnoreCase(action)) {
            audioManager.adjustStreamVolume(3, 1, 1);
            return;
        }
        if ("vol_down".equalsIgnoreCase(action)) {
            audioManager.adjustStreamVolume(3, -1, 1);
            return;
        }
        if ("mute".equalsIgnoreCase(action)) {
            audioManager.adjustStreamVolume(3, AudioManager.ADJUST_TOGGLE_MUTE, 1);
            return;
        }
        if ("click".equalsIgnoreCase(action) || "ok".equalsIgnoreCase(action) || "center".equalsIgnoreCase(action)) {
            performClickOnFocused();
            return;
        }
        if ("dpad_up".equalsIgnoreCase(action)) {
            performDpadNavigation(33);
            return;
        }
        if ("dpad_down".equalsIgnoreCase(action)) {
            performDpadNavigation(130);
        } else if ("dpad_left".equalsIgnoreCase(action)) {
            performDpadNavigation(17);
        } else if ("dpad_right".equalsIgnoreCase(action)) {
            performDpadNavigation(66);
        }
    }

    private void performDpadNavigation(int direction) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            return;
        }
        try {
            AccessibilityNodeInfo focused = root.findFocus(1);
            if (focused == null) {
                focused = root.findFocus(2);
            }
            if (focused != null) {
                try {
                    AccessibilityNodeInfo nextNode = focused.focusSearch(direction);
                    if (nextNode != null) {
                        try {
                            nextNode.performAction(1);
                            nextNode.recycle();
                        } catch (Throwable th) {
                            nextNode.recycle();
                            throw th;
                        }
                    }
                    focused.recycle();
                } catch (Throwable th2) {
                    focused.recycle();
                    throw th2;
                }
            }
        } finally {
            root.recycle();
        }
    }

    private void performClickOnFocused() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            return;
        }
        try {
            AccessibilityNodeInfo focused = root.findFocus(1);
            if (focused != null) {
                try {
                    focused.performAction(16);
                    focused.recycle();
                } catch (Throwable th) {
                    focused.recycle();
                    throw th;
                }
            }
        } finally {
            root.recycle();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setDimLevel(int opacity) {
        int opacity2 = Math.max(0, Math.min(255, opacity));
        this.currentOpacity = opacity2;
        if (opacity2 > 0) {
            this.dimView.setBackgroundColor(Color.argb(opacity2, 0, 0, 0));
            this.layoutParams.dimAmount = opacity2 / 255.0f;
            this.windowManager.updateViewLayout(this.dimView, this.layoutParams);
            this.dimView.setVisibility(0);
            return;
        }
        this.dimView.setVisibility(8);
    }

    @Override // app.olus.cornerlays.MqttClientManager.MqttMessageListener
    public void onMessageReceived(final String topic, final String message) {
        if (TOPIC_HIJACK_CONFIG.equals(topic)) {
            updateHijackedKeycodes(message.trim());
        } else {
            this.handler.post(new Runnable() { // from class: app.olus.cornerlays.DimmerService$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    DimmerService.this.lambda$onMessageReceived$1(topic, message);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onMessageReceived$1(String topic, String message) {
        if (TOPIC_DIM.equals(topic)) {
            String val = message.trim();
            if ("OFF".equalsIgnoreCase(val)) {
                setDimLevel(0);
            } else if ("ON".equalsIgnoreCase(val)) {
                if (this.currentOpacity == 0) {
                    setDimLevel(128);
                }
            } else {
                try {
                    int opacity = Integer.parseInt(val);
                    setDimLevel(opacity);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
            publishInBackground(TOPIC_DIM_STATE, String.valueOf(this.currentOpacity), true);
            return;
        }
        if (TOPIC_ACTION.equals(topic)) {
            handleMqttAction(message.trim());
        }
    }

    private void updateHijackedKeycodes(String payload) {
        this.hijackedKeycodes.clear();
        if (payload.isEmpty()) {
            Log.d("DimmerService", "Hijacked keycodes cleared");
            return;
        }
        for (String part : payload.split(",")) {
            try {
                int code = Integer.parseInt(part.trim());
                this.hijackedKeycodes.add(Integer.valueOf(code));
            } catch (NumberFormatException e) {
                Log.w("DimmerService", "Invalid keycode in hijack config: " + part);
            }
        }
        Log.d("DimmerService", "Hijacked keycodes updated: " + this.hijackedKeycodes);
    }

    private void publishInBackground(final String topic, final String payload, final boolean retained) {
        this.mqttExecutor.execute(new Runnable() { // from class: app.olus.cornerlays.DimmerService$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                DimmerService.this.lambda$publishInBackground$2(topic, payload, retained);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$publishInBackground$2(String topic, String payload, boolean retained) {
        if (this.mqttManager != null && this.mqttManager.isConnected()) {
            this.mqttManager.publish(topic, payload, retained);
        }
    }

    private void publishInBackground(final String topic, final String payload) {
        this.mqttExecutor.execute(new Runnable() { // from class: app.olus.cornerlays.DimmerService$$ExternalSyntheticLambda6
            @Override // java.lang.Runnable
            public final void run() {
                DimmerService.this.lambda$publishInBackground$3(topic, payload);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$publishInBackground$3(String topic, String payload) {
        if (this.mqttManager != null && this.mqttManager.isConnected()) {
            this.mqttManager.publish(topic, payload);
        }
    }

    private boolean isLightingContext(String focusText) {
        if (focusText == null || focusText.isEmpty()) {
            return false;
        }
        String lower = focusText.toLowerCase(Locale.ROOT);
        for (String keyword : LIGHTING_KEYWORDS) {
            if (lower.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    @Override // android.accessibilityservice.AccessibilityService
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == 32) {
            String packageName = event.getPackageName() != null ? event.getPackageName().toString() : "";
            if (!packageName.isEmpty() && !packageName.equals(this.lastFocusApp)) {
                this.lastFocusApp = packageName;
                this.lastFocusText = "";
                this.lastFocusClassName = "";
            }
            if (!packageName.isEmpty() && this.mqttManager != null && this.mqttManager.isConnected()) {
                try {
                    JSONObject json = new JSONObject();
                    json.put("app", packageName);
                    publishInBackground(TOPIC_APP, json.toString(), true);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        if (event.getEventType() == 8) {
            publishFocusUpdate(event, "focused");
        }
        if (event.getEventType() == 2048) {
            String text = extractEventText(event);
            if (!text.isEmpty()) {
                publishFocusUpdate(event, "content_changed");
            }
        }
    }

    private String extractEventText(AccessibilityEvent event) {
        String text = "";
        if (event.getText() != null && !event.getText().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (CharSequence cs : event.getText()) {
                if (cs != null && cs.length() > 0) {
                    if (sb.length() > 0) {
                        sb.append(" | ");
                    }
                    sb.append(cs);
                }
            }
            text = sb.toString();
        }
        if (text.isEmpty() && event.getContentDescription() != null) {
            return event.getContentDescription().toString();
        }
        return text;
    }

    private void publishFocusUpdate(AccessibilityEvent event, String eventType) {
        String packageName = event.getPackageName() != null ? event.getPackageName().toString() : "";
        String text = extractEventText(event);
        String className = event.getClassName() != null ? event.getClassName().toString() : "";
        if (!text.equals(this.lastFocusText) || !className.equals(this.lastFocusClassName)) {
            this.lastFocusText = text;
            this.lastFocusClassName = className;
            if (this.mqttManager != null && this.mqttManager.isConnected()) {
                try {
                    JSONObject json = new JSONObject();
                    json.put("app", packageName);
                    json.put("focus_text", text);
                    json.put("class_name", className);
                    json.put("event_type", eventType);
                    publishInBackground(TOPIC_FOCUS, json.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override // android.accessibilityservice.AccessibilityService
    public void onInterrupt() {
    }

    @Override // android.accessibilityservice.AccessibilityService
    protected boolean onKeyEvent(KeyEvent event) {
        final int keyCode = event.getKeyCode();
        if (event.getAction() == 0) {
            this.mqttExecutor.execute(new Runnable() { // from class: app.olus.cornerlays.DimmerService$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    DimmerService.this.lambda$onKeyEvent$4(keyCode);
                }
            });
        }
        if (this.hijackedKeycodes.contains(Integer.valueOf(keyCode))) {
            if (event.getAction() == 0) {
                this.mqttExecutor.execute(new Runnable() { // from class: app.olus.cornerlays.DimmerService$$ExternalSyntheticLambda3
                    @Override // java.lang.Runnable
                    public final void run() {
                        DimmerService.this.lambda$onKeyEvent$5(keyCode);
                    }
                });
            }
            return true;
        }
        if ((keyCode == 24 || keyCode == 25) && isLightingContext(this.lastFocusText)) {
            if (event.getAction() == 0) {
                final String volumeAction = keyCode == 24 ? "VOLUME_UP" : "VOLUME_DOWN";
                final String focusText = this.lastFocusText;
                this.mqttExecutor.execute(new Runnable() { // from class: app.olus.cornerlays.DimmerService$$ExternalSyntheticLambda4
                    @Override // java.lang.Runnable
                    public final void run() {
                        DimmerService.this.lambda$onKeyEvent$6(volumeAction, focusText);
                    }
                });
            }
            return true;
        }
        if (this.currentOpacity > 0) {
            Log.d("DimmerService", "Auto-dismissing dimmer due to KeyEvent: " + event.toString());
            this.handler.post(new Runnable() { // from class: app.olus.cornerlays.DimmerService$$ExternalSyntheticLambda5
                @Override // java.lang.Runnable
                public final void run() {
                    DimmerService.this.lambda$onKeyEvent$7();
                }
            });
            return false;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onKeyEvent$4(int keyCode) {
        if (this.mqttManager != null && this.mqttManager.isConnected()) {
            try {
                JSONObject json = new JSONObject();
                json.put("key_code", keyCode);
                json.put("key_name", KeyEvent.keyCodeToString(keyCode));
                this.mqttManager.publish(TOPIC_KEY, json.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onKeyEvent$5(int keyCode) {
        try {
            JSONObject json = new JSONObject();
            json.put("code", keyCode);
            json.put("action", "DOWN");
            json.put("hijacked", true);
            json.put("key_name", KeyEvent.keyCodeToString(keyCode));
            if (this.mqttManager != null && this.mqttManager.isConnected()) {
                this.mqttManager.publish(TOPIC_BUTTON_EVENT, json.toString());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onKeyEvent$6(String volumeAction, String focusText) {
        try {
            JSONObject json = new JSONObject();
            json.put("action", volumeAction);
            json.put("context", "lighting_control");
            json.put("focus_text", focusText);
            if (this.mqttManager != null && this.mqttManager.isConnected()) {
                this.mqttManager.publish(TOPIC_CONTEXT_ACTION, json.toString());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onKeyEvent$7() {
        setDimLevel(0);
    }

    @Override // app.olus.cornerlays.MqttClientManager.MqttMessageListener
    public void onMqttConnected() {
        Log.d("DimmerService", "onMqttConnected! Publishing Auto-Discovery...");
        if (this.mqttManager != null && this.mqttManager.isConnected()) {
            this.mqttManager.subscribe(TOPIC_DIM);
            this.mqttManager.subscribe(TOPIC_ACTION);
            this.mqttManager.subscribe(TOPIC_HIJACK_CONFIG);
            publishAutoDiscoveryConfig();
        }
    }

    private void publishAutoDiscoveryConfig() {
        if (this.mqttManager == null || !this.mqttManager.isConnected()) {
            return;
        }
        try {
            JSONObject device = new JSONObject();
            JSONArray identifiers = new JSONArray();
            identifiers.put("android_tv_cornerlays_1");
            device.put("identifiers", identifiers);
            device.put("name", "Android TV Cornerlays");
            device.put("model", "Android TV");
            device.put("manufacturer", "Olus");
            JSONObject lightConfig = new JSONObject();
            lightConfig.put("name", "Android TV Dimmer");
            lightConfig.put("unique_id", "android_tv_dimmer_01");
            lightConfig.put("command_topic", TOPIC_DIM);
            lightConfig.put("brightness_command_topic", TOPIC_DIM);
            lightConfig.put("state_topic", TOPIC_DIM_STATE);
            lightConfig.put("brightness_state_topic", TOPIC_DIM_STATE);
            lightConfig.put("brightness_scale", 255);
            lightConfig.put("optimistic", false);
            lightConfig.put("icon", "mdi:television-shimmer");
            lightConfig.put("device", device);
            this.mqttManager.publish("homeassistant/light/android_tv/dimmer/config", lightConfig.toString(), true);
            publishSensorDiscovery(device, "Android TV Focus", "android_tv_focus_01", TOPIC_FOCUS, "{{ value_json.focus_text }}", "mdi:target", "focus", null);
            publishSensorDiscovery(device, "Android TV Focus Element", "android_tv_focus_class_01", TOPIC_FOCUS, "{{ value_json.class_name }}", "mdi:code-tags", "focus_class", null);
            JSONObject keyExtra = new JSONObject();
            keyExtra.put("json_attributes_topic", TOPIC_KEY);
            publishSensorDiscovery(device, "Android TV Letzte Taste", "android_tv_key_name_01", TOPIC_KEY, "{{ value_json.key_name }}", "mdi:remote-tv", "last_key_name", keyExtra);
            this.mqttManager.publish("homeassistant/sensor/android_tv/last_key/config", "", true);
            publishSensorDiscovery(device, "Android TV App", "android_tv_app_01", TOPIC_APP, "{{ value_json.app }}", "mdi:application", "app", null);
            publishSensorDiscovery(device, "Android TV Media State", "android_tv_media_01", TOPIC_MEDIA, "{{ value_json.state }}", "mdi:play-circle", "media", null);
            publishSensorDiscovery(device, "Android TV Media Titel", "android_tv_media_title_01", TOPIC_MEDIA, "{{ value_json.title }}", "mdi:filmstrip", "media_title", null);
            publishSensorDiscovery(device, "Android TV Media Kuenstler", "android_tv_media_artist_01", TOPIC_MEDIA, "{{ value_json.artist }}", "mdi:account-music", "media_artist", null);
            JSONObject durationExtra = new JSONObject();
            durationExtra.put("unit_of_measurement", "s");
            durationExtra.put("device_class", "duration");
            publishSensorDiscovery(device, "Android TV Media Dauer", "android_tv_media_duration_01", TOPIC_MEDIA, "{{ value_json.duration }}", "mdi:timer-outline", "media_duration", durationExtra);
            JSONObject positionExtra = new JSONObject();
            positionExtra.put("unit_of_measurement", "s");
            positionExtra.put("device_class", "duration");
            publishSensorDiscovery(device, "Android TV Media Position", "android_tv_media_position_01", TOPIC_MEDIA, "{{ value_json.position }}", "mdi:progress-clock", "media_position", positionExtra);
            publishSensorDiscovery(device, "Android TV Media App", "android_tv_media_app_01", TOPIC_MEDIA, "{{ value_json.app }}", "mdi:application-outline", "media_app", null);
            JSONObject btnEvtConfig = new JSONObject();
            btnEvtConfig.put("name", "Android TV Button Event");
            btnEvtConfig.put("unique_id", "android_tv_button_event_01");
            btnEvtConfig.put("state_topic", TOPIC_BUTTON_EVENT);
            btnEvtConfig.put("value_template", "{{ value_json.key_name }}");
            btnEvtConfig.put("json_attributes_topic", TOPIC_BUTTON_EVENT);
            btnEvtConfig.put("icon", "mdi:gesture-tap-button");
            btnEvtConfig.put("device", device);
            this.mqttManager.publish("homeassistant/sensor/android_tv/button_event/config", btnEvtConfig.toString(), true);
            JSONObject ctxConfig = new JSONObject();
            ctxConfig.put("name", "Android TV Kontext Aktion");
            ctxConfig.put("unique_id", "android_tv_context_action_01");
            ctxConfig.put("state_topic", TOPIC_CONTEXT_ACTION);
            ctxConfig.put("value_template", "{{ value_json.action }}");
            ctxConfig.put("json_attributes_topic", TOPIC_CONTEXT_ACTION);
            ctxConfig.put("icon", "mdi:lightbulb-auto");
            ctxConfig.put("device", device);
            this.mqttManager.publish("homeassistant/sensor/android_tv/context_action/config", ctxConfig.toString(), true);
            publishButtonDiscovery(device, "TV Home Taste", "android_tv_btn_home", "home", "mdi:home", "home");
            publishButtonDiscovery(device, "TV Zurueck Taste", "android_tv_btn_back", "back", "mdi:keyboard-return", "back");
            publishButtonDiscovery(device, "TV Power", "android_tv_btn_power", "power", "mdi:power", "power");
            publishButtonDiscovery(device, "TV Play/Pause", "android_tv_btn_play_pause", "play_pause", "mdi:play-pause", "play_pause");
            publishButtonDiscovery(device, "TV Stop", "android_tv_btn_stop", "stop", "mdi:stop", "stop_btn");
            publishButtonDiscovery(device, "TV Next", "android_tv_btn_next", "next", "mdi:skip-next", "next");
            publishButtonDiscovery(device, "TV Previous", "android_tv_btn_prev", "prev", "mdi:skip-previous", "prev");
            publishButtonDiscovery(device, "TV Rewind", "android_tv_btn_rew", "rewind", "mdi:rewind", "rewind");
            publishButtonDiscovery(device, "TV Fast Forward", "android_tv_btn_ff", "fast_forward", "mdi:fast-forward", "fast_forward");
            publishButtonDiscovery(device, "TV Volume Up", "android_tv_btn_volup", "vol_up", "mdi:volume-high", "vol_up");
            publishButtonDiscovery(device, "TV Volume Down", "android_tv_btn_voldown", "vol_down", "mdi:volume-medium", "vol_down");
            publishButtonDiscovery(device, "TV Mute", "android_tv_btn_mute", "mute", "mdi:volume-off", "mute");
            publishButtonDiscovery(device, "TV D-Pad Hoch", "android_tv_btn_dpad_up", "dpad_up", "mdi:arrow-up-bold", "dpad_up");
            publishButtonDiscovery(device, "TV D-Pad Runter", "android_tv_btn_dpad_down", "dpad_down", "mdi:arrow-down-bold", "dpad_down");
            publishButtonDiscovery(device, "TV D-Pad Links", "android_tv_btn_dpad_left", "dpad_left", "mdi:arrow-left-bold", "dpad_left");
            publishButtonDiscovery(device, "TV D-Pad Rechts", "android_tv_btn_dpad_right", "dpad_right", "mdi:arrow-right-bold", "dpad_right");
            publishButtonDiscovery(device, "TV OK / Enter", "android_tv_btn_ok", "ok", "mdi:checkbox-blank-circle", "ok");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void publishSensorDiscovery(JSONObject device, String name, String uniqueId, String stateTopic, String valueTemplate, String icon, String objectId, JSONObject extraFields) throws JSONException {
        JSONObject config = new JSONObject();
        config.put("name", name);
        config.put("unique_id", uniqueId);
        config.put("state_topic", stateTopic);
        config.put("value_template", valueTemplate);
        config.put("icon", icon);
        config.put("device", device);
        if (extraFields != null) {
            Iterator<String> keys = extraFields.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                config.put(key, extraFields.get(key));
            }
        }
        this.mqttManager.publish("homeassistant/sensor/android_tv/" + objectId + "/config", config.toString(), true);
    }

    private void publishButtonDiscovery(JSONObject device, String name, String uniqueId, String payloadPress, String icon, String objectId) throws JSONException {
        JSONObject config = new JSONObject();
        config.put("name", name);
        config.put("unique_id", uniqueId);
        config.put("command_topic", TOPIC_ACTION);
        config.put("payload_press", payloadPress);
        config.put("icon", icon);
        config.put("device", device);
        this.mqttManager.publish("homeassistant/button/android_tv/" + objectId + "/config", config.toString(), true);
    }

    @Override // android.app.Service
    public void onDestroy() {
        super.onDestroy();
        try {
            if (this.handler != null) {
                this.handler.removeCallbacks(this.mediaStateChecker);
            }
            this.mqttExecutor.shutdownNow();
            if (this.mqttManager != null) {
                this.mqttManager.disconnect();
            }
            LocalBroadcastManager.getInstance(this).unregisterReceiver(this.configChangeReceiver);
            unregisterReceiver(this.dimReceiver);
            if (this.windowManager != null && this.dimView != null) {
                this.windowManager.removeView(this.dimView);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
