package app.olus.cornerlays.ha.model;

import androidx.core.view.ViewCompat;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes3.dex */
public class HAOverlay {
    private String attributeName;
    private String displayName;
    private String entityId;
    private String triggerEntityId;
    private String triggerState;
    private String unit;
    private boolean enabled = true;
    private List<HARule> rules = new ArrayList();
    private String displayMode = "Normal";
    private int visibilityMode = 1;
    private int gravity = 8388661;
    private int offsetX = 20;
    private int offsetY = 20;
    private int size = 22;
    private int color = -1;
    private int shadowColor = ViewCompat.MEASURED_STATE_MASK;
    private int cameraUpdateIntervalMs = 2000;
    private String cameraStreamType = "Proxy";
    private float alpha = 1.0f;

    public float getAlpha() {
        return this.alpha;
    }

    public void setAlpha(float alpha) {
        this.alpha = alpha;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getEntityId() {
        return this.entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getAttributeName() {
        return this.attributeName;
    }

    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }

    public List<HARule> getRules() {
        if (this.rules == null) {
            this.rules = new ArrayList();
        }
        return this.rules;
    }

    public void setRules(List<HARule> rules) {
        this.rules = rules;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getUnit() {
        return this.unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getDisplayMode() {
        return this.displayMode;
    }

    public void setDisplayMode(String displayMode) {
        this.displayMode = displayMode;
    }

    public int getVisibilityMode() {
        return this.visibilityMode;
    }

    public void setVisibilityMode(int visibilityMode) {
        this.visibilityMode = visibilityMode;
    }

    public int getGravity() {
        return this.gravity;
    }

    public void setGravity(int gravity) {
        this.gravity = gravity;
    }

    public int getOffsetX() {
        return this.offsetX;
    }

    public void setOffsetX(int offsetX) {
        this.offsetX = offsetX;
    }

    public int getOffsetY() {
        return this.offsetY;
    }

    public void setOffsetY(int offsetY) {
        this.offsetY = offsetY;
    }

    public int getSize() {
        return this.size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getColor() {
        return this.color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public int getShadowColor() {
        return this.shadowColor;
    }

    public void setShadowColor(int shadowColor) {
        this.shadowColor = shadowColor;
    }

    public int getCameraUpdateIntervalMs() {
        return this.cameraUpdateIntervalMs;
    }

    public void setCameraUpdateIntervalMs(int cameraUpdateIntervalMs) {
        this.cameraUpdateIntervalMs = cameraUpdateIntervalMs;
    }

    public String getTriggerEntityId() {
        return this.triggerEntityId;
    }

    public void setTriggerEntityId(String triggerEntityId) {
        this.triggerEntityId = triggerEntityId;
    }

    public String getTriggerState() {
        return this.triggerState;
    }

    public void setTriggerState(String triggerState) {
        this.triggerState = triggerState;
    }

    public String getCameraStreamType() {
        return this.cameraStreamType != null ? this.cameraStreamType : "Proxy";
    }

    public void setCameraStreamType(String cameraStreamType) {
        this.cameraStreamType = cameraStreamType;
    }
}
