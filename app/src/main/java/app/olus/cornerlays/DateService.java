package app.olus.cornerlays;

import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateService extends BaseOverlayService {
    @Override protected int getLayoutResId() { return R.layout.overlay_date; }
    @Override protected String getLogTag() { return "OnScreenHA_DateService"; }

    @Override
    protected void updateTextView(TextView tv) {
        String format = prefs.getString(SettingsManager.KEY_DATE_FORMAT, "dd.MM.yyyy");
        SimpleDateFormat dateFormat = new SimpleDateFormat(format, Locale.getDefault());
        tv.setText(dateFormat.format(new Date()));
    }

    @Override
    protected long getUpdateDelay() { return 60000; }

    // KORRIGIERT: Liefert die spezifischen Schlüssel für das Datum
    @Override protected String getGravityKey() { return SettingsManager.KEY_DATE_GRAVITY; }
    @Override protected String getOffsetXKey() { return SettingsManager.KEY_DATE_OFFSET_X; }
    @Override protected String getOffsetYKey() { return SettingsManager.KEY_DATE_OFFSET_Y; }
    @Override protected String getSizeKey() { return SettingsManager.KEY_DATE_SIZE; }
    @Override protected String getColorKey() { return SettingsManager.KEY_DATE_COLOR; }
    @Override protected String getShadowColorKey() { return SettingsManager.KEY_DATE_SHADOW_COLOR; }
}