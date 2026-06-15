package app.olus.cornerlays.settings;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import app.olus.cornerlays.R;
import app.olus.cornerlays.SettingsManager;
import app.olus.cornerlays.ha.HAOverlaySettingsActivity;
import app.olus.cornerlays.ha.model.HAOverlay;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class HaSettingsFragment extends BaseSettingsFragment {

    private View btnHaOverlay1;
    private View btnHaOverlay2;
    private View btnHaOverlay3;
    private View btnHaOverlay4;

    private TextView textHaOverlay1;
    private TextView textHaOverlay2;
    private TextView textHaOverlay3;
    private TextView textHaOverlay4;

    private final Gson gson = new Gson();

    private final ActivityResultLauncher<Intent> haSettingsLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> loadHaOverlayButtonLabels()
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings_ha, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupListeners();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadHaOverlayButtonLabels();
    }

    private void initViews(View view) {
        btnHaOverlay1 = view.findViewById(R.id.btn_ha_overlay_1);
        btnHaOverlay2 = view.findViewById(R.id.btn_ha_overlay_2);
        btnHaOverlay3 = view.findViewById(R.id.btn_ha_overlay_3);
        btnHaOverlay4 = view.findViewById(R.id.btn_ha_overlay_4);

        textHaOverlay1 = view.findViewById(R.id.text_ha_overlay_1);
        textHaOverlay2 = view.findViewById(R.id.text_ha_overlay_2);
        textHaOverlay3 = view.findViewById(R.id.text_ha_overlay_3);
        textHaOverlay4 = view.findViewById(R.id.text_ha_overlay_4);
    }

    private void setupListeners() {
        btnHaOverlay1.setOnClickListener(v -> openHaOverlaySettings(0));
        btnHaOverlay2.setOnClickListener(v -> openHaOverlaySettings(1));
        btnHaOverlay3.setOnClickListener(v -> openHaOverlaySettings(2));
        btnHaOverlay4.setOnClickListener(v -> openHaOverlaySettings(3));
    }

    private void openHaOverlaySettings(int slotIndex) {
        Intent intent = new Intent(getActivity(), HAOverlaySettingsActivity.class);
        intent.putExtra(HAOverlaySettingsActivity.EXTRA_SLOT_INDEX, slotIndex);
        haSettingsLauncher.launch(intent);
    }

    private void loadHaOverlayButtonLabels() {
        String json = this.sharedPreferences.getString(SettingsManager.KEY_HA_OVERLAYS_JSON, "[]");
        Type listType = new TypeToken<ArrayList<HAOverlay>>() {}.getType();
        List<HAOverlay> overlays = this.gson.fromJson(json, listType);
        if (overlays == null) {
            overlays = new ArrayList<>();
        }
        TextView[] textViews = {textHaOverlay1, textHaOverlay2, textHaOverlay3, textHaOverlay4};
        for (int i = 0; i < textViews.length; i++) {
            if (i < overlays.size()) {
                HAOverlay overlay = overlays.get(i);
                if (overlay.isEnabled() && !TextUtils.isEmpty(overlay.getEntityId())) {
                    textViews[i].setText(overlay.getEntityId());
                } else {
                    textViews[i].setText(getString(R.string.ha_overlay_slot, Integer.valueOf(i + 1)) + " - " + getString(R.string.ha_overlay_not_configured));
                }
            } else {
                textViews[i].setText(getString(R.string.ha_overlay_slot, Integer.valueOf(i + 1)) + " - " + getString(R.string.ha_overlay_not_configured));
            }
        }
    }
}
