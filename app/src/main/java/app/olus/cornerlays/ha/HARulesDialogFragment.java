package app.olus.cornerlays.ha;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import app.olus.cornerlays.ColorPickerDialogFragment;
import app.olus.cornerlays.R;
import app.olus.cornerlays.SettingsManager;
import app.olus.cornerlays.ha.HARulesAdapter;
import app.olus.cornerlays.ha.HARulesDialogFragment;
import app.olus.cornerlays.ha.model.HARule;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.eclipse.paho.client.mqttv3.MqttTopic;

/* loaded from: classes4.dex */
public class HARulesDialogFragment extends DialogFragment implements ColorPickerDialogFragment.ColorPickerListener {
    private static final String ARG_ATTRIBUTE = "arg_attribute";
    private static final String ARG_ENTITY_ID = "arg_entity_id";
    private static final String ARG_RULES_LIST = "arg_rules_list";
    private HARulesAdapter adapter;
    private Button btnAdd;
    private View btnColor;
    private List<HARule> currentRules;
    private EditText editCondition;
    private EditText editDisplay;
    private RulesDialogListener listener;
    private ProgressBar progressBar;
    private TextView textColorLabel;
    private View viewColorPreview;
    private Integer pendingColor = null;
    private int editingPosition = -1;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    /* loaded from: classes4.dex */
    public interface RulesDialogListener {
        void onRulesSaved(List<HARule> list);
    }

    public static HARulesDialogFragment newInstance(String entityId, String attributeName, ArrayList<HARule> rules) {
        HARulesDialogFragment fragment = new HARulesDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ENTITY_ID, entityId);
        args.putString(ARG_ATTRIBUTE, attributeName);
        if (rules == null) {
            args.putSerializable(ARG_RULES_LIST, new ArrayList());
        } else {
            args.putSerializable(ARG_RULES_LIST, rules);
        }
        fragment.setArguments(args);
        return fragment;
    }

    /* JADX WARN: Multi-variable type inference failed */
    @Override // androidx.fragment.app.DialogFragment, androidx.fragment.app.Fragment
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof RulesDialogListener) {
            this.listener = (RulesDialogListener) context;
            return;
        }
        throw new RuntimeException(context.toString() + " must implement RulesDialogListener");
    }

    @Override // androidx.fragment.app.DialogFragment
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_ha_rules, (ViewGroup) null);
        this.currentRules = new ArrayList();
        if (getArguments() != null && getArguments().containsKey(ARG_RULES_LIST)) {
            try {
                ArrayList<HARule> list = (ArrayList) getArguments().getSerializable(ARG_RULES_LIST);
                if (list != null) {
                    this.currentRules.addAll(list);
                }
            } catch (Exception e) {
                Log.e("HA_DEBUG", "Error deserializing rules", e);
            }
        }
        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view_rules);
        this.editCondition = (EditText) view.findViewById(R.id.edit_rule_condition);
        this.editDisplay = (EditText) view.findViewById(R.id.edit_rule_display);
        this.btnColor = view.findViewById(R.id.btn_rule_color);
        this.viewColorPreview = view.findViewById(R.id.view_color_preview_input);
        this.textColorLabel = (TextView) view.findViewById(R.id.text_no_color);
        this.progressBar = (ProgressBar) view.findViewById(R.id.progress_fetching_state);
        this.btnAdd = (Button) view.findViewById(R.id.btn_add_rule);
        Button btnSave = (Button) view.findViewById(R.id.btn_save_rules);
        this.adapter = new HARulesAdapter(this.currentRules, new HARulesAdapter.OnDeleteListener() { // from class: app.olus.cornerlays.ha.HARulesDialogFragment$$ExternalSyntheticLambda0
            @Override // app.olus.cornerlays.ha.HARulesAdapter.OnDeleteListener
            public final void onDelete(int i) {
                HARulesDialogFragment.this.lambda$onCreateDialog$0(i);
            }
        }, new HARulesAdapter.OnEditListener() { // from class: app.olus.cornerlays.ha.HARulesDialogFragment$$ExternalSyntheticLambda1
            @Override // app.olus.cornerlays.ha.HARulesAdapter.OnEditListener
            public final void onEdit(int i, HARule hARule) {
                HARulesDialogFragment.this.lambda$onCreateDialog$1(i, hARule);
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(this.adapter);
        this.btnColor.setOnClickListener(new View.OnClickListener() { // from class: app.olus.cornerlays.ha.HARulesDialogFragment$$ExternalSyntheticLambda2
            @Override // android.view.View.OnClickListener
            public final void onClick(View view2) {
                HARulesDialogFragment.this.lambda$onCreateDialog$2(view2);
            }
        });
        this.btnAdd.setOnClickListener(new View.OnClickListener() { // from class: app.olus.cornerlays.ha.HARulesDialogFragment$$ExternalSyntheticLambda3
            @Override // android.view.View.OnClickListener
            public final void onClick(View view2) {
                HARulesDialogFragment.this.lambda$onCreateDialog$3(view2);
            }
        });
        btnSave.setOnClickListener(new View.OnClickListener() { // from class: app.olus.cornerlays.ha.HARulesDialogFragment$$ExternalSyntheticLambda4
            @Override // android.view.View.OnClickListener
            public final void onClick(View view2) {
                HARulesDialogFragment.this.lambda$onCreateDialog$4(view2);
            }
        });
        builder.setView(view);
        fetchCurrentState();
        return builder.create();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreateDialog$0(int position) {
        this.currentRules.remove(position);
        this.adapter.notifyItemRemoved(position);
        if (position == this.editingPosition) {
            resetInput();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreateDialog$1(int position, HARule rule) {
        this.editingPosition = position;
        this.editCondition.setText(rule.getCondition());
        this.editDisplay.setText(rule.getDisplayText());
        if (rule.getColor() != null) {
            this.pendingColor = rule.getColor();
            this.viewColorPreview.getBackground().setColorFilter(this.pendingColor.intValue(), PorterDuff.Mode.SRC_ATOP);
            this.textColorLabel.setVisibility(4);
        } else {
            resetColorInput();
        }
        this.btnAdd.setText("✓");
        this.editCondition.requestFocus();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreateDialog$2(View v) {
        ColorPickerDialogFragment colorPicker = ColorPickerDialogFragment.newInstance("rule_color");
        colorPicker.show(requireActivity().getSupportFragmentManager(), "color_picker");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreateDialog$3(View v) {
        addOrUpdateRule();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreateDialog$4(View v) {
        String condition = this.editCondition.getText().toString().trim();
        if (!TextUtils.isEmpty(condition)) {
            addOrUpdateRule();
        }
        Log.d("HA_DEBUG", "Saving " + this.currentRules.size() + " rules from dialog.");
        this.listener.onRulesSaved(this.currentRules);
        dismiss();
    }

    private void fetchCurrentState() {
        if (getArguments() == null) {
            return;
        }
        String entityId = getArguments().getString(ARG_ENTITY_ID);
        String attribute = getArguments().getString(ARG_ATTRIBUTE);
        if (TextUtils.isEmpty(entityId)) {
            return;
        }
        this.progressBar.setVisibility(0);
        SharedPreferences prefs = requireContext().getSharedPreferences(SettingsManager.PREFS_NAME, 0);
        String url = prefs.getString(SettingsManager.KEY_HA_URL, "");
        String token = prefs.getString(SettingsManager.KEY_HA_TOKEN, "");
        if (TextUtils.isEmpty(url) || TextUtils.isEmpty(token)) {
            this.progressBar.setVisibility(8);
            return;
        }
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url + "/api/states/" + entityId).header("Authorization", "Bearer " + token).build();
        client.newCall(request).enqueue(new AnonymousClass1(attribute));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: app.olus.cornerlays.ha.HARulesDialogFragment$1, reason: invalid class name */
    /* loaded from: classes4.dex */
    public class AnonymousClass1 implements Callback {
        final /* synthetic */ String val$attribute;

        AnonymousClass1(String str) {
            this.val$attribute = str;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onFailure$0() {
            HARulesDialogFragment.this.progressBar.setVisibility(8);
        }

        @Override // okhttp3.Callback
        public void onFailure(Call call, IOException e) {
            HARulesDialogFragment.this.mainHandler.post(new Runnable() { // from class: app.olus.cornerlays.ha.HARulesDialogFragment$1$$ExternalSyntheticLambda3
                @Override // java.lang.Runnable
                public final void run() {
                    HARulesDialogFragment.AnonymousClass1.this.lambda$onFailure$0();
                }
            });
        }

        @Override // okhttp3.Callback
        public void onResponse(Call call, Response response) throws IOException {
            try {
                if (response.isSuccessful() && response.body() != null) {
                    String json = response.body().string();
                    JsonObject stateObj = JsonParser.parseString(json).getAsJsonObject();
                    String stateValue = "";
                    if (this.val$attribute != null && !TextUtils.isEmpty(this.val$attribute)) {
                        if ("last_changed".equals(this.val$attribute)) {
                            stateValue = stateObj.get("last_changed").getAsString();
                        } else if ("last_updated".equals(this.val$attribute)) {
                            stateValue = stateObj.get("last_updated").getAsString();
                        } else if (stateObj.has("attributes") && stateObj.getAsJsonObject("attributes").has(this.val$attribute)) {
                            stateValue = stateObj.getAsJsonObject("attributes").get(this.val$attribute).getAsString();
                        }
                    } else {
                        stateValue = stateObj.get("state").getAsString();
                    }
                    final String finalStateValue = stateValue;
                    HARulesDialogFragment.this.mainHandler.post(new Runnable() { // from class: app.olus.cornerlays.ha.HARulesDialogFragment$1$$ExternalSyntheticLambda0
                        @Override // java.lang.Runnable
                        public final void run() {
                            HARulesDialogFragment.AnonymousClass1.this.lambda$onResponse$1(finalStateValue);
                        }
                    });
                    return;
                }
                HARulesDialogFragment.this.mainHandler.post(new Runnable() { // from class: app.olus.cornerlays.ha.HARulesDialogFragment$1$$ExternalSyntheticLambda1
                    @Override // java.lang.Runnable
                    public final void run() {
                        HARulesDialogFragment.AnonymousClass1.this.lambda$onResponse$2();
                    }
                });
            } catch (Exception e) {
                HARulesDialogFragment.this.mainHandler.post(new Runnable() { // from class: app.olus.cornerlays.ha.HARulesDialogFragment$1$$ExternalSyntheticLambda2
                    @Override // java.lang.Runnable
                    public final void run() {
                        HARulesDialogFragment.AnonymousClass1.this.lambda$onResponse$3();
                    }
                });
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onResponse$1(String finalStateValue) {
            if (HARulesDialogFragment.this.editingPosition == -1 && TextUtils.isEmpty(HARulesDialogFragment.this.editCondition.getText())) {
                HARulesDialogFragment.this.editCondition.setText(finalStateValue);
                HARulesDialogFragment.this.editDisplay.requestFocus();
            }
            HARulesDialogFragment.this.progressBar.setVisibility(8);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onResponse$2() {
            HARulesDialogFragment.this.progressBar.setVisibility(8);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onResponse$3() {
            HARulesDialogFragment.this.progressBar.setVisibility(8);
        }
    }

    private void addOrUpdateRule() {
        String condition = this.editCondition.getText().toString().trim();
        String display = this.editDisplay.getText().toString().trim();
        if (TextUtils.isEmpty(condition)) {
            Toast.makeText(getContext(), "Bitte Bedingung ausfüllen", 0).show();
            return;
        }
        Log.d("HA_DEBUG", "Adding Rule: Cond='" + condition + "', Display='" + display + "', Color=" + this.pendingColor);
        HARule rule = new HARule(condition, display, this.pendingColor);
        if (this.editingPosition >= 0 && this.editingPosition < this.currentRules.size()) {
            this.currentRules.set(this.editingPosition, rule);
            this.adapter.notifyItemChanged(this.editingPosition);
        } else {
            this.currentRules.add(rule);
            this.adapter.notifyItemInserted(this.currentRules.size() - 1);
        }
        resetInput();
    }

    private void resetInput() {
        this.editingPosition = -1;
        this.btnAdd.setText(MqttTopic.SINGLE_LEVEL_WILDCARD);
        this.editDisplay.setText("");
        this.editCondition.setText("");
        resetColorInput();
        this.editCondition.requestFocus();
    }

    private void resetColorInput() {
        this.pendingColor = null;
        this.viewColorPreview.getBackground().setColorFilter(0, PorterDuff.Mode.SRC_ATOP);
        this.textColorLabel.setVisibility(0);
    }

    @Override // app.olus.cornerlays.ColorPickerDialogFragment.ColorPickerListener
    public void onColorSelected(int color, String tag) {
        Log.d("HA_DEBUG", "Dialog received color: " + color);
        this.pendingColor = Integer.valueOf(color);
        this.viewColorPreview.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
        this.textColorLabel.setVisibility(4);
    }
}
