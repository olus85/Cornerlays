package app.olus.cornerlays.settings;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.provider.Settings;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import app.olus.cornerlays.MqttClientManager;
import app.olus.cornerlays.R;
import app.olus.cornerlays.SettingsManager;
import app.olus.cornerlays.ha.HomeAssistantService;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class GeneralSettingsFragment extends BaseSettingsFragment {

    private TextInputEditText editHaUrl;
    private TextInputEditText editHaToken;
    private TextInputEditText editMqttIp;
    private TextInputEditText editMqttPort;
    private TextInputEditText editMqttUser;
    private TextInputEditText editMqttPass;

    private ImageView iconHaStatus;
    private TextView textHaStatus;
    private ImageView iconMqttStatus;
    private TextView textMqttStatus;

    private Button btnTestConnections;
    private TextView textTestResults;

    private Button btnExportSettings;
    private Button btnImportSettings;
    private TextView textBackupStatus;

    private final Gson gson = new Gson();

    private final ActivityResultLauncher<String[]> requestPermissionLauncher = registerForActivityResult(
        new ActivityResultContracts.RequestMultiplePermissions(),
        permissions -> {
            boolean readGranted = Boolean.TRUE.equals(permissions.get(android.Manifest.permission.READ_EXTERNAL_STORAGE));
            boolean writeGranted = Boolean.TRUE.equals(permissions.get(android.Manifest.permission.WRITE_EXTERNAL_STORAGE));
            if (readGranted && writeGranted) {
                textBackupStatus.setText("Berechtigungen erteilt!");
                textBackupStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.m3_primary));
            } else {
                textBackupStatus.setText("Berechtigungen verweigert. Nutze den File-Picker.");
                textBackupStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.m3_error));
            }
        }
    );


    private void checkAndRequestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean readGranted = ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED;
            boolean writeGranted = ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED;
            if (!readGranted || !writeGranted) {
                requestPermissionLauncher.launch(new String[]{
                        android.Manifest.permission.READ_EXTERNAL_STORAGE,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                });
            }
        }
    }

    private final BroadcastReceiver haConnectionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String status = intent.getStringExtra(SettingsManager.EXTRA_HA_CONNECTION_STATUS);
            updateHaStatusUI(status);
        }
    };

    private final BroadcastReceiver mqttConnectionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String status = intent.getStringExtra(SettingsManager.EXTRA_MQTT_CONNECTION_STATUS);
            updateMqttStatusUI(status);
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings_general, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        loadSettings();
        setupListeners();
        checkAndRequestStoragePermissions();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateHaStatusUI(this.sharedPreferences.getString("ha_last_connection_status", getString(R.string.ha_status_disconnected)));
        boolean isMqttConnected = MqttClientManager.getInstance(requireContext()).isConnected();
        if (isMqttConnected) {
            updateMqttStatusUI(getString(R.string.ha_status_connected));
        } else {
            updateMqttStatusUI(this.sharedPreferences.getString("mqtt_last_connection_status", getString(R.string.ha_status_disconnected)));
        }
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(this.haConnectionReceiver, new IntentFilter(SettingsManager.ACTION_HA_CONNECTION_STATUS_UPDATE));
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(this.mqttConnectionReceiver, new IntentFilter(SettingsManager.ACTION_MQTT_CONNECTION_STATUS_UPDATE));
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(this.haConnectionReceiver);
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(this.mqttConnectionReceiver);
    }

    private void initViews(View view) {
        editHaUrl = view.findViewById(R.id.edit_ha_url);
        editHaToken = view.findViewById(R.id.edit_ha_token);
        iconHaStatus = view.findViewById(R.id.icon_ha_connection_status);
        textHaStatus = view.findViewById(R.id.text_ha_connection_status);

        iconMqttStatus = view.findViewById(R.id.icon_mqtt_connection_status);
        textMqttStatus = view.findViewById(R.id.text_mqtt_connection_status);
        editMqttIp = view.findViewById(R.id.edit_mqtt_ip);
        editMqttPort = view.findViewById(R.id.edit_mqtt_port);
        editMqttUser = view.findViewById(R.id.edit_mqtt_user);
        editMqttPass = view.findViewById(R.id.edit_mqtt_pass);

        btnTestConnections = view.findViewById(R.id.btn_test_connections);
        textTestResults = view.findViewById(R.id.text_test_results);

        btnExportSettings = view.findViewById(R.id.btn_export_settings);
        btnImportSettings = view.findViewById(R.id.btn_import_settings);
        textBackupStatus = view.findViewById(R.id.text_backup_status);
    }

    private void loadSettings() {
        editHaUrl.setText(this.sharedPreferences.getString(SettingsManager.KEY_HA_URL, ""));
        editHaToken.setText(this.sharedPreferences.getString(SettingsManager.KEY_HA_TOKEN, ""));
        editMqttIp.setText(this.sharedPreferences.getString(SettingsManager.KEY_MQTT_IP, ""));
        editMqttPort.setText(this.sharedPreferences.getString(SettingsManager.KEY_MQTT_PORT, "1883"));
        editMqttUser.setText(this.sharedPreferences.getString(SettingsManager.KEY_MQTT_USER, ""));
        editMqttPass.setText(this.sharedPreferences.getString(SettingsManager.KEY_MQTT_PASS, ""));
    }

    private void setupListeners() {
        setupTextInputListeners(requireView().findViewById(R.id.container_ha_url), editHaUrl, SettingsManager.KEY_HA_URL, false);
        setupTextInputListeners(requireView().findViewById(R.id.container_ha_token), editHaToken, SettingsManager.KEY_HA_TOKEN, false);
        setupTextInputListeners(requireView().findViewById(R.id.container_mqtt_ip), editMqttIp, SettingsManager.KEY_MQTT_IP, true);
        setupTextInputListeners(requireView().findViewById(R.id.container_mqtt_port), editMqttPort, SettingsManager.KEY_MQTT_PORT, true);
        setupTextInputListeners(requireView().findViewById(R.id.container_mqtt_user), editMqttUser, SettingsManager.KEY_MQTT_USER, true);
        setupTextInputListeners(requireView().findViewById(R.id.container_mqtt_pass), editMqttPass, SettingsManager.KEY_MQTT_PASS, true);

        btnTestConnections.setOnClickListener(v -> testConnections());
        btnExportSettings.setOnClickListener(v -> exportBackup());
        btnImportSettings.setOnClickListener(v -> importBackup());
    }

    private void setupTextInputListeners(final View container, final TextInputEditText editText, final String key, final boolean isMqtt) {
        container.setOnClickListener(view -> {
            editText.setFocusableInTouchMode(true);
            editText.requestFocus();
            InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
            }
        });
        editText.setOnFocusChangeListener((view, hasFocus) -> {
            if (!hasFocus) {
                String newValue = editText.getText().toString().trim();
                saveValue(key, newValue, isMqtt);
                editText.setFocusable(false);
            }
        });
        editText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == 6 || actionId == 5) {
                String newValue = editText.getText().toString().trim();
                saveValue(key, newValue, isMqtt);
                container.requestFocus();
                InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
                editText.setFocusable(false);
                return true;
            }
            return false;
        });
    }

    private void saveValue(String key, String newValue, boolean isMqtt) {
        String oldValue = this.sharedPreferences.getString(key, "");
        if (!newValue.equals(oldValue)) {
            getEditor().putString(key, newValue).commit();
            if (isMqtt) {
                LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(new Intent("app.olus.cornerlays.action.MQTT_CREDENTIALS_CHANGED"));
            } else {
                saveHaCredentialsAndRestartService();
            }
        }
    }

    private void saveHaCredentialsAndRestartService() {
        boolean hasUrl = !TextUtils.isEmpty(editHaUrl.getText().toString().trim());
        if (getActivity() != null) {
            requireActivity().stopService(new Intent(getActivity(), HomeAssistantService.class));
            if (hasUrl) {
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (getActivity() != null) {
                        requireActivity().startService(new Intent(getActivity(), HomeAssistantService.class));
                    }
                }, 200L);
            }
        }
    }

    private void updateHaStatusUI(String status) {
        if (textHaStatus == null || iconHaStatus == null) return;
        if (status != null) {
            getEditor().putString("ha_last_connection_status", status).apply();
        } else {
            status = this.sharedPreferences.getString("ha_last_connection_status", getString(R.string.ha_status_disconnected));
        }
        textHaStatus.setText(status);
        String connectedString = getString(R.string.ha_status_connected);
        String connectingString = getString(R.string.ha_status_connecting);
        if (status.equals(connectedString)) {
            iconHaStatus.setImageResource(R.drawable.ic_ha_status_connected);
            iconHaStatus.setColorFilter(ContextCompat.getColor(requireContext(), R.color.m3_primary));
        } else if (status.equals(connectingString)) {
            iconHaStatus.setImageResource(R.drawable.ic_ha_status_connecting);
            iconHaStatus.setColorFilter(ContextCompat.getColor(requireContext(), R.color.m3_on_surface));
        } else {
            iconHaStatus.setImageResource(R.drawable.ic_ha_status_disconnected);
            iconHaStatus.setColorFilter(ContextCompat.getColor(requireContext(), R.color.m3_error));
        }
    }

    private void updateMqttStatusUI(String status) {
        if (textMqttStatus == null || iconMqttStatus == null) return;
        if (status != null) {
            getEditor().putString("mqtt_last_connection_status", status).apply();
        } else {
            status = this.sharedPreferences.getString("mqtt_last_connection_status", getString(R.string.ha_status_disconnected));
        }
        textMqttStatus.setText(status);
        String connectedString = getString(R.string.ha_status_connected);
        String connectingString = getString(R.string.ha_status_connecting);
        if (status.equals(connectedString)) {
            iconMqttStatus.setImageResource(R.drawable.ic_ha_status_connected);
            iconMqttStatus.setColorFilter(ContextCompat.getColor(requireContext(), R.color.m3_primary));
        } else if (status.equals(connectingString)) {
            iconMqttStatus.setImageResource(R.drawable.ic_ha_status_connecting);
            iconMqttStatus.setColorFilter(ContextCompat.getColor(requireContext(), R.color.m3_on_surface));
        } else {
            iconMqttStatus.setImageResource(R.drawable.ic_ha_status_disconnected);
            iconMqttStatus.setColorFilter(ContextCompat.getColor(requireContext(), R.color.m3_error));
        }
    }

    // --- Diagnostic Connection Test ---
    private void testConnections() {
        textTestResults.setText("Teste Verbindungen...");
        textTestResults.setTextColor(ContextCompat.getColor(requireContext(), R.color.html_silver));

        String haUrl = editHaUrl.getText().toString().trim();
        String haToken = editHaToken.getText().toString().trim();
        String mqttIp = editMqttIp.getText().toString().trim();
        String mqttPortStr = editMqttPort.getText().toString().trim();
        String mqttUser = editMqttUser.getText().toString().trim();
        String mqttPass = editMqttPass.getText().toString().trim();

        new Thread(() -> {
            final StringBuilder result = new StringBuilder();
            
            // 1. HA Test
            if (TextUtils.isEmpty(haUrl)) {
                result.append("HA: Keine URL konfiguriert\n");
            } else {
                try {
                    OkHttpClient client = new OkHttpClient();
                    Request request = new Request.Builder()
                        .url(haUrl + "/api/")
                        .header("Authorization", "Bearer " + haToken)
                        .build();
                    
                    try (Response response = client.newCall(request).execute()) {
                        if (response.isSuccessful()) {
                            result.append("HA: Verbindung OK (API läuft)\n");
                        } else {
                            result.append("HA: Fehler ").append(response.code()).append(" - ").append(response.message()).append("\n");
                        }
                    }
                } catch (Exception e) {
                    result.append("HA: Fehler: ").append(e.getMessage()).append("\n");
                }
            }

            // 2. MQTT Test
            if (TextUtils.isEmpty(mqttIp)) {
                result.append("MQTT: Keine IP konfiguriert");
            } else {
                try {
                    int port = Integer.parseInt(mqttPortStr);
                    String brokerUrl = "tcp://" + mqttIp + ":" + port;
                    MqttConnectOptions options = new MqttConnectOptions();
                    if (!TextUtils.isEmpty(mqttUser)) {
                        options.setUserName(mqttUser);
                        options.setPassword(mqttPass.toCharArray());
                    }
                    options.setConnectionTimeout(3);
                    options.setAutomaticReconnect(false);

                    MqttClient testClient = new MqttClient(brokerUrl, "test_diag_" + System.currentTimeMillis(), new MemoryPersistence());
                    testClient.connect(options);
                    testClient.disconnect();
                    testClient.close();
                    result.append("MQTT: Verbindung OK");
                } catch (Exception e) {
                    result.append("MQTT: Fehler: ").append(e.getMessage());
                }
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    String finalRes = result.toString().trim();
                    textTestResults.setText(finalRes);
                    if (finalRes.contains("Fehler")) {
                        textTestResults.setTextColor(ContextCompat.getColor(requireContext(), R.color.m3_error));
                    } else {
                        textTestResults.setTextColor(ContextCompat.getColor(requireContext(), R.color.m3_primary));
                    }
                });
            }
        }).start();
    }

    // --- Backup & Restore ---
    private File currentExportDir = Environment.getExternalStorageDirectory();

    private void exportBackup() {
        try {
            showDirectoryPickerDialog();
        } catch (Exception e) {
            textBackupStatus.setText("Export-Fehler: " + e.getMessage());
            textBackupStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.m3_error));
        }
    }

    private void showDirectoryPickerDialog() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                textBackupStatus.setText("Bitte erlaube Cornerlays den Dateizugriff in den Android-Einstellungen.\nFalls bereits erlaubt, starte die App bitte einmal neu (Hintergrunddienst-Berechtigung).");
                textBackupStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.m3_error));
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.setData(Uri.parse("package:" + requireContext().getPackageName()));
                    startActivity(intent);
                } catch (Exception e) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    startActivity(intent);
                }
                return;
            }
        } else {
            boolean readGranted = ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED;
            boolean writeGranted = ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED;
            if (!readGranted || !writeGranted) {
                checkAndRequestStoragePermissions();
                return;
            }
        }

        File[] dirs = currentExportDir.listFiles(file -> file.isDirectory() && !file.getName().startsWith("."));
        final java.util.List<File> dirList = new java.util.ArrayList<>();
        if (dirs != null) {
            java.util.Arrays.sort(dirs, (f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));
            dirList.addAll(java.util.Arrays.asList(dirs));
        }

        java.util.List<String> labels = new java.util.ArrayList<>();
        labels.add("✔ [ DIESEN ORDNER WÄHLEN ]");
        final boolean hasParent = currentExportDir.getParentFile() != null && !currentExportDir.getAbsolutePath().equals("/storage/emulated/0");
        if (hasParent) {
            labels.add(".. (Ordner nach oben)");
        }
        for (File d : dirList) {
            labels.add("📁 " + d.getName());
        }

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
        builder.setTitle("Ordner wählen:\n" + currentExportDir.getAbsolutePath());
        builder.setItems(labels.toArray(new String[0]), (dialog, which) -> {
            if (which == 0) {
                writeBackupToDirectory(currentExportDir);
            } else if (hasParent && which == 1) {
                currentExportDir = currentExportDir.getParentFile();
                showDirectoryPickerDialog();
            } else {
                int dirIdx = hasParent ? (which - 2) : (which - 1);
                currentExportDir = dirList.get(dirIdx);
                showDirectoryPickerDialog();
            }
        });
        builder.setNegativeButton("Abbrechen", null);
        builder.show();
    }

    private void writeBackupToDirectory(File directory) {
        try {
            File backupFile = new File(directory, "cornerlays_backup.json");
            Map<String, ?> allPrefs = this.sharedPreferences.getAll();
            String jsonStr = gson.toJson(allPrefs);

            try (FileOutputStream fos = new FileOutputStream(backupFile);
                 OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
                osw.write(jsonStr);
                osw.flush();
            }

            textBackupStatus.setText("Backup exportiert nach:\n" + backupFile.getAbsolutePath());
            textBackupStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.m3_primary));
        } catch (Exception e) {
            textBackupStatus.setText("Schreib-Fehler: " + e.getMessage());
            textBackupStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.m3_error));
        }
    }

    private void importBackup() {
        try {
            showFilePickerDialog();
        } catch (Exception e) {
            textBackupStatus.setText("Import-Fehler: " + e.getMessage());
            textBackupStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.m3_error));
        }
    }

    private void showFilePickerDialog() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                textBackupStatus.setText("Bitte erlaube Cornerlays den Dateizugriff in den Android-Einstellungen.\nFalls bereits erlaubt, starte die App bitte einmal neu (Hintergrunddienst-Berechtigung).");
                textBackupStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.m3_error));
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.setData(Uri.parse("package:" + requireContext().getPackageName()));
                    startActivity(intent);
                } catch (Exception e) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    startActivity(intent);
                }
                return;
            }
        } else {
            boolean readGranted = ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED;
            boolean writeGranted = ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED;
            if (!readGranted || !writeGranted) {
                checkAndRequestStoragePermissions();
                return;
            }
        }

        File[] allFiles = currentExportDir.listFiles(file -> {
            if (file.isDirectory() && !file.getName().startsWith(".")) {
                return true;
            }
            return file.isFile() && file.getName().toLowerCase().endsWith(".json");
        });

        final java.util.List<File> dirList = new java.util.ArrayList<>();
        final java.util.List<File> fileList = new java.util.ArrayList<>();
        if (allFiles != null) {
            for (File f : allFiles) {
                if (f.isDirectory()) {
                    dirList.add(f);
                } else {
                    fileList.add(f);
                }
            }
            java.util.Collections.sort(dirList, (f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));
            java.util.Collections.sort(fileList, (f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));
        }

        java.util.List<String> labels = new java.util.ArrayList<>();
        final boolean hasParent = currentExportDir.getParentFile() != null && !currentExportDir.getAbsolutePath().equals("/storage/emulated/0");
        if (hasParent) {
            labels.add(".. (Ordner nach oben)");
        }
        for (File d : dirList) {
            labels.add("📁 " + d.getName());
        }
        for (File f : fileList) {
            labels.add("📄 " + f.getName());
        }

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
        builder.setTitle("Backup-Datei wählen:\n" + currentExportDir.getAbsolutePath());
        builder.setItems(labels.toArray(new String[0]), (dialog, which) -> {
            String selectedLabel = labels.get(which);
            if (selectedLabel.equals(".. (Ordner nach oben)")) {
                currentExportDir = currentExportDir.getParentFile();
                showFilePickerDialog();
            } else if (selectedLabel.startsWith("📁 ")) {
                int dirIdx = hasParent ? (which - 1) : which;
                currentExportDir = dirList.get(dirIdx);
                showFilePickerDialog();
            } else if (selectedLabel.startsWith("📄 ")) {
                int fileIdx;
                if (hasParent) {
                    fileIdx = which - 1 - dirList.size();
                } else {
                    fileIdx = which - dirList.size();
                }
                File selectedFile = fileList.get(fileIdx);
                readBackupFromFile(selectedFile);
            }
        });
        builder.setNegativeButton("Abbrechen", null);
        builder.show();
    }

    private void readBackupFromFile(File file) {
        try {
            StringBuilder sb = new StringBuilder();
            try (FileInputStream fis = new FileInputStream(file);
                 InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
                 BufferedReader reader = new BufferedReader(isr)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }

            String jsonStr = sb.toString();
            Map<?, ?> backupMap = gson.fromJson(jsonStr, Map.class);
            SharedPreferences.Editor editor = getEditor();
            editor.clear(); // Clear current settings

            for (Map.Entry<?, ?> entry : backupMap.entrySet()) {
                String key = (String) entry.getKey();
                Object val = entry.getValue();
                if (val instanceof Boolean) {
                    editor.putBoolean(key, (Boolean) val);
                } else if (val instanceof Double) {
                    Double d = (Double) val;
                    if (d == Math.floor(d)) {
                        editor.putInt(key, d.intValue());
                    } else {
                        editor.putFloat(key, d.floatValue());
                    }
                } else if (val instanceof Float) {
                    editor.putFloat(key, (Float) val);
                } else if (val instanceof Integer) {
                    editor.putInt(key, (Integer) val);
                } else if (val instanceof Long) {
                    editor.putLong(key, (Long) val);
                } else if (val instanceof String) {
                    editor.putString(key, (String) val);
                }
            }
            editor.commit();

            textBackupStatus.setText("Backup erfolgreich importiert aus:\n" + file.getName());
            textBackupStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.m3_primary));

            // Reload UI fields
            loadSettings();

            // Restart services
            SettingsManager.checkAndStartServices(requireContext());
            LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(new Intent("app.olus.cornerlays.action.MQTT_CREDENTIALS_CHANGED"));

        } catch (Exception e) {
            textBackupStatus.setText("Import-Fehler: " + e.getMessage());
            textBackupStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.m3_error));
        }
    }
}
