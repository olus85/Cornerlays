package app.olus.cornerlays.ha;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import app.olus.cornerlays.R;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/* loaded from: classes4.dex */
public class ValueMappingAdapter extends RecyclerView.Adapter<ValueMappingAdapter.ViewHolder> {
    private final OnDeleteListener deleteListener;
    private final List<Map.Entry<String, String>> mappingList;

    /* loaded from: classes4.dex */
    public interface OnDeleteListener {
        void onDelete(String str);
    }

    public ValueMappingAdapter(Map<String, String> mappings, OnDeleteListener deleteListener) {
        this.mappingList = new ArrayList(mappings.entrySet());
        this.deleteListener = deleteListener;
    }

    public void updateData(Map<String, String> newMappings) {
        this.mappingList.clear();
        this.mappingList.addAll(newMappings.entrySet());
        notifyDataSetChanged();
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_value_mapping, parent, false);
        return new ViewHolder(view);
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public void onBindViewHolder(ViewHolder holder, int position) {
        final Map.Entry<String, String> entry = this.mappingList.get(position);
        holder.textRaw.setText(entry.getKey());
        holder.textMapped.setText(entry.getValue());
        holder.btnDelete.setOnClickListener(new View.OnClickListener() { // from class: app.olus.cornerlays.ha.ValueMappingAdapter$$ExternalSyntheticLambda0
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                ValueMappingAdapter.this.lambda$onBindViewHolder$0(entry, view);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onBindViewHolder$0(Map.Entry entry, View v) {
        if (this.deleteListener != null) {
            this.deleteListener.onDelete((String) entry.getKey());
        }
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public int getItemCount() {
        return this.mappingList.size();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes4.dex */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageButton btnDelete;
        TextView textMapped;
        TextView textRaw;

        public ViewHolder(View itemView) {
            super(itemView);
            this.textRaw = (TextView) itemView.findViewById(R.id.text_raw_value);
            this.textMapped = (TextView) itemView.findViewById(R.id.text_mapped_value);
            this.btnDelete = (ImageButton) itemView.findViewById(R.id.btn_delete_mapping);
        }
    }
}
