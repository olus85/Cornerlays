package app.olus.cornerlays;

import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ClockService extends BaseOverlayService {
    @Override protected int getLayoutResId() { return R.layout.overlay_clock; }
    @Override protected String getLogTag() { return "OnScreenHA_ClockService"; }

    @Override
    protected void updateTextView(TextView tv) {
        boolean is24h = prefs.getBoolean(SettingsManager.KEY_CLOCK_24H_FORMAT, true);
        boolean showSeconds = prefs.getBoolean(SettingsManager.KEY_CLOCK_SHOW_SECONDS, true);

        String pattern = is24h ? (showSeconds ? "HH:mm:ss" : "HH:mm") : (showSeconds ? "h:mm:ss a" : "h:mm a");
        SimpleDateFormat timeFormat = new SimpleDateFormat(pattern, Locale.getDefault());
        tv.setText(timeFormat.format(new Date()));
    }

    @Override
    protected long getUpdateDelay() {
        return prefs.getBoolean(SettingsManager.KEY_CLOCK_SHOW_SECONDS, true) ? 1000 : 60000;
    }

    // KORRIGIERT: Liefert die spezifischen Schlüssel für die Uhr
    @Override protected String getGravityKey() { return SettingsManager.KEY_CLOCK_GRAVITY; }
    @Override protected String getOffsetXKey() { return SettingsManager.KEY_CLOCK_OFFSET_X; }
    @Override protected String getOffsetYKey() { return SettingsManager.KEY_CLOCK_OFFSET_Y; }
    @Override protected String getSizeKey() { return SettingsManager.KEY_CLOCK_SIZE; }
    @Override protected String getColorKey() { return SettingsManager.KEY_CLOCK_COLOR; }
    @Override protected String getShadowColorKey() { return SettingsManager.KEY_CLOCK_SHADOW_COLOR; }
}