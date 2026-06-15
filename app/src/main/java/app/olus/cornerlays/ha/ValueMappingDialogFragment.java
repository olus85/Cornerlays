package app.olus.cornerlays.ha;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import app.olus.cornerlays.R;
import app.olus.cornerlays.ha.ValueMappingAdapter;
import java.util.HashMap;
import java.util.Map;

/* loaded from: classes4.dex */
public class ValueMappingDialogFragment extends DialogFragment {
    private static final String ARG_MAPPINGS = "arg_mappings";
    private ValueMappingAdapter adapter;
    private Map<String, String> currentMappings;
    private EditText editMapped;
    private EditText editRaw;
    private ValueMappingListener listener;

    /* loaded from: classes4.dex */
    public interface ValueMappingListener {
        void onMappingsSaved(Map<String, String> map);
    }

    public static ValueMappingDialogFragment newInstance(Map<String, String> existingMappings) {
        ValueMappingDialogFragment fragment = new ValueMappingDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_MAPPINGS, new HashMap(existingMappings));
        fragment.setArguments(args);
        return fragment;
    }

    /* JADX WARN: Multi-variable type inference failed */
    @Override // androidx.fragment.app.DialogFragment, androidx.fragment.app.Fragment
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof ValueMappingListener) {
            this.listener = (ValueMappingListener) context;
            return;
        }
        throw new RuntimeException(context.toString() + " must implement ValueMappingListener");
    }

    @Override // androidx.fragment.app.DialogFragment
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_value_mapping, (ViewGroup) null);
        if (getArguments() != null && getArguments().containsKey(ARG_MAPPINGS)) {
            this.currentMappings = (HashMap) getArguments().getSerializable(ARG_MAPPINGS);
        } else {
            this.currentMappings = new HashMap();
        }
        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view_mappings);
        this.editRaw = (EditText) view.findViewById(R.id.edit_raw_value);
        this.editMapped = (EditText) view.findViewById(R.id.edit_mapped_value);
        Button btnAdd = (Button) view.findViewById(R.id.btn_add_mapping);
        Button btnSave = (Button) view.findViewById(R.id.btn_save_mappings);
        this.adapter = new ValueMappingAdapter(this.currentMappings, new ValueMappingAdapter.OnDeleteListener() { // from class: app.olus.cornerlays.ha.ValueMappingDialogFragment$$ExternalSyntheticLambda0
            @Override // app.olus.cornerlays.ha.ValueMappingAdapter.OnDeleteListener
            public final void onDelete(String str) {
                ValueMappingDialogFragment.this.lambda$onCreateDialog$0(str);
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(this.adapter);
        btnAdd.setOnClickListener(new View.OnClickListener() { // from class: app.olus.cornerlays.ha.ValueMappingDialogFragment$$ExternalSyntheticLambda1
            @Override // android.view.View.OnClickListener
            public final void onClick(View view2) {
                ValueMappingDialogFragment.this.lambda$onCreateDialog$1(view2);
            }
        });
        btnSave.setOnClickListener(new View.OnClickListener() { // from class: app.olus.cornerlays.ha.ValueMappingDialogFragment$$ExternalSyntheticLambda2
            @Override // android.view.View.OnClickListener
            public final void onClick(View view2) {
                ValueMappingDialogFragment.this.lambda$onCreateDialog$2(view2);
            }
        });
        builder.setView(view);
        return builder.create();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreateDialog$0(String key) {
        this.currentMappings.remove(key);
        this.adapter.updateData(this.currentMappings);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreateDialog$1(View v) {
        addMapping();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreateDialog$2(View v) {
        this.listener.onMappingsSaved(this.currentMappings);
        dismiss();
    }

    private void addMapping() {
        String raw = this.editRaw.getText().toString().trim();
        String mapped = this.editMapped.getText().toString().trim();
        if (TextUtils.isEmpty(raw) || TextUtils.isEmpty(mapped)) {
            Toast.makeText(getContext(), "Bitte beide Felder ausfüllen", 0).show();
            return;
        }
        this.currentMappings.put(raw, mapped);
        this.adapter.updateData(this.currentMappings);
        this.editRaw.setText("");
        this.editMapped.setText("");
        this.editRaw.requestFocus();
    }
}
