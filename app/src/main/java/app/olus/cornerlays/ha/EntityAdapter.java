package app.olus.cornerlays.ha;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import app.olus.cornerlays.R;
import app.olus.cornerlays.ha.EntityAdapter;
import app.olus.cornerlays.ha.model.HAEntity;
import java.util.List;

/* loaded from: classes4.dex */
public class EntityAdapter extends RecyclerView.Adapter<EntityAdapter.EntityViewHolder> {
    private List<HAEntity> entities;
    private final OnItemClickListener listener;

    /* loaded from: classes4.dex */
    public interface OnItemClickListener {
        void onItemClick(HAEntity hAEntity);
    }

    public EntityAdapter(List<HAEntity> entities, OnItemClickListener listener) {
        this.entities = entities;
        this.listener = listener;
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public EntityViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_entity, parent, false);
        return new EntityViewHolder(view);
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public void onBindViewHolder(EntityViewHolder holder, int position) {
        holder.bind(this.entities.get(position), this.listener);
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public int getItemCount() {
        return this.entities.size();
    }

    public void updateList(List<HAEntity> newList) {
        this.entities = newList;
        notifyDataSetChanged();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes4.dex */
    public static class EntityViewHolder extends RecyclerView.ViewHolder {
        private final TextView entityIdView;
        private final TextView friendlyNameView;

        public EntityViewHolder(View itemView) {
            super(itemView);
            this.entityIdView = (TextView) itemView.findViewById(R.id.text_view_entity_id);
            this.friendlyNameView = (TextView) itemView.findViewById(R.id.text_view_friendly_name);
        }

        public void bind(final HAEntity entity, final OnItemClickListener listener) {
            this.entityIdView.setText(entity.getEntityId());
            this.friendlyNameView.setText(entity.getFriendlyName());
            this.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (listener != null) {
                        listener.onItemClick(entity);
                    }
                }
            });
        }
    }
}
