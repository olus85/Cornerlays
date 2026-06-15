package app.olus.cornerlays.settings;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import app.olus.cornerlays.ColorPickerDialogFragment;
import app.olus.cornerlays.R;
import app.olus.cornerlays.SettingsManager;
import app.olus.cornerlays.WeatherService;
import app.olus.cornerlays.settings.WeatherSettingsFragment;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/* loaded from: classes6.dex */
public class WeatherSettingsFragment extends BaseSettingsFragment implements ColorPickerDialogFragment.ColorPickerListener {
    private static final int DEFAULT_SIZE = 22;
    private static final int MIN_SIZE = 14;
    private View btnWeatherFineTune;
    private View colorWeatherPreview;
    private TextInputLayout containerWeatherCity;
    private RelativeLayout containerWeatherColor;
    private View containerWeatherPosition;
    private View containerWeatherSettings;
    private RelativeLayout containerWeatherShadowColor;
    private TextInputEditText editWeatherCity;
    private SeekBar seekbarWeatherSize;
    private View shadowWeatherPreview;
    private Spinner spinnerWeatherPosition;
    private SwitchCompat switchWeatherEnabled;
    private SwitchCompat switchWeatherFahrenheit;
    private TextView textviewWeatherSizeValue;

    @Override // androidx.fragment.app.Fragment
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings_weather, container, false);
    }

    @Override // androidx.fragment.app.Fragment
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        loadSettings();
        setupListeners();
    }

    private void initViews(View view) {
        this.switchWeatherEnabled = (SwitchCompat) view.findViewById(R.id.switch_weather_enabled);
        this.containerWeatherCity = (TextInputLayout) view.findViewById(R.id.container_weather_city);
        this.editWeatherCity = (TextInputEditText) view.findViewById(R.id.edit_weather_city);
        this.switchWeatherFahrenheit = (SwitchCompat) view.findViewById(R.id.switch_weather_fahrenheit);
        this.containerWeatherPosition = view.findViewById(R.id.container_weather_position);
        this.spinnerWeatherPosition = (Spinner) view.findViewById(R.id.spinner_weather_position);
        this.btnWeatherFineTune = view.findViewById(R.id.btn_weather_fine_tune);
        this.seekbarWeatherSize = (SeekBar) view.findViewById(R.id.seekbar_size);
        this.textviewWeatherSizeValue = (TextView) view.findViewById(R.id.textview_size_value);
        this.containerWeatherColor = (RelativeLayout) view.findViewById(R.id.container_color);
        this.colorWeatherPreview = view.findViewById(R.id.color_preview);
        this.containerWeatherShadowColor = (RelativeLayout) view.findViewById(R.id.container_shadow_color);
        this.shadowWeatherPreview = view.findViewById(R.id.shadow_color_preview);
        this.containerWeatherSettings = view.findViewById(R.id.container_weather_settings);
    }

    private void loadSettings() {
        this.switchWeatherEnabled.setChecked(this.sharedPreferences.getBoolean(SettingsManager.KEY_WEATHER_ENABLED, false));
        this.btnWeatherFineTune.setEnabled(this.switchWeatherEnabled.isChecked());
        this.editWeatherCity.setText(this.sharedPreferences.getString(SettingsManager.KEY_WEATHER_CITY, ""));
        this.switchWeatherFahrenheit.setChecked(this.sharedPreferences.getBoolean(SettingsManager.KEY_WEATHER_FAHRENHEIT, false));
        loadPositionSpinner(this.spinnerWeatherPosition, SettingsManager.KEY_WEATHER_GRAVITY);
        int size = this.sharedPreferences.getInt(SettingsManager.KEY_WEATHER_SIZE, 22);
        this.seekbarWeatherSize.setProgress(size - 14);
        this.textviewWeatherSizeValue.setText(getString(R.string.setting_size_display, Integer.valueOf(size)));
        this.colorWeatherPreview.setBackgroundColor(this.sharedPreferences.getInt(SettingsManager.KEY_WEATHER_COLOR, ContextCompat.getColor(requireContext(), R.color.white)));
        this.shadowWeatherPreview.setBackgroundColor(this.sharedPreferences.getInt(SettingsManager.KEY_WEATHER_SHADOW_COLOR, ContextCompat.getColor(requireContext(), R.color.black)));
        this.containerWeatherSettings.setVisibility(this.switchWeatherEnabled.isChecked() ? 0 : 8);
    }

    private void setupListeners() {
        this.switchWeatherEnabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { // from class: app.olus.cornerlays.settings.WeatherSettingsFragment$$ExternalSyntheticLambda0
            @Override // android.widget.CompoundButton.OnCheckedChangeListener
            public final void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                WeatherSettingsFragment.this.lambda$setupListeners$0(compoundButton, z);
            }
        });
        this.switchWeatherFahrenheit.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { // from class: app.olus.cornerlays.settings.WeatherSettingsFragment$$ExternalSyntheticLambda1
            @Override // android.widget.CompoundButton.OnCheckedChangeListener
            public final void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                WeatherSettingsFragment.this.lambda$setupListeners$1(compoundButton, z);
            }
        });
        this.containerWeatherCity.setOnClickListener(new View.OnClickListener() { // from class: app.olus.cornerlays.settings.WeatherSettingsFragment$$ExternalSyntheticLambda2
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                WeatherSettingsFragment.this.lambda$setupListeners$2(view);
            }
        });
        this.editWeatherCity.setOnFocusChangeListener(new View.OnFocusChangeListener() { // from class: app.olus.cornerlays.settings.WeatherSettingsFragment$$ExternalSyntheticLambda3
            @Override // android.view.View.OnFocusChangeListener
            public final void onFocusChange(View view, boolean z) {
                WeatherSettingsFragment.this.lambda$setupListeners$3(view, z);
            }
        });
        this.editWeatherCity.setOnEditorActionListener(new TextView.OnEditorActionListener() { // from class: app.olus.cornerlays.settings.WeatherSettingsFragment$$ExternalSyntheticLambda4
            @Override // android.widget.TextView.OnEditorActionListener
            public final boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                return WeatherSettingsFragment.lambda$setupListeners$4(textView, i, keyEvent);
            }
        });
        this.containerWeatherPosition.setOnClickListener(new View.OnClickListener() { // from class: app.olus.cornerlays.settings.WeatherSettingsFragment$$ExternalSyntheticLambda5
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                WeatherSettingsFragment.this.lambda$setupListeners$5(view);
            }
        });
        this.spinnerWeatherPosition.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() { // from class: app.olus.cornerlays.settings.WeatherSettingsFragment.1
            @Override // android.widget.AdapterView.OnItemSelectedListener
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int gravity = WeatherSettingsFragment.this.getGravityForSpinnerIndex(position);
                if (WeatherSettingsFragment.this.sharedPreferences.getInt(SettingsManager.KEY_WEATHER_GRAVITY, -1) != gravity) {
                    WeatherSettingsFragment.this.getEditor().putInt(SettingsManager.KEY_WEATHER_GRAVITY, gravity).apply();
                    WeatherSettingsFragment.this.restartServiceIfRunning(WeatherService.class, SettingsManager.KEY_WEATHER_ENABLED);
                }
            }

            @Override // android.widget.AdapterView.OnItemSelectedListener
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        this.btnWeatherFineTune.setOnClickListener(new View.OnClickListener() { // from class: app.olus.cornerlays.settings.WeatherSettingsFragment$$ExternalSyntheticLambda6
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                WeatherSettingsFragment.this.lambda$setupListeners$6(view);
            }
        });
        this.seekbarWeatherSize.setOnSeekBarChangeListener(new AnonymousClass2());
        this.containerWeatherColor.setOnClickListener(new View.OnClickListener() { // from class: app.olus.cornerlays.settings.WeatherSettingsFragment$$ExternalSyntheticLambda7
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                WeatherSettingsFragment.this.lambda$setupListeners$7(view);
            }
        });
        this.containerWeatherShadowColor.setOnClickListener(new View.OnClickListener() { // from class: app.olus.cornerlays.settings.WeatherSettingsFragment$$ExternalSyntheticLambda8
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                WeatherSettingsFragment.this.lambda$setupListeners$8(view);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupListeners$0(CompoundButton v, boolean isChecked) {
        getEditor().putBoolean(SettingsManager.KEY_WEATHER_ENABLED, isChecked).apply();
        this.btnWeatherFineTune.setEnabled(isChecked);
        this.containerWeatherSettings.setVisibility(isChecked ? 0 : 8);
        if (isChecked) {
            if (Settings.canDrawOverlays(getContext())) {
                requireActivity().startService(new Intent(getActivity(), (Class<?>) WeatherService.class));
                return;
            }
            return;
        }
        requireActivity().stopService(new Intent(getActivity(), (Class<?>) WeatherService.class));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupListeners$1(CompoundButton v, boolean isChecked) {
        getEditor().putBoolean(SettingsManager.KEY_WEATHER_FAHRENHEIT, isChecked).apply();
        restartServiceIfRunning(WeatherService.class, SettingsManager.KEY_WEATHER_ENABLED);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupListeners$2(View v) {
        this.editWeatherCity.setFocusableInTouchMode(true);
        this.editWeatherCity.requestFocus();
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService("input_method");
        imm.showSoftInput(this.editWeatherCity, 1);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupListeners$3(View v, boolean hasFocus) {
        if (!hasFocus) {
            String newCity = this.editWeatherCity.getText().toString().trim();
            getEditor().putString(SettingsManager.KEY_WEATHER_CITY, newCity).commit();
            restartServiceIfRunning(WeatherService.class, SettingsManager.KEY_WEATHER_ENABLED);
            this.editWeatherCity.setFocusable(false);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$setupListeners$4(TextView v, int actionId, KeyEvent event) {
        if (actionId == 6) {
            v.clearFocus();
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupListeners$5(View v) {
        this.spinnerWeatherPosition.performClick();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupListeners$6(View v) {
        Intent intent = new Intent(getActivity(), (Class<?>) WeatherService.class);
        intent.setAction(SettingsManager.ACTION_TOGGLE_POSITIONING_MODE);
        requireActivity().startService(intent);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: app.olus.cornerlays.settings.WeatherSettingsFragment$2, reason: invalid class name */
    /* loaded from: classes6.dex */
    public class AnonymousClass2 implements SeekBar.OnSeekBarChangeListener {
        private final Handler handler = new Handler(Looper.getMainLooper());
        private Runnable runnable;

        AnonymousClass2() {
        }

        @Override // android.widget.SeekBar.OnSeekBarChangeListener
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) {
                final int currentSize = progress + 14;
                WeatherSettingsFragment.this.textviewWeatherSizeValue.setText(WeatherSettingsFragment.this.getString(R.string.setting_size_display, Integer.valueOf(currentSize)));
                if (this.runnable != null) {
                    this.handler.removeCallbacks(this.runnable);
                }
                this.runnable = new Runnable() { // from class: app.olus.cornerlays.settings.WeatherSettingsFragment$2$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        WeatherSettingsFragment.AnonymousClass2.this.lambda$onProgressChanged$0(currentSize);
                    }
                };
                this.handler.postDelayed(this.runnable, 300L);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onProgressChanged$0(int currentSize) {
            WeatherSettingsFragment.this.getEditor().putInt(SettingsManager.KEY_WEATHER_SIZE, currentSize).apply();
            WeatherSettingsFragment.this.restartServiceIfRunning(WeatherService.class, SettingsManager.KEY_WEATHER_ENABLED);
        }

        @Override // android.widget.SeekBar.OnSeekBarChangeListener
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override // android.widget.SeekBar.OnSeekBarChangeListener
        public void onStopTrackingTouch(SeekBar seekBar) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupListeners$7(View v) {
        showColorPickerDialog(SettingsManager.KEY_WEATHER_COLOR);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupListeners$8(View v) {
        showColorPickerDialog(SettingsManager.KEY_WEATHER_SHADOW_COLOR);
    }

    private void showColorPickerDialog(String tag) {
        ColorPickerDialogFragment dialog = ColorPickerDialogFragment.newInstance(tag);
        dialog.setTargetFragment(this, 0);
        dialog.show(getParentFragmentManager(), tag);
    }

    @Override // app.olus.cornerlays.ColorPickerDialogFragment.ColorPickerListener
    public void onColorSelected(int color, String tag) {
        if (SettingsManager.KEY_WEATHER_COLOR.equals(tag)) {
            getEditor().putInt(SettingsManager.KEY_WEATHER_COLOR, color).apply();
            this.colorWeatherPreview.setBackgroundColor(color);
        } else if (SettingsManager.KEY_WEATHER_SHADOW_COLOR.equals(tag)) {
            getEditor().putInt(SettingsManager.KEY_WEATHER_SHADOW_COLOR, color).apply();
            this.shadowWeatherPreview.setBackgroundColor(color);
        }
        restartServiceIfRunning(WeatherService.class, SettingsManager.KEY_WEATHER_ENABLED);
    }
}
