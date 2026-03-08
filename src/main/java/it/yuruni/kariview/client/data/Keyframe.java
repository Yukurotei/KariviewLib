package it.yuruni.kariview.client.data;

import it.yuruni.kariview.client.data.actions.Action;

import java.util.ArrayList;
import java.util.List;

public class Keyframe {
    private long timestamp;
    private List<Action> actions;

    public Keyframe() {
        this.actions = new ArrayList<>();
    }

    public Keyframe(long timestamp) {
        this.timestamp = timestamp;
        this.actions = new ArrayList<>();
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public List<Action> getActions() {
        return actions;
    }

    public void setActions(List<Action> actions) {
        this.actions = actions;
    }
}
