package app.olus.cornerlays.ha;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import androidx.fragment.app.Fragment;
import app.olus.cornerlays.ColorPickerDialogFragment;
import app.olus.cornerlays.CombinedSettingsActivity;
import app.olus.cornerlays.R;
import app.olus.cornerlays.SettingsManager;
import app.olus.cornerlays.ha.EntityPickerDialogFragment;
import app.olus.cornerlays.ha.HAOverlaySettingsActivity;
import app.olus.cornerlays.ha.HARulesDialogFragment;
import app.olus.cornerlays.ha.model.HAOverlay;
import app.olus.cornerlays.ha.model.HARule;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import okhttp3.Call;
import okhttp3.Callback;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/* loaded from: classes4.dex */
public class HAOverlaySettingsActivity extends AppCompatActivity implements ColorPickerDialogFragment.ColorPickerListener, EntityPickerDialogFragment.EntityPickerListener, HARulesDialogFragment.RulesDialogListener {
    public static final String EXTRA_SLOT_INDEX = "slot_index";
    private static final int MIN_SIZE = 14;
    private View btnBrowseEntities;
    private View btnFineTune;
    private Button btnReset;
    private Button btnRules;
    private View btnTriggerEntity;
    private Chip chipTriggerState;
    private View colorPreview;
    private RelativeLayout containerAlpha;
    private View containerCameraSettings;
    private RelativeLayout containerColor;
    private TextInputLayout containerDisplayName;
    private RelativeLayout containerShadowColor;
    private View containerTabDisplay;
    private View containerTabSource;
    private View containerTextSettings;
    private TextInputLayout containerTriggerState;
    private TextInputLayout containerUnit;
    private HAOverlay currentOverlay;
    private TextInputEditText editDisplayName;
    private TextInputEditText editTriggerState;
    private TextInputEditText editUnit;
    private Gson gson;
    private List<HAOverlay> haOverlays;
    private TextView labelSize;
    private Runnable saveRunnable;
    private SeekBar seekbarAlpha;
    private SeekBar seekbarCameraInterval;
    private SeekBar seekbarSize;
    private View shadowPreview;
    private SharedPreferences sharedPreferences;
    private int slotIndex;
    private Spinner spinnerDisplayMode;
    private Spinner spinnerPosition;
    private Spinner spinnerVisibilityMode;
    private Spinner spinner_ha_source;
    private SwitchCompat switchEnabled;
    private TextView textAlphaValue;
    private TextView textEntityId;
    private TextView textSizeValue;
    private TextView textTriggerEntityId;
    private TextView textviewCameraIntervalValue;
    private TextView titleView;
    private RadioGroup toggleGroupSettingsTabs;
    private List<SourceOption> sourceOptions = new ArrayList();
    private boolean isUpdatingUI = false;
    private boolean isPickingTriggerEntity = false;
    private boolean isInitialSetup = true;
    private final Handler saveHandler = new Handler(Looper.getMainLooper());
    private final OkHttpClient okHttpClient = new OkHttpClient();

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes4.dex */
    public static class SourceOption {
        String attribute;
        String label;
        String type;

        SourceOption(String label, String type, String attribute) {
            this.label = label;
            this.type = type;
            this.attribute = attribute;
        }

        public String toString() {
            return this.label;
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, androidx.core.app.ComponentActivity, android.app.Activity
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ha_overlay_settings);
        this.sharedPreferences = getSharedPreferences(SettingsManager.PREFS_NAME, 0);
        this.gson = new Gson();
        this.slotIndex = getIntent().getIntExtra(EXTRA_SLOT_INDEX, -1);
        if (this.slotIndex == -1) {
            finish();
        } else {
            initViews();
            setupListeners();
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // androidx.fragment.app.FragmentActivity, android.app.Activity
    public void onResume() {
        super.onResume();
        loadConfig();
        populateViews();
        if (this.toggleGroupSettingsTabs != null) {
            this.toggleGroupSettingsTabs.requestFocus();
        }
    }

    private void loadConfig() {
        String json = this.sharedPreferences.getString(SettingsManager.KEY_HA_OVERLAYS_JSON, "[]");
        Type listType = new TypeToken<ArrayList<HAOverlay>>() { // from class: app.olus.cornerlays.ha.HAOverlaySettingsActivity.1
        }.getType();
        this.haOverlays = (List) this.gson.fromJson(json, listType);
        if (this.haOverlays == null) {
            this.haOverlays = new ArrayList();
        }
        while (this.haOverlays.size() <= this.slotIndex) {
            this.haOverlays.add(new HAOverlay());
        }
        this.currentOverlay = this.haOverlays.get(this.slotIndex);
    }

    private void initViews() {
        this.toggleGroupSettingsTabs = (RadioGroup) findViewById(R.id.toggleGroup_settings_tabs);
        this.containerTabSource = findViewById(R.id.container_tab_source);
        this.containerTabDisplay = findViewById(R.id.container_tab_display);
        this.titleView = (TextView) findViewById(R.id.ha_overlay_settings_title);
        this.switchEnabled = (SwitchCompat) findViewById(R.id.switch_ha_overlay_enabled);
        this.btnBrowseEntities = findViewById(R.id.btn_ha_browse_entities);
        this.textEntityId = (TextView) findViewById(R.id.text_ha_entity_id);
        this.spinner_ha_source = (Spinner) findViewById(R.id.spinner_ha_source);
        this.containerDisplayName = (TextInputLayout) findViewById(R.id.container_ha_display_name);
        this.editDisplayName = (TextInputEditText) findViewById(R.id.edit_ha_display_name);
        this.containerUnit = (TextInputLayout) findViewById(R.id.container_ha_unit);
        this.editUnit = (TextInputEditText) findViewById(R.id.edit_ha_unit);
        this.btnRules = (Button) findViewById(R.id.btn_ha_value_mappings);
        this.btnReset = (Button) findViewById(R.id.btn_reset_overlay);
        this.spinnerDisplayMode = (Spinner) findViewById(R.id.spinner_ha_display_mode);
        this.spinnerVisibilityMode = (Spinner) findViewById(R.id.spinner_ha_visibility_mode);
        this.spinnerPosition = (Spinner) findViewById(R.id.spinner_ha_position);
        this.btnFineTune = findViewById(R.id.btn_ha_fine_tune);
        this.seekbarSize = (SeekBar) findViewById(R.id.seekbar_size);
        this.textSizeValue = (TextView) findViewById(R.id.textview_size_value);
        this.labelSize = (TextView) findViewById(R.id.label_setting_size);
        this.containerAlpha = (RelativeLayout) findViewById(R.id.container_ha_alpha);
        this.seekbarAlpha = (SeekBar) findViewById(R.id.seekbar_alpha);
        this.textAlphaValue = (TextView) findViewById(R.id.textview_alpha_value);
        this.containerColor = (RelativeLayout) findViewById(R.id.container_color);
        this.colorPreview = findViewById(R.id.color_preview);
        this.containerShadowColor = (RelativeLayout) findViewById(R.id.container_shadow_color);
        this.shadowPreview = findViewById(R.id.shadow_color_preview);
        this.containerTextSettings = findViewById(R.id.container_text_settings);
        this.containerCameraSettings = findViewById(R.id.container_camera_settings);
        this.seekbarCameraInterval = (SeekBar) findViewById(R.id.seekbar_camera_interval);
        this.textviewCameraIntervalValue = (TextView) findViewById(R.id.textview_camera_interval_value);
        this.btnTriggerEntity = findViewById(R.id.btn_ha_trigger_entity);
        this.textTriggerEntityId = (TextView) findViewById(R.id.text_ha_trigger_entity_id);
        this.containerTriggerState = (TextInputLayout) findViewById(R.id.container_ha_trigger_state);
        this.editTriggerState = (TextInputEditText) findViewById(R.id.edit_ha_trigger_state);
        this.chipTriggerState = (Chip) findViewById(R.id.chip_current_trigger_state);
    }

    public void populateViews() {
        this.isUpdatingUI = true;
        try {
            // Title
            this.titleView.setText("HA Overlay " + (this.slotIndex + 1));

            // Enabled switch
            this.switchEnabled.setChecked(this.currentOverlay.isEnabled());

            // Entity ID display
            String entityId = this.currentOverlay.getEntityId();
            if (entityId != null && !entityId.isEmpty()) {
                String display = entityId;
                if (this.currentOverlay.getAttributeName() != null) {
                    display = display + "\n(" + this.currentOverlay.getAttributeName() + ")";
                }
                this.textEntityId.setText(display);
            } else {
                this.textEntityId.setText(getString(R.string.ha_overlay_not_configured));
            }

            // Source spinner (state vs. attribute)
            setupSourceSpinner();

            // Display name and unit
            this.editDisplayName.setText(this.currentOverlay.getDisplayName() != null ? this.currentOverlay.getDisplayName() : "");
            this.editUnit.setText(this.currentOverlay.getUnit() != null ? this.currentOverlay.getUnit() : "");

            // Display mode spinner
            ArrayAdapter<CharSequence> displayModeAdapter = ArrayAdapter.createFromResource(this, R.array.ha_display_mode_values, android.R.layout.simple_spinner_item);
            displayModeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            this.spinnerDisplayMode.setAdapter(displayModeAdapter);
            String[] displayModeKeys = getResources().getStringArray(R.array.ha_display_mode_keys);
            String currentDisplayMode = this.currentOverlay.getDisplayMode();
            int displayModeIndex = 0;
            for (int i = 0; i < displayModeKeys.length; i++) {
                if (displayModeKeys[i].equals(currentDisplayMode)) {
                    displayModeIndex = i;
                    break;
                }
            }
            this.spinnerDisplayMode.setSelection(displayModeIndex);

            // Visibility mode spinner
            ArrayAdapter<CharSequence> visibilityAdapter = ArrayAdapter.createFromResource(this, R.array.ha_visibility_mode_options, android.R.layout.simple_spinner_item);
            visibilityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            this.spinnerVisibilityMode.setAdapter(visibilityAdapter);
            this.spinnerVisibilityMode.setSelection(this.currentOverlay.getVisibilityMode());

            // Trigger entity / trigger state visibility based on visibility mode
            int vMode = this.currentOverlay.getVisibilityMode();
            boolean needsTriggerState = vMode == 2 || vMode == 3;
            boolean needsSeparateTriggerEntity = vMode == 3;

            this.containerTriggerState.setVisibility(needsTriggerState ? View.VISIBLE : View.GONE);
            this.btnTriggerEntity.setVisibility(needsSeparateTriggerEntity ? View.VISIBLE : View.GONE);
            this.chipTriggerState.setVisibility(View.GONE);

            if (needsTriggerState) {
                this.editTriggerState.setText(this.currentOverlay.getTriggerState() != null ? this.currentOverlay.getTriggerState() : "");
                if (needsSeparateTriggerEntity) {
                    String triggerEntityId = this.currentOverlay.getTriggerEntityId();
                    this.textTriggerEntityId.setText(triggerEntityId != null && !triggerEntityId.isEmpty() ? triggerEntityId : getString(R.string.ha_overlay_not_configured));
                    fetchTriggerEntityState(triggerEntityId);
                } else {
                    fetchTriggerEntityState(this.currentOverlay.getEntityId());
                }
            }

            // Position spinner
            ArrayAdapter<CharSequence> positionAdapter = ArrayAdapter.createFromResource(this, R.array.position_options_array, android.R.layout.simple_spinner_item);
            positionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            this.spinnerPosition.setAdapter(positionAdapter);
            this.spinnerPosition.setSelection(CombinedSettingsActivity.getSpinnerIndexForGravity(this.currentOverlay.getGravity()));

            // Size seekbar (offset by MIN_SIZE=14)
            int sizeProgress = this.currentOverlay.getSize() - MIN_SIZE;
            this.seekbarSize.setProgress(Math.max(0, sizeProgress));
            this.textSizeValue.setText(getString(R.string.setting_size_display, Integer.valueOf(this.currentOverlay.getSize())));

            // Alpha seekbar (inverted: 0% = fully opaque, 100% = fully transparent)
            int alphaProgress = Math.round((1.0f - this.currentOverlay.getAlpha()) * 100);
            this.seekbarAlpha.setProgress(alphaProgress);
            this.textAlphaValue.setText(alphaProgress + "%");

            // Color previews
            this.colorPreview.setBackgroundColor(this.currentOverlay.getColor());
            this.shadowPreview.setBackgroundColor(this.currentOverlay.getShadowColor());

            // Camera settings
            boolean isCamera = entityId != null && entityId.startsWith("camera.");
            this.containerCameraSettings.setVisibility(isCamera ? View.VISIBLE : View.GONE);
            this.containerTextSettings.setVisibility(isCamera ? View.GONE : View.VISIBLE);
            if (isCamera) {
                int intervalSeconds = this.currentOverlay.getCameraUpdateIntervalMs() / 1000;
                this.seekbarCameraInterval.setProgress(Math.max(0, intervalSeconds - 1));
                this.textviewCameraIntervalValue.setText("Update-Intervall: " + intervalSeconds + "s");
            }

            // Rules button text
            updateRulesButtonText();

        } finally {
            this.isUpdatingUI = false;
            // Delay clearing initialSetup flag to prevent immediate save triggers from spinners
            new Handler(Looper.getMainLooper()).postDelayed(() -> this.isInitialSetup = false, 500);
        }
    }

    private void setupSourceSpinner() {
        this.sourceOptions.clear();
        this.sourceOptions.add(new SourceOption("Zustand (state)", "state", null));

        String entityId = this.currentOverlay.getEntityId();
        if (entityId != null && !entityId.isEmpty()) {
            // Fetch attributes from HA to populate spinner
            String url = this.sharedPreferences.getString(SettingsManager.KEY_HA_URL, "");
            String token = this.sharedPreferences.getString(SettingsManager.KEY_HA_TOKEN, "");
            if (!TextUtils.isEmpty(url) && !TextUtils.isEmpty(token)) {
                Request request = new Request.Builder()
                        .url(url + "/api/states/" + entityId)
                        .header("Authorization", "Bearer " + token)
                        .build();
                this.okHttpClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        // Keep only "state" option
                    }
                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        try {
                            if (response.isSuccessful() && response.body() != null) {
                                String json = response.body().string();
                                JsonObject stateObj = JsonParser.parseString(json).getAsJsonObject();
                                if (stateObj.has("attributes")) {
                                    JsonObject attrs = stateObj.getAsJsonObject("attributes");
                                    for (String key : attrs.keySet()) {
                                        sourceOptions.add(new SourceOption("Attribut: " + key, "attribute", key));
                                    }
                                }
                                runOnUiThread(() -> {
                                    ArrayAdapter<SourceOption> adapter = new ArrayAdapter<>(HAOverlaySettingsActivity.this, android.R.layout.simple_spinner_item, sourceOptions);
                                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                    spinner_ha_source.setAdapter(adapter);
                                    // Select current source
                                    String currentAttr = currentOverlay.getAttributeName();
                                    if (currentAttr != null) {
                                        for (int i = 0; i < sourceOptions.size(); i++) {
                                            if (currentAttr.equals(sourceOptions.get(i).attribute)) {
                                                spinner_ha_source.setSelection(i);
                                                break;
                                            }
                                        }
                                    }
                                });
                            }
                        } catch (Exception e) {
                            Log.e("HA_DEBUG", "Error fetching entity attributes", e);
                        } finally {
                            if (response.body() != null) response.body().close();
                        }
                    }
                });
            }
        }

        ArrayAdapter<SourceOption> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, this.sourceOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this.spinner_ha_source.setAdapter(adapter);

        // Select current attribute if any
        String currentAttr = this.currentOverlay.getAttributeName();
        if (currentAttr != null) {
            for (int i = 0; i < this.sourceOptions.size(); i++) {
                if (currentAttr.equals(this.sourceOptions.get(i).attribute)) {
                    this.spinner_ha_source.setSelection(i);
                    break;
                }
            }
        }
    }


    private void fetchTriggerEntityState(String entityId) {
        if (TextUtils.isEmpty(entityId)) {
            this.chipTriggerState.setVisibility(8);
            return;
        }
        this.chipTriggerState.setVisibility(0);
        this.chipTriggerState.setText("Aktueller Zustand wird geladen...");
        this.chipTriggerState.setOnClickListener(null);
        String url = this.sharedPreferences.getString(SettingsManager.KEY_HA_URL, "");
        String token = this.sharedPreferences.getString(SettingsManager.KEY_HA_TOKEN, "");
        if (TextUtils.isEmpty(url) || TextUtils.isEmpty(token)) {
            this.chipTriggerState.setVisibility(8);
        } else {
            Request request = new Request.Builder().url(url + "/api/states/" + entityId).header("Authorization", "Bearer " + token).build();
            this.okHttpClient.newCall(request).enqueue(new AnonymousClass2());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: app.olus.cornerlays.ha.HAOverlaySettingsActivity$2, reason: invalid class name */
    /* loaded from: classes4.dex */
    public class AnonymousClass2 implements Callback {
        AnonymousClass2() {
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onFailure$0() {
            HAOverlaySettingsActivity.this.chipTriggerState.setVisibility(8);
        }

        @Override // okhttp3.Callback
        public void onFailure(Call call, IOException e) {
            HAOverlaySettingsActivity.this.runOnUiThread(new Runnable() { // from class: app.olus.cornerlays.ha.HAOverlaySettingsActivity$2$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    HAOverlaySettingsActivity.AnonymousClass2.this.lambda$onFailure$0();
                }
            });
        }

        @Override // okhttp3.Callback
        public void onResponse(Call call, Response response) throws IOException {
            try {
                if (response.isSuccessful() && response.body() != null) {
                    String json = response.body().string();
                    JsonObject stateObj = JsonParser.parseString(json).getAsJsonObject();
                    final String stateValue = stateObj.get("state").getAsString();
                    HAOverlaySettingsActivity.this.runOnUiThread(new Runnable() { // from class: app.olus.cornerlays.ha.HAOverlaySettingsActivity$2$$ExternalSyntheticLambda2
                        @Override // java.lang.Runnable
                        public final void run() {
                            HAOverlaySettingsActivity.AnonymousClass2.this.lambda$onResponse$2(stateValue);
                        }
                    });
                } else {
                    HAOverlaySettingsActivity.this.runOnUiThread(new Runnable() { // from class: app.olus.cornerlays.ha.HAOverlaySettingsActivity$2$$ExternalSyntheticLambda3
                        @Override // java.lang.Runnable
                        public final void run() {
                            HAOverlaySettingsActivity.AnonymousClass2.this.lambda$onResponse$3();
                        }
                    });
                }
            } catch (Exception e) {
                HAOverlaySettingsActivity.this.runOnUiThread(new Runnable() { // from class: app.olus.cornerlays.ha.HAOverlaySettingsActivity$2$$ExternalSyntheticLambda4
                    @Override // java.lang.Runnable
                    public final void run() {
                        HAOverlaySettingsActivity.AnonymousClass2.this.lambda$onResponse$4();
                    }
                });
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onResponse$2(final String stateValue) {
            HAOverlaySettingsActivity.this.chipTriggerState.setText("Übernehmen: '" + stateValue + "'");
            HAOverlaySettingsActivity.this.chipTriggerState.setOnClickListener(new View.OnClickListener() { // from class: app.olus.cornerlays.ha.HAOverlaySettingsActivity$2$$ExternalSyntheticLambda0
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    HAOverlaySettingsActivity.AnonymousClass2.this.lambda$onResponse$1(stateValue, view);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onResponse$1(String stateValue, View v) {
            HAOverlaySettingsActivity.this.editTriggerState.setText(stateValue);
            HAOverlaySettingsActivity.this.updateAndSaveConfig();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onResponse$3() {
            HAOverlaySettingsActivity.this.chipTriggerState.setVisibility(8);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onResponse$4() {
            HAOverlaySettingsActivity.this.chipTriggerState.setVisibility(8);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateAndSaveConfig() {
        updateAndSaveConfig(500);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateAndSaveConfig(int delayMs) {
        if (this.isUpdatingUI) {
            return;
        }
        this.saveHandler.removeCallbacks(this.saveRunnable);
        this.saveRunnable = new Runnable() { // from class: app.olus.cornerlays.ha.HAOverlaySettingsActivity$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                HAOverlaySettingsActivity.this.performSave();
            }
        };
        this.saveHandler.postDelayed(this.saveRunnable, delayMs);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void performSave() {
        if (this.isUpdatingUI) {
            return;
        }
        this.currentOverlay.setEnabled(this.switchEnabled.isChecked());
        this.currentOverlay.setDisplayName(this.editDisplayName.getText().toString().trim());
        this.currentOverlay.setUnit(this.editUnit.getText().toString().trim());
        this.currentOverlay.setVisibilityMode(this.spinnerVisibilityMode.getSelectedItemPosition());
        int vMode = this.currentOverlay.getVisibilityMode();
        boolean needsTriggerState = vMode == 2 || vMode == 3;
        String[] displayModeKeys = getResources().getStringArray(R.array.ha_display_mode_keys);
        this.currentOverlay.setDisplayMode(displayModeKeys[this.spinnerDisplayMode.getSelectedItemPosition()]);
        this.currentOverlay.setGravity(CombinedSettingsActivity.getGravityForSpinnerIndex(this.spinnerPosition.getSelectedItemPosition()));
        this.currentOverlay.setSize(this.seekbarSize.getProgress() + 14);
        this.currentOverlay.setAlpha(1.0f - (this.seekbarAlpha.getProgress() / 100.0f));
        this.currentOverlay.setColor(((ColorDrawable) this.colorPreview.getBackground()).getColor());
        this.currentOverlay.setShadowColor(((ColorDrawable) this.shadowPreview.getBackground()).getColor());
        this.currentOverlay.setCameraUpdateIntervalMs((this.seekbarCameraInterval.getProgress() + 1) * 1000);
        if (needsTriggerState) {
            this.currentOverlay.setTriggerState(this.editTriggerState.getText().toString().trim());
            if (vMode == 2) {
                this.currentOverlay.setTriggerEntityId(this.currentOverlay.getEntityId());
            }
        } else {
            this.currentOverlay.setTriggerEntityId(null);
            this.currentOverlay.setTriggerState(null);
        }
        this.haOverlays.set(this.slotIndex, this.currentOverlay);
        String json = this.gson.toJson(this.haOverlays);
        Log.d("HA_DEBUG", "Saving Config JSON: " + json);
        this.sharedPreferences.edit().putString(SettingsManager.KEY_HA_OVERLAYS_JSON, json).apply();
        Intent serviceIntent = new Intent(this, (Class<?>) HomeAssistantService.class);
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        } catch (Exception e) {
        }
    }

    private void updateRulesButtonText() {
        int count = this.currentOverlay.getRules() != null ? this.currentOverlay.getRules().size() : 0;
        if (count > 0) {
            this.btnRules.setText("Zustands-Regeln (" + count + " aktiv)");
        } else {
            this.btnRules.setText("Zustands-Regeln konfigurieren");
        }
    }

    private void setupListeners() {
        View.OnFocusChangeListener tabFocusListener = new View.OnFocusChangeListener() { // from class: app.olus.cornerlays.ha.HAOverlaySettingsActivity$$ExternalSyntheticLambda14
            @Override // android.view.View.OnFocusChangeListener
            public final void onFocusChange(View view, boolean z) {
                HAOverlaySettingsActivity.lambda$setupListeners$1(view, z);
            }
        };
        for (int i = 0; i < this.toggleGroupSettingsTabs.getChildCount(); i++) {
            this.toggleGroupSettingsTabs.getChildAt(i).setOnFocusChangeListener(tabFocusListener);
        }
        this.toggleGroupSettingsTabs.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() { // from class: app.olus.cornerlays.ha.HAOverlaySettingsActivity$$ExternalSyntheticLambda19
            @Override // android.widget.RadioGroup.OnCheckedChangeListener
            public final void onCheckedChanged(RadioGroup radioGroup, int i2) {
                HAOverlaySettingsActivity.this.lambda$setupListeners$2(radioGroup, i2);
            }
        });
        this.switchEnabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { // from class: app.olus.cornerlays.ha.HAOverlaySettingsActivity$$ExternalSyntheticLambda1
            @Override // android.widget.CompoundButton.OnCheckedChangeListener
            public final void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                HAOverlaySettingsActivity.this.lambda$setupListeners$3(compoundButton, z);
            }
        });
        this.seekbarCameraInterval.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() { // from class: app.olus.cornerlays.ha.HAOverlaySettingsActivity.3
            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    int seconds = progress + 1;
                    HAOverlaySettingsActivity.this.textviewCameraIntervalValue.setText("Update-Intervall: " + seconds + "s");
                    HAOverlaySettingsActivity.this.updateAndSaveConfig();
                }
            }

            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        findViewById(R.id.container_ha_visibility_mode).setOnClickListener(new View.OnClickListener() { // from class: app.olus.cornerlays.ha.HAOverlaySettingsActivity$$ExternalSyntheticLambda2
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                HAOverlaySettingsActivity.this.lambda$setupListeners$4(view);
            }
        });
        this.spinnerVisibilityMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() { // from class: app.olus.cornerlays.ha.HAOverlaySettingsActivity.4
            @Override // android.widget.AdapterView.OnItemSelectedListener
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (HAOverlaySettingsActivity.this.isUpdatingUI || HAOverlaySettingsActivity.this.isInitialSetup || HAOverlaySettingsActivity.this.currentOverlay.getVisibilityMode() == position) {
                    return;
                }
                HAOverlaySettingsActivity.this.currentOverlay.setVisibilityMode(position);
                HAOverlaySettingsActivity.this.updateAndSaveConfig(100);
                HAOverlaySettingsActivity.this.populateViews();
            }

            @Override // android.widget.AdapterView.OnItemSelectedListener
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        findViewById(R.id.container_ha_source).setOnClickListener(new View.OnClickListener() { // from class: app.olus.cornerlays.ha.HAOverlaySettingsActivity$$ExternalSyntheticLambda3
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                HAOverlaySettingsActivity.this.lambda$setupListeners$5(view);
            }
        });
        this.spinner_ha_source.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() { // from class: app.olus.cornerlays.ha.HAOverlaySettingsActivity.5
            @Override // android.widget.AdapterView.OnItemSelectedListener
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (HAOverlaySettingsActivity.this.isUpdatingUI || HAOverlaySettingsActivity.this.isInitialSetup) {
                    return;
                }
                SourceOption opt = (SourceOption) HAOverlaySettingsActivity.this.sourceOptions.get(position);
                if (opt.type.equals(HAOverlaySettingsActivity.this.currentOverlay.getCameraStreamType())) {
                    if (opt.attribute == null) {
                        if (HAOverlaySettingsActivity.this.currentOverlay.getAttributeName() == null) {
                            return;
                        }
                    } else if (opt.attribute.equals(HAOverlaySettingsActivity.this.currentOverlay.getAttributeName())) {
                        return;
                    }
                }
                HAOverlaySettingsActivity.this.currentOverlay.setCameraStreamType(opt.type);
                HAOverlaySettingsActivity.this.currentOverlay.setAttributeName(opt.attribute);
                HAOverlaySettingsActivity.this.updateAndSaveConfig(100);
                HAOverlaySettingsActivity.this.populateViews();
            }

            @Override // android.widget.AdapterView.OnItemSelectedListener
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        this.btnTriggerEntity.setOnClickListener(new View.OnClickListener() { // from class: app.olus.cornerlays.ha.HAOverlaySettingsActivity$$ExternalSyntheticLambda4
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                HAOverlaySettingsActivity.this.lambda$setupListeners$6(view);
            }
        });
        setupTextInputListeners(this.containerTriggerState, this.editTriggerState);
        this.btnBrowseEntities.setOnClickListener(new View.OnClickListener() { // from class: app.olus.cornerlays.ha.HAOverlaySettingsActivity$$ExternalSyntheticLambda5
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                HAOverlaySettingsActivity.this.lambda$setupListeners$7(view);
            }
        });
        this.btnRules.setOnClickListener(new View.OnClickListener() { // from class: app.olus.cornerlays.ha.HAOverlaySettingsActivity$$ExternalSyntheticLambda6
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                HAOverlaySettingsActivity.this.lambda$setupListeners$8(view);
            }
        });
        this.btnReset.setOnClickListener(new View.OnClickListener() { // from class: app.olus.cornerlays.ha.HAOverlaySettingsActivity$$ExternalSyntheticLambda7
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                HAOverlaySettingsActivity.this.lambda$setupListeners$10(view);
            }
        });
        setupTextInputListeners(this.containerDisplayName, this.editDisplayName);
        setupTextInputListeners(this.containerUnit, this.editUnit);
        findViewById(R.id.container_ha_display_mode).setOnClickListener(new View.OnClickListener() { // from class: app.olus.cornerlays.ha.HAOverlaySettingsActivity$$ExternalSyntheticLambda8
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                HAOverlaySettingsActivity.this.lambda$setupListeners$11(view);
            }
        });
        this.spinnerDisplayMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() { // from class: app.olus.cornerlays.ha.HAOverlaySettingsActivity.6
            @Override // android.widget.AdapterView.OnItemSelectedListener
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (HAOverlaySettingsActivity.this.isUpdatingUI || HAOverlaySettingsActivity.this.isInitialSetup) {
                    return;
                }
                String[] displayModeKeys = HAOverlaySettingsActivity.this.getResources().getStringArray(R.array.ha_display_mode_keys);
                if (displayModeKeys[position].equals(HAOverlaySettingsActivity.this.currentOverlay.getDisplayMode())) {
                    return;
                }
                HAOverlaySettingsActivity.this.updateAndSaveConfig(100);
            }

            @Override // android.widget.AdapterView.OnItemSelectedListener
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        findViewById(R.id.container_ha_position).setOnClickListener(new View.OnClickListener() { // from class: app.olus.cornerlays.ha.HAOverlaySettingsActivity$$ExternalSyntheticLambda15
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                HAOverlaySettingsActivity.this.lambda$setupListeners$12(view);
            }
        });
        this.spinnerPosition.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() { // from class: app.olus.cornerlays.ha.HAOverlaySettingsActivity.7
            @Override // android.widget.AdapterView.OnItemSelectedListener
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (HAOverlaySettingsActivity.this.isUpdatingUI || HAOverlaySettingsActivity.this.isInitialSetup || CombinedSettingsActivity.getGravityForSpinnerIndex(position) == HAOverlaySettingsActivity.this.currentOverlay.getGravity()) {
                    return;
                }
                HAOverlaySettingsActivity.this.updateAndSaveConfig(100);
            }

            @Override // android.widget.AdapterView.OnItemSelectedListener
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        this.btnFineTune.setOnClickListener(new View.OnClickListener() { // from class: app.olus.cornerlays.ha.HAOverlaySettingsActivity$$ExternalSyntheticLambda16
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                HAOverlaySettingsActivity.this.lambda$setupListeners$13(view);
            }
        });
        this.containerColor.setOnClickListener(new View.OnClickListener() { // from class: app.olus.cornerlays.ha.HAOverlaySettingsActivity$$ExternalSyntheticLambda17
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                HAOverlaySettingsActivity.this.lambda$setupListeners$14(view);
            }
        });
        this.containerShadowColor.setOnClickListener(new View.OnClickListener() { // from class: app.olus.cornerlays.ha.HAOverlaySettingsActivity$$ExternalSyntheticLambda18
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                HAOverlaySettingsActivity.this.lambda$setupListeners$15(view);
            }
        });
        this.seekbarSize.setOnSeekBarChangeListener(new AnonymousClass8());
        this.seekbarAlpha.setOnSeekBarChangeListener(new AnonymousClass9());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$setupListeners$1(View v, boolean hasFocus) {
        if (hasFocus && (v instanceof RadioButton)) {
            ((RadioButton) v).setChecked(true);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupListeners$2(RadioGroup group, int checkedId) {
        this.containerTabSource.setVisibility(checkedId == R.id.tab_source ? 0 : 8);
        this.containerTabDisplay.setVisibility(checkedId != R.id.tab_display ? 8 : 0);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupListeners$3(CompoundButton v, boolean isChecked) {
        if (this.isUpdatingUI || this.isInitialSetup || this.currentOverlay.isEnabled() == isChecked) {
            return;
        }
        updateAndSaveConfig(100);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupListeners$4(View v) {
        this.spinnerVisibilityMode.performClick();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupListeners$5(View v) {
        this.spinner_ha_source.performClick();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupListeners$6(View v) {
        this.isPickingTriggerEntity = true;
        EntityPickerDialogFragment dialog = new EntityPickerDialogFragment();
        dialog.show(getSupportFragmentManager(), "entity_picker");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupListeners$7(View v) {
        this.isPickingTriggerEntity = false;
        EntityPickerDialogFragment dialog = new EntityPickerDialogFragment();
        dialog.show(getSupportFragmentManager(), "entity_picker");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupListeners$8(View v) {
        ArrayList<HARule> rulesList = new ArrayList<>();
        if (this.currentOverlay.getRules() != null) {
            rulesList.addAll(this.currentOverlay.getRules());
        }
        HARulesDialogFragment dialog = HARulesDialogFragment.newInstance(this.currentOverlay.getEntityId(), this.currentOverlay.getAttributeName(), rulesList);
        dialog.show(getSupportFragmentManager(), "rules_dialog");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupListeners$10(View v) {
        new AlertDialog.Builder(this).setTitle("Zurücksetzen").setMessage("Möchtest du dieses Overlay wirklich auf die Standardwerte zurücksetzen? Alle Einstellungen für diesen Slot gehen verloren.").setPositiveButton("Ja, zurücksetzen", new DialogInterface.OnClickListener() { // from class: app.olus.cornerlays.ha.HAOverlaySettingsActivity$$ExternalSyntheticLambda9
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                HAOverlaySettingsActivity.this.lambda$setupListeners$9(dialogInterface, i);
            }
        }).setNegativeButton("Abbrechen", (DialogInterface.OnClickListener) null).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupListeners$9(DialogInterface dialog, int which) {
        this.currentOverlay = new HAOverlay();
        this.haOverlays.set(this.slotIndex, this.currentOverlay);
        String json = this.gson.toJson(this.haOverlays);
        this.sharedPreferences.edit().putString(SettingsManager.KEY_HA_OVERLAYS_JSON, json).apply();
        populateViews();
        updateAndSaveConfig();
        Toast.makeText(this, "Overlay zurückgesetzt", 0).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupListeners$11(View v) {
        this.spinnerDisplayMode.performClick();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupListeners$12(View v) {
        this.spinnerPosition.performClick();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupListeners$13(View v) {
        Intent intent = new Intent(this, (Class<?>) HomeAssistantService.class);
        intent.setAction(SettingsManager.ACTION_TOGGLE_HA_POSITIONING_MODE);
        intent.putExtra(SettingsManager.EXTRA_HA_SLOT_INDEX, this.slotIndex);
        startService(intent);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupListeners$14(View v) {
        showColorPickerDialog("color");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupListeners$15(View v) {
        showColorPickerDialog("shadow");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: app.olus.cornerlays.ha.HAOverlaySettingsActivity$8, reason: invalid class name */
    /* loaded from: classes4.dex */
    public class AnonymousClass8 implements SeekBar.OnSeekBarChangeListener {
        private final Handler handler = new Handler(Looper.getMainLooper());
        private Runnable runnable;

        AnonymousClass8() {
        }

        @Override // android.widget.SeekBar.OnSeekBarChangeListener
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) {
                HAOverlaySettingsActivity.this.textSizeValue.setText(HAOverlaySettingsActivity.this.getString(R.string.setting_size_display, new Object[]{Integer.valueOf(progress + 14)}));
                if (this.runnable != null) {
                    this.handler.removeCallbacks(this.runnable);
                }
                this.runnable = new Runnable() { // from class: app.olus.cornerlays.ha.HAOverlaySettingsActivity$8$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        HAOverlaySettingsActivity.AnonymousClass8.this.lambda$onProgressChanged$0();
                    }
                };
                this.handler.postDelayed(this.runnable, 300L);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onProgressChanged$0() {
            HAOverlaySettingsActivity.this.updateAndSaveConfig();
        }

        @Override // android.widget.SeekBar.OnSeekBarChangeListener
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override // android.widget.SeekBar.OnSeekBarChangeListener
        public void onStopTrackingTouch(SeekBar seekBar) {
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: app.olus.cornerlays.ha.HAOverlaySettingsActivity$9, reason: invalid class name */
    /* loaded from: classes4.dex */
    public class AnonymousClass9 implements SeekBar.OnSeekBarChangeListener {
        private final Handler handler = new Handler(Looper.getMainLooper());
        private Runnable runnable;

        AnonymousClass9() {
        }

        @Override // android.widget.SeekBar.OnSeekBarChangeListener
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) {
                HAOverlaySettingsActivity.this.textAlphaValue.setText(progress + "%");
                if (this.runnable != null) {
                    this.handler.removeCallbacks(this.runnable);
                }
                this.runnable = new Runnable() { // from class: app.olus.cornerlays.ha.HAOverlaySettingsActivity$9$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        HAOverlaySettingsActivity.AnonymousClass9.this.lambda$onProgressChanged$0();
                    }
                };
                this.handler.postDelayed(this.runnable, 300L);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onProgressChanged$0() {
            HAOverlaySettingsActivity.this.updateAndSaveConfig();
        }

        @Override // android.widget.SeekBar.OnSeekBarChangeListener
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override // android.widget.SeekBar.OnSeekBarChangeListener
        public void onStopTrackingTouch(SeekBar seekBar) {
        }
    }

    private void setupTextInputListeners(final View container, final TextInputEditText editText) {
        container.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editText.setFocusableInTouchMode(true);
                editText.requestFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService("input_method");
                if (imm != null) {
                    imm.showSoftInput(editText, 1);
                }
            }
        });
        editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (!hasFocus && !HAOverlaySettingsActivity.this.isUpdatingUI) {
                    HAOverlaySettingsActivity.this.updateAndSaveConfig(10);
                    editText.setFocusable(false);
                }
            }
        });
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == 6 || actionId == 5) {
                    if (!HAOverlaySettingsActivity.this.isUpdatingUI) {
                        HAOverlaySettingsActivity.this.updateAndSaveConfig(10);
                    }
                    container.requestFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService("input_method");
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    }
                    editText.setFocusable(false);
                    return true;
                }
                return false;
            }
        });
    }

    private void showColorPickerDialog(String tag) {
        ColorPickerDialogFragment dialog = ColorPickerDialogFragment.newInstance(tag);
        dialog.setColorPickerListener(this);
        dialog.show(getSupportFragmentManager(), "ha_color_picker");
    }

    @Override // app.olus.cornerlays.ColorPickerDialogFragment.ColorPickerListener
    public void onColorSelected(int color, String tag) {
        Log.d("HA_DEBUG", "Activity onColorSelected: " + color + ", Tag: " + tag);
        if ("rule_color".equals(tag)) {
            Fragment frag = getSupportFragmentManager().findFragmentByTag("rules_dialog");
            if (frag instanceof HARulesDialogFragment) {
                Log.d("HA_DEBUG", "Passing color to RulesDialog");
                ((HARulesDialogFragment) frag).onColorSelected(color, tag);
                return;
            } else {
                Log.e("HA_DEBUG", "RulesDialog Fragment not found via findFragmentByTag");
                return;
            }
        }
        if ("color".equals(tag)) {
            this.colorPreview.setBackgroundColor(color);
        } else if ("shadow".equals(tag)) {
            this.shadowPreview.setBackgroundColor(color);
        }
        updateAndSaveConfig();
    }

    @Override // app.olus.cornerlays.ha.EntityPickerDialogFragment.EntityPickerListener
    public void onEntitySelected(String entityId, String attributeName) {
        if (this.isPickingTriggerEntity) {
            this.currentOverlay.setTriggerEntityId(entityId);
            this.textTriggerEntityId.setText(entityId);
        } else {
            this.currentOverlay.setEntityId(entityId);
            this.currentOverlay.setAttributeName(attributeName);
            String display = entityId;
            if (attributeName != null) {
                display = display + "\n(" + attributeName + ")";
            }
            this.textEntityId.setText(display);
        }
        updateAndSaveConfig();
        populateViews();
    }

    @Override // app.olus.cornerlays.ha.HARulesDialogFragment.RulesDialogListener
    public void onRulesSaved(List<HARule> rules) {
        Log.d("HA_DEBUG", "Received " + rules.size() + " rules in Activity.");
        this.currentOverlay.setRules(rules);
        updateRulesButtonText();
        updateAndSaveConfig();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // androidx.appcompat.app.AppCompatActivity, androidx.fragment.app.FragmentActivity, android.app.Activity
    public void onDestroy() {
        super.onDestroy();
        this.saveHandler.removeCallbacks(this.saveRunnable);
        this.okHttpClient.dispatcher().cancelAll();
    }
}
