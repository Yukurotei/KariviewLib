package it.yuruni.kariview.client.data;

import it.yuruni.kariview.client.data.actions.Action;

import java.util.List;

public class Keyframe {
    private int timestamp;
    private List<Action> actions;

    public int getTimestamp() {
        return timestamp;
    }

    public List<Action> getActions() {
        return actions;
    }
}
