package app.olus.cornerlays.ha;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import app.olus.cornerlays.R;
import app.olus.cornerlays.SettingsManager;
import app.olus.cornerlays.ha.EntityAdapter;
import app.olus.cornerlays.ha.EntityPickerDialogFragment;
import app.olus.cornerlays.ha.model.HAEntity;
import com.google.android.material.chip.Chip;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/* loaded from: classes4.dex */
public class EntityPickerDialogFragment extends DialogFragment {
    private EntityAdapter adapter;
    private EntityPickerListener listener;
    private ProgressBar progressBar;
    private RecyclerView recyclerView;
    private EditText searchInput;
    private List<HAEntity> allEntities = new ArrayList();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    /* loaded from: classes4.dex */
    public interface EntityPickerListener {
        void onEntitySelected(String str, String str2);
    }

    /* JADX WARN: Multi-variable type inference failed */
    @Override // androidx.fragment.app.DialogFragment, androidx.fragment.app.Fragment
    public void onAttach(Context context) {
        super.onAttach(context);
        if (getTargetFragment() instanceof EntityPickerListener) {
            this.listener = (EntityPickerListener) getTargetFragment();
        } else {
            if (context instanceof EntityPickerListener) {
                this.listener = (EntityPickerListener) context;
                return;
            }
            throw new ClassCastException("Calling context must implement EntityPickerListener");
        }
    }

    @Override // androidx.fragment.app.DialogFragment
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_entity_picker, (ViewGroup) null);
        this.recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view_entities);
        this.progressBar = (ProgressBar) view.findViewById(R.id.progress_bar_entities);
        this.searchInput = (EditText) view.findViewById(R.id.edit_text_search_entity);
        this.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        this.adapter = new EntityAdapter(new ArrayList(), new EntityAdapter.OnItemClickListener() { // from class: app.olus.cornerlays.ha.EntityPickerDialogFragment$$ExternalSyntheticLambda1
            @Override // app.olus.cornerlays.ha.EntityAdapter.OnItemClickListener
            public final void onItemClick(HAEntity hAEntity) {
                EntityPickerDialogFragment.this.handleEntityClick(hAEntity);
            }
        });
        this.recyclerView.setAdapter(this.adapter);
        setupSearch();
        setupChips(view);
        fetchEntities();
        builder.setView(view).setTitle(R.string.entity_picker_title);
        return builder.create();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleEntityClick(HAEntity entity) {
        if (this.listener != null) {
            this.listener.onEntitySelected(entity.getEntityId(), null);
        }
        dismiss();
    }

    private void setupChips(View view) {
        setupChip(view, R.id.chip_light);
        setupChip(view, R.id.chip_switch);
        setupChip(view, R.id.chip_sensor);
        setupChip(view, R.id.chip_camera);
        setupChip(view, R.id.chip_media);
        setupChip(view, R.id.chip_binary);
        setupChip(view, R.id.chip_cover);
        setupChip(view, R.id.chip_wohnzimmer);
        setupChip(view, R.id.chip_kueche);
        setupChip(view, R.id.chip_schlafzimmer);
    }

    private void setupChip(View view, int chipId) {
        final Chip chip = (Chip) view.findViewById(chipId);
        if (chip != null) {
            chip.setOnClickListener(new View.OnClickListener() { // from class: app.olus.cornerlays.ha.EntityPickerDialogFragment$$ExternalSyntheticLambda0
                @Override // android.view.View.OnClickListener
                public final void onClick(View view2) {
                    EntityPickerDialogFragment.this.lambda$setupChip$0(chip, view2);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupChip$0(Chip chip, View v) {
        String chipText = chip.getText().toString();
        String currentText = this.searchInput.getText().toString();
        if (!currentText.contains(chipText)) {
            String newText = currentText.isEmpty() ? chipText : currentText + " " + chipText;
            this.searchInput.setText(newText);
            this.searchInput.setSelection(newText.length());
        }
    }

    private void setupSearch() {
        this.searchInput.addTextChangedListener(new TextWatcher() { // from class: app.olus.cornerlays.ha.EntityPickerDialogFragment.1
            @Override // android.text.TextWatcher
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override // android.text.TextWatcher
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                EntityPickerDialogFragment.this.filter(s.toString());
            }

            @Override // android.text.TextWatcher
            public void afterTextChanged(Editable s) {
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void filter(String text) {
        List<HAEntity> filteredList = new ArrayList<>();
        for (HAEntity entity : this.allEntities) {
            if (entity.getEntityId().toLowerCase().contains(text.toLowerCase()) || (entity.getFriendlyName() != null && entity.getFriendlyName().toLowerCase().contains(text.toLowerCase()))) {
                filteredList.add(entity);
            }
        }
        this.adapter.updateList(filteredList);
    }

    private void fetchEntities() {
        this.progressBar.setVisibility(0);
        SharedPreferences prefs = requireActivity().getSharedPreferences(SettingsManager.PREFS_NAME, 0);
        String url = prefs.getString(SettingsManager.KEY_HA_URL, "");
        String token = prefs.getString(SettingsManager.KEY_HA_TOKEN, "");
        if (TextUtils.isEmpty(url) || TextUtils.isEmpty(token)) {
            Toast.makeText(getContext(), "URL oder Token nicht gesetzt.", 0).show();
            this.progressBar.setVisibility(8);
        } else {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder().url(url + "/api/states").header("Authorization", "Bearer " + token).build();
            client.newCall(request).enqueue(new AnonymousClass2());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: app.olus.cornerlays.ha.EntityPickerDialogFragment$2, reason: invalid class name */
    /* loaded from: classes4.dex */
    public class AnonymousClass2 implements Callback {
        AnonymousClass2() {
        }

        @Override // okhttp3.Callback
        public void onFailure(Call call, final IOException e) {
            EntityPickerDialogFragment.this.mainHandler.post(new Runnable() { // from class: app.olus.cornerlays.ha.EntityPickerDialogFragment$2$$ExternalSyntheticLambda4
                @Override // java.lang.Runnable
                public final void run() {
                    EntityPickerDialogFragment.AnonymousClass2.this.lambda$onFailure$0(e);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onFailure$0(IOException e) {
            EntityPickerDialogFragment.this.progressBar.setVisibility(8);
            Toast.makeText(EntityPickerDialogFragment.this.getContext(), "Fehler: " + e.getMessage(), 0).show();
        }

        @Override // okhttp3.Callback
        public void onResponse(Call call, Response response) throws IOException {
            if (response.isSuccessful()) {
                try {
                    String responseBody = response.body().string();
                    JsonArray states = JsonParser.parseString(responseBody).getAsJsonArray();
                    EntityPickerDialogFragment.this.allEntities.clear();
                    Iterator<JsonElement> it = states.iterator();
                    while (it.hasNext()) {
                        JsonElement element = it.next();
                        JsonObject stateObj = element.getAsJsonObject();
                        String entityId = stateObj.get("entity_id").getAsString();
                        String friendlyName = "";
                        JsonObject attributes = new JsonObject();
                        if (stateObj.has("attributes")) {
                            attributes = stateObj.getAsJsonObject("attributes");
                            if (attributes.has("friendly_name")) {
                                friendlyName = attributes.get("friendly_name").getAsString();
                            }
                        }
                        EntityPickerDialogFragment.this.allEntities.add(new HAEntity(entityId, friendlyName, attributes));
                    }
                    Collections.sort(EntityPickerDialogFragment.this.allEntities, new Comparator() { // from class: app.olus.cornerlays.ha.EntityPickerDialogFragment$2$$ExternalSyntheticLambda0
                        @Override // java.util.Comparator
                        public final int compare(Object obj, Object obj2) {
                            int compareTo;
                            compareTo = ((HAEntity) obj).getEntityId().compareTo(((HAEntity) obj2).getEntityId());
                            return compareTo;
                        }
                    });
                    EntityPickerDialogFragment.this.mainHandler.post(new Runnable() { // from class: app.olus.cornerlays.ha.EntityPickerDialogFragment$2$$ExternalSyntheticLambda1
                        @Override // java.lang.Runnable
                        public final void run() {
                            EntityPickerDialogFragment.AnonymousClass2.this.lambda$onResponse$2();
                        }
                    });
                    return;
                } catch (Exception e) {
                    EntityPickerDialogFragment.this.mainHandler.post(new Runnable() { // from class: app.olus.cornerlays.ha.EntityPickerDialogFragment$2$$ExternalSyntheticLambda2
                        @Override // java.lang.Runnable
                        public final void run() {
                            EntityPickerDialogFragment.AnonymousClass2.this.lambda$onResponse$3(e);
                        }
                    });
                    return;
                }
            }
            EntityPickerDialogFragment.this.mainHandler.post(new Runnable() { // from class: app.olus.cornerlays.ha.EntityPickerDialogFragment$2$$ExternalSyntheticLambda3
                @Override // java.lang.Runnable
                public final void run() {
                    EntityPickerDialogFragment.AnonymousClass2.this.lambda$onResponse$4();
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onResponse$2() {
            EntityPickerDialogFragment.this.progressBar.setVisibility(8);
            EntityPickerDialogFragment.this.adapter.updateList(EntityPickerDialogFragment.this.allEntities);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onResponse$3(Exception e) {
            EntityPickerDialogFragment.this.progressBar.setVisibility(8);
            Toast.makeText(EntityPickerDialogFragment.this.getContext(), "Fehler beim Parsen: " + e.getMessage(), 0).show();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onResponse$4() {
            EntityPickerDialogFragment.this.progressBar.setVisibility(8);
            Toast.makeText(EntityPickerDialogFragment.this.getContext(), "Fehler beim Abrufen der Entitäten.", 0).show();
        }
    }
}
