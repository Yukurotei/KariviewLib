package it.yuruni.kariview.client.data;

import it.yuruni.kariview.client.data.actions.Action;

import java.util.List;

public class Keyframe {
    private long timestamp;
    private List<Action> actions;

    public long getTimestamp() {
        return timestamp;
    }

    public List<Action> getActions() {
        return actions;
    }
}
