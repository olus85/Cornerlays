package app.olus.cornerlays.settings;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import app.olus.cornerlays.ClockService;
import app.olus.cornerlays.ColorPickerDialogFragment;
import app.olus.cornerlays.DateService;
import app.olus.cornerlays.R;
import app.olus.cornerlays.SettingsManager;
import app.olus.cornerlays.settings.ClockDateSettingsFragment;

/* loaded from: classes6.dex */
public class ClockDateSettingsFragment extends BaseSettingsFragment implements ColorPickerDialogFragment.ColorPickerListener {
    private static final int DEFAULT_SIZE = 22;
    private static final int MIN_SIZE = 14;
    private View btnClockFineTune;
    private View btnDateFineTune;
    private View colorClockPreview;
    private View colorDatePreview;
    private RelativeLayout containerClockColor;
    private View containerClockPosition;
    private View containerClockSettings;
    private RelativeLayout containerClockShadowColor;
    private RelativeLayout containerDateColor;
    private View containerDateFormat;
    private View containerDatePosition;
    private View containerDateSettings;
    private RelativeLayout containerDateShadowColor;
    private String[] dateFormatKeys;
    private SeekBar seekbarClockSize;
    private SeekBar seekbarDateSize;
    private View shadowClockPreview;
    private View shadowDatePreview;
    private Spinner spinnerClockPosition;
    private Spinner spinnerDateFormat;
    private Spinner spinnerDatePosition;
    private SwitchCompat switchClock24h;
    private SwitchCompat switchClockEnabled;
    private SwitchCompat switchClockShowSeconds;
    private SwitchCompat switchDateEnabled;
    private TextView textviewClockSizeValue;
    private TextView textviewDateSizeValue;

    @Override // androidx.fragment.app.Fragment
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings_clock_date, container, false);
    }

    @Override // androidx.fragment.app.Fragment
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        loadSettings();
        setupListeners();
    }

    private void initViews(View view) {
        this.switchClockEnabled = (SwitchCompat) view.findViewById(R.id.switch_clock_enabled);
        this.switchClockShowSeconds = (SwitchCompat) view.findViewById(R.id.switch_clock_show_seconds);
        this.switchClock24h = (SwitchCompat) view.findViewById(R.id.switch_clock_24h);
        this.containerClockPosition = view.findViewById(R.id.container_clock_position);
        this.spinnerClockPosition = (Spinner) view.findViewById(R.id.spinner_clock_position);
        this.btnClockFineTune = view.findViewById(R.id.btn_clock_fine_tune);
        this.seekbarClockSize = (SeekBar) view.findViewById(R.id.seekbar_clock_size);
        this.textviewClockSizeValue = (TextView) view.findViewById(R.id.textview_clock_size_value);
        this.containerClockColor = (RelativeLayout) view.findViewById(R.id.container_clock_color);
        this.colorClockPreview = view.findViewById(R.id.color_clock_preview);
        this.containerClockShadowColor = (RelativeLayout) view.findViewById(R.id.container_clock_shadow_color);
        this.shadowClockPreview = view.findViewById(R.id.shadow_clock_color_preview);
        this.containerClockSettings = view.findViewById(R.id.container_clock_settings);
        this.switchDateEnabled = (SwitchCompat) view.findViewById(R.id.switch_date_enabled);
        this.containerDateFormat = view.findViewById(R.id.container_date_format);
        this.spinnerDateFormat = (Spinner) view.findViewById(R.id.spinner_date_format);
        this.containerDatePosition = view.findViewById(R.id.container_date_position);
        this.spinnerDatePosition = (Spinner) view.findViewById(R.id.spinner_date_position);
        this.btnDateFineTune = view.findViewById(R.id.btn_date_fine_tune);
        this.seekbarDateSize = (SeekBar) view.findViewById(R.id.seekbar_date_size);
        this.textviewDateSizeValue = (TextView) view.findViewById(R.id.textview_date_size_value);
        this.containerDateColor = (RelativeLayout) view.findViewById(R.id.container_date_color);
        this.colorDatePreview = view.findViewById(R.id.color_date_preview);
        this.containerDateShadowColor = (RelativeLayout) view.findViewById(R.id.container_date_shadow_color);
        this.shadowDatePreview = view.findViewById(R.id.shadow_date_color_preview);
        this.containerDateSettings = view.findViewById(R.id.container_date_settings);
    }

    private void loadSettings() {
        this.switchClockEnabled.setChecked(this.sharedPreferences.getBoolean(SettingsManager.KEY_CLOCK_ENABLED, false));
        this.btnClockFineTune.setEnabled(this.switchClockEnabled.isChecked());
        this.switchClockShowSeconds.setChecked(this.sharedPreferences.getBoolean(SettingsManager.KEY_CLOCK_SHOW_SECONDS, true));
        this.switchClock24h.setChecked(this.sharedPreferences.getBoolean(SettingsManager.KEY_CLOCK_24H_FORMAT, true));
        loadPositionSpinner(this.spinnerClockPosition, SettingsManager.KEY_CLOCK_GRAVITY);
        int clockSize = this.sharedPreferences.getInt(SettingsManager.KEY_CLOCK_SIZE, 22);
        this.seekbarClockSize.setProgress(clockSize - 14);
        this.textviewClockSizeValue.setText(getString(R.string.setting_size_display, Integer.valueOf(clockSize)));
        this.colorClockPreview.setBackgroundColor(this.sharedPreferences.getInt(SettingsManager.KEY_CLOCK_COLOR, ContextCompat.getColor(requireContext(), R.color.white)));
        this.shadowClockPreview.setBackgroundColor(this.sharedPreferences.getInt(SettingsManager.KEY_CLOCK_SHADOW_COLOR, ContextCompat.getColor(requireContext(), R.color.black)));
        this.switchDateEnabled.setChecked(this.sharedPreferences.getBoolean(SettingsManager.KEY_DATE_ENABLED, false));
        this.btnDateFineTune.setEnabled(this.switchDateEnabled.isChecked());
        this.dateFormatKeys = getResources().getStringArray(R.array.date_format_options_array_keys);
        ArrayAdapter<CharSequence> dateFormatAdapter = ArrayAdapter.createFromResource(requireContext(), R.array.date_format_options_array_values, R.layout.custom_spinner_item);
        dateFormatAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this.spinnerDateFormat.setAdapter((SpinnerAdapter) dateFormatAdapter);
        String currentFormat = this.sharedPreferences.getString(SettingsManager.KEY_DATE_FORMAT, this.dateFormatKeys[0]);
        int i = 0;
        while (true) {
            if (i >= this.dateFormatKeys.length) {
                break;
            }
            if (!this.dateFormatKeys[i].equals(currentFormat)) {
                i++;
            } else {
                this.spinnerDateFormat.setSelection(i);
                break;
            }
        }
        loadPositionSpinner(this.spinnerDatePosition, SettingsManager.KEY_DATE_GRAVITY);
        int dateSize = this.sharedPreferences.getInt(SettingsManager.KEY_DATE_SIZE, 22);
        this.seekbarDateSize.setProgress(dateSize - 14);
        this.textviewDateSizeValue.setText(getString(R.string.setting_size_display, Integer.valueOf(dateSize)));
        this.colorDatePreview.setBackgroundColor(this.sharedPreferences.getInt(SettingsManager.KEY_DATE_COLOR, ContextCompat.getColor(requireContext(), R.color.white)));
        this.shadowDatePreview.setBackgroundColor(this.sharedPreferences.getInt(SettingsManager.KEY_DATE_SHADOW_COLOR, ContextCompat.getColor(requireContext(), R.color.black)));
        this.containerClockSettings.setVisibility(this.switchClockEnabled.isChecked() ? 0 : 8);
        this.containerDateSettings.setVisibility(this.switchDateEnabled.isChecked() ? 0 : 8);
    }

    private void setupListeners() {
        this.switchClockEnabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { // from class: app.olus.cornerlays.settings.ClockDateSettingsFragment$$ExternalSyntheticLambda0
            @Override // android.widget.CompoundButton.OnCheckedChangeListener
            public final void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                ClockDateSettingsFragment.this.lambda$setupListeners$0(compoundButton, z);
            }
        });
        this.switchClockShowSeconds.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { // from class: app.olus.cornerlays.settings.ClockDateSettingsFragment$$ExternalSyntheticLambda7
            @Override // android.widget.CompoundButton.OnCheckedChangeListener
            public final void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                ClockDateSettingsFragment.this.lambda$setupListeners$1(compoundButton, z);
            }
        });
        this.switchClock24h.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { // from class: app.olus.cornerlays.settings.ClockDateSettingsFragment$$ExternalSyntheticLambda8
            @Override // android.widget.CompoundButton.OnCheckedChangeListener
            public final void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                ClockDateSettingsFragment.this.lambda$setupListeners$2(compoundButton, z);
            }
        });
        this.containerClockPosition.setOnClickListener(new View.OnClickListener() { // from class: app.olus.cornerlays.settings.ClockDateSettingsFragment$$ExternalSyntheticLambda9
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                ClockDateSettingsFragment.this.lambda$setupListeners$3(view);
            }
        });
        this.spinnerClockPosition.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() { // from class: app.olus.cornerlays.settings.ClockDateSettingsFragment.1
            @Override // android.widget.AdapterView.OnItemSelectedListener
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int gravity = ClockDateSettingsFragment.this.getGravityForSpinnerIndex(position);
                if (ClockDateSettingsFragment.this.sharedPreferences.getInt(SettingsManager.KEY_CLOCK_GRAVITY, -1) != gravity) {
                    ClockDateSettingsFragment.this.getEditor().putInt(SettingsManager.KEY_CLOCK_GRAVITY, gravity).apply();
                    ClockDateSettingsFragment.this.restartServiceIfRunning(ClockService.class, SettingsManager.KEY_CLOCK_ENABLED);
                }
            }

            @Override // android.widget.AdapterView.OnItemSelectedListener
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        this.btnClockFineTune.setOnClickListener(new View.OnClickListener() { // from class: app.olus.cornerlays.settings.ClockDateSettingsFragment$$ExternalSyntheticLambda10
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                ClockDateSettingsFragment.this.lambda$setupListeners$4(view);
            }
        });
        this.seekbarClockSize.setOnSeekBarChangeListener(new AnonymousClass2());
        this.containerClockColor.setOnClickListener(new View.OnClickListener() { // from class: app.olus.cornerlays.settings.ClockDateSettingsFragment$$ExternalSyntheticLambda11
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                ClockDateSettingsFragment.this.lambda$setupListeners$5(view);
            }
        });
        this.containerClockShadowColor.setOnClickListener(new View.OnClickListener() { // from class: app.olus.cornerlays.settings.ClockDateSettingsFragment$$ExternalSyntheticLambda12
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                ClockDateSettingsFragment.this.lambda$setupListeners$6(view);
            }
        });
        this.switchDateEnabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { // from class: app.olus.cornerlays.settings.ClockDateSettingsFragment$$ExternalSyntheticLambda1
            @Override // android.widget.CompoundButton.OnCheckedChangeListener
            public final void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                ClockDateSettingsFragment.this.lambda$setupListeners$7(compoundButton, z);
            }
        });
        this.containerDateFormat.setOnClickListener(new View.OnClickListener() { // from class: app.olus.cornerlays.settings.ClockDateSettingsFragment$$ExternalSyntheticLambda2
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                ClockDateSettingsFragment.this.lambda$setupListeners$8(view);
            }
        });
        this.spinnerDateFormat.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() { // from class: app.olus.cornerlays.settings.ClockDateSettingsFragment.3
            @Override // android.widget.AdapterView.OnItemSelectedListener
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedFormat = ClockDateSettingsFragment.this.dateFormatKeys[position];
                if (!selectedFormat.equals(ClockDateSettingsFragment.this.sharedPreferences.getString(SettingsManager.KEY_DATE_FORMAT, ""))) {
                    ClockDateSettingsFragment.this.getEditor().putString(SettingsManager.KEY_DATE_FORMAT, selectedFormat).apply();
                    ClockDateSettingsFragment.this.restartServiceIfRunning(DateService.class, SettingsManager.KEY_DATE_ENABLED);
                }
            }

            @Override // android.widget.AdapterView.OnItemSelectedListener
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        this.containerDatePosition.setOnClickListener(new View.OnClickListener() { // from class: app.olus.cornerlays.settings.ClockDateSettingsFragment$$ExternalSyntheticLambda3
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                ClockDateSettingsFragment.this.lambda$setupListeners$9(view);
            }
        });
        this.spinnerDatePosition.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() { // from class: app.olus.cornerlays.settings.ClockDateSettingsFragment.4
            @Override // android.widget.AdapterView.OnItemSelectedListener
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int gravity = ClockDateSettingsFragment.this.getGravityForSpinnerIndex(position);
                if (ClockDateSettingsFragment.this.sharedPreferences.getInt(SettingsManager.KEY_DATE_GRAVITY, -1) != gravity) {
                    ClockDateSettingsFragment.this.getEditor().putInt(SettingsManager.KEY_DATE_GRAVITY, gravity).apply();
                    ClockDateSettingsFragment.this.restartServiceIfRunning(DateService.class, SettingsManager.KEY_DATE_ENABLED);
                }
            }

            @Override // android.widget.AdapterView.OnItemSelectedListener
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        this.btnDateFineTune.setOnClickListener(new View.OnClickListener() { // from class: app.olus.cornerlays.settings.ClockDateSettingsFragment$$ExternalSyntheticLambda4
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                ClockDateSettingsFragment.this.lambda$setupListeners$10(view);
            }
        });
        this.seekbarDateSize.setOnSeekBarChangeListener(new AnonymousClass5());
        this.containerDateColor.setOnClickListener(new View.OnClickListener() { // from class: app.olus.cornerlays.settings.ClockDateSettingsFragment$$ExternalSyntheticLambda5
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                ClockDateSettingsFragment.this.lambda$setupListeners$11(view);
            }
        });
        this.containerDateShadowColor.setOnClickListener(new View.OnClickListener() { // from class: app.olus.cornerlays.settings.ClockDateSettingsFragment$$ExternalSyntheticLambda6
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                ClockDateSettingsFragment.this.lambda$setupListeners$12(view);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupListeners$0(CompoundButton v, boolean isChecked) {
        getEditor().putBoolean(SettingsManager.KEY_CLOCK_ENABLED, isChecked).apply();
        this.btnClockFineTune.setEnabled(isChecked);
        this.containerClockSettings.setVisibility(isChecked ? 0 : 8);
        if (isChecked) {
            if (Settings.canDrawOverlays(getContext())) {
                requireActivity().startService(new Intent(getActivity(), (Class<?>) ClockService.class));
                return;
            }
            return;
        }
        requireActivity().stopService(new Intent(getActivity(), (Class<?>) ClockService.class));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupListeners$1(CompoundButton v, boolean isChecked) {
        getEditor().putBoolean(SettingsManager.KEY_CLOCK_SHOW_SECONDS, isChecked).apply();
        restartServiceIfRunning(ClockService.class, SettingsManager.KEY_CLOCK_ENABLED);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupListeners$2(CompoundButton v, boolean isChecked) {
        getEditor().putBoolean(SettingsManager.KEY_CLOCK_24H_FORMAT, isChecked).apply();
        restartServiceIfRunning(ClockService.class, SettingsManager.KEY_CLOCK_ENABLED);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupListeners$3(View v) {
        this.spinnerClockPosition.performClick();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupListeners$4(View v) {
        Intent intent = new Intent(getActivity(), (Class<?>) ClockService.class);
        intent.setAction(SettingsManager.ACTION_TOGGLE_POSITIONING_MODE);
        requireActivity().startService(intent);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: app.olus.cornerlays.settings.ClockDateSettingsFragment$2, reason: invalid class name */
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
                ClockDateSettingsFragment.this.textviewClockSizeValue.setText(ClockDateSettingsFragment.this.getString(R.string.setting_size_display, Integer.valueOf(currentSize)));
                if (this.runnable != null) {
                    this.handler.removeCallbacks(this.runnable);
                }
                this.runnable = new Runnable() { // from class: app.olus.cornerlays.settings.ClockDateSettingsFragment$2$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        ClockDateSettingsFragment.AnonymousClass2.this.lambda$onProgressChanged$0(currentSize);
                    }
                };
                this.handler.postDelayed(this.runnable, 300L);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onProgressChanged$0(int currentSize) {
            ClockDateSettingsFragment.this.getEditor().putInt(SettingsManager.KEY_CLOCK_SIZE, currentSize).apply();
            ClockDateSettingsFragment.this.restartServiceIfRunning(ClockService.class, SettingsManager.KEY_CLOCK_ENABLED);
        }

        @Override // android.widget.SeekBar.OnSeekBarChangeListener
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override // android.widget.SeekBar.OnSeekBarChangeListener
        public void onStopTrackingTouch(SeekBar seekBar) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupListeners$5(View v) {
        showColorPickerDialog(SettingsManager.KEY_CLOCK_COLOR);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupListeners$6(View v) {
        showColorPickerDialog(SettingsManager.KEY_CLOCK_SHADOW_COLOR);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupListeners$7(CompoundButton v, boolean isChecked) {
        getEditor().putBoolean(SettingsManager.KEY_DATE_ENABLED, isChecked).apply();
        this.btnDateFineTune.setEnabled(isChecked);
        this.containerDateSettings.setVisibility(isChecked ? 0 : 8);
        if (isChecked) {
            if (Settings.canDrawOverlays(getContext())) {
                requireActivity().startService(new Intent(getActivity(), (Class<?>) DateService.class));
                return;
            }
            return;
        }
        requireActivity().stopService(new Intent(getActivity(), (Class<?>) DateService.class));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupListeners$8(View v) {
        this.spinnerDateFormat.performClick();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupListeners$9(View v) {
        this.spinnerDatePosition.performClick();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupListeners$10(View v) {
        Intent intent = new Intent(getActivity(), (Class<?>) DateService.class);
        intent.setAction(SettingsManager.ACTION_TOGGLE_POSITIONING_MODE);
        requireActivity().startService(intent);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: app.olus.cornerlays.settings.ClockDateSettingsFragment$5, reason: invalid class name */
    /* loaded from: classes6.dex */
    public class AnonymousClass5 implements SeekBar.OnSeekBarChangeListener {
        private final Handler handler = new Handler(Looper.getMainLooper());
        private Runnable runnable;

        AnonymousClass5() {
        }

        @Override // android.widget.SeekBar.OnSeekBarChangeListener
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) {
                final int currentSize = progress + 14;
                ClockDateSettingsFragment.this.textviewDateSizeValue.setText(ClockDateSettingsFragment.this.getString(R.string.setting_size_display, Integer.valueOf(currentSize)));
                if (this.runnable != null) {
                    this.handler.removeCallbacks(this.runnable);
                }
                this.runnable = new Runnable() { // from class: app.olus.cornerlays.settings.ClockDateSettingsFragment$5$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        ClockDateSettingsFragment.AnonymousClass5.this.lambda$onProgressChanged$0(currentSize);
                    }
                };
                this.handler.postDelayed(this.runnable, 300L);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onProgressChanged$0(int currentSize) {
            ClockDateSettingsFragment.this.getEditor().putInt(SettingsManager.KEY_DATE_SIZE, currentSize).apply();
            ClockDateSettingsFragment.this.restartServiceIfRunning(DateService.class, SettingsManager.KEY_DATE_ENABLED);
        }

        @Override // android.widget.SeekBar.OnSeekBarChangeListener
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override // android.widget.SeekBar.OnSeekBarChangeListener
        public void onStopTrackingTouch(SeekBar seekBar) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupListeners$11(View v) {
        showColorPickerDialog(SettingsManager.KEY_DATE_COLOR);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupListeners$12(View v) {
        showColorPickerDialog(SettingsManager.KEY_DATE_SHADOW_COLOR);
    }

    private void showColorPickerDialog(String tag) {
        ColorPickerDialogFragment dialog = ColorPickerDialogFragment.newInstance(tag);
        dialog.setTargetFragment(this, 0);
        dialog.show(getParentFragmentManager(), tag);
    }

    @Override // app.olus.cornerlays.ColorPickerDialogFragment.ColorPickerListener
    public void onColorSelected(int color, String tag) {
        if (SettingsManager.KEY_CLOCK_COLOR.equals(tag)) {
            getEditor().putInt(SettingsManager.KEY_CLOCK_COLOR, color).apply();
            this.colorClockPreview.setBackgroundColor(color);
            restartServiceIfRunning(ClockService.class, SettingsManager.KEY_CLOCK_ENABLED);
            return;
        }
        if (SettingsManager.KEY_CLOCK_SHADOW_COLOR.equals(tag)) {
            getEditor().putInt(SettingsManager.KEY_CLOCK_SHADOW_COLOR, color).apply();
            this.shadowClockPreview.setBackgroundColor(color);
            restartServiceIfRunning(ClockService.class, SettingsManager.KEY_CLOCK_ENABLED);
        } else if (SettingsManager.KEY_DATE_COLOR.equals(tag)) {
            getEditor().putInt(SettingsManager.KEY_DATE_COLOR, color).apply();
            this.colorDatePreview.setBackgroundColor(color);
            restartServiceIfRunning(DateService.class, SettingsManager.KEY_DATE_ENABLED);
        } else if (SettingsManager.KEY_DATE_SHADOW_COLOR.equals(tag)) {
            getEditor().putInt(SettingsManager.KEY_DATE_SHADOW_COLOR, color).apply();
            this.shadowDatePreview.setBackgroundColor(color);
            restartServiceIfRunning(DateService.class, SettingsManager.KEY_DATE_ENABLED);
        }
    }
}
