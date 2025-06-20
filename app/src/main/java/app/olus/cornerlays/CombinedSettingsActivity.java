package app.olus.cornerlays;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import app.olus.cornerlays.ha.HomeAssistantService;
import app.olus.cornerlays.settings.SettingsFragmentAdapter;

public class CombinedSettingsActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private RadioGroup tabGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_combined_settings);

        viewPager = findViewById(R.id.settings_view_pager);
        tabGroup = findViewById(R.id.tab_group);

        SettingsFragmentAdapter adapter = new SettingsFragmentAdapter(this);
        viewPager.setAdapter(adapter);

        setupTabLogic();
        checkOverlayPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkAndStartServices();
    }

    private void setupTabLogic() {
        View.OnFocusChangeListener tabFocusListener = (v, hasFocus) -> {
            if (hasFocus && v instanceof RadioButton) {
                ((RadioButton) v).setChecked(true);
            }
        };

        for (int i = 0; i < tabGroup.getChildCount(); i++) {
            tabGroup.getChildAt(i).setOnFocusChangeListener(tabFocusListener);
        }

        tabGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.tab_clock) {
                viewPager.setCurrentItem(0, false);
            } else if (checkedId == R.id.tab_date) {
                viewPager.setCurrentItem(1, false);
            } else if (checkedId == R.id.tab_weather) {
                viewPager.setCurrentItem(2, false);
            } else if (checkedId == R.id.tab_ha) {
                viewPager.setCurrentItem(3, false);
            }
        });

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                RadioButton buttonToCheck = (RadioButton) tabGroup.getChildAt(position);
                if (!buttonToCheck.isChecked()) {
                    buttonToCheck.setChecked(true);
                }
            }
        });
    }

    private void checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            requestOverlayPermission();
        }
    }

    private void requestOverlayPermission() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
        overlayPermissionLauncher.launch(intent);
    }

    private final ActivityResultLauncher<Intent> overlayPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (!Settings.canDrawOverlays(this)) {
                        Toast.makeText(this, "Overlay-Berechtigung wurde nicht erteilt.", Toast.LENGTH_LONG).show();
                    }
                }
            });

    private void checkAndStartServices() {
        SharedPreferences prefs = getSharedPreferences(SettingsManager.PREFS_NAME, MODE_PRIVATE);
        if (prefs.getBoolean(SettingsManager.KEY_CLOCK_ENABLED, false)) {
            startService(new Intent(this, ClockService.class));
        }
        if (prefs.getBoolean(SettingsManager.KEY_DATE_ENABLED, false)) {
            startService(new Intent(this, DateService.class));
        }
        if (prefs.getBoolean(SettingsManager.KEY_WEATHER_ENABLED, false)) {
            startService(new Intent(this, WeatherService.class));
        }
        // KORRIGIERT: Service starten, wenn eine URL vorhanden ist.
        if (!TextUtils.isEmpty(prefs.getString(SettingsManager.KEY_HA_URL, ""))) {
            startService(new Intent(this, HomeAssistantService.class));
        }
    }

    public static int getGravityForSpinnerIndex(int index) {
        switch (index) {
            case 1: return Gravity.TOP | Gravity.START;
            case 2: return Gravity.BOTTOM | Gravity.END;
            case 3: return Gravity.BOTTOM | Gravity.START;
            default: return Gravity.TOP | Gravity.END;
        }
    }

    public static int getSpinnerIndexForGravity(int gravity) {
        if (gravity == (Gravity.TOP | Gravity.START)) return 1;
        if (gravity == (Gravity.BOTTOM | Gravity.END)) return 2;
        if (gravity == (Gravity.BOTTOM | Gravity.START)) return 3;
        return 0;
    }
}