package app.olus.cornerlays;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

/* loaded from: classes5.dex */
public class MqttClientManager {
    private static final String TAG = "MqttClientManager";
    private static MqttClientManager instance;
    private final Context context;
    private MqttMessageListener messageListener;
    private MqttClient mqttClient;

    /* loaded from: classes5.dex */
    public interface MqttMessageListener {
        void onMessageReceived(String str, String str2);

        void onMqttConnected();
    }

    private MqttClientManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public static synchronized MqttClientManager getInstance(Context context) {
        MqttClientManager mqttClientManager;
        synchronized (MqttClientManager.class) {
            if (instance == null) {
                instance = new MqttClientManager(context);
            }
            mqttClientManager = instance;
        }
        return mqttClientManager;
    }

    public void setListener(MqttMessageListener listener) {
        this.messageListener = listener;
    }

    public void connect(String brokerUrl, String clientId, String username, String password) {
        try {
            if (this.mqttClient != null && this.mqttClient.isConnected()) {
                Log.d(TAG, "Already connected to MQTT broker.");
                return;
            }
            this.mqttClient = new MqttClient(brokerUrl, clientId, new MemoryPersistence());
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setAutomaticReconnect(true);
            if (username != null && !username.isEmpty()) {
                options.setUserName(username);
            }
            if (password != null && !password.isEmpty()) {
                options.setPassword(password.toCharArray());
            }
            this.mqttClient.setCallback(new MqttCallback() { // from class: app.olus.cornerlays.MqttClientManager.1
                @Override // org.eclipse.paho.client.mqttv3.MqttCallback
                public void connectionLost(Throwable cause) {
                    Log.w(MqttClientManager.TAG, "MQTT Connection lost.", cause);
                    MqttClientManager.this.broadcastStatus(MqttClientManager.this.context.getString(R.string.ha_status_disconnected));
                }

                @Override // org.eclipse.paho.client.mqttv3.MqttCallback
                public void messageArrived(String topic, MqttMessage message) {
                    String payload = new String(message.getPayload());
                    Log.d(MqttClientManager.TAG, "Message arrived: Topic=" + topic + ", Payload=" + payload);
                    if (MqttClientManager.this.messageListener != null) {
                        MqttClientManager.this.messageListener.onMessageReceived(topic, payload);
                    }
                }

                @Override // org.eclipse.paho.client.mqttv3.MqttCallback
                public void deliveryComplete(IMqttDeliveryToken token) {
                }
            });
            Log.d(TAG, "Connecting to MQTT broker: " + brokerUrl);
            broadcastStatus(this.context.getString(R.string.ha_status_connecting));
            this.mqttClient.connect(options);
            Log.d(TAG, "Mqtt Client Connected");
            broadcastStatus(this.context.getString(R.string.ha_status_connected));
            if (this.messageListener != null) {
                this.messageListener.onMqttConnected();
            }
        } catch (MqttException e) {
            Log.e(TAG, "Error connecting to MQTT Broker", e);
            broadcastStatus(this.context.getString(R.string.ha_status_error));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void broadcastStatus(String status) {
        Intent intent = new Intent(SettingsManager.ACTION_MQTT_CONNECTION_STATUS_UPDATE);
        intent.putExtra(SettingsManager.EXTRA_MQTT_CONNECTION_STATUS, status);
        LocalBroadcastManager.getInstance(this.context).sendBroadcast(intent);
    }

    public void subscribe(String topic) {
        if (this.mqttClient != null && this.mqttClient.isConnected()) {
            try {
                this.mqttClient.subscribe(topic, 0);
                Log.d(TAG, "Subscribed to " + topic);
                return;
            } catch (MqttException e) {
                Log.e(TAG, "Failed to subscribe to " + topic, e);
                return;
            }
        }
        Log.w(TAG, "Cannot subscribe. MQTT client is not connected.");
    }

    public void publish(String topic, String payload) {
        publish(topic, payload, false);
    }

    public void publish(String topic, String payload, boolean retained) {
        if (this.mqttClient != null && this.mqttClient.isConnected()) {
            try {
                MqttMessage message = new MqttMessage(payload.getBytes());
                message.setQos(1);
                message.setRetained(retained);
                this.mqttClient.publish(topic, message);
                Log.d(TAG, "Published to " + topic + " (retained: " + retained + "): " + payload);
                return;
            } catch (MqttException e) {
                Log.e(TAG, "Failed to publish to " + topic, e);
                return;
            }
        }
        Log.w(TAG, "Cannot publish. MQTT client is not connected.");
    }

    public void disconnect() {
        if (this.mqttClient != null && this.mqttClient.isConnected()) {
            try {
                this.mqttClient.disconnect();
                Log.d(TAG, "Disconnected from MQTT broker.");
                broadcastStatus(this.context.getString(R.string.ha_status_disconnected));
            } catch (MqttException e) {
                Log.e(TAG, "Error disconnecting from MQTT broker", e);
            }
        }
    }

    public boolean isConnected() {
        return this.mqttClient != null && this.mqttClient.isConnected();
    }
}
