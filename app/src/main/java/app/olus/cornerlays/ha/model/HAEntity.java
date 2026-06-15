package app.olus.cornerlays.ha.model;

import com.google.gson.JsonObject;

/* loaded from: classes3.dex */
public class HAEntity {
    private final JsonObject attributes;
    private final String entityId;
    private final String friendlyName;

    public HAEntity(String entityId, String friendlyName, JsonObject attributes) {
        this.entityId = entityId;
        this.friendlyName = friendlyName;
        this.attributes = attributes;
    }

    public String getEntityId() {
        return this.entityId;
    }

    public String getFriendlyName() {
        return this.friendlyName;
    }

    public JsonObject getAttributes() {
        return this.attributes;
    }
}
