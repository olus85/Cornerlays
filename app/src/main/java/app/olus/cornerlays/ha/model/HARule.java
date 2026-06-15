package app.olus.cornerlays.ha.model;

import java.io.Serializable;

/* loaded from: classes3.dex */
public class HARule implements Serializable {
    private static final long serialVersionUID = 1;
    private Integer color;
    private String condition;
    private String displayText;

    public HARule() {
    }

    public HARule(String condition, String displayText, Integer color) {
        this.condition = condition;
        this.displayText = displayText;
        this.color = color;
    }

    public String getCondition() {
        return this.condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public String getDisplayText() {
        return this.displayText;
    }

    public void setDisplayText(String displayText) {
        this.displayText = displayText;
    }

    public Integer getColor() {
        return this.color;
    }

    public void setColor(Integer color) {
        this.color = color;
    }
}
