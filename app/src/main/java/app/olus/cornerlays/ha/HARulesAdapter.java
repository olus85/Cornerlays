package app.olus.cornerlays.ha;

import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import app.olus.cornerlays.R;
import app.olus.cornerlays.ha.model.HARule;
import java.util.List;

/* loaded from: classes4.dex */
public class HARulesAdapter extends RecyclerView.Adapter<HARulesAdapter.RuleViewHolder> {
    private final OnDeleteListener deleteListener;
    private final OnEditListener editListener;
    private final List<HARule> rules;

    /* loaded from: classes4.dex */
    public interface OnDeleteListener {
        void onDelete(int i);
    }

    /* loaded from: classes4.dex */
    public interface OnEditListener {
        void onEdit(int i, HARule hARule);
    }

    public HARulesAdapter(List<HARule> rules, OnDeleteListener deleteListener, OnEditListener editListener) {
        this.rules = rules;
        this.deleteListener = deleteListener;
        this.editListener = editListener;
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public RuleViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ha_rule, parent, false);
        return new RuleViewHolder(view);
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public void onBindViewHolder(final RuleViewHolder holder, int position) {
        final HARule rule = this.rules.get(position);
        holder.tvCondition.setText(rule.getCondition());
        holder.tvDisplay.setText(rule.getDisplayText());
        if (rule.getColor() != null) {
            holder.colorIndicator.setVisibility(0);
            holder.colorIndicator.getBackground().setColorFilter(rule.getColor().intValue(), PorterDuff.Mode.SRC_ATOP);
        } else {
            holder.colorIndicator.setVisibility(8);
        }
        holder.containerContent.setOnClickListener(new View.OnClickListener() { // from class: app.olus.cornerlays.ha.HARulesAdapter$$ExternalSyntheticLambda0
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                HARulesAdapter.this.lambda$onBindViewHolder$0(holder, rule, view);
            }
        });
        holder.btnDelete.setOnClickListener(new View.OnClickListener() { // from class: app.olus.cornerlays.ha.HARulesAdapter$$ExternalSyntheticLambda1
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                HARulesAdapter.this.lambda$onBindViewHolder$1(holder, view);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onBindViewHolder$0(RuleViewHolder holder, HARule rule, View v) {
        if (this.editListener != null) {
            this.editListener.onEdit(holder.getAdapterPosition(), rule);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onBindViewHolder$1(RuleViewHolder holder, View v) {
        if (this.deleteListener != null) {
            this.deleteListener.onDelete(holder.getAdapterPosition());
        }
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public int getItemCount() {
        return this.rules.size();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes4.dex */
    public static class RuleViewHolder extends RecyclerView.ViewHolder {
        ImageButton btnDelete;
        View colorIndicator;
        View containerContent;
        TextView tvCondition;
        TextView tvDisplay;

        public RuleViewHolder(View itemView) {
            super(itemView);
            this.containerContent = itemView.findViewById(R.id.container_rule_content);
            this.tvCondition = (TextView) itemView.findViewById(R.id.text_rule_condition);
            this.tvDisplay = (TextView) itemView.findViewById(R.id.text_rule_display);
            this.colorIndicator = itemView.findViewById(R.id.view_rule_color_indicator);
            this.btnDelete = (ImageButton) itemView.findViewById(R.id.btn_delete_rule);
        }
    }
}
