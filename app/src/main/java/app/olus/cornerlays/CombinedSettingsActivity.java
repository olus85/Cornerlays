package app.olus.cornerlays;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class CombinedSettingsActivity extends AppCompatActivity implements ColorPickerDialogFragment.ColorPickerListener {

    private SharedPreferences sharedPreferences;
    private static final int MIN_SIZE = 14;
    private static final int DEFAULT_SIZE = 22;

    // UI Elemente
    private LinearLayout containerClockSettings, containerDateSettings, containerWeatherSettings;
    private SwitchCompat switchClockEnabled, switchClockShowSeconds, switchClock24h;
    private LinearLayout containerClockPosition;
    private Spinner spinnerClockPosition;
    private Button btnClockFineTune;
    private SeekBar seekbarClockSize;
    private TextView textviewClockSizeValue;
    private RelativeLayout containerClockColor, containerClockShadowColor;
    private View colorClockPreview, shadowClockPreview;

    private SwitchCompat switchDateEnabled;
    private LinearLayout containerDateFormat;
    private Spinner spinnerDateFormat;
    private LinearLayout containerDatePosition;
    private Spinner spinnerDatePosition;
    private Button btnDateFineTune;
    private SeekBar seekbarDateSize;
    private TextView textviewDateSizeValue;
    private RelativeLayout containerDateColor, containerDateShadowColor;
    private View colorDatePreview, shadowDatePreview;

    private SwitchCompat switchWeatherEnabled, switchWeatherFahrenheit;
    private TextInputLayout containerWeatherCity; // GEÄNDERT
    private TextInputEditText editWeatherCity;
    private LinearLayout containerWeatherPosition;
    private Spinner spinnerWeatherPosition;
    private Button btnWeatherFineTune;
    private SeekBar seekbarWeatherSize;
    private TextView textviewWeatherSizeValue;
    private RelativeLayout containerWeatherColor, containerWeatherShadowColor;
    private View colorWeatherPreview, shadowWeatherPreview;

    private String[] dateFormatKeys;
    private String currentlyPickingColorFor = "clock";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_combined_settings);
        sharedPreferences = getSharedPreferences(SettingsManager.PREFS_NAME, Context.MODE_PRIVATE);

        initViews();
        setupTabLogic();
        setupClockListeners();
        setupDateListeners();
        setupWeatherListeners();
        loadAllSettings();
        checkAndStartServices();
    }

    private void initViews() {
        containerClockSettings = findViewById(R.id.container_clock_settings);
        containerDateSettings = findViewById(R.id.container_date_settings);
        containerWeatherSettings = findViewById(R.id.container_weather_settings);

        // Uhr
        switchClockEnabled = containerClockSettings.findViewById(R.id.switch_clock_enabled);
        switchClockShowSeconds = containerClockSettings.findViewById(R.id.switch_clock_show_seconds);
        switchClock24h = containerClockSettings.findViewById(R.id.switch_clock_24h);
        containerClockPosition = containerClockSettings.findViewById(R.id.container_clock_position);
        spinnerClockPosition = containerClockSettings.findViewById(R.id.spinner_clock_position);
        btnClockFineTune = containerClockSettings.findViewById(R.id.btn_clock_fine_tune);
        seekbarClockSize = containerClockSettings.findViewById(R.id.seekbar_clock_size);
        textviewClockSizeValue = containerClockSettings.findViewById(R.id.textview_clock_size_value);
        containerClockColor = containerClockSettings.findViewById(R.id.container_clock_color);
        containerClockShadowColor = containerClockSettings.findViewById(R.id.container_clock_shadow_color);
        colorClockPreview = containerClockSettings.findViewById(R.id.color_clock_preview);
        shadowClockPreview = containerClockSettings.findViewById(R.id.shadow_clock_preview);

        // Datum
        switchDateEnabled = containerDateSettings.findViewById(R.id.switch_date_enabled);
        containerDateFormat = containerDateSettings.findViewById(R.id.container_date_format);
        spinnerDateFormat = containerDateSettings.findViewById(R.id.spinner_date_format);
        containerDatePosition = containerDateSettings.findViewById(R.id.container_date_position);
        spinnerDatePosition = containerDateSettings.findViewById(R.id.spinner_date_position);
        btnDateFineTune = containerDateSettings.findViewById(R.id.btn_date_fine_tune);
        seekbarDateSize = containerDateSettings.findViewById(R.id.seekbar_date_size);
        textviewDateSizeValue = containerDateSettings.findViewById(R.id.textview_date_size_value);
        containerDateColor = containerDateSettings.findViewById(R.id.container_date_color);
        containerDateShadowColor = containerDateSettings.findViewById(R.id.container_date_shadow_color);
        colorDatePreview = containerDateSettings.findViewById(R.id.color_date_preview);
        shadowDatePreview = containerDateSettings.findViewById(R.id.shadow_date_preview);

        // Wetter
        switchWeatherEnabled = containerWeatherSettings.findViewById(R.id.switch_weather_enabled);
        containerWeatherCity = containerWeatherSettings.findViewById(R.id.container_weather_city); // GEÄNDERT
        editWeatherCity = containerWeatherSettings.findViewById(R.id.edit_weather_city);
        switchWeatherFahrenheit = containerWeatherSettings.findViewById(R.id.switch_weather_fahrenheit);
        containerWeatherPosition = containerWeatherSettings.findViewById(R.id.container_weather_position);
        spinnerWeatherPosition = containerWeatherSettings.findViewById(R.id.spinner_weather_position);
        btnWeatherFineTune = containerWeatherSettings.findViewById(R.id.btn_weather_fine_tune);
        seekbarWeatherSize = containerWeatherSettings.findViewById(R.id.seekbar_weather_size);
        textviewWeatherSizeValue = containerWeatherSettings.findViewById(R.id.textview_weather_size_value);
        containerWeatherColor = containerWeatherSettings.findViewById(R.id.container_weather_color);
        containerWeatherShadowColor = containerWeatherSettings.findViewById(R.id.container_weather_shadow_color);
        colorWeatherPreview = containerWeatherSettings.findViewById(R.id.color_weather_preview);
        shadowWeatherPreview = containerWeatherSettings.findViewById(R.id.shadow_weather_preview);
    }

    private void setupTabLogic() {
        RadioGroup tabGroup = findViewById(R.id.tab_group);
        RadioButton tabClock = findViewById(R.id.tab_clock);
        RadioButton tabDate = findViewById(R.id.tab_date);
        RadioButton tabWeather = findViewById(R.id.tab_weather);

        View.OnFocusChangeListener focusListener = (v, hasFocus) -> {
            if (hasFocus && v instanceof RadioButton) {
                if (!((RadioButton) v).isChecked()) {
                    ((RadioButton) v).setChecked(true);
                }
            }
        };

        tabClock.setOnFocusChangeListener(focusListener);
        tabDate.setOnFocusChangeListener(focusListener);
        tabWeather.setOnFocusChangeListener(focusListener);

        tabGroup.setOnCheckedChangeListener((group, checkedId) -> {
            updateVisibleContainer(checkedId);
        });
    }

    private void updateVisibleContainer(int checkedId) {
        containerClockSettings.setVisibility(View.GONE);
        containerDateSettings.setVisibility(View.GONE);
        containerWeatherSettings.setVisibility(View.GONE);

        if (checkedId == R.id.tab_clock) {
            containerClockSettings.setVisibility(View.VISIBLE);
        } else if (checkedId == R.id.tab_date) {
            containerDateSettings.setVisibility(View.VISIBLE);
        } else if (checkedId == R.id.tab_weather) {
            containerWeatherSettings.setVisibility(View.VISIBLE);
        }
    }

    private void loadAllSettings() {
        // Uhr
        switchClockEnabled.setChecked(sharedPreferences.getBoolean(SettingsManager.KEY_CLOCK_ENABLED, false));
        btnClockFineTune.setEnabled(switchClockEnabled.isChecked());
        switchClockShowSeconds.setChecked(sharedPreferences.getBoolean(SettingsManager.KEY_CLOCK_SHOW_SECONDS, true));
        switchClock24h.setChecked(sharedPreferences.getBoolean(SettingsManager.KEY_CLOCK_24H_FORMAT, true));
        loadAppearanceSettings("clock", seekbarClockSize, textviewClockSizeValue, colorClockPreview, shadowClockPreview);
        loadPositionSpinner(spinnerClockPosition, SettingsManager.KEY_CLOCK_GRAVITY);

        // Datum
        switchDateEnabled.setChecked(sharedPreferences.getBoolean(SettingsManager.KEY_DATE_ENABLED, false));
        btnDateFineTune.setEnabled(switchDateEnabled.isChecked());
        loadAppearanceSettings("date", seekbarDateSize, textviewDateSizeValue, colorDatePreview, shadowDatePreview);
        loadPositionSpinner(spinnerDatePosition, SettingsManager.KEY_DATE_GRAVITY);
        dateFormatKeys = getResources().getStringArray(R.array.date_format_options_array_keys);
        ArrayAdapter<CharSequence> dateFormatAdapter = ArrayAdapter.createFromResource(this, R.array.date_format_options_array_values, R.layout.custom_spinner_item);
        dateFormatAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDateFormat.setAdapter(dateFormatAdapter);
        String currentFormat = sharedPreferences.getString(SettingsManager.KEY_DATE_FORMAT, dateFormatKeys[0]);
        for (int i = 0; i < dateFormatKeys.length; i++) {
            if (dateFormatKeys[i].equals(currentFormat)) {
                spinnerDateFormat.setSelection(i);
                break;
            }
        }

        // Wetter
        switchWeatherEnabled.setChecked(sharedPreferences.getBoolean(SettingsManager.KEY_WEATHER_ENABLED, false));
        btnWeatherFineTune.setEnabled(switchWeatherEnabled.isChecked());
        editWeatherCity.setText(sharedPreferences.getString(SettingsManager.KEY_WEATHER_CITY, ""));
        switchWeatherFahrenheit.setChecked(sharedPreferences.getBoolean(SettingsManager.KEY_WEATHER_FAHRENHEIT, false));
        loadAppearanceSettings("weather", seekbarWeatherSize, textviewWeatherSizeValue, colorWeatherPreview, shadowWeatherPreview);
        loadPositionSpinner(spinnerWeatherPosition, SettingsManager.KEY_WEATHER_GRAVITY);
    }

    private void setupClockListeners() {
        switchClockEnabled.setOnCheckedChangeListener((v, isChecked) -> handleEnableSwitch(isChecked, ClockService.class, SettingsManager.KEY_CLOCK_ENABLED, btnClockFineTune));
        switchClockShowSeconds.setOnCheckedChangeListener((v, isChecked) -> {
            getEditor().putBoolean(SettingsManager.KEY_CLOCK_SHOW_SECONDS, isChecked).apply();
            restartServiceIfRunning(ClockService.class, SettingsManager.KEY_CLOCK_ENABLED);
        });
        switchClock24h.setOnCheckedChangeListener((v, isChecked) -> {
            getEditor().putBoolean(SettingsManager.KEY_CLOCK_24H_FORMAT, isChecked).apply();
            restartServiceIfRunning(ClockService.class, SettingsManager.KEY_CLOCK_ENABLED);
        });
        containerClockPosition.setOnClickListener(v -> spinnerClockPosition.performClick());
        setupPositionSpinnerListener(spinnerClockPosition, SettingsManager.KEY_CLOCK_GRAVITY, ClockService.class, SettingsManager.KEY_CLOCK_ENABLED);
        setupAppearanceListeners("clock", seekbarClockSize, textviewClockSizeValue, containerClockColor, containerClockShadowColor, ClockService.class, SettingsManager.KEY_CLOCK_ENABLED);
        btnClockFineTune.setOnClickListener(v -> startFineTune(ClockService.class));
    }

    private void setupDateListeners() {
        switchDateEnabled.setOnCheckedChangeListener((v, isChecked) -> handleEnableSwitch(isChecked, DateService.class, SettingsManager.KEY_DATE_ENABLED, btnDateFineTune));
        containerDateFormat.setOnClickListener(v -> spinnerDateFormat.performClick());
        spinnerDateFormat.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedFormat = dateFormatKeys[position];
                if (!selectedFormat.equals(sharedPreferences.getString(SettingsManager.KEY_DATE_FORMAT, ""))) {
                    getEditor().putString(SettingsManager.KEY_DATE_FORMAT, selectedFormat).apply();
                    restartServiceIfRunning(DateService.class, SettingsManager.KEY_DATE_ENABLED);
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        containerDatePosition.setOnClickListener(v -> spinnerDatePosition.performClick());
        setupPositionSpinnerListener(spinnerDatePosition, SettingsManager.KEY_DATE_GRAVITY, DateService.class, SettingsManager.KEY_DATE_ENABLED);
        setupAppearanceListeners("date", seekbarDateSize, textviewDateSizeValue, containerDateColor, containerDateShadowColor, DateService.class, SettingsManager.KEY_DATE_ENABLED);
        btnDateFineTune.setOnClickListener(v -> startFineTune(DateService.class));
    }

    private void setupWeatherListeners() {
        switchWeatherEnabled.setOnCheckedChangeListener((v, isChecked) -> handleEnableSwitch(isChecked, WeatherService.class, SettingsManager.KEY_WEATHER_ENABLED, btnWeatherFineTune));
        switchWeatherFahrenheit.setOnCheckedChangeListener((v, isChecked) -> {
            getEditor().putBoolean(SettingsManager.KEY_WEATHER_FAHRENHEIT, isChecked).apply();
            restartServiceIfRunning(WeatherService.class, SettingsManager.KEY_WEATHER_ENABLED);
        });

        // HIER STARTET DIE NEUE LOGIK
        containerWeatherCity.setOnClickListener(v -> {
            editWeatherCity.setFocusable(true);
            editWeatherCity.setFocusableInTouchMode(true);
            editWeatherCity.requestFocus();
            editWeatherCity.setCursorVisible(true);
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(editWeatherCity, InputMethodManager.SHOW_IMPLICIT);
        });

        editWeatherCity.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) { // Wird aufgerufen, wenn das Feld den Fokus verliert
                // Speichern der Stadt
                String newCity = editWeatherCity.getText().toString().trim();
                String oldCity = sharedPreferences.getString(SettingsManager.KEY_WEATHER_CITY, "");
                if (!newCity.equals(oldCity)) {
                    getEditor().putString(SettingsManager.KEY_WEATHER_CITY, newCity).commit();
                    restartServiceIfRunning(WeatherService.class, SettingsManager.KEY_WEATHER_ENABLED);
                }
                // UI zurücksetzen
                editWeatherCity.setFocusable(false);
                editWeatherCity.setFocusableInTouchMode(false);
                editWeatherCity.setCursorVisible(false);
            }
        });

        editWeatherCity.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                // Fokus manuell entziehen, um den OnFocusChangeListener zu triggern
                containerWeatherCity.requestFocus();
                // Tastatur ausblenden
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                return true;
            }
            return false;
        });
        // HIER ENDET DIE NEUE LOGIK

        containerWeatherPosition.setOnClickListener(v -> spinnerWeatherPosition.performClick());
        setupPositionSpinnerListener(spinnerWeatherPosition, SettingsManager.KEY_WEATHER_GRAVITY, WeatherService.class, SettingsManager.KEY_WEATHER_ENABLED);
        setupAppearanceListeners("weather", seekbarWeatherSize, textviewWeatherSizeValue, containerWeatherColor, containerWeatherShadowColor, WeatherService.class, SettingsManager.KEY_WEATHER_ENABLED);
        btnWeatherFineTune.setOnClickListener(v -> startFineTune(WeatherService.class));
    }


    private void handleEnableSwitch(boolean isChecked, Class<?> serviceClass, String enabledKey, Button fineTuneButton) {
        getEditor().putBoolean(enabledKey, isChecked).apply();
        fineTuneButton.setEnabled(isChecked);
        if (isChecked) {
            if (checkOverlayPermission()) {
                startService(new Intent(this, serviceClass));
            } else {
                requestOverlayPermission();
            }
        } else {
            stopService(new Intent(this, serviceClass));
        }
    }

    private void loadPositionSpinner(Spinner spinner, String gravityKey) {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.position_options_array, R.layout.custom_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        int gravity = sharedPreferences.getInt(gravityKey, Gravity.TOP | Gravity.END);
        spinner.setSelection(getSpinnerIndexForGravity(gravity));
    }

    private void setupPositionSpinnerListener(Spinner spinner, String gravityKey, Class<?> serviceClass, String enabledKey) {
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int gravity = getGravityForSpinnerIndex(position);
                if (sharedPreferences.getInt(gravityKey, -1) != gravity) {
                    getEditor().putInt(gravityKey, gravity).apply();
                    restartServiceIfRunning(serviceClass, enabledKey);
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadAppearanceSettings(String prefix, SeekBar seekBar, TextView sizeValue, View colorPreview, View shadowPreview) {
        String sizeKey, colorKey, shadowKey;
        if ("clock".equals(prefix)) {
            sizeKey = SettingsManager.KEY_CLOCK_SIZE;
            colorKey = SettingsManager.KEY_CLOCK_COLOR;
            shadowKey = SettingsManager.KEY_CLOCK_SHADOW_COLOR;
        } else if ("date".equals(prefix)) {
            sizeKey = SettingsManager.KEY_DATE_SIZE;
            colorKey = SettingsManager.KEY_DATE_COLOR;
            shadowKey = SettingsManager.KEY_DATE_SHADOW_COLOR;
        } else { // weather
            sizeKey = SettingsManager.KEY_WEATHER_SIZE;
            colorKey = SettingsManager.KEY_WEATHER_COLOR;
            shadowKey = SettingsManager.KEY_WEATHER_SHADOW_COLOR;
        }
        int actualSize = sharedPreferences.getInt(sizeKey, DEFAULT_SIZE);
        seekBar.setProgress(actualSize - MIN_SIZE);
        sizeValue.setText(getString(R.string.setting_size_display, actualSize));
        colorPreview.setBackgroundColor(sharedPreferences.getInt(colorKey, ContextCompat.getColor(this, R.color.white)));
        shadowPreview.setBackgroundColor(sharedPreferences.getInt(shadowKey, ContextCompat.getColor(this, R.color.black)));
    }

    private void setupAppearanceListeners(String prefix, SeekBar seekBar, TextView sizeValue, View colorContainer, View shadowContainer, Class<?> serviceClass, String enabledKey) {
        String sizeKey;
        if ("clock".equals(prefix)) {
            sizeKey = SettingsManager.KEY_CLOCK_SIZE;
        } else if ("date".equals(prefix)) {
            sizeKey = SettingsManager.KEY_DATE_SIZE;
        } else { // weather
            sizeKey = SettingsManager.KEY_WEATHER_SIZE;
        }

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    int currentSize = MIN_SIZE + progress;
                    sizeValue.setText(getString(R.string.setting_size_display, currentSize));
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        getEditor().putInt(sizeKey, currentSize).apply();
                        restartServiceIfRunning(serviceClass, enabledKey);
                    }, 300);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        colorContainer.setOnClickListener(v -> {
            currentlyPickingColorFor = prefix;
            showColorPickerDialog("color");
        });
        shadowContainer.setOnClickListener(v -> {
            currentlyPickingColorFor = prefix;
            showColorPickerDialog("shadow");
        });
    }

    @Override
    public void onColorSelected(int color, String tag) {
        String colorKey, enabledKey;
        Class<?> serviceClass;
        switch (currentlyPickingColorFor) {
            case "date":
                colorKey = "color".equals(tag) ? SettingsManager.KEY_DATE_COLOR : SettingsManager.KEY_DATE_SHADOW_COLOR;
                serviceClass = DateService.class;
                enabledKey = SettingsManager.KEY_DATE_ENABLED;
                break;
            case "weather":
                colorKey = "color".equals(tag) ? SettingsManager.KEY_WEATHER_COLOR : SettingsManager.KEY_WEATHER_SHADOW_COLOR;
                serviceClass = WeatherService.class;
                enabledKey = SettingsManager.KEY_WEATHER_ENABLED;
                break;
            default: // clock
                colorKey = "color".equals(tag) ? SettingsManager.KEY_CLOCK_COLOR : SettingsManager.KEY_CLOCK_SHADOW_COLOR;
                serviceClass = ClockService.class;
                enabledKey = SettingsManager.KEY_CLOCK_ENABLED;
                break;
        }
        getEditor().putInt(colorKey, color).apply();
        loadAllSettings();
        restartServiceIfRunning(serviceClass, enabledKey);
    }

    private void showColorPickerDialog(String tag) {
        ColorPickerDialogFragment dialog = ColorPickerDialogFragment.newInstance(tag);
        dialog.setColorPickerListener(this);
        dialog.show(getSupportFragmentManager(), tag);
    }

    private void startFineTune(Class<?> serviceClass) {
        Intent intent = new Intent(this, serviceClass);
        intent.setAction(SettingsManager.ACTION_TOGGLE_POSITIONING_MODE);
        startService(intent);
    }

    private void restartServiceIfRunning(Class<?> serviceClass, String enabledKey) {
        if (sharedPreferences.getBoolean(enabledKey, false)) {
            stopService(new Intent(this, serviceClass));
            new Handler(Looper.getMainLooper()).postDelayed(() -> startService(new Intent(this, serviceClass)), 200);
        }
    }

    private final ActivityResultLauncher<Intent> overlayPermissionLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (!checkOverlayPermission()) {
            Toast.makeText(this, "Overlay-Berechtigung wurde nicht erteilt.", Toast.LENGTH_LONG).show();
        }
    });

    private boolean checkOverlayPermission() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this);
    }

    private void requestOverlayPermission() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
        overlayPermissionLauncher.launch(intent);
    }

    private SharedPreferences.Editor getEditor() {
        return sharedPreferences.edit();
    }

    private int getGravityForSpinnerIndex(int index) {
        switch (index) {
            case 1: return Gravity.TOP | Gravity.START;
            case 2: return Gravity.BOTTOM | Gravity.END;
            case 3: return Gravity.BOTTOM | Gravity.START;
            default: return Gravity.TOP | Gravity.END;
        }
    }

    private int getSpinnerIndexForGravity(int gravity) {
        if (gravity == (Gravity.TOP | Gravity.START)) return 1;
        if (gravity == (Gravity.BOTTOM | Gravity.END)) return 2;
        if (gravity == (Gravity.BOTTOM | Gravity.START)) return 3;
        return 0;
    }

    private void checkAndStartServices() {
        if (sharedPreferences.getBoolean(SettingsManager.KEY_CLOCK_ENABLED, false)) {
            startService(new Intent(this, ClockService.class));
        }
        if (sharedPreferences.getBoolean(SettingsManager.KEY_DATE_ENABLED, false)) {
            startService(new Intent(this, DateService.class));
        }
        if (sharedPreferences.getBoolean(SettingsManager.KEY_WEATHER_ENABLED, false)) {
            startService(new Intent(this, WeatherService.class));
        }
    }
}